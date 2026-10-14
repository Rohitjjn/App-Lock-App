package com.example.util

/** Stable numeric release tags only. Unknown/prerelease formats never trigger a downgrade. */
internal object ReleaseVersion {
    fun isNewer(latest: String, current: String): Boolean {
        fun parse(version: String): List<Int>? {
            val value = version.removePrefix("v")
            if (!value.matches(Regex("[0-9]+(\\.[0-9]+){0,3}"))) return null
            return value.split('.').map { it.toIntOrNull() ?: return null }
        }
        val left = parse(latest) ?: return false
        val right = parse(current) ?: return false
        for (index in 0 until maxOf(left.size, right.size)) {
            val comparison = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }
}
