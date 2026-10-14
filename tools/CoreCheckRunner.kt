import com.example.util.HashRegressionChecks

fun main() {
    HashRegressionChecks.rejectsMissingAndMalformedCredentials()
    HashRegressionChecks.preservesLegacyHashFormat()
    HashRegressionChecks.roundTripsAndUsesRandomSalts()
    println("PASS: 3 hash regression groups (production Kotlin/JCA, no Android mocks)")
}
