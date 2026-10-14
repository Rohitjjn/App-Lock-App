package com.example.util

internal object CooldownPolicy {
    fun isActive(endTimeMs: Long, nowMs: Long): Boolean = endTimeMs > nowMs

    fun remainingSeconds(endTimeMs: Long, nowMs: Long): Long {
        if (!isActive(endTimeMs, nowMs)) return 0
        val remaining = endTimeMs - nowMs
        return remaining / 1000 + if (remaining % 1000 == 0L) 0 else 1
    }
}
