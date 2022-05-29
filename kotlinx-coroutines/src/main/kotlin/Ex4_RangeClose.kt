package fr.amanin.concurrency.examples.coroutines

import fr.amanin.concurrency.examples.common.log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

// Note: a sequence generator would be more fitted for this kind of use case.
suspend fun fibonacci(n: Int, output: Channel<Int>, delayBetweenEachElement: Duration = 10.milliseconds) {
    var next = 0 to 1
    for (i in 0 until n) {
        val (x, y) = next
        log("send $x")
        delay(delayBetweenEachElement)
        output.send(x)
        next = y to x + y
    }
    output.close()
}

fun main() : Unit = runBlocking {
    val capacity = 10
    val transport = Channel<Int>(capacity)
    launch(newSingleThreadContext("Fibonacci")) {
        fibonacci(capacity, transport)
    }

    log("Consume first half")
    for (i in 0 until capacity / 2) println(transport.receive())

    delay(10)
    log("Wait for channel completion")
    while (!transport.isClosedForSend) {
        delay(50)
    }

    log("Consume remaining elements")
    for (value in transport) println(value)

    log("Channel closed ? for reception: ${transport.isClosedForReceive} ; for publication: ${transport.isClosedForSend}")
}