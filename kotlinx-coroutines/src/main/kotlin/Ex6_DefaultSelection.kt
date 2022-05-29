package fr.amanin.concurrency.examples.coroutines

import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select

private enum class State(val label: String) { tick("tick"), boom("BOOM"), waiting(".") }

fun main() : Unit = runBlocking {
    val tick = ticker(100)
    val boom = ticker(500)
    var next : State
    do {
        next = select {
            tick.onReceive { State.tick }
            boom.onReceive { State.boom }
            onTimeout(50) { State.waiting }
        }
        println(next.label)
    } while (next != State.boom)
}