/**
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *     https://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.ap2sample.agent

import com.example.ap2sample.platform.PlatformLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "GeminiClient"
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

/**
 * Cross-platform Gemini REST API client using Ktor. Replaces the Android-only
 * com.google.ai.client.generativeai SDK.
 */
class GeminiClient(
        private val apiKey: String,
        private val modelName: String = "gemini-2.5-flash",
) {
        private val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
        }

        private val client = HttpClient { install(ContentNegotiation) { json(json) } }

        private val history = mutableListOf<GeminiContent>()

        /** Tool declarations for the shopping assistant. */
        val tools: List<GeminiTool> =
                listOf(
                        GeminiTool(
                                functionDeclarations =
                                        listOf(
                                                FunctionDeclaration(
                                                        name = "find_products",
                                                        description =
                                                                "Finds products based on a user's description.",
                                                        parameters =
                                                                FunctionParameters(
                                                                        type = "OBJECT",
                                                                        properties =
                                                                                mapOf(
                                                                                        "description" to
                                                                                                PropertySchema(
                                                                                                        type =
                                                                                                                "STRING",
                                                                                                        description =
                                                                                                                "The user's product search query."
                                                                                                )
                                                                                ),
                                                                        required =
                                                                                listOf(
                                                                                        "description"
                                                                                ),
                                                                ),
                                                ),
                                                FunctionDeclaration(
                                                        name = "select_product",
                                                        description =
                                                                "Selects a product from the list of options.",
                                                        parameters =
                                                                FunctionParameters(
                                                                        type = "OBJECT",
                                                                        properties =
                                                                                mapOf(
                                                                                        "itemName" to
                                                                                                PropertySchema(
                                                                                                        type =
                                                                                                                "STRING",
                                                                                                        description =
                                                                                                                "The item name of the product to select."
                                                                                                )
                                                                                ),
                                                                        required =
                                                                                listOf("itemName"),
                                                                ),
                                                ),
                                                FunctionDeclaration(
                                                        name = "get_shipping_address",
                                                        description =
                                                                "Gets the shipping address from a credential provider.",
                                                        parameters =
                                                                FunctionParameters(
                                                                        type = "OBJECT",
                                                                        properties =
                                                                                mapOf(
                                                                                        "email" to
                                                                                                PropertySchema(
                                                                                                        type =
                                                                                                                "STRING",
                                                                                                        description =
                                                                                                                "The user's email address."
                                                                                                )
                                                                                ),
                                                                        required = listOf("email"),
                                                                ),
                                                ),
                                                FunctionDeclaration(
                                                        name = "update_cart",
                                                        description =
                                                                "Updates the cart with the user's shipping address.",
                                                ),
                                                FunctionDeclaration(
                                                        name = "retrieve_dpc_options",
                                                        description =
                                                                "Handles the entire payment flow, from getting options to final validation.",
                                                ),
                                                FunctionDeclaration(
                                                        name = "initiate_payment_with_otp",
                                                        description =
                                                                "Retries payment with an OTP.",
                                                        parameters =
                                                                FunctionParameters(
                                                                        type = "OBJECT",
                                                                        properties =
                                                                                mapOf(
                                                                                        "otp" to
                                                                                                PropertySchema(
                                                                                                        type =
                                                                                                                "STRING",
                                                                                                        description =
                                                                                                                "The one-time password from the user."
                                                                                                )
                                                                                ),
                                                                        required = listOf("otp"),
                                                                ),
                                                ),
                                        )
                        )
                )

        /** Sends a message and returns the response. Handles the Gemini generateContent API. */
        suspend fun sendMessage(
                userMessage: String,
                systemInstruction: String,
        ): GeminiResponse {
                // Add user message to history
                history.add(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(text = userMessage)))
                )

                val request =
                        GenerateContentRequest(
                                contents = history.toList(),
                                tools = tools,
                                systemInstruction =
                                        GeminiContent(
                                                role = "system",
                                                parts = listOf(GeminiPart(text = systemInstruction))
                                        ),
                        )

                val response: JsonObject =
                        client
                                .post {
                                        url(
                                                "$BASE_URL/models/$modelName:generateContent?key=$apiKey"
                                        )
                                        contentType(ContentType.Application.Json)
                                        setBody(request)
                                }
                                .body()

                PlatformLogger.d(TAG, "Gemini response: $response")
                return parseResponse(response)
        }

        /** Sends a function response back to the model. */
        suspend fun sendFunctionResponse(
                functionName: String,
                responseData: JsonObject,
                systemInstruction: String,
        ): GeminiResponse {
                history.add(
                        GeminiContent(
                                role = "function",
                                parts =
                                        listOf(
                                                GeminiPart(
                                                        functionResponse =
                                                                FunctionResponse(
                                                                        name = functionName,
                                                                        response = responseData,
                                                                )
                                                )
                                        )
                        )
                )

                val request =
                        GenerateContentRequest(
                                contents = history.toList(),
                                tools = tools,
                                systemInstruction =
                                        GeminiContent(
                                                role = "system",
                                                parts = listOf(GeminiPart(text = systemInstruction))
                                        ),
                        )

                val response: JsonObject =
                        client
                                .post {
                                        url(
                                                "$BASE_URL/models/$modelName:generateContent?key=$apiKey"
                                        )
                                        contentType(ContentType.Application.Json)
                                        setBody(request)
                                }
                                .body()

                PlatformLogger.d(TAG, "Gemini function response: $response")
                return parseResponse(response)
        }

        private fun parseResponse(response: JsonObject): GeminiResponse {
                try {
                        val candidates = response["candidates"]?.jsonArray
                        if (candidates.isNullOrEmpty()) {
                                return GeminiResponse(text = "No response generated.")
                        }

                        val content = candidates[0].jsonObject["content"]?.jsonObject
                        val parts = content?.get("parts")?.jsonArray

                        if (parts.isNullOrEmpty()) {
                                return GeminiResponse(text = "Empty response.")
                        }

                        // Add model response to history
                        val role = content["role"]?.jsonPrimitive?.content ?: "model"
                        val historyParts = mutableListOf<GeminiPart>()

                        for (part in parts) {
                                val partObj = part.jsonObject

                                // Check for function call
                                val functionCall = partObj["functionCall"]?.jsonObject
                                if (functionCall != null) {
                                        val name =
                                                functionCall["name"]?.jsonPrimitive?.content ?: ""
                                        val args =
                                                functionCall["args"]?.jsonObject
                                                        ?: JsonObject(emptyMap())
                                        val argsMap =
                                                args.entries.associate { (k, v) ->
                                                        k to (v.jsonPrimitive.content)
                                                }

                                        historyParts.add(
                                                GeminiPart(
                                                        functionCall =
                                                                FunctionCall(
                                                                        name = name,
                                                                        args = args
                                                                )
                                                )
                                        )
                                        history.add(
                                                GeminiContent(
                                                        role = role,
                                                        parts = historyParts.toList()
                                                )
                                        )

                                        return GeminiResponse(
                                                functionCall =
                                                        GeminiFunctionCall(
                                                                name = name,
                                                                args = argsMap
                                                        )
                                        )
                                }

                                // Check for text
                                val text = partObj["text"]?.jsonPrimitive?.content
                                if (text != null) {
                                        historyParts.add(GeminiPart(text = text))
                                        history.add(
                                                GeminiContent(
                                                        role = role,
                                                        parts = historyParts.toList()
                                                )
                                        )
                                        return GeminiResponse(text = text)
                                }
                        }

                        return GeminiResponse(text = "Unable to parse response.")
                } catch (e: Exception) {
                        PlatformLogger.e(TAG, "Failed to parse Gemini response", e)
                        return GeminiResponse(text = "Error parsing response: ${e.message}")
                }
        }
}

// region Request/Response types

@Serializable
data class GenerateContentRequest(
        val contents: List<GeminiContent>,
        val tools: List<GeminiTool>? = null,
        @SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
)

@Serializable
data class GeminiContent(
        val role: String,
        val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
        val text: String? = null,
        val functionCall: FunctionCall? = null,
        val functionResponse: FunctionResponse? = null,
)

@Serializable
data class FunctionCall(
        val name: String,
        val args: JsonObject? = null,
)

@Serializable
data class FunctionResponse(
        val name: String,
        val response: JsonObject,
)

@Serializable
data class GeminiTool(
        @SerialName("function_declarations") val functionDeclarations: List<FunctionDeclaration>,
)

@Serializable
data class FunctionDeclaration(
        val name: String,
        val description: String,
        val parameters: FunctionParameters? = null,
)

@Serializable
data class FunctionParameters(
        val type: String = "OBJECT",
        val properties: Map<String, PropertySchema>? = null,
        val required: List<String>? = null,
)

@Serializable
data class PropertySchema(
        val type: String,
        val description: String,
)

/** Parsed response from Gemini API */
data class GeminiResponse(
        val text: String? = null,
        val functionCall: GeminiFunctionCall? = null,
)

data class GeminiFunctionCall(
        val name: String,
        val args: Map<String, Any?>,
)

// endregion
