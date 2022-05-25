package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult
import java.time.Duration

// Note: So far, examples are really bending Reactor usage to mimic Go channels.
// However, it is really not the way to do things properly, so beware: this examples
// are only here for fun and to see how much Reactor can be "repurposed".

// This is a proper infinite/cancellable/backpressure aware implementation of Fibonacci
fun fibonacciShouldBeSomethingLikeThat() : Flux<Long> = Mono.just(0L to 0L)
    .expand { (x, y) -> Mono.just(y to x + y) }
    .map { (x, _) -> x }

fun fibonacci(output: Sinks.Many<Int>, quitSignal: Sinks.Empty<Nothing>) {
    Mono.just(0 to 1)
        .expand { (x, y) ->
            // select "equivalent"
            Mono.firstWithSignal(
                Mono.fromCallable { output.tryEmitNext(x) }
                    .filter { EmitResult.OK == it }
                    .repeatWhenEmpty { it.delayElements(Duration.ofMillis(10)) }
                    .map { y to x + y },
                quitSignal.asMono()
                )
        }
        .blockLast()
}

fun main() {
    val channel = Sinks.many().multicast().directAllOrNothing<Int>()
    val quit = Sinks.empty<Nothing>()
    go("Print then quit") {
        channel.asFlux().take(10)
            .doFinally { quit.emitEmpty(::failFast) }
            .subscribe(
                { next -> println(next) },
                { err  -> log("Failure while consuming Fibonacci values. Reason: ${err.message}") }
            )
    }

    fibonacci(channel, quit)
}