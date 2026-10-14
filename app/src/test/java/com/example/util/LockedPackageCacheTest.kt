package com.example.util

import org.junit.Test

class LockedPackageCacheTest {
    @Test fun snapshotsAreDefensiveAndStable() = CacheRegressionChecks.snapshotsAreDefensiveAndStable()
    @Test fun concurrentReadersSeeCompleteSnapshots() = CacheRegressionChecks.concurrentReadersSeeCompleteSnapshots()
}
