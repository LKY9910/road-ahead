package com.roadahead.core.advice

import com.roadahead.core.drive.Motion
import com.roadahead.core.geo.*
import com.roadahead.core.model.*
import com.roadahead.core.signal.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

data class SignalConfig(val approachRangeM: Double = 300.0, val stopLineM: Double = 60.0, val coneDeg: Double = 30.0,
    val turnMatchM: Double = 40.0, val waitSoonSec: Double = 5.0, val waitStartMinSec: Double = 8.0,
    val defaultApproachMps: Double = 8.0, val convention: DirectionConvention = DirectionConvention.APPROACH)
data class SignalView(val intersection: Intersection, val movement: Movement, val reading: SignalReading, val distanceM: Double)

class SignalAdvisor(private val cfg: SignalConfig = SignalConfig()) {
    fun view(navMode: NavMode, turn: Turn?, fix: Fix?, intersections: List<Intersection>, tracker: SignalTracker, nowMonoMs: Long): SignalView? {
        val bearing = fix?.bearingDeg ?: return null
        val live = tracker.liveKeys()
        val (intersection, distance) = intersections.mapNotNull { intersection ->
            if (intersection.key !in live) return@mapNotNull null
            val distance = aheadDistance(fix, intersection.point, cfg.coneDeg) ?: return@mapNotNull null
            if (distance <= cfg.approachRangeM) intersection to distance else null
        }.minByOrNull { it.second } ?: return null
        val movement = if (navMode == NavMode.ROUTE && turn != null && abs(turn.distanceM - distance) <= cfg.turnMatchM) {
            turn.movement ?: return null
        } else Movement.STRAIGHT
        val reading = tracker.reading(intersection.key, signalDirection(bearing, cfg.convention), movement, nowMonoMs) ?: return null
        return SignalView(intersection, movement, reading, distance)
    }

    fun advices(view: SignalView?, navMode: NavMode, motion: Motion, speedMps: Double?): List<Advice> {
        if (view == null) return emptyList()
        val key = "sig:${view.intersection.key}"
        val seconds = view.reading.remainingSec
        val color = view.reading.color
        val route = navMode == NavMode.ROUTE
        fun advice(suffix: String, text: String) = Advice("$key:$suffix", Priority.SIGNAL, text)
        if (motion == Motion.STOPPED) {
            if (!route || view.distanceM > cfg.stopLineM || color != LightColor.RED || seconds == null) return emptyList()
            return listOfNotNull(when {
                seconds >= cfg.waitStartMinSec -> advice("wait", "${view.movement.label} 신호까지 ${floor(seconds).toInt()}초 남았어요.")
                seconds <= cfg.waitSoonSec -> advice("soon", "곧 ${view.movement.label} 신호로 바뀌어요.")
                else -> null
            })
        }
        val eta = view.distanceM / max(speedMps ?: cfg.defaultApproachMps, 1.0)
        return listOfNotNull(when {
            color == LightColor.RED && (seconds == null || seconds > eta) ->
                advice("red", if (route) "앞 신호 빨간불이에요. 속도를 줄이세요." else "앞 신호 빨간불이에요.")
            route && color in listOf(LightColor.GREEN, LightColor.YELLOW) && seconds != null && seconds < eta ->
                advice("soonred", "앞 신호가 곧 빨간불로 바뀌어요. 속도를 줄이세요.")
            else -> null
        })
    }
}
