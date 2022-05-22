package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.scheduler.Schedulers
import java.time.Duration

/**
 * Note: I cannot find an equivalent to Go non-buffered channels. Reactive-streams work a little differently.
 * They use user requests to trigger a set of production action.
 * The closest APIs for channels are [FluxSink] and [reactor.core.publisher.Sinks.Many].
 * However, they're both behave more like buffered channels than unbuffered ones.
 *
 * In this example, we'll use [FluxSink]. It differs from Go channel example, because here, the sum processing won't
 * start until a consumer subscribe to the link data stream. Once a user connect to the channel, both sums will be
 * computed and submitted without further suspension/waiting.
 *
 * The example could have used [Sinks.Many] (see previous file version in Git repository). However, it makes it more
 * complex, because "hot streams" (ie. non-repeatable flows triggered upstream by data producer instead of consumer)
 * require a lot of additional control code to ensure good wealth and back-pressure control.
 */
fun sum(values: IntArray, from: Int, to: Int, channel: FluxSink<Int>) {
    var sum = 0
    for (i in from until to) sum += values[i]
    channel.next(sum)
    log("$sum emitted")
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

    val half = values.size / 2

    val channel = Flux.create<Int> { sink ->
        go("first half") { sum(values, 0, half, sink) }
        go("second half") { sum(values, half, values.size, sink) }
    }

    log("Tasks submitted !")

    val (x, y) = Mono.delay(Duration.ofMillis(100))
        .then(channel.take(2).collectList())
        .block() ?: throw IllegalStateException("Cannot take value from channel")

    println("$x  + $y = ${x + y}")
}