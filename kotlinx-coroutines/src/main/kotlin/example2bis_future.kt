package fr.amanin.concurrency.examples.coroutines

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun sum(values: IntArray, from: Int, to: Int) : Int {
    var sum = 0
    for (i in from until to) sum += values[i]
    log("$sum computed")
    return sum
}

fun main() :Unit = runBlocking {
    val values = intArrayOf(7, 2, 8, -9, 4, 0)
    val half = values.size / 2

    val futureX = async { sum(values, 0, half) }
    val futureY = async { sum(values, half, values.size) }

    log("sums submitted")
    delay(100)

    // future result is also a job
    log("jobs are started ? ${
        when {
            futureX.isActive && futureX.isActive -> "Both"
            futureY.isActive || futureY.isActive ->  "One"
            else -> "None"
        }
    }")

    log("query values")
    val x = futureX.await()
    delay(100)
    val y = futureY.await()

    delay(100)
    println("$x + $y = ${x + y}")
}