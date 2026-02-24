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

import com.example.ap2sample.ap2.a2a.A2aClient
import com.example.ap2sample.ap2.a2a.A2aMessageBuilder
import com.example.ap2sample.ap2.dpc.constructDPCRequest
import com.example.ap2sample.ap2.model.ArtifactResult
import com.example.ap2sample.ap2.model.CartMandate
import com.example.ap2sample.ap2.model.ContactAddress
import com.example.ap2sample.ap2.model.FullCartMandateWrapper
import com.example.ap2sample.ap2.model.IntentMandate
import com.example.ap2sample.ap2.model.JsonRpcResponse
import com.example.ap2sample.ap2.model.ToolContext
import com.example.ap2sample.platform.CredentialManagerProvider
import com.example.ap2sample.platform.PlatformLogger
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

private const val TAG = "ShoppingTools"

class ShoppingTools(
        private val merchantAgent: A2aClient,
        private val credentialManagerProvider: CredentialManagerProvider,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        suspend fun initiateShoppingTools(
                merchantAgentUrl: String,
                credentialManagerProvider: CredentialManagerProvider,
        ): Result<ShoppingTools> {
            PlatformLogger.d(TAG, "Fetching agent card from: $merchantAgentUrl")
            try {
                val client = A2aClient.setUpClient("merchant_agent", merchantAgentUrl)
                PlatformLogger.i(TAG, "SUCCESS: Agent Card for '${client.agentCard?.name}' loaded.")

                val merchantAgent =
                        A2aClient(
                                name = "merchant_agent",
                                baseUrl = merchantAgentUrl,
                                agentCard = client.agentCard,
                        )

                val tools = ShoppingTools(merchantAgent, credentialManagerProvider)
                return Result.success(tools)
            } catch (e: Exception) {
                PlatformLogger.e(TAG, "FAILED: Could not fetch or parse agent card.", e)
                return Result.failure(e)
            }
        }
    }

    suspend fun findProducts(
            naturalLanguageDescription: String,
            toolContext: ToolContext,
    ): List<CartMandate> {
        PlatformLogger.d(TAG, "Searching for products matching: '$naturalLanguageDescription'")
        val intentMandate = createIntentMandate(naturalLanguageDescription)

        toolContext.state.shoppingContextId = "123"
        toolContext.state.intentMandate = intentMandate

        val message =
                A2aMessageBuilder()
                        .addText("Find products that match the user's IntentMandate.")
                        .addData(key = "ap2.mandates.IntentMandate", data = intentMandate)
                        .setContextId("123")
                        .build()
        val responseJson = merchantAgent.sendMessage(message)

        try {
            val rpcResponse =
                    json.decodeFromJsonElement<JsonRpcResponse<ArtifactResult>>(responseJson)
            val listCartMandate = mutableListOf<CartMandate>()
            rpcResponse.result.artifacts.mapNotNull { artifact ->
                val part =
                        artifact.parts.firstOrNull { it.kind == "data" } ?: return@mapNotNull null
                val wrapper = json.decodeFromJsonElement<FullCartMandateWrapper>(part.data)
                listCartMandate.add(wrapper.cartMandate)
            }
            toolContext.state.productOptions = listCartMandate
            return listCartMandate
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to parse product search results", e)
        }
        return emptyList()
    }

    suspend fun updateCart(
            cartId: String,
            shippingAddress: ContactAddress,
            toolContext: ToolContext,
    ): CartMandate? {
        PlatformLogger.d(TAG, "Updating cart '$cartId' with new shipping address")

        if (toolContext.state.shoppingContextId == null) {
            return null
        }

        val message =
                A2aMessageBuilder()
                        .addText("Update the cart with the user's shipping address.")
                        .setContextId(contextId = toolContext.state.shoppingContextId!!)
                        .addData("cart_id", cartId)
                        .addData("shipping_address", shippingAddress)
                        .build()
        val responseJson = merchantAgent.sendMessage(message)

        return try {
            val rpcResponse =
                    json.decodeFromJsonElement<JsonRpcResponse<ArtifactResult>>(responseJson)
            val artifact = rpcResponse.result.artifacts.first()
            val part = artifact.parts.first { it.kind == "data" }
            val wrapper = json.decodeFromJsonElement<FullCartMandateWrapper>(part.data)

            toolContext.state.cartMandate = wrapper.cartMandate
            toolContext.state.shippingAddress = shippingAddress
            PlatformLogger.i(TAG, "Cart updated successfully")
            wrapper.cartMandate
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to parse updated cart", e)
            null
        }
    }

    suspend fun retrieveDpcOptions(toolContext: ToolContext): PaymentResult {
        PlatformLogger.d(TAG, "Starting DPC payment flow")

        val cart =
                toolContext.state.cartMandate
                        ?: return PaymentResult.Failure("No cart selected for payment.")

        // 1. Construct the OpenId4VP request
        val dpcRequestJson =
                constructDPCRequest(
                        cartMandate = cart,
                        merchantName = cart.contents.merchantName,
                )

        // 2. Invoke Credential Manager API and get a token
        val tokenResult = credentialManagerProvider.getDigitalCredential(dpcRequestJson)
        val token =
                tokenResult.getOrElse { e ->
                    return PaymentResult.Failure(e.message ?: "User cancelled the payment.")
                }
        toolContext.state.signedPaymentMandate = token

        // 3. Send the token back to the merchant for validation
        PlatformLogger.i(TAG, "Sending DPC response to merchant for validation")
        val sendDpcResponseMessage =
                A2aMessageBuilder()
                        .addText("Validate the Digital Payment Credentials (DPC) response")
                        .addData(key = "dpc_response", data = token)
                        .build()
        val finalResponseJson = merchantAgent.sendMessage(sendDpcResponseMessage)

        return try {
            val rpcResponse =
                    json.decodeFromJsonElement<JsonRpcResponse<ArtifactResult>>(finalResponseJson)
            val artifact = rpcResponse.result.artifacts.first()
            val part = artifact.parts.first { it.kind == "data" }
            val paymentStatus = part.data.jsonObject["payment_status"]!!.toString()
            PlatformLogger.i(TAG, "Payment validation status: $paymentStatus")
            if (paymentStatus == "\"SUCCESS\"") PaymentResult.Success
            else PaymentResult.Failure("Payment validation failed.")
        } catch (e: Exception) {
            PlatformLogger.e(TAG, "Failed to parse final payment validation response", e)
            PaymentResult.Failure("An error occurred during final payment validation.")
        }
    }

    private fun createIntentMandate(naturalLanguageDescription: String): IntentMandate {
        val expiry = (Clock.System.now() + kotlin.time.Duration.parse("1d")).toString()
        return IntentMandate(
                userPromptRequired = true,
                naturalLanguageDescription = naturalLanguageDescription,
                intentExpiry = expiry,
        )
    }
}
