package com.example.util

object LifecycleRegressionChecks {
    fun stableForegroundRechecks() {
        val target = ForegroundTarget()
        check(target.update(null) == null)
        check(target.update("protected") == "protected")
        repeat(100) { check(target.update(null) == "protected") }
        check(target.update("home") == "home")
    }

    fun staleAuthenticationRejected() {
        val requests = AuthRequestGeneration()
        val old = requests.current
        requests.invalidate()
        check(!requests.accepts(old))
        check(requests.accepts(requests.current))
    }

    fun sharedPersistentAttempts() {
        var count = 0
        var end = 0L
        var now = 1_000L
        val store = object : AuthAttemptLimiter.Store {
            override fun attempts() = count
            override fun deadline() = end
            override fun write(attempts: Int, deadline: Long) { count = attempts; end = deadline }
        }
        val limiter = AuthAttemptLimiter(store) { now }
        repeat(3) { check(!limiter.verify("incorrect", null)) }
        check(limiter.remainingSeconds() == 30L)
        check(!limiter.acceptSuccess())
        check(AuthAttemptLimiter(store) { now }.remainingSeconds() == 30L)
        now = 30_999L
        check(limiter.remainingSeconds() == 1L)
        now = 31_000L
        check(limiter.recordFailure() == 1)
        check(limiter.acceptSuccess())
        check(count == 0 && end == 0L)
    }
}
