package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

fun say(message: String, delay : Duration = Duration.ofMillis(500), nTimes : Int = 5) : Flux<String> {
    return Flux.range(0, nTimes)
        .concatMap { idx -> Mono.just("$message (i=$idx ; thread=${Thread.currentThread().name})").delayElement(delay) }
}

fun main() {
    Flux.merge(
        say("hello"),
        say("world")
    )
        .doOnNext { println(it) }
        .blockLast()
}