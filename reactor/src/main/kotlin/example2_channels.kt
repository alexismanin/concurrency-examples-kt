package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult.OK
import reactor.core.scheduler.Schedulers
import java.time.Duration

// TODO: this does not match example 2. This is 'more' example 3, I think.

/**
 * Reactor equivalent of a buffered channel is a "sink".
 * In reactive stream terms, it is a "hot" flux (in short: non-repeatable stream, triggered by producer instead of consumer).
 */
fun sum(values: IntArray, from: Int, to: Int, channel: Sinks.Many<Int>) {
    var sum = 0
    for (i in from until to) sum += values[i]
    val result = channel.tryEmitNext(sum)
    when (result) {
        // Here, we could manage different emission failures. But there's no need in this simple example
        OK   -> log("$sum emitted")
        else -> throw IllegalStateException("Cannot push sum to the channel. Emission failure: $result")
    }
}

/*
 * Simulate go command: Create a standalone task (single signal stream) that will run user action
 */
private val goThread = Schedulers.newSingle("Go !", true)
fun go(title: String, action: () -> Unit) = Mono.fromRunnable<Void>(action)
    .subscribeOn(goThread)
    .subscribe(
    { log("should never reach here") },
    { err -> log("Error on $title. Reason: ${err.message}") },
    { log("\"$title\" task done") }
)

fun main() {
    val values = intArrayOf(7, 2, 8, -9, 4, 0)
    /*
     * There's a lot of Hot streams
     */
    val channel = Sinks.many().multicast().onBackpressureBuffer<Int>()
    val half = values.size / 2
    go("first half") { sum(values, 0, half, channel) }
    go("second half") { sum(values, half, values.size, channel) }

    log("Tasks submitted !")

    val (x, y) = Mono.delay(Duration.ofMillis(100))
        .then(
            channel.asFlux()
                .take(2)
                .collectList()
        )
        .block() ?: throw IllegalStateException("Cannot take value from channel")

    println("$x  + $y = ${x + y}")

    val complete = channel.tryEmitComplete()
    log {
        when (complete) {
            OK -> "Channel closed"
            else -> "Error while closing channel: $complete"
        }
    }
}