package com.roadahead.core.signal

import com.roadahead.core.model.*
import java.time.Duration
import java.time.LocalDateTime

data class SignalReading(val color: LightColor, val remainingSec: Double?)

class SignalTracker(private val staleAfterMs: Long = 3_000) {
    private data class Sample(val produced: LocalDateTime, val raw: Long?)
    private class Series(var color: LightColor) {
        val samples = mutableListOf<Sample>()
        var scale: Int? = null
    }
    private data class Tracked(val snapshot: SignalSnapshot, val series: MutableMap<Pair<Compass8, Movement>, Series>)
    private val intersections = mutableMapOf<String, Tracked>()

    fun accept(s: SignalSnapshot) {
        val previous = intersections[s.key]
        if (previous != null && !s.produced.isAfter(previous.snapshot.produced)) return
        val series = previous?.series ?: mutableMapOf()
        for (signal in s.signals) {
            val history = series.getOrPut(signal.approach to signal.movement) { Series(signal.color) }
            if (history.color != signal.color) history.samples.clear()
            history.color = signal.color
            history.samples += Sample(s.produced, signal.raw)
            if (history.samples.size > 3) history.samples.removeAt(0)
            if (history.samples.size >= 3) {
                val rates = history.samples.zipWithNext { a, b ->
                    if (a.raw == null || b.raw == null) Double.NaN
                    else (a.raw - b.raw).toDouble() / (Duration.between(a.produced, b.produced).toMillis() / 1000.0)
                }
                history.scale = when {
                    rates.all { it in 850.0..1150.0 } -> 1000
                    rates.all { it in 85.0..115.0 } -> 100
                    else -> null
                }
            }
        }
        intersections[s.key] = Tracked(s, series)
    }

    fun reading(key: String, approach: Compass8, movement: Movement, nowMonoMs: Long): SignalReading? {
        val tracked = intersections[key] ?: return null
        val snapshot = tracked.snapshot
        if (nowMonoMs - snapshot.receivedMonoMs > staleAfterMs) return null
        val signal = snapshot.signals.firstOrNull { it.approach == approach && it.movement == movement } ?: return null
        val scale = tracked.series[approach to movement]?.scale
        val remaining = if (scale != null && signal.raw != null) {
            (signal.raw.toDouble() / scale - (nowMonoMs - snapshot.receivedMonoMs) / 1000.0).coerceAtLeast(0.0).takeIf { it <= 200.0 }
        } else null
        return SignalReading(signal.color, remaining)
    }

    fun liveKeys(): Set<String> = intersections.keys.toSet()
}
