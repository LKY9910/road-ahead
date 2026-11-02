package com.roadahead

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.security.NetworkSecurityPolicy
import android.util.Log
import com.roadahead.car.CarSignals
import com.roadahead.car.Speech
import com.roadahead.core.*
import com.roadahead.core.advice.SignalConfig
import com.roadahead.core.model.*
import com.roadahead.core.signal.parseIntersectionPage
import com.roadahead.feed.*
import com.roadahead.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.ZoneId

data class GuidanceState(
    val screen: Screen = Screen(null, null, false),
    val parked: Boolean = false,
    val speechAvailable: Boolean = false,
    val note: String? = null,
    val running: Boolean = false,
    val overlayPermissionMissing: Boolean = false
)

class RoadAheadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var car: CarSignals? = null
    private var speech: Speech? = null
    private var feed: Feed? = null
    private var inputJob: Job? = null
    private var runJob: Job? = null
    private var logOnlySpeech = false
    private var engine: RoadAheadEngine? = null
    private var questionNumber = 0
    private var questionKey: String? = null
    private val overlay by lazy { OverlayCard(this) }
    private var current: GuidanceState
        get() = mutableState.value
        set(value) { mutableState.value = value }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            val policy = NetworkSecurityPolicy.getInstance()
            Log.i("RoadAhead", "seoul cleartext=${policy.isCleartextTrafficPermitted("openapi.seoul.go.kr")} other=${policy.isCleartextTrafficPermitted("example.com")}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_ASK_COMPANIONS) {
            if (current.parked) engine?.let {
                questionKey?.let(it::withdraw)
                val key = "companions:ask:${++questionNumber}"
                questionKey = key
                it.announce(Advice(key, Priority.NORMAL, "누구와 함께 가세요? 화면에서 골라 주세요."))
            }
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_WITHDRAW_COMPANIONS) {
            questionKey?.let { engine?.withdraw(it) }
            questionKey = null
            return START_NOT_STICKY
        }
        logOnlySpeech = BuildConfig.DEBUG && intent?.getBooleanExtra("logOnlySpeech", false) == true
        showNotification()
        if (!Settings.canDrawOverlays(this)) {
            current = current.copy(overlayPermissionMissing = true)
            stopSelf()
        } else if (car == null) startInputs() else restartFeed()
        return START_NOT_STICKY
    }

    private fun showNotification() {
        val channel = NotificationChannel("road_ahead", getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).putExtra("logOnlySpeech", logOnlySpeech), pendingFlags)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, RoadAheadService::class.java).setAction(ACTION_STOP), pendingFlags)
        val notification = Notification.Builder(this, "road_ahead")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.guidance_running, getString(R.string.app_name)))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.stop_guidance), stop).build())
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(25, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(25, notification)
        }
    }

    private fun startInputs() {
        stopInputs()
        if (REQUIRED_PERMISSIONS.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            current = current.copy(note = "위치·차량 속도 권한이 필요해요")
            stopSelf()
            return
        }
        current = current.copy(running = true, overlayPermissionMissing = false)
        val signals = CarSignals(this).also { car = it }
        val voice = Speech(this).also { speech = it }
        inputJob = serviceScope.launch {
            launch { signals.parked.collect { current = current.copy(parked = it) } }
            launch { voice.available.collect { current = current.copy(speechAvailable = it) } }
        }
        signals.start(serviceScope)
        restartFeed()
    }

    private fun restartFeed() {
        stopFeed()
        overlay.show(null)
        current = current.copy(screen = Screen(null, null, false), note = null)
        val signals = car ?: return
        val voice = speech ?: return
        val prefs = readPrefs(this)
        val companions = prefs.companions.orEmpty()
        val travelMode = prefs.travelMode || prefs.mode == AppMode.DEMO_E
        val convention = if (prefs.mode == AppMode.LIVE) prefs.convention else DirectionConvention.APPROACH
        val engine = RoadAheadEngine(EngineConfig(signal = SignalConfig(convention = convention))).also { this.engine = it }
        val activeFeed: Feed = when (prefs.mode) {
            AppMode.LIVE -> LiveFeed(this, signals).also {
                it.restoreSignalBudget(engine, SystemClock.elapsedRealtime(), Instant.now())
            }
            AppMode.DEMO_A -> ReplayFeed(this, "demo/scene-a.jsonl")
            AppMode.DEMO_B -> ReplayFeed(this, "demo/scene-b.jsonl")
            AppMode.DEMO_C -> ReplayFeed(this, "demo/scene-c.jsonl")
            AppMode.DEMO_E -> ReplayFeed(this, "demo/scene-e.jsonl")
        }
        feed = activeFeed
        runJob = serviceScope.launch {
            val intersections = mutableMapOf<String, Intersection>()
            launch { activeFeed.speed.collect { (mps, monoMs) -> engine.onSpeed(mps, monoMs) } }
            launch {
                activeFeed.data.collect { event ->
                    handleDataEvent({ Log.w("RoadAhead", "data rejected ${it.javaClass.simpleName}") }) {
                        when (event) {
                            is DataEvent.Intersections -> {
                                parseIntersectionPage(event.json).forEach { intersections[it.key] = it }
                                engine.onIntersections(intersections.values.toList())
                                Log.i("RoadAhead", "intersections count=${intersections.size}")
                            }
                            is DataEvent.Signals -> {
                                engine.onSignalPage(event.json, event.receivedMonoMs)
                                val code = Json.parseToJsonElement(event.json).jsonObject["header"]?.jsonObject
                                    ?.get("resultCode")?.jsonPrimitive?.content
                                Log.i("RoadAhead", "signals code=${code?.takeIf { it.matches(Regex("K\\d{1,3}")) }}")
                            }
                            is DataEvent.Weather -> engine.onHazards(HazardSource.WEATHER, event.hazards)
                            is DataEvent.Events -> engine.onHazards(HazardSource.ITS_EVENT, event.hazards)
                            is DataEvent.Disasters -> engine.onHazards(HazardSource.ITS_DISASTER, event.hazards)
                            is DataEvent.ParkingLots -> engine.onParkingLots(event.destKey, event.lots)
                            is DataEvent.TourData -> engine.onTourData(event.destKey, event.data)
                        }
                    }
                }
            }
            activeFeed.start(this, engine)
            var lastScreen: Screen? = null
            while (isActive) {
                if (!Settings.canDrawOverlays(this@RoadAheadService)) {
                    overlay.close()
                    current = current.copy(overlayPermissionMissing = true)
                    stopSelf()
                    break
                }
                val now = Instant.now()
                val destination = activeFeed.destination.value
                engine.parkingRequest(destination, activeFeed.navMode.value, travelMode)?.let(activeFeed::requestParking)
                engine.tourRequest(destination, travelMode)?.let {
                    activeFeed.requestTour(it, now.plusSeconds(it.remainTimeSec?.toLong() ?: 0)
                        .atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime(), companions)
                }
                val result = engine.tick(Inputs(activeFeed.navMode.value, activeFeed.turn.value, activeFeed.fix.value,
                    activeFeed.wiperOn.value, activeFeed.defrosterOn.value, activeFeed.naviSpeaking.value, voice.speaking.value,
                    parked = activeFeed.parked.value, travelMode = travelMode, destination = destination),
                    SystemClock.elapsedRealtime(), now)
                current = current.copy(screen = result.screen, note = activeFeed.label.value)
                overlay.show(result.screen.card)
                if (lastScreen != result.screen) { Log.i("RoadAhead", "screen=${result.screen}"); lastScreen = result.screen }
                result.speak?.let { advice ->
                    Log.i("RoadAhead", "speak=${advice.text}")
                    if (!logOnlySpeech) voice.speak(advice.text)
                }
                delay(250)
            }
        }
    }

    private fun stopFeed() {
        runJob?.cancel(); runJob = null; feed?.stop(); feed = null
        engine = null; questionKey = null
    }
    private fun stopInputs() {
        stopFeed()
        inputJob?.cancel(); inputJob = null
        car?.stop(); car = null
        speech?.release(); speech = null
        current = current.copy(parked = false, speechAvailable = false, running = false)
    }

    override fun onDestroy() {
        stopInputs()
        overlay.close()
        serviceScope.cancel()
        current = GuidanceState(overlayPermissionMissing = !Settings.canDrawOverlays(this))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_STOP = "com.roadahead.STOP"
        const val ACTION_ASK_COMPANIONS = "com.roadahead.ASK_COMPANIONS"
        const val ACTION_WITHDRAW_COMPANIONS = "com.roadahead.WITHDRAW_COMPANIONS"
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION, "android.car.permission.CAR_SPEED")
        private val mutableState = MutableStateFlow(GuidanceState())
        val state = mutableState.asStateFlow()
    }
}

internal inline fun handleDataEvent(onRejected: (Exception) -> Unit, handle: () -> Unit) {
    try {
        handle()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        onRejected(e)
    }
}
