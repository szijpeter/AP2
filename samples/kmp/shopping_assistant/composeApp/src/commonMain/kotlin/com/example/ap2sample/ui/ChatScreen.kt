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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ap2sample.ap2.model.ChatMessage
import com.example.ap2sample.ap2.model.SenderRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onSettingsClicked: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "App Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("A2A Chat Assistant", fontWeight = FontWeight.Bold)
                                    Text(
                                            "Demonstrating Conceptual Agent Routing",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        actions = {
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            IconButton(
                                    onClick = {
                                        viewModel.triggerDebugCheckout { uri ->
                                            uriHandler.openUri(uri)
                                        }
                                    }
                            ) {
                                Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "Debug Checkout"
                                )
                            }
                            IconButton(onClick = { onSettingsClicked() }) {
                                Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                )
            },
            bottomBar = {
                ChatInputFooter(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSend = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        isLoading = uiState.isLoading,
                        statusText = uiState.statusText,
                )
            },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(uiState.messages) { message -> ChatMessageItem(message) }
                if (uiState.isLoading) {
                    item { ThinkingIndicator() }
                }
            }

            // Auto-scroll to the latest message
            LaunchedEffect(uiState.messages.size, uiState.isLoading) {
                coroutineScope.launch {
                    listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment =
            if (message.sender == SenderRole.USER) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            }
    val backgroundColor =
            when (message.sender) {
                SenderRole.USER -> MaterialTheme.colorScheme.primaryContainer
                SenderRole.GEMINI -> MaterialTheme.colorScheme.secondaryContainer
                SenderRole.AGENT -> MaterialTheme.colorScheme.tertiaryContainer
                else -> Color.Transparent
            }
    val icon =
            when (message.sender) {
                SenderRole.USER -> Icons.Default.Person
                SenderRole.GEMINI -> Icons.Rounded.AutoAwesome
                SenderRole.AGENT -> Icons.Default.Build
                else -> null
            }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.widthIn(max = 300.dp),
        ) {
            if (message.sender != SenderRole.USER) {
                icon?.let {
                    Icon(
                            imageVector = it,
                            contentDescription = "Sender Icon",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                    modifier =
                            Modifier.background(backgroundColor, RoundedCornerShape(16.dp))
                                    .padding(12.dp)
            ) {
                if (message.agentName != null) {
                    Text(
                            text = message.agentName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Text(
                        text = message.text.replace("\\n", "\n"),
                        color =
                                when (message.sender) {
                                    SenderRole.USER -> MaterialTheme.colorScheme.onPrimaryContainer
                                    SenderRole.GEMINI ->
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    SenderRole.AGENT ->
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> Color.Transparent
                                },
                )
            }

            if (message.sender == SenderRole.USER) {
                icon?.let {
                    Icon(
                            imageVector = it,
                            contentDescription = "Sender Icon",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "alpha")
    val alpha by
            infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(durationMillis = 700, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse,
                            ),
                    label = "thinking",
            )

    Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp).alpha(alpha),
    ) {
        Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Thinking",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
                text = "Thinking...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ChatInputFooter(
        value: String,
        onValueChange: (String) -> Unit,
        onSend: () -> Unit,
        isLoading: Boolean,
        statusText: String,
) {
    Surface(shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = statusText.isNotBlank()) {
                Text(
                        text = statusText,
                        modifier =
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        enabled = !isLoading,
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                        onClick = onSend,
                        enabled = value.isNotBlank() && !isLoading,
                        colors =
                                IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                ),
                ) { Icon(imageVector = Icons.Default.Send, contentDescription = "Send Message") }
            }
        }
    }
}
