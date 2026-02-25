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
package com.example.ap2sample.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo

/**
 * Reads the Gemini API key on iOS.
 *
 * Resolution order:
 * 1. `GEMINI_API_KEY` environment variable (useful for Xcode scheme → Run → Environment Variables).
 * 2. `GeminiApiKey` entry in the app's `Info.plist`.
 * 3. Falls back to an empty string (calls will fail with an auth error).
 */
actual fun getGeminiApiKey(): String {
    // 1. Environment variable (e.g. set in Xcode Scheme)
    val envKey = NSProcessInfo.processInfo.environment["GEMINI_API_KEY"] as? String
    if (!envKey.isNullOrBlank()) return envKey

    // 2. Info.plist entry
    val plistKey = NSBundle.mainBundle.objectForInfoDictionaryKey("GeminiApiKey") as? String
    if (!plistKey.isNullOrBlank()) return plistKey

    PlatformLogger.w(
            "ApiKeyProvider",
            "No Gemini API key found. Set GEMINI_API_KEY in Xcode scheme " +
                    "environment variables or add GeminiApiKey to Info.plist."
    )
    return ""
}
