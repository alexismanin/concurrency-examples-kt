package fr.amanin.concurrency.examples.reactor

import fr.amanin.concurrency.examples.common.FetchedPage
import fr.amanin.concurrency.examples.common.fakeFetch
import fr.amanin.concurrency.examples.common.log
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.*
import reactor.core.publisher.Sinks
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds

sealed interface FetchResult { val url: String ; val depth: Int }
data class FetchError(override val url: String, override val depth: Int, val error: java.lang.Exception) : FetchResult
data class FetchSuccess(override val url: String, override val depth: Int, val page : FetchedPage) : FetchResult

fun fetch(url: String) : Mono<FetchedPage> = delay(ofMillis(100)).then(fromCallable { fakeFetch(url) })

fun crawl(url: String, fetcher: (String) -> Mono<FetchedPage>, maxDepth: Int, maxConcurrency: Int = 4) : Flux<FetchedPage> {
    // Use a "companion" hot flux to build visited url history while crawling.
    // Main pipeline will push elements to it as it run (crawl pages).
    // An index will be processed concurrently, and each fetching operation will query it to filter out urls
    // referenced by the history.
    //
    // Note: Reactor has a "distinct" operator that allow to filter out already seen elements.
    // However, it does not fit here, because it filters elements after their emission from upstream.
    // It would have the effect to make the pipeline heavily running but printing/producing nothing.
    // i.e the crawling would recurse without regulation, and removal of elements would be done only after.
    val history = Sinks.unsafe().many().unicast().onBackpressureBuffer<String>()
    val historyIndex = history.asFlux()
        .scan(HashSet<String>()) { accumulator, value ->
            if (accumulator.contains(value)) accumulator
            else HashSet(accumulator).apply {
                add(value)
            }
        }
        .cache(1)

    // Force url index to build itself eagerly (as soon as someone push a value in it).
    // Without this line, the visited url will be buffered in above sink, and the index would start to build
    // only on the first subscription by a consumer.
    // Note: Ok, well, in this case, it would be as good to remove this, because the expansion will query the history
    // index almost immediately.
    val historySubscription = historyIndex.subscribe(
        { log("Size of history index: ${it.size}") },
        { log("Error on history index: $it") }
    )

    fun fetch(url : String, depth: Int) : Mono<FetchResult> {
        log("[Depth $depth] Fetching $url")
        return fetcher(url)
            .map<FetchResult> { FetchSuccess(url, depth, it) }
            // Replace fetching error with a report
            .onErrorResume(Exception::class.java) { just(FetchError(url, depth, it)) }
            // Mark URL as visited, so we can filter future visits
            .doOnNext {
                history.emitNext(it.url) { s, r ->
                    log("Fail to push url to visited history. Reason: $r")
                    false
                }
            }
    }

    return defer { fetch(url, maxDepth) }
        // Contrary to the tree example, expand operator is used more naturally here.
        // However, the operator does not offer control over recursion depth limit, so we have to add it manually.
        .expand { result ->
            if (result.depth < 1 || result !is FetchSuccess) empty()
            else defer { log("Get history index") ; historyIndex.next() }
                .flatMapIterable { index -> result.page.urls.filterNot(index::contains) }
                .flatMap( { nextUrl -> fetch(nextUrl, result.depth - 1) }, maxConcurrency)
        }
        .mapNotNull<FetchedPage> { if (it is FetchSuccess) it.page else null }
        .doFinally {
            runCatching { historySubscription.dispose() }.onFailure(::log)
            history.emitComplete { s, r -> log("Cannot cancel history channel (no pun intended). Reason: $r") ; false }
        }
}

fun main() {
    // High depth allow to ensure our "distinct" operator is working well and that crawler ignores already visited urls early.
    crawl("https://golang.org", ::fetch, maxDepth = 50)
        .doOnNext { println(it) }
        .blockLast(ofSeconds(10))
}