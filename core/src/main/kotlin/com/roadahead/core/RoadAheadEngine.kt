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
    val zone: ZoneId = ZoneId.of("Asia/Seoul"),
    val parking: ParkingConfig = ParkingConfig(), val travel: TravelConfig = TravelConfig(), val travelListMs: Long = 60_000)

data class Inputs(val navMode: NavMode, val turn: Turn?, val fix: Fix?, val wiperOn: Boolean, val defrosterOn: Boolean,
    val naviSpeaking: Boolean, val selfSpeaking: Boolean,
    val parked: Boolean = false, val travelMode: Boolean = false, val destination: Destination? = null)
data class SignalCard(val movement: Movement, val color: LightColor, val remainingSec: Int?, val distanceM: Int)
data class Screen(val signal: SignalCard?, val hazardText: String?, val signalBudgetExhausted: Boolean, val card: Card? = null)
data class TickResult(val screen: Screen, val speak: Advice?)

class RoadAheadEngine(private val cfg: EngineConfig = EngineConfig()) {
    private val tracker = SignalTracker()
    private val stopDetector = StopDetector()
    private val signalAdvisor = SignalAdvisor(cfg.signal)
    private val hazardAdvisor = HazardAdvisor(cfg.hazard)
    private val parkingAdvisor = ParkingAdvisor(cfg.parking)
    private val travelAdvisor = TravelAdvisor(cfg.travel)
    private val announcer = Announcer()
    private var intersections = emptyList<Intersection>()
    private val hazards = mutableMapOf<HazardSource, List<Hazard>>()
    private var speedMps: Double? = null
    private var lastMovingBearing: Double? = null
    private var pollDate: LocalDate? = null
    private var pollsToday = 0
    private var lastPollMonoMs: Long? = null
    private var destinationInitialized = false
    private var destinationKey: String? = null
    private var parkingRequested = false
    private var tourRequested = false
    private var parkingLots = emptyList<ParkingLot>()
    private var tourData = TourData(emptyList(), emptyList(), emptySet())
    private val announcements = mutableMapOf<String, Advice>()
    private var travelListStartedMs: Long? = null
    private var travelListClosed = false

    private fun selectDestination(key: String?) {
        // Navigation can temporarily omit the destination while recalculating a route.
        if (key == null || (destinationInitialized && destinationKey == key)) return
        destinationKey?.let {
            announcer.forget("parking:$it")
            announcer.forget("travel:$it")
        }
        destinationInitialized = true
        destinationKey = key
        parkingRequested = false
        tourRequested = false
        parkingLots = emptyList()
        tourData = TourData(emptyList(), emptyList(), emptySet())
        travelListStartedMs = null
        travelListClosed = false
    }

    fun parkingRequest(dest: Destination?, navMode: NavMode, travelMode: Boolean): Destination? {
        selectDestination(dest?.key)
        if (!travelMode || parkingRequested || !parkingAdvisor.due(dest, navMode)) return null
        parkingRequested = true
        return dest
    }

    fun tourRequest(dest: Destination?, travelMode: Boolean): Destination? {
        selectDestination(dest?.key)
        if (tourRequested || !travelMode || dest == null) return null
        tourRequested = true
        return dest
    }

    fun onParkingLots(destKey: String, lots: List<ParkingLot>) {
        if (!destinationInitialized) selectDestination(destKey)
        if (destinationKey == destKey) parkingLots = lots.toList()
    }

    fun onTourData(destKey: String, data: TourData) {
        if (!destinationInitialized) selectDestination(destKey)
        if (destinationKey == destKey) tourData = data
    }

    fun announce(advice: Advice) { announcements[advice.key] = advice }

    fun withdraw(key: String) { announcements.remove(key) }

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
        selectDestination(inputs.destination?.key)
        val motion = stopDetector.motion(nowMonoMs)
        val fix = effectiveFix(inputs.fix, motion)
        val view = signalAdvisor.view(inputs.navMode, inputs.turn, fix, intersections, tracker, nowMonoMs)
        val signalAdvices = signalAdvisor.advices(view, inputs.navMode, motion, speedMps)
        val hazardAdvices = hazardAdvisor.advices(hazards.values.flatten(), fix, inputs.wiperOn || inputs.defrosterOn, now)
        val signal = view?.let {
            SignalCard(it.movement, it.reading.color, it.reading.remainingSec?.let { seconds -> floor(seconds).toInt() }, it.distanceM.roundToInt())
        }
        val dest = inputs.destination
        val parking = dest?.takeIf { inputs.travelMode && parkingAdvisor.due(it, inputs.navMode) }?.let {
            parkingAdvisor.pick(it, parkingLots, now)
        }
        val travel = dest?.takeIf { inputs.travelMode }?.let {
            travelAdvisor.pick(it, tourData,
                now.plusSeconds(it.remainTimeSec?.toLong() ?: 0).atZone(cfg.zone).toLocalDate(),
                now.atZone(cfg.zone).toLocalDate())
        }
        travelListStartedMs?.let {
            if (!inputs.parked || nowMonoMs - it >= cfg.travelListMs) travelListClosed = true
        }
        val travelLines = travel?.lines?.takeIf { inputs.parked && !travelListClosed }?.also {
            if (travelListStartedMs == null) travelListStartedMs = nowMonoMs
        }
        val hazard = hazardAdvices.minByOrNull { it.priority }
        val card = when {
            travelLines != null -> Card.Travel(travelLines, travel.weatherNote)
            hazard?.priority == Priority.HIGH -> Card.Hazard(hazard.text)
            signal != null -> Card.Signal(signal)
            hazard != null -> Card.Hazard(hazard.text)
            parking != null -> Card.Parking(parking.first)
            else -> null
        }
        val screen = Screen(signal, hazard?.text, pollsToday >= cfg.signalDailyLimit, card)
        val candidates = signalAdvices + hazardAdvices + listOfNotNull(parking?.second, travel?.advice) + announcements.values
        val speak = announcer.pick(candidates, inputs.naviSpeaking, inputs.selfSpeaking, nowMonoMs)
        speak?.let { announcements.remove(it.key) }
        return TickResult(screen, speak)
    }

    private fun effectiveFix(fix: Fix?, motion: Motion): Fix? {
        if (fix == null) return null
        if (motion == Motion.MOVING && fix.bearingDeg != null) lastMovingBearing = fix.bearingDeg
        return if (motion == Motion.STOPPED || fix.bearingDeg == null) fix.copy(bearingDeg = lastMovingBearing) else fix
    }
}
