package com.roadahead.car

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import com.roadahead.core.geo.bearingDegrees
import com.roadahead.core.geo.distanceMeters
import com.roadahead.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun bearingForFix(previous: Fix?, point: GeoPoint, providerBearing: Double?): Double? = when {
    previous != null && distanceMeters(previous.point, point) >= 5 -> bearingDegrees(previous.point, point)
    else -> previous?.bearingDeg ?: providerBearing
}

class GpsFix(context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val _fix = MutableStateFlow<Fix?>(null)
    val fix = _fix.asStateFlow()
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val point = GeoPoint(location.latitude, location.longitude)
            val previous = _fix.value
            val bearing = bearingForFix(previous, point, location.bearing.toDouble().takeIf { location.hasBearing() })
            _fix.value = Fix(point, bearing, SystemClock.elapsedRealtime())
            Log.d("RoadAhead", "GPS bearing=$bearing")
        }
        override fun onProviderDisabled(provider: String) { _fix.value = null }
        override fun onProviderEnabled(provider: String) = Unit
        @Deprecated("Legacy provider callback")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }
    fun start() {
        try { manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000, 0f, listener) }
        catch (e: SecurityException) { Log.w("RoadAhead", "GPS permission denied") }
    }
    fun stop() { manager.removeUpdates(listener) }
}
