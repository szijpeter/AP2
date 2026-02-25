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
package com.example.ap2sample

import androidx.compose.ui.window.ComposeUIViewController
import com.example.ap2sample.platform.CredentialManagerProvider
import com.example.ap2sample.ui.App
import com.example.ap2sample.ui.ChatViewModel

/**
 * Entry point for the iOS app. Called from SwiftUI's ContentView.
 *
 * Note: The API key should be set via environment or configuration. For the sample, it's hardcoded
 * as empty — set it before running.
 */
// TODO: Replace this global ViewModel reference with proper DI / service-locator.
//  Currently, if SwiftUI recreates the ComposeUIViewController the old ViewModel leaks
//  and the new one is never wired up for deep link handling.
private var activeViewModel: ChatViewModel? = null

fun MainViewController() = ComposeUIViewController {
    val apiKey = "" // TODO: Set API key from iOS configuration
    val credentialManagerProvider = CredentialManagerProvider()
    val viewModel = ChatViewModel(apiKey, credentialManagerProvider)
    activeViewModel = viewModel
    App(viewModel)
}

fun handleIosDeepLink(url: String) {
    com.example.ap2sample.platform.PlatformLogger.d(
            "MainViewController",
            "Intercepted iOS App Link response: $url"
    )
    activeViewModel?.handleDeepLink(url)
}
