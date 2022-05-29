package fr.amanin.concurrency.examples.coroutines

import fr.amanin.concurrency.examples.common.log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

suspend fun sum(values: IntArray, from: Int, to: Int, channel: Channel<Int>) {
    var sum = 0
    for (i in from until to) sum += values[i]
    log("Ready to send $sum")
    channel.send(sum)
    log("$sum sent")
}

fun main() : Unit = runBlocking {
    val values = intArrayOf(7, 2, 8, -9, 4, 0)
    /*
     * When creating a channel, we provide a buffering capacity, so it might "prefetch" a certain amount of results.
     * In this example, we provide an empty capacity.
     * It will force producers to suspend until a consumer asks data to the channel.
     * Note: we could add additional arguments to specify a different behavior.
     * Instead of suspending producers, we could drop values to make room in the channel.
     */
    val channel = Channel<Int>(0)
    val half = values.size / 2

    // Launch start a task without return value.
    // A job is immediately returned to user to help him manage task
    // By default, it uses the same coroutine scope (dispatcher/executor/scheduler) than the coroutine that created it.
    // However, We can give it another context of execution. For example, let's use a different one for both tasks.
    // We will let firs job be executed on main thread (cause of runBlocking), but we will dispatch the second job
    // to the IO threads (elastic pool, ideal to deal with external blocking code nested in a coroutine)
    val job1 = launch { sum(values, 0, half, channel) }
    val job2 = launch(Dispatchers.IO) { sum(values, half, values.size, channel) }

    log("sums submitted")
    delay(100)

    // jobs give information about the state of the tasks.
    // It also provides list of its subtasks, if any.
    log("jobs are started ? ${
        when {
            job1.isActive && job2.isActive -> "Both"
            job1.isActive || job2.isActive ->  "One"
            else -> "None"
        }
    }")

    log("query values")
    val x = withTimeout(1000) { channel.receive() }
    delay(100)
    val y = withTimeout(1000) { channel.receive() }

    delay(100)
    println("$x + $y = ${x + y}")
    channel.close()
}