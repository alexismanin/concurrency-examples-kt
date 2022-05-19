package fr.amanin.concurrency.examples.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

inline fun log(message : () -> String) = System.err.println(message())

suspend fun sum(values: IntArray, from: Int, to: Int, channel: Channel<Int>) {
    var sum = 0
    for (i in from until to) sum += values[i]
    log { "Ready to send $sum" }
    channel.send(sum)
    log { "$sum sent from ${Thread.currentThread().name}" }
}

fun main() : Unit = runBlocking {
    val values = intArrayOf(7, 2, 8, -9, 4, 0)
    val channel = Channel<Int>(0) // Force producer to suspend until an element is asked by a consumer
    val half = values.size / 2
    launch { sum(values, 0, half, channel) }
    launch { sum(values, half, values.size, channel) }

    log { "sums submitted" }
    delay(100)

    log { "query values" }
    val x = withTimeout(1000) { channel.receive() }
    delay(100)
    val y = withTimeout(1000) { channel.receive() }

    delay(100)
    println("$x + $y = ${x + y}")
    channel.close()
}