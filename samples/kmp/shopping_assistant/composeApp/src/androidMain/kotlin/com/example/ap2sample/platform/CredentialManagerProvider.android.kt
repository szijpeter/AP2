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

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.exceptions.NoCredentialException
import com.example.ap2sample.platform.acquirer.SharedOpenId4VpDcApiAcquirer
import com.russhwolf.settings.Settings
import org.json.JSONArray
import org.json.JSONObject

/** Android implementation using the CredentialManager API for Digital Payment Credentials. */
actual class CredentialManagerProvider(private val activity: Activity) {

    private val credentialManager = CredentialManager.create(activity)
    private val settings = Settings()

    @OptIn(ExperimentalDigitalCredentialApi::class)
    actual suspend fun getDigitalCredential(requestJson: String): Result<String> {
        val useNative = settings.getBoolean("use_android_credential_manager", true)

        if (!useNative) {
            PlatformLogger.i(
                    "CredentialManager",
                    "Native Credential Manager bypassed via settings. Using Shared KMP Acquirer."
            )
            return SharedOpenId4VpDcApiAcquirer.acquire(requestJson)
        }

        return try {
            val jsonFromMerchant = JSONObject(requestJson)
            val protocol = jsonFromMerchant.getString("protocol")
            val data = jsonFromMerchant.getJSONObject("request")

            val request =
                    JSONObject().apply {
                        put("protocol", protocol)
                        put("data", data)
                    }
            val requests =
                    JSONObject().apply { put("requests", JSONArray().apply { put(request) }) }

            val reqStr = requests.toString()
            PlatformLogger.d("CredentialManager", "Invoking DPC with request: $reqStr")

            val digitalCredentialOption = GetDigitalCredentialOption(reqStr)
            val credential =
                    credentialManager.getCredential(
                            activity,
                            GetCredentialRequest(listOf(digitalCredentialOption)),
                    )
            val dpcCredential = credential.credential as DigitalCredential
            PlatformLogger.i("CredentialManager", "Credential Manager returned a token.")
            Result.success(dpcCredential.credentialJson)
        } catch (e: NoCredentialException) {
            PlatformLogger.e(
                    "CredentialManager",
                    "No credentials found in Native Credential Manager.",
                    e
            )
            Result.failure(Exception("No eligible credentials found", e))
        } catch (e: Exception) {
            PlatformLogger.e(
                    "CredentialManager",
                    "Credential Manager failed. Falling back to Shared KMP Acquirer.",
                    e
            )
            SharedOpenId4VpDcApiAcquirer.acquire(requestJson)
        }
    }
}
