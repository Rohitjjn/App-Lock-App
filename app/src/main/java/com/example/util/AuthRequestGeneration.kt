package com.example.util

/** Main-thread-owned token: callbacks from a previous target/lifecycle must not authorize a new one. */
internal class AuthRequestGeneration {
    var current = 0L
        private set

    fun invalidate(): Long = ++current
    fun accepts(token: Long): Boolean = token == current
}
