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
import com.example.ap2sample.ap2.model.ChatMessage
import com.example.ap2sample.ap2.model.SenderRole
import com.example.ap2sample.platform.CredentialManagerProvider
import com.example.ap2sample.platform.PlatformLogger
import com.example.ap2sample.platform.currentTimeMillis
import com.example.ap2sample.platform.randomUuid
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ChatViewModel"
private const val AGENT_CARD_URL_KEY = "agent_card_url"

data class ChatUiState(
        val messages: List<ChatMessage> = emptyList(),
        val isLoading: Boolean = false,
        val statusText: String = "",
)

class ChatViewModel(
        apiKey: String,
        credentialManagerProvider: CredentialManagerProvider,
) : ViewModel() {

    private val repository = ChatRepository(apiKey, credentialManagerProvider)
    private val settings = Settings()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _agentCardUrl = MutableStateFlow("")
    val agentCardUrl = _agentCardUrl.asStateFlow()

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
                            _uiState.update { it.copy(messages = it.messages + errorMessage) }
                        }
                        .onSuccess {
                            PlatformLogger.i(TAG, "Repository initialization successful.")
                            val successMessage =
                                    ChatMessage(
                                            id = randomUuid(),
                                            text = "Successfully connected to the agent.",
                                            sender = SenderRole.GEMINI,
                                            timestamp = currentTimeMillis(),
                                    )
                            _uiState.update { it.copy(messages = it.messages + successMessage) }
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
                                            text = "Failed to connect to agent: ${error.message}",
                                            sender = SenderRole.GEMINI,
                                            timestamp = currentTimeMillis(),
                                    )
                            _uiState.update { it.copy(messages = it.messages + errorMessage) }
                        }
                        .onSuccess {
                            val successMessage =
                                    ChatMessage(
                                            id = randomUuid(),
                                            text = "Successfully connected to the agent.",
                                            sender = SenderRole.GEMINI,
                                            timestamp = currentTimeMillis(),
                                    )
                            _uiState.update { it.copy(messages = it.messages + successMessage) }
                        }
            }
        }
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
        _uiState.update { it.copy(messages = it.messages + userChatMessage, isLoading = true) }

        viewModelScope.launch {
            val result =
                    repository.getResponse(userMessage) { newStatus ->
                        _uiState.update { it.copy(statusText = newStatus) }
                    }

            result
                    .onSuccess { responseText ->
                        PlatformLogger.d(TAG, "Repository returned success: '$responseText'")
                        val geminiMessage =
                                ChatMessage(
                                        id = randomUuid(),
                                        text = responseText,
                                        sender = SenderRole.GEMINI,
                                        timestamp = currentTimeMillis(),
                                )
                        _uiState.update {
                            it.copy(messages = it.messages + geminiMessage, isLoading = false)
                        }
                    }
                    .onFailure {
                        PlatformLogger.e(TAG, "Repository returned failure", it)
                        val errorMessage =
                                ChatMessage(
                                        id = randomUuid(),
                                        text = "Sorry, an error occurred: ${it.message}",
                                        sender = SenderRole.GEMINI,
                                        timestamp = currentTimeMillis(),
                                )
                        _uiState.update {
                            it.copy(messages = it.messages + errorMessage, isLoading = false)
                        }
                    }
        }
    }
}
