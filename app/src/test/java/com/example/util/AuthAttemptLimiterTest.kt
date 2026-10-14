package com.example.util

import org.junit.Assert.*
import org.junit.Test

class AuthAttemptLimiterTest {
    private class MemoryStore(var count: Int = 0, var end: Long = 0) : AuthAttemptLimiter.Store {
        override fun attempts() = count
        override fun deadline() = end
        override fun write(attempts: Int, deadline: Long) { count = attempts; end = deadline }
    }

    @Test fun allEntryPointsShareFailuresAndRejectLateSuccess() {
        val store = MemoryStore()
        val limiter = AuthAttemptLimiter(store) { 1_000L }
        assertFalse(limiter.verify("wrong pin", null))
        assertFalse(limiter.verify("wrong pattern", "malformed"))
        assertEquals(3, limiter.recordFailure()) // recovery
        assertEquals(30L, limiter.remainingSeconds())
        assertFalse(limiter.acceptSuccess()) // late biometric
        assertEquals(0, limiter.recordFailure())
        assertEquals(31_000L, store.end) // blocked attempts don't extend the deadline
    }

    @Test fun persistedDeadlineSurvivesNewPolicyInstanceAndRoundsUp() {
        val store = MemoryStore(3, 31_000L)
        var now = 30_999L
        val limiter = AuthAttemptLimiter(store) { now }
        assertEquals(1L, limiter.remainingSeconds())
        assertFalse(limiter.acceptSuccess())
        now = 31_000L
        assertEquals(1, limiter.recordFailure())
        assertEquals(0L, store.end)
    }

    @Test fun successfulVerificationClearsFailuresButNotDuringCooldown() {
        val store = MemoryStore(2)
        var now = 10_000L
        val limiter = AuthAttemptLimiter(store) { now }
        val hash = HashUtil.hash("correct")
        assertTrue(limiter.verify("correct", hash))
        assertEquals(0, store.count)
        repeat(3) { limiter.recordFailure() }
        assertFalse(limiter.verify("correct", hash))
        now += 30_000L
        assertTrue(limiter.verify("correct", hash))
        assertEquals(0L, store.end)
    }

    @Test fun alternatingScreensCannotResetTheFailureCount() {
        val store = MemoryStore()
        assertEquals(1, AuthAttemptLimiter(store) { 0L }.recordFailure())
        assertEquals(2, AuthAttemptLimiter(store) { 0L }.recordFailure())
        assertEquals(3, AuthAttemptLimiter(store) { 0L }.recordFailure())
        assertFalse(AuthAttemptLimiter(store) { 1L }.acceptSuccess())
    }

    @Test fun concurrentFailuresOnApplicationOwnedLimiterStopAtThree() {
        val store = MemoryStore()
        val limiter = AuthAttemptLimiter(store) { 1_000L }
        val workers = List(8) { Thread { repeat(20) { limiter.recordFailure() } } }
        workers.forEach { it.start() }
        workers.forEach { it.join() }
        assertEquals(3, store.count)
        assertEquals(31_000L, store.end)
    }
}
