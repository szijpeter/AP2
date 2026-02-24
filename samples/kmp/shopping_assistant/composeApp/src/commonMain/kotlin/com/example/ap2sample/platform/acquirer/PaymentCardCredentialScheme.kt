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

import at.asitplus.wallet.lib.data.ConstantIndex

/**
 * Custom Scheme Definition for the Payment Card test credential. Maps to com.emvco.payment_card
 * requested by the Digital Credentials API OpenID4VP flow.
 */
object PaymentCardCredentialScheme : ConstantIndex.CredentialScheme {
    const val CLAIM_CARD_NUMBER = "card_number"
    const val CLAIM_HOLDER_NAME = "holder_name"

    override val schemaUri: String = "https://example.com/schemas/PaymentCard.json"
    override val isoNamespace: String = "com.emvco.payment_card.1"
    override val isoDocType: String = "com.emvco.payment_card"

    override val claimNames: Collection<String> = listOf(CLAIM_CARD_NUMBER, CLAIM_HOLDER_NAME)

    override val supportedRepresentations: Collection<ConstantIndex.CredentialRepresentation> =
            listOf(ConstantIndex.CredentialRepresentation.ISO_MDOC)
}
