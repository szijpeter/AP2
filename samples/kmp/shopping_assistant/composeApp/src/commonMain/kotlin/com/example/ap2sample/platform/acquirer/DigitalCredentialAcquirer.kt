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
package com.example.ap2sample.platform.acquirer

/**
 * Shared interface for performing a Digital Payment Credential verification flow.
 *
 * This provides a consistent boundary for the `CredentialManagerProvider` to delegate to the best
 * available implementation based on the platform, abstracting away the specifics of Android's
 * `androidx.credentials` or an internal Kotlin Multiplatform OpenID4VP engine.
 */
interface DigitalCredentialAcquirer {

    /**
     * Executes the OpenID4VP/DCAPI credential acquisition.
     *
     * @param requestJson The JSON payload containing `protocol: "openid4vp-v1-unsigned"` and the
     * OpenID4VP/DCAPI `request`.
     * @return Result containing either the successful base64url encoded JSON response or an failure
     * exception.
     */
    suspend fun acquire(requestJson: String): Result<String>
}
