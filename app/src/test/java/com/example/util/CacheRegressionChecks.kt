package com.example.util

import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

object CacheRegressionChecks {
    fun snapshotsAreDefensiveAndStable() {
        val cache = LockedPackageCache()
        val input = mutableSetOf("com.example.protected")
        cache.replace(input)
        val previous = cache.snapshot()
        input.clear()
        check(cache.contains("com.example.protected"))
        try {
            (previous as MutableSet<String>).clear()
            error("Snapshot was mutable")
        } catch (_: UnsupportedOperationException) {
            // Expected: callers cannot mutate the protection list.
        }
        cache.replace(setOf("other"))
        check(previous == setOf("com.example.protected"))
        check(cache.snapshot() == setOf("other"))
        cache.replace(emptySet())
        check(cache.snapshot().isEmpty())
    }

    fun concurrentReadersSeeCompleteSnapshots() {
        val cache = LockedPackageCache()
        val first = (1..100).map { "first.$it" }.toSet()
        val second = (1..100).map { "second.$it" }.toSet()
        cache.replace(first)
        val failure = AtomicReference<Throwable?>()
        val writer = thread { repeat(10000) { cache.replace(if (it % 2 == 0) second else first) } }
        val readers = List(4) {
            thread {
                try {
                    repeat(20000) {
                        val snapshot = cache.snapshot()
                        check(snapshot == first || snapshot == second)
                    }
                } catch (error: Throwable) {
                    failure.set(error)
                }
            }
        }
        writer.join()
        readers.forEach { it.join() }
        failure.get()?.let { throw it }
    }
}
