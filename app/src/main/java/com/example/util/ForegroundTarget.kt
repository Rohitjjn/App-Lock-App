package com.example.util

/** Poll-session state. No new UsageEvents does not mean the foreground app disappeared. */
internal class ForegroundTarget {
    private var current: String? = null

    fun update(resumedPackage: String?): String? {
        if (!resumedPackage.isNullOrBlank()) current = resumedPackage
        return current
    }
}
