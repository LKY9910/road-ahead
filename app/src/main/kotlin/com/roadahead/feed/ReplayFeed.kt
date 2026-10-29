package com.roadahead.feed

import android.content.Context
import android.os.SystemClock
import com.roadahead.core.RoadAheadEngine
import com.roadahead.core.hazard.parseItsEvents
import com.roadahead.core.hazard.parseWeatherCsv
import com.roadahead.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.time.Instant

class ReplayFeed internal constructor(
    private val events: List<JsonObject>,
    private val monoMs: () -> Long,
    private val now: () -> Instant
) : Feed {
    constructor(context: Context, asset: String) : this(
        context.assets.open(asset).bufferedReader().useLines { lines ->
            lines.map { Json.parseToJsonElement(it).jsonObject }.toList()
        }, SystemClock::elapsedRealtime, Instant::now)

    private var job: Job? = null
    private var lastSpeed: Double? = null
    private val _navMode = MutableStateFlow(NavMode.UNKNOWN)
    private val _turn = MutableStateFlow<Turn?>(null)
    private val _fix = MutableStateFlow<Fix?>(null)
    private val _speed = MutableSharedFlow<Pair<Double, Long>>(extraBufferCapacity = 4)
    private val _wiperOn = MutableStateFlow(false)
    private val _defrosterOn = MutableStateFlow(false)
    private val _naviSpeaking = MutableStateFlow(false)
    private val _data = MutableSharedFlow<DataEvent>(extraBufferCapacity = 16)
    private val _label = MutableStateFlow<String?>(null)
    override val navMode = _navMode.asStateFlow()
    override val turn = _turn.asStateFlow()
    override val fix = _fix.asStateFlow()
    override val speed = _speed.asSharedFlow()
    override val wiperOn = _wiperOn.asStateFlow()
    override val defrosterOn = _defrosterOn.asStateFlow()
    override val naviSpeaking = _naviSpeaking.asStateFlow()
    override val data = _data.asSharedFlow()
    override val label = _label.asStateFlow()

    init {
        val times = events.map { it.getValue("t").jsonPrimitive.long }
        require(times.isNotEmpty() && times.first() >= 0 && times.zipWithNext().all { (a, b) -> a <= b })
    }

    override fun start(scope: CoroutineScope, engine: RoadAheadEngine) {
        check(job == null)
        job = scope.launch {
            val started = monoMs()
            launch {
                while (isActive) {
                    delay(200)
                    emitSpeed(monoMs())
                }
            }
            for (event in events) {
                delay((event.getValue("t").jsonPrimitive.long - (monoMs() - started)).coerceAtLeast(0))
                emitEvent(event, monoMs(), now())
            }
        }
    }

    internal suspend fun emitSpeed(receivedMonoMs: Long) {
        lastSpeed?.let { _speed.emit(it to receivedMonoMs) }
    }

    internal suspend fun emitEvent(event: JsonObject, receivedMonoMs: Long, produced: Instant) {
        fun text(key: String) = event.getValue(key).jsonPrimitive.content
        fun number(key: String) = event.getValue(key).jsonPrimitive.double
        fun on() = event.getValue("on").jsonPrimitive.boolean
        when (text("kind")) {
            "label" -> _label.value = text("text")
            "nav" -> _navMode.value = NavMode.valueOf(text("mode"))
            "turn" -> _turn.value = Turn(event["movement"]?.jsonPrimitive?.contentOrNull?.let(Movement::valueOf), number("distanceM"))
            "fix" -> _fix.value = Fix(GeoPoint(number("lat"), number("lon")), event["bearing"]?.jsonPrimitive?.doubleOrNull, receivedMonoMs)
            "speed" -> { lastSpeed = number("mps"); emitSpeed(receivedMonoMs) }
            "wiper" -> _wiperOn.value = on()
            "defrost" -> _defrosterOn.value = on()
            "navvoice" -> _naviSpeaking.value = on()
            "map" -> _data.emit(DataEvent.Intersections(text("body")))
            "signals" -> _data.emit(DataEvent.Signals(text("body"), receivedMonoMs))
            // Recorded hazards use replay time so the engine can enforce its normal age limit.
            "weather" -> _data.emit(DataEvent.Weather(parseWeatherCsv(text("csv"), HazardKind.valueOf(text("hazard")), produced)))
            "events" -> _data.emit(DataEvent.Events(parseItsEvents(text("body"), produced)))
            else -> error("Unknown replay event")
        }
    }

    override fun stop() { job?.cancel() }
}
