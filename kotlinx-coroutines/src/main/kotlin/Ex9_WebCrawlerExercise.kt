package fr.amanin.concurrency.examples.coroutines

import fr.amanin.concurrency.examples.common.FetchedPage
import fr.amanin.concurrency.examples.common.fakeFetch
import fr.amanin.concurrency.examples.common.log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Could have been defined using an interface:
 * ```kt
 * fun interface Fetcher {
 *    suspend fun fetch(url: String) : Result<FetchedPage>
 * }
 * ```
 */
typealias Fetcher = suspend (String) -> FetchedPage

suspend fun fetch(url: String) : FetchedPage {
    // Simulate work/download, etc.
    delay(100)
    return fakeFetch(url)
}

/**
 * Mimic Spring project Reactor "Flux.expandDeep" operator with Kotlin flows.
 */
suspend fun <T> Flow<T>.expand(maxDepth: Int, fetchChildren: (T) -> Flow<T>) : Flow<T> {
    if (maxDepth < 1) return emptyFlow()
    val nextMaxDepth = maxDepth - 1
    return flow {
        this@expand.collect {
            emit(it)
            emitAll(fetchChildren(it).expand(nextMaxDepth, fetchChildren))
        }
    }
}

suspend fun crawl(url: String, fetcher: Fetcher, depth: Int): Flow<FetchedPage> {
    val root = flow { emit(fetcher(url)) }
    return root.expand(depth) { page ->
        channelFlow {
            for (nextUrl in page.urls) {
                // try/catch must be inside async block, because by the time we check
                // async job status, the coroutine might already have signaled failure to its parent,
                // therefore cancelling the whole flow independently of our will.
                // Note that other technics allow to control that behavior, such as using a supervisorScope.
                launch(Dispatchers.Default) {
                    try {
                        val nextPage = fetcher(nextUrl)
                        send(nextPage)
                    } catch (e: Exception) {
                        log(e.message ?: "Error without message")
                    }
                }
            }
        }
    }
}

/**
 * Very naive suspendable cache that remind errors as well as successes.
 * The main point here is to avoid duplicating work because a result has not been pushed yet.
 * Instead, as soon as a new input is sent, we register a deferred result by launching computation
 * asynchronously.
 * By doing that, any subsequent task can just suspend until the computing is done.
 *
 * Note: we could have simplified that drastically, because the aim of the exercise is only to skip known urls.
 * Doing that would necessitate only a Set of Urls, but caching deferred actions is more fun for learning.
 * Also, why do simple when we can spice things
 */
fun <I, O> caching(action: suspend (I) -> O) : suspend (I) -> CacheResult<O> {
    val cache: MutableMap<I, Deferred<O>> = HashMap()
    val mutex = Mutex()

    suspend fun cache(input: I) : CacheResult<O> {
        val task = mutex.withLock {
            val cachedResult = cache[input]
            if (cachedResult == null) {
                // Using supervisor scope prevent termination of the entire hierarchy of coroutines in case an error is raised by action.
                val deferredAction = supervisorScope {
                    async { action(input) }
                }
                cache[input] = deferredAction
                supervisorScope {
                    async { runCatching { NewValue(deferredAction.await()) }.getOrElse { NewError(it) } }
                }
            } else {
                supervisorScope {
                    async { runCatching { CachedValue(cachedResult.await()) }.getOrElse { CachedError(it) } }
                }
            }
        }

        return task.await()
    }

    return ::cache
}

sealed interface CacheResult<out V>
data class NewValue<V>(val value: V) : CacheResult<V>
data class CachedValue<V>(val value: V) : CacheResult<V>
data class NewError<V>(val error: Throwable) : CacheResult<V>
data class CachedError<V>(val error: Throwable) : CacheResult<V>

fun <I, O> filterNewValues(cacheAction: suspend (I) -> CacheResult<O>): suspend (I) -> O {
    return { input ->
        when (val result = cacheAction(input)) {
            is NewValue -> result.value
            is NewError -> throw result.error
            is CachedValue -> throw IllegalStateException("Fail on already encountered value")
            is CachedError -> throw IllegalStateException("Cached error", result.error)
        }
    }
}

fun main() : Unit = runBlocking {
    val fetchCaching = caching(::fetch)
    val fetchOnlyNewValues = filterNewValues(fetchCaching)
    crawl("https://golang.org", fetchOnlyNewValues, 4)
        .map { it.body }
        .collect(::println)
}