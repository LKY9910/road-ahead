package com.roadahead.car

import android.content.Context
import android.util.Log
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.constants.*
import ai.pleos.playground.navi.data.*
import com.roadahead.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PleosNavi(context: Context) {
    private val helper = NaviHelper(context)
    private val _navMode = MutableStateFlow(NavMode.UNKNOWN)
    private val _turn = MutableStateFlow<Turn?>(null)
    val navMode = _navMode.asStateFlow()
    val turn = _turn.asStateFlow()
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
        override fun onRouteCancelled() { _turn.value = null; _navMode.value = NavMode.FREE }
        override fun onRouteStateInfo(info: RouteStateInfo) {
            _navMode.value = when (info.naviMode) {
                NaviMode.SAFE_GUIDING -> NavMode.FREE
                NaviMode.ROUTE_GUIDING -> NavMode.ROUTE
                else -> NavMode.UNKNOWN
            }
        }
        override fun onError(code: NaviErrorCode) {
            _navMode.value = NavMode.UNKNOWN; _turn.value = null
            Log.w("RoadAhead", "navi failed $code")
        }
    }

    fun start() = helper.initialize(onConnected = {
        if (!closed) {
            helper.addListener(listener)
            helper.getRouteStateInfo()
            helper.getTBTInfo()
        }
    }, onFailed = { Log.w("RoadAhead", "navi initialization failed $it") })

    fun stop() { closed = true; helper.removeListener(listener); helper.release() }
}
