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

import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.iso.IssuerSignedItem
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.signum.indispensable.CryptoPublicKey
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.ConstantIndex
import com.example.ap2sample.platform.PlatformLogger
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Responsible for deterministically seeding the test wallet with a demo Payment Card.
 *
 * Uses A-SIT plus IssuerAgent to issue an mDoc credential directly into the provided Holder engine,
 * bypassing remote issuance to ensure the sample runs out of the box.
 */
object DemoCredentialSeeder {

        /**
         * Seeds the Holder store with an `MsoMdoc` representation of 'com.emvco.payment_card' if
         * one doesn't already exist.
         */
        suspend fun seedIfNeeded(
                holderAgent: HolderAgent,
                issuerAgent: IssuerAgent,
                holderKeyMaterial: KeyMaterial
        ): Boolean {
                return try {
                        val existingCredentials = holderAgent.getCredentials()

                        val hasPaymentCard =
                                existingCredentials?.any {
                                        it is SubjectCredentialStore.StoreEntry.Iso &&
                                                it.scheme == PaymentCardCredentialScheme
                                } == true

                        if (hasPaymentCard) {
                                PlatformLogger.i(
                                        "DemoCredentialSeeder",
                                        "Test Payment Card already seeded."
                                )
                                return false
                        }

                        PlatformLogger.i(
                                "DemoCredentialSeeder",
                                "Seeding test Payment Card into Holder Store..."
                        )

                        val credentialToBeIssued =
                                getTestPaymentCardCredential(
                                                subjectPublicKey = holderKeyMaterial.publicKey,
                                                credentialScheme = PaymentCardCredentialScheme
                                        )
                                        .getOrThrow()

                        val issuedCredential =
                                issuerAgent.issueCredential(credentialToBeIssued).getOrThrow()

                        val storeInput = issuedCredential.toStoreCredentialInput()

                        holderAgent.storeCredential(storeInput).getOrThrow()

                        PlatformLogger.i(
                                "DemoCredentialSeeder",
                                "Successfully seeded test Payment Card."
                        )
                        true
                } catch (e: Exception) {
                        PlatformLogger.e(
                                "DemoCredentialSeeder",
                                "Failed to seed demo credential: ${e.message}",
                                e
                        )
                        false
                }
        }

        private fun getTestPaymentCardCredential(
                subjectPublicKey: CryptoPublicKey,
                credentialScheme: ConstantIndex.CredentialScheme,
        ): KmmResult<CredentialToBeIssued> = catching {
                val expiration = Clock.System.now() + 365.days
                val claims =
                        listOf(
                                ClaimToBeIssued(
                                        PaymentCardCredentialScheme.CLAIM_CARD_NUMBER,
                                        "1111222233334444"
                                ),
                                ClaimToBeIssued(
                                        PaymentCardCredentialScheme.CLAIM_HOLDER_NAME,
                                        "Test User"
                                ),
                        )

                CredentialToBeIssued.Iso(
                        issuerSignedItems =
                                claims.mapIndexed { index, claim ->
                                        IssuerSignedItem(
                                                digestId = index.toUInt(),
                                                random = Random.nextBytes(16),
                                                elementIdentifier = claim.name,
                                                elementValue = claim.value
                                        )
                                },
                        expiration = expiration,
                        scheme = credentialScheme,
                        subjectPublicKey = subjectPublicKey,
                        userInfo =
                                OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject"))
                                        .getOrThrow(),
                )
        }
}
