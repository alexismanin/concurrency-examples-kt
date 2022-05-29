package fr.amanin.concurrency.examples.coroutines

import fr.amanin.concurrency.examples.common.log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Allow to associate and increment a counter to arbitrary keys.
 * Use Kotlin functional capabilities to avoid exposing mutex to the user and therefore,
 * avoid any mutex misuse.
 */
class SafeCounter() {
    private val mutex: Mutex = Mutex()
    private val counts: MutableMap<String, Long> = HashMap()

    suspend fun <R> use(key: String, action: (Long) -> R) : R = mutex.withLock {
        val value = counts.get(key) ?: throw NoSuchElementException("No value for key [$key]")
        action(value)
    }

    suspend fun inc(key: String) = mutex.withLock {
        counts.merge(key, 1, Math::addExact)
            ?: throw IllegalStateException("Merge function result should never be null")
    }
}

fun main() : Unit = runBlocking {
    val c = SafeCounter()

    val key = "some key"
    // Explicit scope that will suspend current coroutine until all children tasks are completed
    coroutineScope {
        for (i in 1..1000) {
            launch(Dispatchers.Default) {
                // Note: log is outside mutex scope, to it won't necessarily appear ordered.
                // But that's fine. We do not want it to interfere with workers competing for the mutex.
                val newValue = c.inc(key)
                if (newValue % 50L == 0L) {
                    log("Increment: $newValue")
                }
            }
        }
    }

    c.use(key) { log("Final value: $it") }
}