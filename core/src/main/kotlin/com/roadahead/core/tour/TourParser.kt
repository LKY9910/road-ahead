package com.roadahead.core.tour

import com.roadahead.core.model.*
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull.orEmpty().trim()

internal fun <T : Any> tourItems(json: String, read: (JsonObject) -> T?): List<T> = try {
    val response = Json.parseToJsonElement(json).jsonObject.getValue("response").jsonObject
    if (response.getValue("header").jsonObject.text("resultCode") != "0000") throw ApiException("result")
    val container = response.getValue("body").jsonObject.getValue("items")
    val rows = if (container is JsonPrimitive && container.isString && container.content.isBlank()) emptyList()
        else when (val item = container.jsonObject.getValue("item")) {
            is JsonArray -> item.toList()
            is JsonObject -> listOf(item)
            else -> throw ApiException("parse")
        }
    var failures = 0
    val result = rows.mapNotNull {
        try { read(it.jsonObject) } catch (_: Exception) { failures++; null }
    }
    if (rows.isNotEmpty() && failures == rows.size) throw ApiException("parse")
    result
} catch (error: ApiException) {
    throw error
} catch (_: Exception) {
    throw ApiException("parse")
}

private fun JsonObject.point(): GeoPoint {
    val lat = text("mapy").toDouble()
    val lon = text("mapx").toDouble()
    require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0 && lat != 0.0 && lon != 0.0)
    return GeoPoint(lat, lon)
}

private fun JsonObject.required(key: String) = text(key).also { require(it.isNotEmpty()) }

private fun JsonObject.date(key: String): LocalDate {
    val text = required(key)
    require(text.matches(Regex("\\d{8}")))
    return LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE)
}

fun parseTourPlaces(json: String): List<TourItem> = tourItems(json) { row ->
    val kind = when (row.text("contenttypeid")) {
        "12" -> TourKind.ATTRACTION
        "14" -> TourKind.CULTURE
        "28" -> TourKind.LEISURE
        else -> return@tourItems null
    }
    TourItem(row.required("contentid"), kind, row.required("title"), row.point(), null, null)
}

fun parseTourFestivals(json: String): List<TourItem> = tourItems(json) { row ->
    TourItem(row.required("contentid"), TourKind.FESTIVAL, row.required("title"), row.point(),
        row.date("eventstartdate"), row.date("eventenddate"))
}
