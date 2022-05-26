package fr.amanin.concurrency.examples.coroutines

import fr.amanin.concurrency.examples.common.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult

suspend fun Tree.walk(consume: suspend (Int) -> Unit) {
    left?.walk(consume)
    log("consume $value")
    consume(value)
    right?.walk(consume)
}

suspend fun Tree.walk(channel: Channel<Int>) {
    walk(channel::send)
    channel.close()
}

suspend fun same(t1 : Tree, t2 : Tree) : Boolean = coroutineScope {
    val c1 = Channel<Int>(0)
    val c2 = Channel<Int>(0)

    launch { t1.walk(c1) }
    launch { t2.walk(c2) }

    var same = true
    var v1: ChannelResult<Int>
    var v2: ChannelResult<Int>
    do {
        v1 = c1.receiveCatching()
        v2 = c2.receiveCatching()

        if (v1.getOrNull() != v2.getOrNull())  {
            same = false
            break
        }
    } while (v1.isSuccess && v2.isSuccess)

    v1.exceptionOrNull()?.let { throw it }
    v2.exceptionOrNull()?.let { throw it }

    try {
        same && v1.isClosed && v2.isClosed
    } finally {
        c1.cancel()
        c2.cancel()
    }
}

fun main() : Unit = runBlocking {
    val channel = Channel<Int>(0)
    launch(Dispatchers.IO) { bbTree(0..10).walk(channel) }
    for (v in channel) println(v)

    val same = same(bbTree(0..10), randomBTree(0..10))
    if (same) println("SAME !") else println("DIFFERENT !")

    var different = same(bbTree(0..10), randomBTree(0..11))
    if (different) println("SAME !") else println("DIFFERENT !")

    different = same(bbTree(0..10), randomBTree(1..10))
    if (different) println("SAME !") else println("DIFFERENT !")
}