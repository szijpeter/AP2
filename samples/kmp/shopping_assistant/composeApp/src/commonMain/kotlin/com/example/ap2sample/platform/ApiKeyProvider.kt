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
 * Returns the Gemini API key from the platform's configuration.
 *
 * - **Android**: reads from `BuildConfig.GEMINI_API_KEY` (sourced from `local.properties`).
 * - **iOS**: reads the `GEMINI_API_KEY` environment variable, or falls back to a `GeminiApiKey`
 * entry in `Info.plist`.
 *
 * Set the key in `local.properties` at the project root:
 * ```
 * GEMINI_API_KEY=your_key_here
 * ```
 */
expect fun getGeminiApiKey(): String
