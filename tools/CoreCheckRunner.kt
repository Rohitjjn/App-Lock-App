import com.example.util.HashRegressionChecks

import com.example.util.CacheRegressionChecks

fun main() {
    HashRegressionChecks.rejectsMissingAndMalformedCredentials()
    HashRegressionChecks.preservesLegacyHashFormat()
    HashRegressionChecks.roundTripsAndUsesRandomSalts()
    CacheRegressionChecks.snapshotsAreDefensiveAndStable()
    CacheRegressionChecks.concurrentReadersSeeCompleteSnapshots()
    println("PASS: 5 hash/cache regression groups (production Kotlin/JCA, no Android mocks)")
}
