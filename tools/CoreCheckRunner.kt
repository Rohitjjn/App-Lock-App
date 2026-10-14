import com.example.util.HashRegressionChecks

import com.example.util.CacheRegressionChecks

import com.example.util.VersionRegressionChecks

fun main() {
    HashRegressionChecks.rejectsMissingAndMalformedCredentials()
    HashRegressionChecks.preservesLegacyHashFormat()
    HashRegressionChecks.roundTripsAndUsesRandomSalts()
    CacheRegressionChecks.snapshotsAreDefensiveAndStable()
    CacheRegressionChecks.concurrentReadersSeeCompleteSnapshots()
    VersionRegressionChecks.comparesNumericVersions()
    VersionRegressionChecks.rejectsUnknownVersions()
    println("PASS: 7 hash/cache/version regression groups (production Kotlin/JCA, no Android mocks)")
}
