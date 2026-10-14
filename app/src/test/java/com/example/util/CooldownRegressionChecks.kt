package com.example.util

object CooldownRegressionChecks {
    fun roundsUpUntilTheExactDeadline() {
        check(CooldownPolicy.remainingSeconds(30000, 0) == 30L)
        check(CooldownPolicy.remainingSeconds(30000, 29001) == 1L)
        check(CooldownPolicy.isActive(30000, 29999))
        check(CooldownPolicy.remainingSeconds(30000, 29999) == 1L)
        check(!CooldownPolicy.isActive(30000, 30000))
        check(CooldownPolicy.remainingSeconds(30000, 30000) == 0L)
        check(CooldownPolicy.remainingSeconds(30000, 40000) == 0L)
        check(CooldownPolicy.remainingSeconds(0, 1) == 0L)
    }
}
