import com.example.util.HashRegressionChecks

import com.example.util.CacheRegressionChecks

import com.example.util.VersionRegressionChecks

import com.example.util.CooldownRegressionChecks

fun main() {
    HashRegressionChecks.rejectsMissingAndMalformedCredentials()
    HashRegressionChecks.preservesLegacyHashFormat()
    HashRegressionChecks.roundTripsAndUsesRandomSalts()
    CacheRegressionChecks.snapshotsAreDefensiveAndStable()
    CacheRegressionChecks.concurrentReadersSeeCompleteSnapshots()
    VersionRegressionChecks.comparesNumericVersions()
    VersionRegressionChecks.rejectsUnknownVersions()
    CooldownRegressionChecks.roundsUpUntilTheExactDeadline()
    println("PASS: 8 hash/cache/version/cooldown regression groups (production Kotlin/JCA, no Android mocks)")
}
