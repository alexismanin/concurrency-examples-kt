package fr.amanin.concurrency.examples.reactor

fun log(message : String) = System.err.println("[from ${threadName()}]: $message")
inline fun log(message: () -> String) = log(message())

private fun threadName() : String {
    val name = Thread.currentThread().name
    val size = name.length
    return when {
        size == 10 -> name
        size  < 10 -> name.padEnd(10)
        else       -> name.substring(size - 10)
    }
}