package com.roadahead.feed

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.roadahead.BuildConfig
import com.roadahead.car.*
import com.roadahead.core.RoadAheadEngine
import com.roadahead.core.geo.distanceMeters
import com.roadahead.core.forecast.BadWeather
import com.roadahead.core.model.*
import com.roadahead.core.parking.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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
    private var activeScope: CoroutineScope? = null
    private val parkingLots by lazy {
        context.assets.open("parking/lots.tsv").bufferedReader().use { parseParkingAsset(it.readText()) }
    }
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
    override val destination = navi.destination
    override val parked = car.parked
    private val signalRegions = listOf("3100000000")
    private val weatherRoads = listOf("045")

    override fun requestParking(dest: Destination) {
        activeScope?.launch {
            attempt("parking") {
                val nearby = withContext(Dispatchers.IO) {
                    parkingLots.filter { distanceMeters(dest.point, it.point) <= 500.0 }
                }
                var lots = nearby
                val seoul = BuildConfig.SEOUL_KEY.isNotBlank() && nearby.any { it.id in SEOUL_TO_TS.values }
                if (seoul) attempt("seoul parking") {
                    lots = applySeoul(nearby, apis.seoulParking(), Instant.now())
                }
                _data.emit(DataEvent.ParkingLots(dest.key, lots))
                Log.i("RoadAhead", "parking received key=${dest.key} lots=${lots.size} seoulCalls=${if (seoul) 1 else 0}")
            }
        }
    }

    override fun requestTour(dest: Destination, arrival: LocalDateTime, companions: Set<Companion>) {
        activeScope?.launch {
            attempt("tour") {
                val source = LiveTourSource(apis)
                val data = collectTour(source, dest, arrival, companions)
                _data.emit(DataEvent.TourData(dest.key, data))
                Log.i("RoadAhead", "tour received key=${dest.key} places=${data.places.size} festivals=${data.festivals.size} companions=$companions weather=${data.weather} calls=${source.calls}")
            }
        }
    }

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
        val activeScope = CoroutineScope(scope.coroutineContext + activeJob).also { this.activeScope = it }
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
        activeScope = null
        navi.stop(); gps.stop(); voice.stop()
    }
}

private class LiveTourSource(private val apis: Apis) : TourSource {
    val calls = mutableMapOf<String, Int>()
    private suspend fun <T> called(service: String, block: suspend () -> T): T {
        calls[service] = (calls[service] ?: 0) + 1
        return block()
    }
    override suspend fun places(point: GeoPoint) = called("KorService2") { apis.tourPlaces(point) }
    override suspend fun festivals(date: LocalDate) = called("KorService2") { apis.tourFestivals(date) }
    override suspend fun petPlaces(point: GeoPoint) = called("KorPetTourService2") { apis.tourPetPlaces(point) }
    override suspend fun petInfo(id: String) = called("KorPetTourService2") { apis.tourPetInfo(id) }
    override suspend fun facilities(id: String) = called("KorWithService2") { apis.tourFacilities(id) }
    override suspend fun weather(point: GeoPoint, arrival: LocalDateTime): BadWeather? =
        called("VilageFcstInfoService_2.0") { apis.forecast(point, arrival) }
}
