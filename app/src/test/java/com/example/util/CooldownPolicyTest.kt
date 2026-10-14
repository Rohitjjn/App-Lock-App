package com.example.util

import org.junit.Test

class CooldownPolicyTest {
    @Test fun roundsUpUntilTheExactDeadline() = CooldownRegressionChecks.roundsUpUntilTheExactDeadline()
}
