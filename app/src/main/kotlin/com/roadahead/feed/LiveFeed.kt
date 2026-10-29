package com.roadahead.feed

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.roadahead.BuildConfig
import com.roadahead.car.*
import com.roadahead.core.RoadAheadEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.time.Instant
import java.time.ZoneId

internal fun signalBudgetDay(now: Instant): String = now.atZone(ZoneId.of("Asia/Seoul")).toLocalDate().toString()
internal fun savedSignalPolls(day: String?, count: Int, now: Instant): Int =
    if (day == signalBudgetDay(now)) count.coerceIn(0, 4_500) else 0

class LiveFeed(context: Context, private val car: CarSignals) : Feed {
    private val navi = PleosNavi(context)
    private val gps = GpsFix(context)
    private val voice = NaviVoice(context)
    private val apis = Apis()
    private val budget = context.getSharedPreferences("signal-budget", Context.MODE_PRIVATE)
    private var job: Job? = null
    private var stopped = false
    private val _data = MutableSharedFlow<DataEvent>(extraBufferCapacity = 16)
    override val data = _data.asSharedFlow()
    override val navMode = navi.navMode
    override val turn = navi.turn
    override val fix = gps.fix
    override val speed = car.speed
    override val wiperOn = car.wiperOn
    override val defrosterOn = car.defrosterOn
    override val naviSpeaking = voice.speaking
    override val label: StateFlow<String?> = MutableStateFlow(null)
    private val signalRegions = listOf("3100000000")
    private val weatherRoads = listOf("045")

    private fun pollsToday(now: Instant) = savedSignalPolls(budget.getString("day", null), budget.getInt("count", 0), now)

    fun restoreSignalBudget(engine: RoadAheadEngine, monoMs: Long, now: Instant) {
        val count = pollsToday(now)
        // shortcut: replay at most 4,500 polls; replace when the engine has a budget restore API.
        repeat(count) { engine.recordSignalPoll(monoMs, now) }
        Log.i("RoadAhead", "signal polls restored=$count")
    }

    private suspend fun attempt(source: String, block: suspend () -> Unit): Boolean = try {
        block(); true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("RoadAhead", "$source failed ${e.javaClass.simpleName}"); false
    }

    override fun start(scope: CoroutineScope, engine: RoadAheadEngine) {
        check(job == null && !stopped)
        val activeJob = SupervisorJob(scope.coroutineContext[Job]).also { job = it }
        val activeScope = CoroutineScope(scope.coroutineContext + activeJob)
        navi.start(); gps.start(); voice.start()
        if (BuildConfig.SIGNAL_KEY.isNotBlank()) {
            activeScope.launch {
                for (region in signalRegions) attempt("intersections") {
                    apis.intersections(region).forEach { _data.emit(DataEvent.Intersections(it)) }
                }
            }
            activeScope.launch {
                while (isActive) {
                    val now = Instant.now()
                    val monoMs = SystemClock.elapsedRealtime()
                    if (engine.shouldPollSignals(fix.value, monoMs, now)) {
                        for (region in signalRegions) attempt("signals") {
                            val count = pollsToday(now)
                            if (count >= 4_500) return@attempt
                            val saved = withContext(Dispatchers.IO) {
                                budget.edit().putString("day", signalBudgetDay(now)).putInt("count", count + 1).commit()
                            }
                            if (!saved) throw IOException("signal budget not saved")
                            engine.recordSignalPoll(monoMs, now)
                            Log.i("RoadAhead", "signal polls today=${count + 1}")
                            val json = apis.signals(region)
                            _data.emit(DataEvent.Signals(json, SystemClock.elapsedRealtime()))
                        }
                    }
                    delay(250)
                }
            }
        } else Log.w("RoadAhead", "signal key missing")
        if (BuildConfig.WEATHER_KEY.isNotBlank()) activeScope.launch {
            while (isActive) {
                val success = attempt("weather") {
                    val hazards = weatherRoads.flatMap { apis.weather(it, Instant.now()) }
                    _data.emit(DataEvent.Weather(hazards))
                }
                delay(if (success) 300_000 else 60_000)
            }
        }
        if (BuildConfig.DISASTER_KEY.isNotBlank()) activeScope.launch {
            while (isActive) {
                val current = fix.value
                if (current == null) { delay(1_000); continue }
                attempt("events") { _data.emit(DataEvent.Events(apis.events(current, Instant.now()))) }
                attempt("disasters") { _data.emit(DataEvent.Disasters(apis.disasters(current, Instant.now()))) }
                delay(120_000)
            }
        }
    }

    override fun stop() {
        if (stopped) return
        stopped = true
        job?.cancel()
        navi.stop(); gps.stop(); voice.stop()
    }
}
