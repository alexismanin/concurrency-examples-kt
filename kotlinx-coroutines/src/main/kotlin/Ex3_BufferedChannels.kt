package fr.amanin.concurrency.examples.coroutines

import fr.amanin.concurrency.examples.common.log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

fun main() : Unit = runBlocking {
    val channel = Channel<Int>(2)
    channel.send(1)
    channel.send(2)
    // Note: buffer overflow is handled differently from Go.
    // In Go, sending to a channel without any consumer will trigger an error immediately.
    // This is not the case here.
    // The sending coroutine will wait indefinitely, in case someone later subscribes to the channel.
    // To force a failure, we should specify a timeout
    withTimeoutOrNull(100) { channel.send(3) }
        ?: log("Send operation timed out !")
    // Or we can use trySend instead of send, to avoid suspension:
    channel.trySend(4)
        .onFailure { log("Cannot send element !") } // Note: why Throwable is nullable here ?

    log(channel.receive())
    log(channel.receive())
}