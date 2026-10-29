package com.roadahead.spike

import android.app.Activity
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.*
import ai.pleos.playground.navi.constants.*
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var output: TextView
    private lateinit var vehicle: Vehicle
    private lateinit var navi: NaviHelper
    private lateinit var speech: SpeechProbe
    private lateinit var audio: AudioManager
    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private var restrictions: CarUxRestrictionsManager? = null
    private var speaking = false
    private var overlapArmed = false
    private var naviReady = false
    private val gps = object : LocationListener {
        override fun onLocationChanged(location: Location) = record("04 ANDROID_GPS lat=${location.latitude} lon=${location.longitude} bearing=${location.bearing} speed=${location.speed} accuracy=${location.accuracy} time=${location.time}")
    }
    private val playback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) = logPlayback(configs)
    }
    private val speed = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) = record("05 SPEED callback value=${value.value} status=${value.status} timestamp=${value.timestamp}")
        override fun onErrorEvent(propertyId: Int, areaId: Int) = record("05 SPEED error property=$propertyId area=$areaId")
    }
    private val events = object : NaviHelperEventListener {
        override fun onNavigationStatus(status: NavigationStatus) = record("04 NAV_STATUS $status")
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) = record("04 NAV_LOCATION $info heading=${info.heading}")
        override fun onTBTInfo(info: List<TBTInfo>) = record("04 TBT $info")
        override fun onRouteStateInfo(info: RouteStateInfo) {
            record("04 ROUTE_STATE $info")
            record("04 TRAFFIC_EVENTS ${info.trafficStatus?.trafficEventList}")
        }
        override fun onCautionPointInfo(info: List<CautionPoint>) = record("04 CAUTION $info")
        override fun onDestinationInfo(info: DestinationInfo) = record("04 DESTINATION $info")
        override fun onRouteStarted(info: RouteStartInfo) { record("04 ROUTE_STARTED $info"); query() }
        override fun onRouteCancelled() = record("04 ROUTE_CANCELLED")
        override fun onError(code: NaviErrorCode) = record("04 NAV_ERROR $code")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply { textSize = 16f; setTextIsSelectable(true) }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (op in listOf("query", "route", "cancel", "state", "tts")) {
            layout.addView(Button(this).apply { text = op; setOnClickListener { run(op) } })
        }
        layout.addView(output)
        setContentView(ScrollView(this).apply { addView(layout) })
        record("ENV multiwindow=$isInMultiWindowMode widthDp=${resources.configuration.screenWidthDp}")
        audio = getSystemService(AudioManager::class.java)
        audio.registerAudioPlaybackCallback(playback, Handler(Looper.getMainLooper()))
        logPlayback(audio.activePlaybackConfigurations)
        vehicle = Vehicle(this).also { it.initialize() }
        speech = SpeechProbe(this, ::record)
        navi = NaviHelper(this)
        navi.initialize(onConnected = {
            naviReady = true
            record("04 NAV_CONNECTED")
            navi.addListener(events)
            query()
        }, onFailed = { record("04 NAV_FAILED $it") })
        attempt("05 CAR") {
            car = Car.createCar(this, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (ready) {
                    attempt("05 SPEED") {
                        properties = connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                        val config = properties!!.getCarPropertyConfig(VehiclePropertyIds.PERF_VEHICLE_SPEED)
                        record("05 SPEED config=$config")
                        record("05 SPEED register=${properties!!.registerCallback(speed, VehiclePropertyIds.PERF_VEHICLE_SPEED, 5f)}")
                        readSpeed()
                    }
                    attempt("07 UX") {
                        restrictions = connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                        restrictions!!.registerListener { record("07 UX requiresDO=${it.isRequiresDistractionOptimization} active=${it.activeRestrictions}") }
                        record("07 UX current=${restrictions!!.currentCarUxRestrictions}")
                    }
                }
            }
        }
        attempt("04 GPS") { getSystemService(LocationManager::class.java).requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, gps) }
        scope.launch { delay(1500); state(); run(intent.getStringExtra("op") ?: "query") }
    }

    private fun record(message: String) {
        val line = "${SystemClock.elapsedRealtime()} $message"
        Log.i("T14", line)
        runOnUiThread { output.append("$line\n") }
    }
    private fun attempt(label: String, block: () -> Unit) {
        try { block() } catch (e: Exception) { record("$label FAIL ${e.javaClass.simpleName}: ${e.message}") }
    }
    private fun logPlayback(configs: List<AudioPlaybackConfiguration>) {
        record("09 AUDIO count=${configs.size} mode=${audio.mode} musicActive=${audio.isMusicActive} musicVolume=${audio.getStreamVolume(AudioManager.STREAM_MUSIC)}/${audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)} fixed=${audio.isVolumeFixed} carVolumePermission=${checkSelfPermission("android.car.permission.CAR_CONTROL_AUDIO_VOLUME")}")
        configs.forEach { record("09 AUDIO usage=${it.audioAttributes.usage} content=${it.audioAttributes.contentType} config=$it") }
        if (overlapArmed && configs.any { it.audioAttributes.usage == 12 }) {
            overlapArmed = false
            record("09 OVERLAP_TRIGGER navigation usage=12 active")
            run("tts")
        }
    }
    private fun readSpeed() = attempt("05 SPEED read") {
        record("05 SPEED read=${properties?.getProperty(Float::class.javaObjectType, VehiclePropertyIds.PERF_VEHICLE_SPEED, 0)}")
    }
    private fun state() {
        record("06 STATE_REQUEST")
        val fail: (Exception) -> Unit = { record("06 STATE_ERROR ${it.javaClass.simpleName}: ${it.message}") }
        vehicle.getWiper().checkWiperStateCapabilities({ config, areas -> record("06 WIPER capability=$config areas=$areas") }, fail)
        vehicle.getHVAC().checkHvacDefrosterCapabilities({ config -> record("06 DEFROSTER capability=$config") }, fail)
        for (window in listOf(WindowValue.FRONT_WINDSHIELD, WindowValue.REAR_WINDSHIELD)) {
            val area = WindowArea(window)
            vehicle.getWiper().getCurrentWiperState(area, { record("06 WIPER area=$window value=$it") }, fail)
            vehicle.getHVAC().getHvacDefrosterState(area, { record("06 DEFROSTER area=$window value=$it") }, fail)
        }
        vehicle.getLight().checkFrontFogLightStateCapabilities({ config, values -> record("06 FOG_FRONT capability=$config values=$values") }, fail)
        vehicle.getLight().checkRearFogLightStateCapabilities({ config, values -> record("06 FOG_REAR capability=$config values=$values") }, fail)
        vehicle.getLight().getCurrentFrontFogLightState({ record("06 FOG_FRONT value=$it") }, fail)
        vehicle.getLight().getCurrentRearFogLightState({ record("06 FOG_REAR value=$it") }, fail)
        readSpeed()
        logPlayback(audio.activePlaybackConfigurations)
    }
    private fun query() {
        if (!naviReady) { record("04 NAV_NOT_READY"); return }
        navi.getCurrentLocationInfo()
        navi.getTBTInfo()
        navi.getRouteStateInfo()
        navi.getDestinationInfo()
    }
    private fun run(op: String) {
        record("OP $op")
        attempt(op) {
            when (op) {
                "query" -> query()
                "route", "overlap" -> {
                    check(naviReady)
                    overlapArmed = op == "overlap"
                    record("04 ROUTE_REQUEST lat=35.512239 lon=129.430752")
                    navi.requestRoute(RouteInfo(latitude = 35.512239, longitude = 129.430752,
                        poiName = "오지벌사거리", poiId = "", poiSubId = "", address = "울산광역시"))
                }
                "cancel" -> if (naviReady) navi.cancelRoute()
                "state" -> state()
                "tts", "tts-nav" -> if (!speaking) scope.launch {
                    speaking = true
                    try { speech.tts(op == "tts-nav") } finally { speaking = false }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); run(intent.getStringExtra("op") ?: "query") }
    override fun onDestroy() {
        scope.cancel()
        speech.close()
        audio.unregisterAudioPlaybackCallback(playback)
        properties?.unregisterCallback(speed)
        restrictions?.unregisterListener()
        car?.disconnect()
        getSystemService(LocationManager::class.java).removeUpdates(gps)
        if (naviReady) navi.removeListener(events)
        navi.release()
        vehicle.release()
        super.onDestroy()
    }
}
