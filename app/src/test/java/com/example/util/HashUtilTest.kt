package com.example.util

import org.junit.Test

class HashUtilTest {
    @Test fun rejectsMissingAndMalformedCredentials() = HashRegressionChecks.rejectsMissingAndMalformedCredentials()
    @Test fun preservesLegacyHashFormat() = HashRegressionChecks.preservesLegacyHashFormat()
    @Test fun roundTripsAndUsesRandomSalts() = HashRegressionChecks.roundTripsAndUsesRandomSalts()
}
