package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Reactive streams model an asynchronous/non-blocking sequence of signals/events/messages.
 * As so, they're a sort of pipeline assembly and execution engine.
 * Any task must be expressed through the stream API.
 * In this case, we express that this function "assemble" a set of task that produces a stream of text elements.
 */
fun say(message: String, delay : Duration = Duration.ofMillis(100), nTimes : Int = 5) : Flux<String> {
    // We create a finite cold stream (that will be launched on demand, and repeatable, unlike java Stream).
    return Flux.range(0, nTimes)
        // Each element will be of the stream will be mapped as an independent task or set of task
        // We also ask that produced tasks are concatenated, i.e that each source element must only be
        // evaluated/transform to (a set of) task after the task produced by previous value is done.
        .concatMap { idx -> Mono.just("$message (i=$idx)").delayElement(delay) }
        // With such abstraction, reactive streams are very hard to reason about at first.
        // But they really shine when you need to gain fine control over the pipeline of operation.
        // Now, if we want to "time" our signals/results, there is no need to manually fetch system time,
        // store and retrieve previous set time, etc...
        // There's all kind of operators bundled in. Example:
        .timed()
        .map { "${it.get()} (from start: ${it.elapsedSinceSubscription().toMillis()} ms - from previous: ${it.elapsed().toMillis()} ms" }
}

fun main() {
    var pipeline = Flux.merge(
        say("hello"),
        say("world")
    )
    // Derive the pipeline by adding a non-interfering side-effect.
    pipeline = pipeline.doOnNext { log(it) }

    // Bridge to blocking world: launch the pipeline and wait for its completion.
    pipeline.blockLast()

    log("Done !")
}