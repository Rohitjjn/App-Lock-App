import com.example.util.HashRegressionChecks

import com.example.util.CacheRegressionChecks

import com.example.util.VersionRegressionChecks

import com.example.util.CooldownRegressionChecks

import com.example.util.LifecycleRegressionChecks

fun main() {
    HashRegressionChecks.rejectsMissingAndMalformedCredentials()
    HashRegressionChecks.preservesLegacyHashFormat()
    HashRegressionChecks.roundTripsAndUsesRandomSalts()
    CacheRegressionChecks.snapshotsAreDefensiveAndStable()
    CacheRegressionChecks.concurrentReadersSeeCompleteSnapshots()
    VersionRegressionChecks.comparesNumericVersions()
    VersionRegressionChecks.rejectsUnknownVersions()
    CooldownRegressionChecks.roundsUpUntilTheExactDeadline()
    LifecycleRegressionChecks.stableForegroundRechecks()
    LifecycleRegressionChecks.staleAuthenticationRejected()
    LifecycleRegressionChecks.sharedPersistentAttempts()
    println("PASS: 11 hash/cache/version/cooldown/lifecycle-policy regression groups (production Kotlin/JCA, no Android mocks)")
}
