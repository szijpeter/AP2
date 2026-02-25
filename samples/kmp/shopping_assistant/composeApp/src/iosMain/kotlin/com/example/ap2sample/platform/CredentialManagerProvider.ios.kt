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

import com.example.ap2sample.platform.acquirer.SharedOpenId4VpDcApiAcquirer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** iOS placeholder for CredentialManagerProvider using A-SIT Plus VCK. */
actual class CredentialManagerProvider {
    actual suspend fun getDigitalCredential(requestJson: String): Result<String> {
        return try {
            val jsonFromMerchant = Json.parseToJsonElement(requestJson) as JsonObject
            val protocol = jsonFromMerchant["protocol"]?.jsonPrimitive?.contentOrNull

            PlatformLogger.i("CredentialManager", "iOS DPC flow triggered for protocol $protocol.")

            // Delegate acquisition to the shared a-sit-plus OpenID4VP/DCAPI implementation
            PlatformLogger.i(
                    "CredentialManager",
                    "Delegating to SharedOpenId4VpDcApiAcquirer for iOS."
            )

            SharedOpenId4VpDcApiAcquirer.acquire(requestJson)
        } catch (e: Exception) {
            PlatformLogger.e(
                    "CredentialManager",
                    "Failed to process OpenID4VP request on iOS via Shared Acquirer",
                    e
            )
            Result.failure(e)
        }
    }
}
