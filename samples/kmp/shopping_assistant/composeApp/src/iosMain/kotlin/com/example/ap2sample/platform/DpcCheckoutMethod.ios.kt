package com.example.ap2sample.platform

actual val platformCheckoutMethods: List<DpcCheckoutMethod> =
        listOf(DpcCheckoutMethod.APP_LINK, DpcCheckoutMethod.MOCK_KMP_FLOW)
