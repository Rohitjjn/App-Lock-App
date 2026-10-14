package com.example.util

/** One application-owned limiter shared by settings, lock entry, and recovery.
 * Uses the existing persisted keys; no credential or storage-format migration.
 */
class AuthAttemptLimiter(
    private val store: Store,
    private val now: () -> Long = System::currentTimeMillis
) {
    interface Store {
        fun attempts(): Int
        fun deadline(): Long
        fun write(attempts: Int, deadline: Long)
    }

    @Synchronized
    fun remainingSeconds(): Long = CooldownPolicy.remainingSeconds(store.deadline(), now())

    /** Zero means the attempt was blocked, otherwise the new failure count. */
    @Synchronized
    fun recordFailure(): Int {
        val time = now()
        if (CooldownPolicy.isActive(store.deadline(), time)) return 0
        val previous = store.attempts().coerceIn(0, 3)
        val count = (if (previous >= 3) 0 else previous) + 1
        store.write(count, if (count >= 3) time + 30_000L else 0L)
        return count
    }

    /** Also rechecks the deadline for delayed biometric/pad success callbacks. */
    @Synchronized
    fun acceptSuccess(): Boolean {
        if (remainingSeconds() > 0) return false
        store.write(0, 0L)
        return true
    }

    @Synchronized
    fun verify(credential: String, hash: String?): Boolean {
        if (remainingSeconds() > 0) return false
        if (HashUtil.verify(credential, hash)) return acceptSuccess()
        recordFailure()
        return false
    }
}
