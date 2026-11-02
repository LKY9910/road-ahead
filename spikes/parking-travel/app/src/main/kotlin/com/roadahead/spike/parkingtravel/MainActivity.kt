package com.roadahead.spike.parkingtravel

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

class MainActivity : Activity() {
    private lateinit var navi: NaviHelper
    private lateinit var output: TextView
    private var connected = false
    private val events = object : NaviHelperEventListener {
        override fun onDestinationInfo(info: DestinationInfo) = record("DESTINATION $info")
        override fun onDestinationChanged() = record("DESTINATION_CHANGED")
        override fun onNavigationStatus(status: NavigationStatus) = record("NAV_STATUS $status")
        override fun onRouteStarted(info: RouteStartInfo) = record("ROUTE_STARTED $info")
        override fun onRouteEnded() = record("ROUTE_ENDED")
        override fun onRouteCancelled() = record("ROUTE_CANCELLED")
        override fun onRoutingCompleted() = record("ROUTING_COMPLETED")
        override fun onReRouteEnded() = record("REROUTE_ENDED")
        override fun onRouteStateInfo(info: RouteStateInfo) = record("ROUTE_STATE $info")
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) = record("LOCATION $info")
        override fun onBookmarkInfo(info: List<BookmarkInfo>) = record("BOOKMARK $info")
        override fun onBookMarkChanged() = record("BOOKMARK_CHANGED")
        override fun onRecentDestinationInfo(info: List<RecentDestinationInfo>) = record("RECENT $info")
        override fun onError(code: NaviErrorCode) = record("ERROR $code")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply { textSize = 16f }
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (op in listOf("query", "route", "cancel", "mark")) {
            panel.addView(Button(this).apply { text = op; setOnClickListener { run(op, intent) } })
        }
        panel.addView(output)
        setContentView(ScrollView(this).apply { addView(panel) })
        record("CREATE")
        navi = NaviHelper(this)
        navi.initialize(onConnected = {
            connected = true
            record("CONNECTED")
            navi.addListener(events)
            run(intent.getStringExtra("op") ?: "query", intent)
        }, onFailed = { record("CONNECT_FAILED $it") })
    }

    private fun record(message: String) {
        val line = "wallMs=${System.currentTimeMillis()} monoMs=${SystemClock.elapsedRealtime()} $message"
        Log.i("T23", line)
        runOnUiThread { output.append("$line\n") }
    }

    private fun run(op: String, command: Intent) {
        record("OP $op label=${command.getStringExtra("label") ?: ""}")
        if (!connected) { record("NOT_CONNECTED"); return }
        try {
            when (op) {
                "query" -> {
                    record("QUERY_BEGIN")
                    navi.getDestinationInfo()
                    navi.getRouteStateInfo()
                    navi.getCurrentLocationInfo()
                    navi.getBookmarkInfo()
                    navi.getRecentDestinationInfo()
                }
                "route" -> {
                    val latitude = command.getStringExtra("lat")?.toDouble() ?: 37.394776
                    val longitude = command.getStringExtra("lon")?.toDouble() ?: 127.111149
                    record("REQUEST_ROUTE latitude=$latitude longitude=$longitude")
                    navi.requestRoute(RouteInfo(latitude = latitude, longitude = longitude,
                        poiName = command.getStringExtra("name") ?: "판교역", poiId = "", poiSubId = "",
                        address = "경기도 성남시"))
                }
                "cancel" -> navi.cancelRoute()
            }
        } catch (error: Exception) { record("FAIL ${error.javaClass.simpleName}: ${error.message}") }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        run(intent.getStringExtra("op") ?: "mark", intent)
    }

    override fun onDestroy() {
        if (connected) navi.removeListener(events)
        navi.release()
        record("DESTROY")
        super.onDestroy()
    }
}
