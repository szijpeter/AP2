package com.example.ap2sample.ui

import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

data class CredentialInfo(
        val cardName: String,
        val cardNumberLast4: String,
        val cardArtUrl: String? = null
)

data class ConfirmationRequest(
        val merchantName: String,
        val amount: String,
        val credentialInfo: CredentialInfo,
        val onResult: (Boolean) -> Unit
)

/**
 * Singleton controller that bridges the gap between the suspending credential acquisition flow and
 * the Compose UI that requests user confirmation.
 */
object CredentialConfirmationController {

    private val _request = MutableStateFlow<ConfirmationRequest?>(null)
    val request = _request.asStateFlow()

    /**
     * Suspends until the user confirms or cancels the request via the UI. The UI should observe
     * [request] and call the `onResult` callback when the user interacts.
     */
    suspend fun requestConfirmation(
            merchantName: String,
            amount: String,
            credentialInfo: CredentialInfo
    ): Boolean = suspendCancellableCoroutine { continuation ->
        _request.value =
                ConfirmationRequest(merchantName, amount, credentialInfo) { isConfirmed ->
                    _request.value = null
                    if (continuation.isActive) {
                        continuation.resume(isConfirmed)
                    }
                }

        continuation.invokeOnCancellation { _request.value = null }
    }
}
