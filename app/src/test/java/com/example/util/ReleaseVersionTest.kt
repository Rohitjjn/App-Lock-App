package com.example.util

import org.junit.Test

class ReleaseVersionTest {
    @Test fun comparesNumericVersions() = VersionRegressionChecks.comparesNumericVersions()
    @Test fun rejectsUnknownVersions() = VersionRegressionChecks.rejectsUnknownVersions()
}
