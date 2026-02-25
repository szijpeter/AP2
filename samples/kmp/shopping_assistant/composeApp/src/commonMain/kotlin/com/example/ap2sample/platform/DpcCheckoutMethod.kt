package com.example.ap2sample.platform

enum class DpcCheckoutMethod(val displayName: String, val description: String) {
    CREDENTIAL_MANAGER(
            "Android Credential Manager",
            "Uses OS CredentialManager to securely request credentials from registered wallet apps."
    ),
    APP_LINK(
            "Universal App Link",
            "Fires an openid4vp:// Intent/URL to launch a 3rd party wallet app directly."
    ),
    MOCK_KMP_FLOW(
            "Mock KMP Flow",
            "Uses the custom Shared Compose UI and an in-memory HolderAgent to simulate a checkout."
    )
}

expect val platformCheckoutMethods: List<DpcCheckoutMethod>
