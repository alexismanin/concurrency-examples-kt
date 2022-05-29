package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Sinks
import java.util.concurrent.ArrayBlockingQueue

fun fibonacci(n: Int, output: Sinks.Many<Int>) {

    var next = 0 to 1
    for (i in 1..n) {
        val (x, y) = next
        output.emitNext(x, ::failFast)
        next = y to x + y
    }

    output.emitComplete(::failFast)
}

fun main() {
    val capacity = 10

    val transport = Sinks.many().unicast().onBackpressureBuffer<Int>(ArrayBlockingQueue(10))

    go("Compute Fibonacci") { fibonacci(capacity, transport) }

    // TODO: try to reproduce advanced consumption behavior like in coroutine example.
    // Consuming the sink using two different subscribers does not produce expected result.
    transport.asFlux()
        .doOnNext { println(it) }
        .blockLast()
}