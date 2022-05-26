package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration


fun main() {
    val tictac = Flux.interval(Duration.ofMillis(100)).map { if (it % 2 == 0L) "tic" else "tac" }
    val boom = Mono.delay(Duration.ofMillis(500)).map { "BOOM" }
    val default = Flux.interval(Duration.ofMillis(50)).map { "." }

    val tictacboom = Flux.merge(tictac, boom, default).takeUntil { it == "BOOM"}
    tictacboom.doOnNext { println(it) }.blockLast()

    // Repeatable
    log("REPEAT")
    tictacboom.doOnNext { println(it) }.blockLast()

    log("OTHER")
    // We can also assemble it this way:
    tictac.mergeWith(default)
        .takeUntilOther(boom)
        .concatWith(Mono.just("BOOM"))
        .doOnNext { println(it) }
        .blockLast()
}