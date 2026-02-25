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

/**
 * Reads the Gemini API key from the `GEMINI_API_KEY` environment variable.
 *
 * For Android, the key is baked into `BuildConfig` at compile time from `local.properties` and
 * passed to `ChatViewModel` directly from `MainActivity`. This function serves as a fallback for
 * shared code that doesn't have access to `BuildConfig`.
 */
actual fun getGeminiApiKey(): String {
    return System.getenv("GEMINI_API_KEY") ?: ""
}
