package com.example.util

object VersionRegressionChecks {
    fun comparesNumericVersions() {
        check(ReleaseVersion.isNewer("v2.10.0", "2.9.0"))
        check(ReleaseVersion.isNewer("3", "2.99.99"))
        check(!ReleaseVersion.isNewer("v1.0.0", "2.0.0"))
        check(!ReleaseVersion.isNewer("2.0", "v2.0.0"))
        check(!ReleaseVersion.isNewer("2.0.0", "2.0.0"))
    }

    fun rejectsUnknownVersions() {
        for (version in listOf("", "v", "2.1-beta", "2..1", "-1", "999999999999", "2.1.1.1.1")) {
            check(!ReleaseVersion.isNewer(version, "2.0.0"))
            check(!ReleaseVersion.isNewer("3.0.0", version))
        }
    }
}
