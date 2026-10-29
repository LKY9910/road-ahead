package com.roadahead.core.hazard

import com.roadahead.core.model.*
import kotlinx.serialization.json.*
import java.time.Instant

private fun JsonObject.text(name: String): String = get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun itsItems(json: String, read: (JsonObject) -> Hazard?): List<Hazard> = try {
    val root = Json.parseToJsonElement(json).jsonObject
    val code = root.getValue("header").jsonObject.text("resultCode")
    if (code != "0") throw ApiException(code)
    val body = root.getValue("body").jsonObject
    val items = body["items"]?.jsonArray.orEmpty()
    var failures = 0
    val hazards = items.mapNotNull {
        try {
            read(it.jsonObject)
        } catch (_: Exception) {
            failures++
            null
        }
    }
    if (items.isNotEmpty() && failures == items.size) throw ApiException("parse")
    hazards
} catch (e: ApiException) {
    throw e
} catch (_: Exception) {
    throw ApiException("parse")
}

fun parseItsEvents(json: String, produced: Instant): List<Hazard> = itsItems(json) { item ->
    val type = item.text("eventType")
    val kind = when (type) {
        "공사" -> HazardKind.CONSTRUCTION
        "교통사고" -> HazardKind.ACCIDENT
        else -> return@itsItems null
    }
    val point = GeoPoint(item.text("coordY").toDouble(), item.text("coordX").toDouble())
    val detail = item.text("lanesBlocked").trim()
    val fullClosure = item.text("lanesBlockType") == "4" || detail.contains("전면")
    Hazard("evt:${item.text("linkId")}:${item.text("startDate")}:$type", kind,
        if (fullClosure) Severity.DANGER else Severity.CAUTION, point, point, detail, fullClosure, produced)
}

fun parseItsDisasters(json: String, produced: Instant): List<Hazard> = itsItems(json) { item ->
    val type = item.text("eventType")
    val detail = when (type) {
        "D03" -> "지하차도 침수"
        "D04" -> "하천 범람"
        "D06" -> "땅꺼짐"
        "D07" -> "산불"
        else -> return@itsItems null
    }
    val status = item.text("status")
    if ((type == "D06" && status == "3") || (type == "D07" && status in listOf("2", "3"))) return@itsItems null
    val points = item.text("locationInfo").split(',').map { coordinates ->
        val pair = coordinates.trim().split(Regex("\\s+"))
        require(pair.size == 2)
        GeoPoint(pair[1].toDouble(), pair[0].toDouble())
    }
    Hazard("dis:${item.text("eventId")}", HazardKind.DISASTER, Severity.DANGER,
        points.first(), points.last(), detail, false, produced)
}
