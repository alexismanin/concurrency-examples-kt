package fr.amanin.concurrency.examples.reactor

import fr.amanin.concurrency.examples.common.Tree
import fr.amanin.concurrency.examples.common.bbTree
import fr.amanin.concurrency.examples.common.log
import fr.amanin.concurrency.examples.common.randomBTree
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Objects

/**
 * Implement using the awesome [Flux.expandDeep] operator.
 * It is kind of a special recursive "flatMap", which makes it useful to traverse linked lists or tree structures.
 * However, we have to apply some tweaks.
 * We do not want to traverse the tree in a [breadth-first][Flux.expand] or [depth-first][Flux.expandDeep] manner.
 * To make a sorted traversal, we will emit tree nodes with their associated value attached along.
 * A node without its value attached means that we must traverse its left values first, then emit its value,
 * then continue by browsing its left side.
 * Tree nodes with a value attached are considered already "searched": their value will be emitted,
 * but their left/right children will be ignored.
 */
fun Tree.walk() : Flux<Int> {
    return Mono.just(this to (null as Int?))
        .expandDeep { (node, value) ->
            when (value) {
                null -> Flux.concat(
                    node.left?.let { Mono.just(it to (null as Int?)) } ?: Mono.empty(),
                    Mono.just(node to node.value),
                    node.right?.let { Mono.just(it to (null as Int?)) } ?: Mono.empty()
                )
                else -> Mono.empty()
            }
        }
        .mapNotNull { (_, value) -> value } as Flux<Int>
}

/**
 * Transform a stream of values to a stream of "marked" values. Each value will be attached to a "true" boolean value,
 * meaning that the value is coming from the source data stream. A final value is attached with a "false" mark, meaning
 * this is an external value.
 * We just use it to add an end marker to the flux, a sort of "poison" element.
 */
fun Flux<Int>.withMark() = map { it to true }.concatWith(Mono.just(0 to false))

/**
 * Use zip operator to sequentially compare each value of both trees together.
 * Stop walk/comparison once a difference is found using [Flux.takeUntil] operator.
 *
 * Note: That's the same logic than with coroutine flows.
 */
fun same(t1: Tree, t2: Tree) : Mono<Boolean> {
    val comparisonFlow = t1.walk().withMark()
        .zipWith(t2.walk().withMark(), Objects::equals)
        .takeUntil { it == false }

    /* Bonus: to match function requirement, we could have just returned above flow "last()" element.
     * But, I'd like to log the number of compared value pairs, as a diagnostic, to ensure my algorithm do the
     * minimal amount of work.
     * To do that, there's many possible answer:
     *  - Using "scan()" operator to maintain a count of traversed elements,
     *  - using "doOnNext()" side-effect to increment a "LongAdder" element as values are seen
     *  - Or, design a sort of tee, to connect this flow to two different operators: one that counts and log,
     *    and another that will traverse the tree to catch and return the final result.
     *
     * The "easiest" and probably the most optimized solution would be using scan (regarding back-pressure, etc.).
     * However, the last solution is more fun, and demonstrate advanced usage through a simple example, so let's do it.
     * Required steps:
     *  1. Transform input data stream into a "shared" data-source, i.e ensure teeing/splitting will trigger source
     *     flow only once (emitted elements are then broadcast to consumers) instead of being triggered from the start
     *     by each subscriber.
     *  2. Design our two flow consumers: counting and getting last value.
     *  3. Merge back our two derived operators, so user will just trigger the "same" operator, and all operations are
     *     triggered by that call.
     */
    val sharedFlow = comparisonFlow
        // this log allow to verify that comparison operator is triggered only once
        .doOnSubscribe { log("Comparison start") }
        .publish()
        .autoConnect(2)
    val counter = sharedFlow.count()
    val result = sharedFlow.last()
    return counter.zipWith(result) { count, last ->
        log("Count of value pairs: $count")
        last
    }
}

fun main() {
    bbTree(0..10)
        .walk()
        .doOnNext { println(it) }
        .blockLast()

    Flux.concat(
        same(bbTree(0..10), randomBTree(0..10)),
        same(randomBTree(0..10), randomBTree(0..11)),
        same(randomBTree(0..10), randomBTree(1..10))
    )
        .doOnNext { same -> println(if (same) "SAME" else "DIFFERENT") }
        .blockLast()
}