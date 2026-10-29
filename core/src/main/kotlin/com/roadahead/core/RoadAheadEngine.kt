package com.roadahead.core

import com.roadahead.core.model.*
import com.roadahead.core.advice.*
import com.roadahead.core.drive.*
import com.roadahead.core.geo.aheadDistance
import com.roadahead.core.signal.*
import com.roadahead.core.speak.Announcer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt

data class EngineConfig(val signal: SignalConfig = SignalConfig(), val hazard: HazardConfig = HazardConfig(),
    val signalPollMs: Long = 2_000, val signalPollRangeM: Double = 1_000.0, val signalDailyLimit: Int = 4_500,
    val zone: ZoneId = ZoneId.of("Asia/Seoul"))

data class Inputs(val navMode: NavMode, val turn: Turn?, val fix: Fix?, val wiperOn: Boolean, val defrosterOn: Boolean,
    val naviSpeaking: Boolean, val selfSpeaking: Boolean)
data class SignalCard(val movement: Movement, val color: LightColor, val remainingSec: Int?, val distanceM: Int)
data class Screen(val signal: SignalCard?, val hazardText: String?, val signalBudgetExhausted: Boolean)
data class TickResult(val screen: Screen, val speak: Advice?)

class RoadAheadEngine(private val cfg: EngineConfig = EngineConfig()) {
    private val tracker = SignalTracker()
    private val stopDetector = StopDetector()
    private val signalAdvisor = SignalAdvisor(cfg.signal)
    private val hazardAdvisor = HazardAdvisor(cfg.hazard)
    private val announcer = Announcer()
    private var intersections = emptyList<Intersection>()
    private val hazards = mutableMapOf<HazardSource, List<Hazard>>()
    private var speedMps: Double? = null
    private var lastMovingBearing: Double? = null
    private var pollDate: LocalDate? = null
    private var pollsToday = 0
    private var lastPollMonoMs: Long? = null

    fun onIntersections(list: List<Intersection>) {
        intersections = list.toList()
    }

    fun onSignalPage(json: String, receivedMonoMs: Long) {
        parseSignalPage(json, receivedMonoMs).forEach(tracker::accept)
    }

    fun onHazards(source: HazardSource, hazards: List<Hazard>) {
        this.hazards[source] = hazards.toList()
    }

    fun onSpeed(mps: Double, monoMs: Long) {
        speedMps = mps
        stopDetector.onSpeed(mps, monoMs)
    }

    private fun resetBudget(now: Instant) {
        val date = now.atZone(cfg.zone).toLocalDate()
        if (pollDate != date) {
            pollDate = date
            pollsToday = 0
        }
    }

    fun shouldPollSignals(fix: Fix?, nowMonoMs: Long, now: Instant): Boolean {
        resetBudget(now)
        val effectiveFix = effectiveFix(fix, stopDetector.motion(nowMonoMs))
        if (effectiveFix?.bearingDeg == null || pollsToday >= cfg.signalDailyLimit) return false
        if (lastPollMonoMs?.let { nowMonoMs - it < cfg.signalPollMs } == true) return false
        val live = tracker.liveKeys()
        return intersections.any { intersection ->
            (live.isEmpty() || intersection.key in live) &&
                aheadDistance(effectiveFix, intersection.point, cfg.signal.coneDeg)?.let { it <= cfg.signalPollRangeM } == true
        }
    }

    fun recordSignalPoll(nowMonoMs: Long, now: Instant) {
        resetBudget(now)
        pollsToday++
        lastPollMonoMs = nowMonoMs
    }

    fun tick(inputs: Inputs, nowMonoMs: Long, now: Instant): TickResult {
        resetBudget(now)
        val motion = stopDetector.motion(nowMonoMs)
        val fix = effectiveFix(inputs.fix, motion)
        val view = signalAdvisor.view(inputs.navMode, inputs.turn, fix, intersections, tracker, nowMonoMs)
        val signalAdvices = signalAdvisor.advices(view, inputs.navMode, motion, speedMps)
        val hazardAdvices = hazardAdvisor.advices(hazards.values.flatten(), fix, inputs.wiperOn || inputs.defrosterOn, now)
        val card = view?.let {
            SignalCard(it.movement, it.reading.color, it.reading.remainingSec?.let { seconds -> floor(seconds).toInt() }, it.distanceM.roundToInt())
        }
        val screen = Screen(card, hazardAdvices.minByOrNull { it.priority }?.text, pollsToday >= cfg.signalDailyLimit)
        return TickResult(screen, announcer.pick(signalAdvices + hazardAdvices, inputs.naviSpeaking, inputs.selfSpeaking, nowMonoMs))
    }

    private fun effectiveFix(fix: Fix?, motion: Motion): Fix? {
        if (fix == null) return null
        if (motion == Motion.MOVING && fix.bearingDeg != null) lastMovingBearing = fix.bearingDeg
        return if (motion == Motion.STOPPED || fix.bearingDeg == null) fix.copy(bearingDeg = lastMovingBearing) else fix
    }
}
