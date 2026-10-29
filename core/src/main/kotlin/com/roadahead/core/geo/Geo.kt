package com.roadahead.core.geo

import com.roadahead.core.model.*
import kotlin.math.*

fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val latA = Math.toRadians(a.lat)
    val latB = Math.toRadians(b.lat)
    val dLat = latB - latA
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2).pow(2) + cos(latA) * cos(latB) * sin(dLon / 2).pow(2)
    return 2 * 6_371_000.0 * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

fun bearingDegrees(from: GeoPoint, to: GeoPoint): Double {
    val fromLat = Math.toRadians(from.lat)
    val toLat = Math.toRadians(to.lat)
    val dLon = Math.toRadians(to.lon - from.lon)
    val y = sin(dLon) * cos(toLat)
    val x = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLon)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}

fun angleDiff(a: Double, b: Double): Double {
    val difference = abs((a % 360.0) - (b % 360.0)) % 360.0
    return min(difference, 360.0 - difference)
}

fun compassOf(bearingDeg: Double): Compass8 {
    val normalized = (bearingDeg % 360.0 + 360.0) % 360.0
    return Compass8.entries[((normalized + 22.5) / 45.0).toInt() % 8]
}

fun signalDirection(vehicleBearingDeg: Double, convention: DirectionConvention): Compass8 =
    compassOf(vehicleBearingDeg + if (convention == DirectionConvention.APPROACH) 180.0 else 0.0)

fun aheadDistance(fix: Fix, target: GeoPoint, coneDeg: Double): Double? {
    val bearing = fix.bearingDeg ?: return null
    if (angleDiff(bearing, bearingDegrees(fix.point, target)) > coneDeg) return null
    return distanceMeters(fix.point, target)
}

fun segmentAheadDistance(fix: Fix, from: GeoPoint, to: GeoPoint, coneDeg: Double, alignDeg: Double): Double? {
    val bearing = fix.bearingDeg ?: return null
    if (angleDiff(bearing, bearingDegrees(from, to)) > alignDeg) return null
    val midpoint = GeoPoint((from.lat + to.lat) / 2, (from.lon + to.lon) / 2)
    return listOf(from, midpoint, to).mapNotNull { aheadDistance(fix, it, coneDeg) }.minOrNull()
}
