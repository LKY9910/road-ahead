package com.roadahead.car

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.constants.*
import ai.pleos.playground.navi.data.*
import com.roadahead.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

internal fun navigationDestination(lat: Double, lon: Double, name: String?, remainM: Double, remainSec: Int): Destination? {
    if (name.isNullOrBlank() || !lat.isFinite() || !lon.isFinite() || lat == 0.0 || lon == 0.0 ||
        lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return Destination(String.format(Locale.ROOT, "%.5f,%.5f", lat, lon), name, GeoPoint(lat, lon), remainM, remainSec)
}

class PleosNavi(context: Context) {
    private val helper = NaviHelper(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _navMode = MutableStateFlow(NavMode.UNKNOWN)
    private val _turn = MutableStateFlow<Turn?>(null)
    private val _destination = MutableStateFlow<Destination?>(null)
    val navMode = _navMode.asStateFlow()
    val turn = _turn.asStateFlow()
    val destination = _destination.asStateFlow()
    private var closed = false
    private val listener = object : NaviHelperEventListener {
        override fun onNavigationStatus(status: NavigationStatus) {
            _navMode.value = when (status) {
                NavigationStatus.ROUTE_DRIVE -> NavMode.ROUTE
                NavigationStatus.FREE_DRIVE -> NavMode.FREE
                else -> NavMode.UNKNOWN
            }
            if (_navMode.value != NavMode.ROUTE) _turn.value = null
            Log.i("RoadAhead", "navMode=${_navMode.value}")
        }
        override fun onTBTInfo(info: List<TBTInfo>) {
            _turn.value = info.firstOrNull()?.let {
                Turn(when (it.type) {
                    TurnType.TURN_STRAIGHT -> Movement.STRAIGHT
                    TurnType.TURN_LEFT -> Movement.LEFT
                    TurnType.TURN_UTURN -> Movement.UTURN
                    else -> null
                }, it.distance)
            }
        }
        override fun onRouteStarted(info: RouteStartInfo) { scope.launch { requestDestination() } }
        override fun onDestinationInfo(info: DestinationInfo) {
            _destination.value = navigationDestination(info.latitude, info.longitude, info.name,
                info.remainDistance.toDouble(), info.remainTime)
            Log.i("RoadAhead", "destination key=${_destination.value?.key}")
        }
        override fun onRouteCancelled() { _turn.value = null; _navMode.value = NavMode.FREE; _destination.value = null }
        override fun onRouteStateInfo(info: RouteStateInfo) {
            _navMode.value = when (info.naviMode) {
                NaviMode.SAFE_GUIDING -> NavMode.FREE
                NaviMode.ROUTE_GUIDING -> NavMode.ROUTE
                else -> NavMode.UNKNOWN
            }
        }
        override fun onError(code: NaviErrorCode) {
            _navMode.value = NavMode.UNKNOWN; _turn.value = null; _destination.value = null
            Log.w("RoadAhead", "navi failed $code")
        }
    }

    fun start() = helper.initialize(onConnected = {
        if (!closed) {
            helper.addListener(listener)
            scope.launch {
                navMode.collectLatest { mode ->
                    if (mode == NavMode.ROUTE) while (isActive) {
                        requestDestination()
                        delay(5_000)
                    }
                }
            }
            helper.getRouteStateInfo()
            helper.getTBTInfo()
        }
    }, onFailed = { Log.w("RoadAhead", "navi initialization failed $it") })

    private fun requestDestination() {
        if (closed) return
        try { helper.getDestinationInfo() }
        catch (error: Exception) { Log.w("RoadAhead", "destination query failed ${error.javaClass.simpleName}") }
    }

    fun stop() { closed = true; scope.cancel(); helper.removeListener(listener); helper.release() }
}
