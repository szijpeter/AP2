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

import com.example.ap2sample.ap2.model.CartMandate
import com.example.ap2sample.ap2.model.ContactAddress
import com.example.ap2sample.ap2.model.ToolContext
import com.example.ap2sample.platform.CredentialManagerProvider
import com.example.ap2sample.platform.PlatformLogger
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "ChatRepository"

class ChatRepository(
        private val apiKey: String,
        private val credentialManagerProvider: CredentialManagerProvider,
) {
    private val toolContext = ToolContext()
    private var shoppingTools: ShoppingTools? = null

    private val geminiClient by lazy { GeminiClient(apiKey) }

    private val rootAgentInstruction =
            """
        You are a friendly and helpful shopping assistant. Your goal is to make the user's shopping
        experience as smooth as possible.

        Here's how you'll guide the user through the process:

        **Part 1: Finding and Selecting the Perfect Item**
        1.  Start by asking the user what they're looking for. Be conversational and friendly.
        2.  Once you have a good description, use the `find_products` tool to search for matching items.
        3.  Present the search results to the user in a clear, easy-to-read format. For each item,
            show the name, price, and any other relevant details.
        4.  Ask the user which item they would like to purchase.
        5.  Once the user makes a choice, call the `select_product` tool with the `itemName` of their choice.

        **Part 2: Shipping**
        1.  After a product is selected, ask the user for their shipping address. They can either provide it manually or you can
            offer to fetch it from their account by calling the `get_shipping_address` tool.
        2.  If they choose to use their saved address, confirm the address with them before proceeding.
        3.  Once the shipping address is confirmed, use the `update_cart` tool to add the address to the order.
        4.  Display a final order summary, including the item, price, tax, shipping, and total, and ask if the
        user wants to finalize it or if they want to continue shopping, in which case the whole flow will repeat.

        **Part 3: Payment**
        1.  Once the user has finalized shopping and want to purchase, call the 'retrieve_dpc_options' tool to get an openId4VP JSON from the merchant.
        The same tool will get a response an openIdVp request from the merchant and then invoke credential manager API with that JSON.  This API
        displays a summary of what the user is about to buy and also displays user's available payment option on a
        separate system UI - the user will select one option and that will close the UI that will return a
        final JSON token. Once the payment token is retrieved, it is sent back to the merchant for validation from within
        the same 'retrieve_dpc_options' tool. When the validation is received from the merchant the whole flow succeeds still
        within the same tool and now finally the tool returns successfully.

        **Part 4: Finalizing the Flow**
        1.  Once the 'retrieve_dpc_options' returns successfully, merchant has confirmed the payment.
        2.  Once the payment is successful, display a formatted payment receipt for the user.
        6.  End the conversation by saying "I am done for now".
    """

    suspend fun initialize(url: String): Result<Unit> {
        PlatformLogger.d(TAG, "Initializing repository with agent URL: $url")
        ShoppingTools.initiateShoppingTools(url, credentialManagerProvider)
                .map {
                    shoppingTools = it
                    PlatformLogger.i(TAG, "Repository initialized shopping tools")
                    return Result.success(Unit)
                }
                .onFailure { PlatformLogger.e(TAG, "Repository initialization failed", it) }
        return Result.failure(Exception("Repository initialization failed."))
    }

    suspend fun getResponse(
            userMessage: String,
            onStatusUpdate: (String) -> Unit,
    ): Result<String> {
        onStatusUpdate("Thinking...")

        try {
            var response = geminiClient.sendMessage(userMessage, rootAgentInstruction)

            while (true) {
                val functionCall = response.functionCall
                if (functionCall != null) {
                    onStatusUpdate("Executing: ${functionCall.name}...")
                    PlatformLogger.d(
                            TAG,
                            "Executing tool: ${functionCall.name} with args: ${functionCall.args}"
                    )
                    val toolResponse = executeTool(functionCall.name, functionCall.args)
                    PlatformLogger.d(TAG, "Tool response: $toolResponse")

                    onStatusUpdate("Thinking...")
                    response =
                            geminiClient.sendFunctionResponse(
                                    functionCall.name,
                                    toolResponse,
                                    rootAgentInstruction,
                            )
                } else {
                    onStatusUpdate("") // Clear status
                    return Result.success(response.text ?: "Done.")
                }
            }
        } catch (e: Exception) {
            val stackTrace = e.stackTraceToString()
            PlatformLogger.e(TAG, "An error occurred in getResponse: ${e.message}\n$stackTrace")
            onStatusUpdate("An error occurred.")
            return Result.failure(e)
        }
    }

    private suspend fun executeTool(
            name: String,
            args: Map<String, Any?>,
    ): JsonObject {
        val tools = shoppingTools

        if (tools == null) {
            return buildJsonObject {
                put("status", "error")
                put(
                        "message",
                        "Not connected to the merchant_agent. Please make sure you " +
                                "have the right url, and re-connect from Settings"
                )
            }
        }

        return when (name) {
            "find_products" -> {
                val description = args["description"] as? String ?: ""
                val cartMandateList = tools.findProducts(description, toolContext)
                if (cartMandateList.isEmpty()) {
                    buildJsonObject {
                        put("status", "error")
                        put(
                                "response_text",
                                "Sorry, I couldn't find any products matching that description."
                        )
                    }
                } else {
                    toolContext.state.productOptions = cartMandateList
                    val productListString =
                            cartMandateList.joinToString(separator = "\n") {
                                "- ${it.contents.paymentRequest.details.displayItems[0].label} for ${it.contents.paymentRequest.details.total}"
                            }
                    buildJsonObject {
                        put("status", "success")
                        put("response_text", "I found a few options for you:\n$productListString")
                    }
                }
            }
            "select_product" -> {
                val itemName = args["itemName"] as? String ?: ""
                PlatformLogger.d(TAG, "Finding product: $itemName")
                val selectedProduct =
                        getItemFromCartMandate(itemName, toolContext.state.productOptions)
                if (selectedProduct == null) {
                    buildJsonObject {
                        put("status", "error")
                        put("response_text", "Could not find item $itemName")
                    }
                } else {
                    toolContext.state.cartMandate = selectedProduct
                    buildJsonObject {
                        put("status", "success")
                        put(
                                "response_text",
                                "Selected ${selectedProduct.contents.paymentRequest.details.displayItems[0].label}"
                        )
                    }
                }
            }
            "get_shipping_address" -> {
                val address = ContactAddress("456 Oak Ave", "Otherville", "NY", "54321")
                toolContext.state.shippingAddress = address
                buildJsonObject {
                    put("status", "success")
                    put("streetAddress", address.streetAddress)
                    put("city", address.city)
                    put("state", address.state)
                    put("zipCode", address.zipCode)
                }
            }
            "update_cart" -> {
                val cart = toolContext.state.cartMandate!!
                val address = toolContext.state.shippingAddress!!
                val cartMandate = tools.updateCart(cart.contents.id, address, toolContext)
                if (cartMandate == null) {
                    buildJsonObject {
                        put("status", "error")
                        put("response_text", "Could not update cart")
                    }
                } else {
                    toolContext.state.cartMandate = cartMandate
                    buildJsonObject { put("status", "success") }
                }
            }
            "retrieve_dpc_options" -> {
                val result = tools.retrieveDpcOptions(toolContext)
                handlePaymentResult(result)
            }
            else -> {
                PlatformLogger.e(TAG, "Unknown tool: $name")
                buildJsonObject {
                    put("status", "error")
                    put("message", "Unknown tool: $name")
                }
            }
        }
    }

    private fun getItemFromCartMandate(
            itemName: String,
            cartMandateList: List<CartMandate>?,
    ): CartMandate? {
        return cartMandateList?.find {
            it.contents.paymentRequest.details.displayItems[0].label == itemName
        }
    }

    private fun handlePaymentResult(result: PaymentResult): JsonObject {
        return when (result) {
            is PaymentResult.Success ->
                    buildJsonObject {
                        put("status", "success")
                        put("message", "Payment successful!")
                    }
            is PaymentResult.OtpRequired ->
                    buildJsonObject {
                        put("status", "otp_required")
                        put("message", result.message)
                    }
            is PaymentResult.Failure ->
                    buildJsonObject {
                        put("status", "error")
                        put("message", result.message)
                    }
        }
    }
}
