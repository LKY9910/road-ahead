package com.roadahead.core.parking

import com.roadahead.core.model.*

fun parseParkingAsset(tsv: String): List<ParkingLot> = tsv.lineSequence().withIndex().mapNotNull { (index, line) ->
    if (line.isBlank() || (index == 0 && line.startsWith('#'))) return@mapNotNull null
    val fields = line.split('\t').map(String::trim)
    if (fields.size != 6) return@mapNotNull null
    try {
        val (id, name, latitude, longitude) = fields
        val lat = latitude.toDouble()
        val lon = longitude.toDouble()
        require(id.isNotEmpty() && name.isNotEmpty() && lat.isFinite() && lon.isFinite() &&
            lat in -90.0..90.0 && lon in -180.0..180.0 && lat != 0.0 && lon != 0.0)
        ParkingLot(id, name, GeoPoint(lat, lon), null, null,
            fields[4].takeIf(String::isNotEmpty)?.toInt(), fields[5].takeIf(String::isNotEmpty)?.toInt())
    } catch (_: IllegalArgumentException) { null }
}.toList()
