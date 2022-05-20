package fr.amanin.concurrency.examples.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration

/**
 * On of the key principle of the language is "code must be explicit".
 * Therefore, any function that can be paused without blocking runner thread must be marked "suspendable".
 */
suspend fun say(message: String, delay: Duration = 100.toDuration(MILLISECONDS), nTimes: Int = 5) {
    val startTime = System.nanoTime()
    var previousTime = startTime
    for (i in 0 until nTimes) {
        delay(delay)
        val currentTime  = System.nanoTime()
        val fromPrevious = currentTime - previousTime
        val fromStart    = currentTime - startTime
        previousTime = currentTime
        log("$message (i=$i) (from start: ${(fromStart * 1e-6).toLong()} ms - from previous: ${(fromPrevious * 1e-6).toLong()} ms")
    }
}

/**
 * Main program function is a blocking action.
 * To "bind" coroutine/suspendable world with it, it must be enclosed in a [runBlocking] code block.
 * It will make caller thread a runner for inside coroutine, and will cause it to wait until it is finished.
 */
fun main() = runBlocking {
    launch(Dispatchers.IO) { say("hello") }
    say("world")
}