package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult
import reactor.core.publisher.Sinks.EmitResult.*
import java.util.concurrent.ArrayBlockingQueue

private fun logTrial(signal: SignalType, result: EmitResult) : Boolean {
    when (result) {
        OK -> { /* Success */ }
        FAIL_CANCELLED, FAIL_TERMINATED -> log("$signal: Channel already shutdown")
        FAIL_ZERO_SUBSCRIBER, FAIL_OVERFLOW -> log("$signal: Signal lost: buffer capacity exceeded")
        FAIL_NON_SERIALIZED -> log("$signal: Signal lost: concurrency state issue: actions that should have happened sequentially/serialy happened in the same time (at least partially overlapping)")
    }
    return false
}

fun main() {
    // Closest representation of a buffering channel is a Sink using a fixed sized queue.
    val channel = Sinks.many().unicast().onBackpressureBuffer<Int>(ArrayBlockingQueue(2))
    channel.emitNext(1, ::logTrial)
    channel.emitNext(2, ::logTrial)
    // Channel overflow can be managed either by a callback, or by testing the emission result directly
    channel.emitNext(3, ::logTrial)
    val result = channel.tryEmitNext(4)
    when (result) {
        OK -> { /* Success */ }
        else -> log("Cannot emit element. Reason: $result")
    }

    channel.asFlux().doOnNext { println(it) }
        .take(2)
        .blockLast()

    channel.emitComplete(::logTrial)
}