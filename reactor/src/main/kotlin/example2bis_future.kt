package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.Mono
import java.time.Duration

fun sum(values: IntArray, from: Int, to: Int) : Int {
    var sum = 0
    for (i in from until to) sum += values[i]
    log("$sum computed")
    return sum
}

fun main() {

    val values = intArrayOf(7, 2, 8, -9, 4, 0)

    val half = values.size / 2
    val asyncX = Mono.fromCallable { sum(values, 0, half) }
    val asyncY = Mono.fromCallable { sum(values, half, values.size) }

    log("Tasks emitted")

    val printAction : Mono<Unit> = Mono.delay(Duration.ofMillis(100))
        .then(Mono.fromRunnable<Unit> { log("query values") })
        .then(asyncX.zipWith(asyncY) {
            x, y -> log("$x + $y = ${x+y}")
        })

    printAction.block(Duration.ofSeconds(1))
}