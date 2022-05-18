package fr.amanin.concurrency.examples.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration

suspend fun say(message: String, delay: Duration = 100.toDuration(MILLISECONDS), nTimes: Int = 5) {
    for (i in 0 until nTimes) {
        delay(delay)
        println("$message (i=$i ; thread=${Thread.currentThread().name})")
    }
}

fun main() {
    runBlocking {
        launch(Dispatchers.IO) { say("hello") }
        say("world")
    }
}