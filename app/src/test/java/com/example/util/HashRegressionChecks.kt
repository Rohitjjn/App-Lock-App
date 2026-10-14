package com.example.util

/** Shared by JUnit and the dependency-free host check runner. No Android mocks. */
object HashRegressionChecks {
    fun rejectsMissingAndMalformedCredentials() {
        val malformed = listOf(null, "", ":", "a:b:c", "!!!!:!!!!", "AA==:AA==", "x".repeat(129))
        malformed.forEach { check(!HashUtil.verify("1234", it)) { "Malformed hash accepted" } }
        val valid = HashUtil.hash("1234")
        check(!HashUtil.verify("1234", ":${valid.substringAfter(':')}"))
        check(!HashUtil.verify("1234", "${valid.substringBefore(':')}:"))
    }

    fun preservesLegacyHashFormat() {
        // Independently generated PBKDF2-HMAC-SHA256 vector: 10000 iterations, salt bytes 0..15.
        val legacy = "AAECAwQFBgcICQoLDA0ODw==:keoFm64DM6KWn8jM18d4Ud2kP1rblrruokamJM30iew="
        check(HashUtil.verify("1234", legacy))
        check(!HashUtil.verify("4321", legacy))
    }

    fun roundTripsAndUsesRandomSalts() {
        for (credential in listOf("1234", "0,1,4,7", "pässword 🔒", "a".repeat(1024))) {
            val first = HashUtil.hash(credential)
            val second = HashUtil.hash(credential)
            check(first != second)
            check(HashUtil.verify(credential, first))
            check(HashUtil.verify(credential, second))
            check(!HashUtil.verify(credential + "x", first))
        }
    }
}
