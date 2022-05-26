package fr.amanin.concurrency.examples.reactor

import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks

internal fun failFast(s: SignalType, r: Sinks.EmitResult) : Boolean = throw IllegalStateException("Cannot emt signal $s. Reason: $r")
