package fr.amanin.concurrency.examples.common

fun log(message : Any) = System.err.println("[from ${threadName()}]: $message")

private fun threadName() : String {
    val name = Thread.currentThread().name
    val size = name.length
    return when {
        size == 10 -> name
        size  < 10 -> name.padEnd(10)
        else       -> name.substring(size - 10)
    }
}

fun IntProgression.size() = if (isEmpty()) 0 else (last - first + step) / step
fun IntProgression.toArray() = IntArray(size()) { i -> first + step * i }
