package fr.amanin.concurrency.examples.common

/**
 * Note: we could have designed page fetch result as a sealed class with an "error" subtype.
 * However, it would show error management strategy at the language level.
 * We're more interested to demonstrate how to cope with error using target concurrency framework.
 * Therefore, the fetch result will only contain success data. Error will bubble up as exceptions.
 */
data class FetchedPage(val body: String, val urls: List<String>)

class InvalidUrlException(message: String? = null, cause: Exception? = null): IllegalArgumentException(message, cause)

fun fakeFetch(url: String) = when (url) {
    go, "$go/"     -> FetchedPage("Go programming language", listOf("$go/pkg/", "$go/cmd/"))
    "$go/pkg/"     -> FetchedPage("Packages", listOf(go, "$go/cmd/", "$go/pkg/fmt/", "$go/pkg/os/"))
    "$go/pkg/fmt/" -> FetchedPage("Package fmt", listOf(go, "$go/pkg/"))
    "$go/pkg/os/"  -> FetchedPage("Package os", listOf(go, "$go/pkg/"))
    else           -> throw InvalidUrlException("Cannot fake $url")
}

private const val go = "https://golang.org"