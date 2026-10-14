package com.example.util

import org.junit.Assert.*
import org.junit.Test

class ForegroundTargetTest {
    @Test fun noEventBeforeFirstResumeHasNoTarget() {
        assertNull(ForegroundTarget().update(null))
    }

    @Test fun stableTargetIsRecheckedWithoutAnotherResumeEvent() {
        val target = ForegroundTarget()
        assertEquals("locked.app", target.update("locked.app"))
        repeat(20) { assertEquals("locked.app", target.update(null)) }
        assertEquals("locked.app", target.update(""))
        assertEquals("home", target.update("home"))
        assertEquals("home", target.update(null))
    }

    @Test fun screenOnPollingSessionStartsFresh() {
        ForegroundTarget().update("old.app")
        assertNull(ForegroundTarget().update(null))
    }
}
