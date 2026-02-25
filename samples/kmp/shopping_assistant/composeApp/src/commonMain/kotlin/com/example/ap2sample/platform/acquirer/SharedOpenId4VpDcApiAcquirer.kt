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

import at.asitplus.dcapi.request.DCAPIWalletRequest
import at.asitplus.openid.RequestParameters
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.InMemorySubjectCredentialStore
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.data.rfc3986.UniformResourceIdentifier
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import com.example.ap2sample.platform.PlatformLogger
import com.example.ap2sample.ui.CredentialConfirmationController
import com.example.ap2sample.ui.CredentialInfo
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Shared implementation of Digital Payment Credentials OpenID4VP resolution.
 *
 * Parses incoming AP2 JSON enveloped requests and orchestrates `a-sit-plus` Holder processing and
 * Wallet response preparation.
 */
object SharedOpenId4VpDcApiAcquirer : DigitalCredentialAcquirer {

        private val json = Json { ignoreUnknownKeys = true }
        private val settings = Settings()

        private val keyMaterial by lazy { EphemeralKeyWithSelfSignedCert() }
        private val holderStore by lazy { InMemorySubjectCredentialStore() }

        private val holderAgent by lazy {
                HolderAgent(keyMaterial = keyMaterial, subjectCredentialStore = holderStore)
        }

        private val issuerAgent by lazy {
                IssuerAgent(
                        keyMaterial = EphemeralKeyWithSelfSignedCert(),
                        identifier = UniformResourceIdentifier("https://merchant.ap2.example.com")
                )
        }

        private val wallet by lazy {
                OpenId4VpWallet(
                        engine = HttpClient().engine,
                        keyMaterial = keyMaterial,
                        holderAgent = holderAgent
                )
        }

        override suspend fun acquire(requestJson: String): Result<String> {
                return try {
                        PlatformLogger.i("SharedDPC", "Starting Shared DCAPI Processing...")

                        val useMock = settings.getBoolean("use_mocked_credentials", true)

                        // 1. Seed deterministic credential if missing and mock is enabled
                        if (useMock) {
                                PlatformLogger.i(
                                        "SharedDPC",
                                        "Mock credentials enabled. Seeding demo credential..."
                                )
                                DemoCredentialSeeder.seedIfNeeded(
                                        holderAgent,
                                        issuerAgent,
                                        keyMaterial
                                )
                        }

                        // 2. Unpack envelope {"protocol": "...", "request": {...}}
                        val jsonFromMerchant = json.parseToJsonElement(requestJson) as JsonObject
                        val protocol =
                                jsonFromMerchant["protocol"]?.jsonPrimitive?.content
                                        ?: "openid4vp-v1-unsigned"
                        val requestObject =
                                jsonFromMerchant["request"]?.jsonObject
                                        ?: throw IllegalArgumentException(
                                                "Missing 'request' object in DPC JSON"
                                        )

                        val requestString = requestObject.toString()
                        PlatformLogger.d("SharedDPC", "Extracted request string: $requestString")

                        // 3. Decode into VCK structure
                        val requestParameters =
                                joseCompliantSerializer.decodeFromString<RequestParameters>(
                                        requestString
                                )

                        // Compute the DC API ID for the stored credential so
                        // VCK's filterById matches correctly
                        val storedCreds = holderAgent.getCredentials()
                        val firstCredDcApiId = storedCreds?.firstOrNull()?.getDcApiId() ?: ""
                        PlatformLogger.d(
                                "SharedDPC",
                                "First credential DC API ID: $firstCredDcApiId"
                        )

                        val dcapiRequest =
                                DCAPIWalletRequest.OpenId4VpUnsigned(
                                        request = requestParameters,
                                        credentialId = firstCredDcApiId,
                                        callingPackageName = "com.example.ap2sample",
                                        callingOrigin = "ap2.sample.app"
                                )

                        // 4. Initialize Response Protocol
                        val preparationState =
                                wallet.startAuthorizationResponsePreparation(dcapiRequest)
                                        .getOrThrow()

                        // 5. Match credentials against the DCQL query
                        PlatformLogger.i("SharedDPC", "Matching credentials in Holder store...")
                        val matchesResult = wallet.getMatchingCredentials(preparationState)
                        matchesResult.getOrThrow()

                        // Extract merchant and amount for confirmation UI
                        // transaction_data is an array of base64url-encoded JSON strings
                        val transactionDataArray = requestObject["transaction_data"]?.jsonArray
                        val firstTransactionB64 =
                                transactionDataArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
                        val firstTransaction =
                                firstTransactionB64?.let {
                                        @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
                                        val decodedBytes =
                                                kotlin.io.encoding.Base64.UrlSafe.decode(it)
                                        val decoded = decodedBytes.decodeToString()
                                        json.parseToJsonElement(decoded).jsonObject
                                }
                        val payee =
                                firstTransaction?.get("merchant_name")?.jsonPrimitive?.contentOrNull
                                        ?: firstTransaction?.get("payee")
                                                ?.jsonPrimitive
                                                ?.contentOrNull
                                                ?: "Unknown Merchant"
                        val typeStr =
                                firstTransaction?.get("type")?.jsonPrimitive?.contentOrNull
                                        ?: "transaction"

                        val amountStr =
                                firstTransaction?.get("amount")?.jsonPrimitive?.contentOrNull
                                        ?: firstTransaction?.get("custom_amount")
                                                ?.jsonPrimitive
                                                ?.contentOrNull
                                                ?: "10.00"
                        val currencyStr =
                                firstTransaction?.get("custom_currency")
                                        ?.jsonPrimitive
                                        ?.contentOrNull
                                        ?: "USD"
                        val displayAmount =
                                amountStr.let {
                                        if (it.startsWith("US ") || it.startsWith("USD")) it
                                        else "$currencyStr $it"
                                }

                        // For now we use generic presentation unless we deeply parse VCK entries
                        val credentialInfo =
                                CredentialInfo(
                                        cardName =
                                                if (useMock) "Demo DemoCard" else "My Payment Card",
                                        cardNumberLast4 = "XX42"
                                )

                        PlatformLogger.i("SharedDPC", "Requesting user confirmation...")
                        val isConfirmed =
                                CredentialConfirmationController.requestConfirmation(
                                        merchantName = payee,
                                        amount = displayAmount,
                                        credentialInfo = credentialInfo
                                )

                        if (!isConfirmed) {
                                PlatformLogger.i(
                                        "SharedDPC",
                                        "User explicitly canceled the credential request."
                                )
                                return Result.failure(Exception("User cancelled the request"))
                        }

                        PlatformLogger.i(
                                "SharedDPC",
                                "User confirmed. Finalizing VP Authorization Response..."
                        )
                        val authResult =
                                wallet.finalizeAuthorizationResponse(
                                                preparationState = preparationState,
                                                credentialPresentation = null
                                        )
                                        .getOrThrow()

                        // 6. Format Return JSON
                        when (authResult) {
                                is OpenId4VpWallet.AuthenticationForward -> {
                                        val resultData = authResult.authenticationResponseResult
                                        PlatformLogger.i(
                                                "SharedDPC",
                                                "Auth response ready type DCAPI. Data class: ${resultData::class.simpleName}"
                                        )

                                        val dataJson =
                                                try {
                                                        vckJsonSerializer.encodeToJsonElement(
                                                                resultData
                                                        )
                                                } catch (e: Exception) {
                                                        PlatformLogger.w(
                                                                "SharedDPC",
                                                                "Manual serialization fallback for ${resultData::class.simpleName}: ${e.message}"
                                                        )
                                                        if (resultData::class.simpleName == "DcApi"
                                                        ) {
                                                                val dcApiStr = resultData.toString()
                                                                val vpTokenPrefix = "vpToken="
                                                                val psPrefix =
                                                                        "presentationSubmission="

                                                                val vpTokenStart =
                                                                        dcApiStr.indexOf(
                                                                                vpTokenPrefix
                                                                        )
                                                                val psStart =
                                                                        dcApiStr.indexOf(psPrefix)

                                                                if (vpTokenStart != -1 &&
                                                                                psStart != -1
                                                                ) {
                                                                        val vpToken =
                                                                                dcApiStr.substring(
                                                                                        vpTokenStart +
                                                                                                vpTokenPrefix
                                                                                                        .length,
                                                                                        psStart - 2
                                                                                ) // -2 for ", "
                                                                        val presentationSubmission =
                                                                                dcApiStr.substring(
                                                                                        psStart +
                                                                                                psPrefix.length,
                                                                                        dcApiStr.length -
                                                                                                1
                                                                                )

                                                                        buildJsonObject {
                                                                                put(
                                                                                        "vp_token",
                                                                                        vpToken
                                                                                )
                                                                                put(
                                                                                        "presentation_submission",
                                                                                        presentationSubmission
                                                                                )
                                                                        }
                                                                } else {
                                                                        throw IllegalStateException(
                                                                                "DcApi not serializable, couldn't parse: $dcApiStr",
                                                                                e
                                                                        )
                                                                }
                                                        } else {
                                                                throw e
                                                        }
                                                }

                                        val dict = buildJsonObject {
                                                put("protocol", protocol)
                                                put("data", dataJson)
                                        }
                                        Result.success(dict.toString())
                                }
                                else -> {
                                        throw IllegalStateException(
                                                "Expected DCAPI AuthenticationForward, got: ${authResult::class.simpleName}"
                                        )
                                }
                        }
                } catch (e: Exception) {
                        PlatformLogger.e(
                                "SharedDPC",
                                "Failed to acquire credentials via SharedEngine",
                                e
                        )
                        Result.failure(e)
                }
        }
}
