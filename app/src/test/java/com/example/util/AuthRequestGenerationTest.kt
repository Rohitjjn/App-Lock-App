package com.example.util

import org.junit.Assert.*
import org.junit.Test

class AuthRequestGenerationTest {
    @Test fun replacedOrStoppedRequestCannotAuthorizeCurrentTarget() {
        val requests = AuthRequestGeneration()
        val oldTarget = requests.current
        val newTarget = requests.invalidate()
        assertFalse(requests.accepts(oldTarget))
        assertTrue(requests.accepts(newTarget))
        requests.invalidate() // stop/cancel
        assertFalse(requests.accepts(newTarget))
    }

    @Test fun repeatedIntentForSameTargetAlsoInvalidatesOldAuthentication() {
        val requests = AuthRequestGeneration()
        val first = requests.current
        repeat(3) { requests.invalidate() }
        assertFalse(requests.accepts(first))
    }
}
