package com.example.util

import java.util.Collections

/** Publishes a complete immutable snapshot; readers never see a half-refreshed lock list. */
internal class LockedPackageCache {
    @Volatile
    private var packages: Set<String> = emptySet()

    fun replace(packageNames: Set<String>) {
        packages = Collections.unmodifiableSet(HashSet(packageNames))
    }

    fun snapshot(): Set<String> = packages

    fun contains(packageName: String): Boolean = packages.contains(packageName)
}
