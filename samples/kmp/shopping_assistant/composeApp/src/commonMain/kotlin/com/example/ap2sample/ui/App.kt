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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ap2sample.ui.theme.AppTheme

/**
 * Root composable for the app. Handles navigation between ChatScreen and SettingsScreen. Uses
 * simple state-based navigation to avoid KMP navigation library dependencies.
 */
@Composable
fun App(viewModel: ChatViewModel) {
    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var currentScreen by remember { mutableStateOf("chat") }
            val agentCardUrl by viewModel.agentCardUrl.collectAsState()
            val confirmationRequest by CredentialConfirmationController.request.collectAsState()

            when (currentScreen) {
                "settings" ->
                        SettingsScreen(
                                agentCardUrl = agentCardUrl,
                                dpcCheckoutMethod =
                                        viewModel.dpcCheckoutMethod.collectAsState().value,
                                onDoneClicked = { newUrl, newMethod ->
                                    viewModel.setAgentCardUrl(newUrl)
                                    viewModel.setDpcCheckoutMethod(newMethod)
                                    currentScreen = "chat"
                                },
                        )
                else ->
                        ChatScreen(
                                viewModel = viewModel,
                                onSettingsClicked = { currentScreen = "settings" },
                        )
            }

            confirmationRequest?.let { request ->
                CredentialConfirmationSheet(
                        request = request,
                        onDismissRequest = {
                            // The sheet manages resolving the deferred with false
                        }
                )
            }
        }
    }
}
