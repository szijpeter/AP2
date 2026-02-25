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
package com.example.ap2sample.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ap2sample.agent.ChatRepository
import com.example.ap2sample.ap2.dpc.constructDPCRequest
import com.example.ap2sample.ap2.model.Amount
import com.example.ap2sample.ap2.model.CartContents
import com.example.ap2sample.ap2.model.CartMandate
import com.example.ap2sample.ap2.model.ChatMessage
import com.example.ap2sample.ap2.model.DisplayItem
import com.example.ap2sample.ap2.model.PaymentDetails
import com.example.ap2sample.ap2.model.PaymentOptions
import com.example.ap2sample.ap2.model.PaymentRequestDetails
import com.example.ap2sample.ap2.model.SenderRole
import com.example.ap2sample.platform.CredentialManagerProvider
import com.example.ap2sample.platform.DpcCheckoutMethod
import com.example.ap2sample.platform.PlatformLogger
import com.example.ap2sample.platform.acquirer.SharedOpenId4VpDcApiAcquirer
import com.example.ap2sample.platform.currentTimeMillis
import com.example.ap2sample.platform.platformCheckoutMethods
import com.example.ap2sample.platform.randomUuid
import com.russhwolf.settings.Settings
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ChatViewModel"
private const val AGENT_CARD_URL_KEY = "agent_card_url"
private const val DPC_CHECKOUT_METHOD_KEY = "dpc_checkout_method"

data class ChatUiState(
        val messages: List<ChatMessage> = emptyList(),
        val isLoading: Boolean = false,
        val statusText: String = "",
)

class ChatViewModel(
        apiKey: String,
        private val credentialManagerProvider: CredentialManagerProvider,
) : ViewModel() {

        private val repository = ChatRepository(apiKey, credentialManagerProvider)
        private val settings = Settings()

        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState = _uiState.asStateFlow()

        private val _agentCardUrl = MutableStateFlow("")
        val agentCardUrl = _agentCardUrl.asStateFlow()

        private val _dpcCheckoutMethod = MutableStateFlow(platformCheckoutMethods.first())
        val dpcCheckoutMethod = _dpcCheckoutMethod.asStateFlow()

        init {
                PlatformLogger.i(TAG, "ChatViewModel initialized.")
                _uiState.update {
                        it.copy(
                                messages =
                                        listOf(
                                                ChatMessage(
                                                        id = randomUuid(),
                                                        text =
                                                                "Hello! I'm your shopping assistant. How can I help you today? Try asking 'I want to buy some shoes'.",
                                                        sender = SenderRole.GEMINI,
                                                        timestamp = currentTimeMillis(),
                                                )
                                        )
                        )
                }

                // Load saved URL and initialize
                viewModelScope.launch {
                        val url = settings.getStringOrNull(AGENT_CARD_URL_KEY) ?: ""
                        PlatformLogger.d(TAG, "Settings loaded URL: $url")
                        _agentCardUrl.value = url

                        val savedMethodName = settings.getStringOrNull(DPC_CHECKOUT_METHOD_KEY)
                        val savedMethod =
                                platformCheckoutMethods.find { it.name == savedMethodName }
                        _dpcCheckoutMethod.value = savedMethod ?: platformCheckoutMethods.first()

                        if (url.isNotBlank()) {
                                PlatformLogger.d(
                                        TAG,
                                        "ViewModel launching repository initialization from URL: $url"
                                )
                                repository
                                        .initialize(url)
                                        .onFailure { error ->
                                                PlatformLogger.w(
                                                        TAG,
                                                        "Repository initialization failed: ${error.message}"
                                                )
                                                val errorMessage =
                                                        ChatMessage(
                                                                id = randomUuid(),
                                                                text =
                                                                        "Failed to connect to agent. Please check the URL and your network connection.",
                                                                sender = SenderRole.GEMINI,
                                                                timestamp = currentTimeMillis(),
                                                        )
                                                _uiState.update {
                                                        it.copy(
                                                                messages =
                                                                        it.messages + errorMessage
                                                        )
                                                }
                                        }
                                        .onSuccess {
                                                PlatformLogger.i(
                                                        TAG,
                                                        "Repository initialization successful."
                                                )
                                                val successMessage =
                                                        ChatMessage(
                                                                id = randomUuid(),
                                                                text =
                                                                        "Successfully connected to the agent.",
                                                                sender = SenderRole.GEMINI,
                                                                timestamp = currentTimeMillis(),
                                                        )
                                                _uiState.update {
                                                        it.copy(
                                                                messages =
                                                                        it.messages + successMessage
                                                        )
                                                }
                                        }
                        } else {
                                PlatformLogger.d(TAG, "Agent card URL is blank.")
                                val noteMessage =
                                        ChatMessage(
                                                id = randomUuid(),
                                                text =
                                                        "Note: Agent card URL is not set. Please configure it in the settings.",
                                                sender = SenderRole.GEMINI,
                                                timestamp = currentTimeMillis(),
                                        )
                                _uiState.update { it.copy(messages = it.messages + noteMessage) }
                        }
                }
        }

        fun setAgentCardUrl(url: String) {
                PlatformLogger.d(TAG, "setAgentCardUrl called with url: $url")
                settings.putString(AGENT_CARD_URL_KEY, url)
                _agentCardUrl.value = url

                // Re-initialize with the new URL
                if (url.isNotBlank()) {
                        viewModelScope.launch {
                                repository
                                        .initialize(url)
                                        .onFailure { error ->
                                                val errorMessage =
                                                        ChatMessage(
                                                                id = randomUuid(),
                                                                text =
                                                                        "Failed to connect to agent: ${error.message}",
                                                                sender = SenderRole.GEMINI,
                                                                timestamp = currentTimeMillis(),
                                                        )
                                                _uiState.update {
                                                        it.copy(
                                                                messages =
                                                                        it.messages + errorMessage
                                                        )
                                                }
                                        }
                                        .onSuccess {
                                                val successMessage =
                                                        ChatMessage(
                                                                id = randomUuid(),
                                                                text =
                                                                        "Successfully connected to the agent.",
                                                                sender = SenderRole.GEMINI,
                                                                timestamp = currentTimeMillis(),
                                                        )
                                                _uiState.update {
                                                        it.copy(
                                                                messages =
                                                                        it.messages + successMessage
                                                        )
                                                }
                                        }
                        }
                }
        }

        fun setDpcCheckoutMethod(method: DpcCheckoutMethod) {
                settings.putString(DPC_CHECKOUT_METHOD_KEY, method.name)
                _dpcCheckoutMethod.value = method
        }

        fun sendMessage(userMessage: String) {
                PlatformLogger.d(TAG, "sendMessage called with message: $userMessage")
                if (userMessage.isBlank()) {
                        PlatformLogger.d(TAG, "User message is blank, ignoring.")
                        return
                }

                val userChatMessage =
                        ChatMessage(
                                id = randomUuid(),
                                text = userMessage,
                                sender = SenderRole.USER,
                                timestamp = currentTimeMillis(),
                        )
                _uiState.update {
                        it.copy(messages = it.messages + userChatMessage, isLoading = true)
                }

                viewModelScope.launch {
                        val result =
                                repository.getResponse(userMessage) { newStatus ->
                                        _uiState.update { it.copy(statusText = newStatus) }
                                }

                        result
                                .onSuccess { responseText ->
                                        PlatformLogger.d(
                                                TAG,
                                                "Repository returned success: '$responseText'"
                                        )
                                        val geminiMessage =
                                                ChatMessage(
                                                        id = randomUuid(),
                                                        text = responseText,
                                                        sender = SenderRole.GEMINI,
                                                        timestamp = currentTimeMillis(),
                                                )
                                        _uiState.update {
                                                it.copy(
                                                        messages = it.messages + geminiMessage,
                                                        isLoading = false
                                                )
                                        }
                                }
                                .onFailure {
                                        PlatformLogger.e(TAG, "Repository returned failure", it)
                                        val errorMessage =
                                                ChatMessage(
                                                        id = randomUuid(),
                                                        text =
                                                                "Sorry, an error occurred: ${it.message}",
                                                        sender = SenderRole.GEMINI,
                                                        timestamp = currentTimeMillis(),
                                                )
                                        _uiState.update {
                                                it.copy(
                                                        messages = it.messages + errorMessage,
                                                        isLoading = false
                                                )
                                        }
                                }
                }
        }
        fun triggerDebugCheckout(onLaunchUrl: (String) -> Unit) {
                PlatformLogger.d(TAG, "Triggering debug DCAPI checkout flow directly")
                _uiState.update {
                        it.copy(isLoading = true, statusText = "Launching Debug Checkout...")
                }

                viewModelScope.launch {
                        try {
                                val mockCart = createDebugCart()
                                val dpcRequestJson =
                                        constructDPCRequest(mockCart, DEBUG_MERCHANT_NAME)

                                when (_dpcCheckoutMethod.value) {
                                        DpcCheckoutMethod.APP_LINK -> {
                                                val encodedJson =
                                                        dpcRequestJson.encodeURLParameter()
                                                val uri = "openid4vp://?request=$encodedJson"
                                                PlatformLogger.d(TAG, "Launching App Link: $uri")
                                                try {
                                                        onLaunchUrl(uri)
                                                        _uiState.update {
                                                                it.copy(
                                                                        isLoading = true,
                                                                        statusText =
                                                                                "Waiting for Wallet Response..."
                                                                )
                                                        }
                                                } catch (e: Exception) {
                                                        throw Exception(
                                                                "No wallet app found to handle the 'openid4vp://' App Link. " +
                                                                        "Install a wallet app, or switch to Credential Manager / Mock KMP Flow in Settings."
                                                        )
                                                }
                                        }
                                        else -> {
                                                val tokenResult =
                                                        if (_dpcCheckoutMethod.value ==
                                                                        DpcCheckoutMethod
                                                                                .MOCK_KMP_FLOW
                                                        ) {
                                                                SharedOpenId4VpDcApiAcquirer
                                                                        .acquire(dpcRequestJson)
                                                        } else {
                                                                credentialManagerProvider
                                                                        .getDigitalCredential(
                                                                                dpcRequestJson
                                                                        )
                                                        }

                                                tokenResult
                                                        .onSuccess { token ->
                                                                appendMessage(
                                                                        "Debug Checkout Success! Token received:\n\n${token.take(50)}..."
                                                                )
                                                        }
                                                        .onFailure { e ->
                                                                appendMessage(
                                                                        "Debug Checkout Cancelled/Failed: ${e.message}"
                                                                )
                                                        }
                                        }
                                }
                        } catch (e: Exception) {
                                appendMessage("Error launching debug checkout: ${e.message}")
                        }
                }
        }

        /**
         * Receives the deep link callback from a wallet app after an App Link checkout.
         *
         * Currently this only acknowledges receipt by updating the UI status. A production
         * implementation should:
         * 1. Parse the `response` query parameter from the [url].
         * 2. Validate the VP token / presentation submission.
         * 3. Resume the checkout flow or display the result to the user.
         */
        fun handleDeepLink(url: String) {
                PlatformLogger.i(TAG, "ChatViewModel received deep link response: $url")
                // TODO(peterSzij): Parse the VP token from the response URL and complete the
                //  checkout flow end-to-end.
                appendMessage(
                        "Wallet response received via App Link!\nRaw URL: ${url.take(120)}..."
                )
        }

        /** Appends a GEMINI-role message and clears the loading state. */
        private fun appendMessage(text: String) {
                val msg =
                        ChatMessage(
                                id = randomUuid(),
                                text = text,
                                sender = SenderRole.GEMINI,
                                timestamp = currentTimeMillis()
                        )
                _uiState.update {
                        it.copy(messages = it.messages + msg, isLoading = false, statusText = "")
                }
        }
}

private const val DEBUG_MERCHANT_NAME = "Test Demo Store"

/** Creates a hardcoded [CartMandate] for testing the DPC checkout flow. */
private fun createDebugCart(): CartMandate {
        val amount = Amount("USD", 42.99)
        val displayItem = DisplayItem(label = "Debug Magic Shoes", amount = amount)
        val paymentDetails =
                PaymentDetails(
                        id = "debug-order-1",
                        displayItems = listOf(displayItem),
                        total = displayItem
                )
        val paymentOptions =
                PaymentOptions(
                        requestPayerName = true,
                        requestPayerEmail = true,
                        requestPayerPhone = true,
                        requestShipping = true
                )
        val requestDetails =
                PaymentRequestDetails(
                        methodData = emptyList(),
                        details = paymentDetails,
                        options = paymentOptions
                )
        val cartContents =
                CartContents(
                        id = "debug-cart-1",
                        userCartConfirmationRequired = false,
                        paymentRequest = requestDetails,
                        cartExpiry = "2099-12-31T23:59:59Z",
                        merchantName = DEBUG_MERCHANT_NAME
                )
        return CartMandate(contents = cartContents, merchantAuthorization = null)
}
