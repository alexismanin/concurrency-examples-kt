package fr.amanin.concurrency.examples.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select

suspend fun fibonacci(output: Channel<Int>, quit: Channel<Int>) {
    var next : Pair<Int, Int>? = 0 to 1
    while (next != null) {
        val (x, y) = next

        next = select {
            output.onSend(x) { y to x + y }
            quit.onReceive { value -> null }
        }
    }
}

fun main() : Unit = runBlocking {
    val channel = Channel<Int>(0)
    val quit = Channel<Int>(0)
    launch {
        for (i in 1..10) {
            println(channel.receive())
        }
        quit.send(0)
    }

    fibonacci(channel, quit)
}