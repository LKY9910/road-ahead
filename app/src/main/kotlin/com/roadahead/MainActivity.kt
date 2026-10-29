package com.roadahead

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.roadahead.car.CarSignals
import com.roadahead.car.Speech
import com.roadahead.core.*
import com.roadahead.core.advice.SignalConfig
import com.roadahead.core.model.*
import com.roadahead.core.signal.parseIntersectionPage
import com.roadahead.feed.*
import com.roadahead.ui.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import java.time.Instant

class MainActivity : ComponentActivity() {
    private var car: CarSignals? = null
    private var speech: Speech? = null
    private var feed: Feed? = null
    private var inputJob: Job? = null
    private var runJob: Job? = null
    private var parked by mutableStateOf(false)
    private var speechAvailable by mutableStateOf(false)
    private var screen by mutableStateOf(Screen(null, null, false))
    private var note by mutableStateOf<String?>(null)
    private var prefs by mutableStateOf(Prefs(AppMode.LIVE, DirectionConvention.APPROACH, false))
    private val requiredPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION, "android.car.permission.CAR_SPEED")
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        Log.i("RoadAhead", "permissions=$it")
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) startInputs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = readPrefs(this)
        val missing = requiredPermissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissions.launch(missing.toTypedArray())
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var settingsOpen by remember { mutableStateOf(false) }
                val currentParked = parked
                LaunchedEffect(currentParked) { if (!currentParked) settingsOpen = false }
                Surface {
                    if (currentParked && settingsOpen) Settings(prefs, ::savePrefs, { settingsOpen = false })
                    else RoadAheadScreen(screen, AppStatus(prefs.mode.label.takeIf { prefs.mode == AppMode.LIVE }, speechAvailable, note),
                        currentParked, { settingsOpen = true })
                    if (currentParked && !prefs.disclaimerSeen) AlertDialog(
                        onDismissRequest = {}, text = { Text(getString(R.string.disclaimer)) },
                        confirmButton = { TextButton(onClick = {
                            savePrefs(prefs.copy(disclaimerSeen = true))
                        }) { Text("확인") } })
                }
            }
        }
    }

    private fun savePrefs(next: Prefs) {
        val restart = prefs.mode != next.mode || prefs.convention != next.convention
        prefs = next
        writePrefs(this, next)
        if (restart) restartFeed()
    }

    override fun onStart() { super.onStart(); startInputs() }

    private fun startInputs() {
        stopInputs()
        val signals = CarSignals(this).also { car = it }
        val voice = Speech(this).also { speech = it }
        inputJob = lifecycleScope.launch {
            launch { signals.parked.collect { parked = it } }
            launch { voice.available.collect { speechAvailable = it } }
        }
        signals.start(lifecycleScope)
        restartFeed()
    }

    private fun restartFeed() {
        stopFeed()
        screen = Screen(null, null, false)
        note = null
        val signals = car ?: return
        val voice = speech ?: return
        if (requiredPermissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
            note = "위치·차량 속도 권한이 필요해요"
            return
        }
        val convention = if (prefs.mode == AppMode.LIVE) prefs.convention else DirectionConvention.APPROACH
        val engine = RoadAheadEngine(EngineConfig(signal = SignalConfig(convention = convention)))
        val activeFeed: Feed = when (prefs.mode) {
            AppMode.LIVE -> LiveFeed(this, signals).also {
                it.restoreSignalBudget(engine, SystemClock.elapsedRealtime(), Instant.now())
            }
            AppMode.DEMO_A -> ReplayFeed(this, "demo/scene-a.jsonl")
            AppMode.DEMO_B -> ReplayFeed(this, "demo/scene-b.jsonl")
            AppMode.DEMO_C -> ReplayFeed(this, "demo/scene-c.jsonl")
        }
        feed = activeFeed
        runJob = lifecycleScope.launch {
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
                        }
                    }
                }
            }
            activeFeed.start(this, engine)
            var lastScreen: Screen? = null
            while (isActive) {
                val result = engine.tick(Inputs(activeFeed.navMode.value, activeFeed.turn.value, activeFeed.fix.value,
                    activeFeed.wiperOn.value, activeFeed.defrosterOn.value, activeFeed.naviSpeaking.value, voice.speaking.value),
                    SystemClock.elapsedRealtime(), Instant.now())
                screen = result.screen
                note = activeFeed.label.value
                if (lastScreen != screen) { Log.i("RoadAhead", "screen=$screen"); lastScreen = screen }
                result.speak?.let { advice ->
                    Log.i("RoadAhead", "speak=${advice.text}")
                    if (!(BuildConfig.DEBUG && intent.getBooleanExtra("logOnlySpeech", false))) voice.speak(advice.text)
                }
                delay(250)
            }
        }
    }

    private fun stopFeed() { runJob?.cancel(); runJob = null; feed?.stop(); feed = null }
    private fun stopInputs() {
        stopFeed()
        inputJob?.cancel(); inputJob = null
        car?.stop(); car = null
        speech?.release(); speech = null
        parked = false
        speechAvailable = false
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); restartFeed() }
    override fun onStop() {
        stopInputs()
        super.onStop()
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
