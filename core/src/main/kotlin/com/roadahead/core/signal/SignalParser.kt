package com.roadahead.core.signal

import com.roadahead.core.model.*
import kotlinx.serialization.json.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val signalTime = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

private fun JsonObject.text(name: String): String = get(name)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun <T> signalBody(json: String, read: (JsonObject) -> T): T = try {
    val root = Json.parseToJsonElement(json).jsonObject
    val code = root.getValue("header").jsonObject.text("resultCode")
    if (code != "K0" && code != "K3") throw ApiException(code)
    read(if (code == "K3") JsonObject(emptyMap()) else root.getValue("body").jsonObject)
} catch (e: ApiException) {
    throw e
} catch (_: Exception) {
    throw ApiException("parse")
}

private fun <T : Any> JsonObject.items(read: (JsonObject) -> T): List<T> {
    val items = get("items")?.jsonObject?.get("item")?.jsonArray.orEmpty()
    var failures = 0
    val result = items.mapNotNull {
        try {
            read(it.jsonObject)
        } catch (_: Exception) {
            failures++
            null
        }
    }
    if (items.isNotEmpty() && failures == items.size) throw ApiException("parse")
    return result
}

fun parseSignalPage(json: String, receivedMonoMs: Long): List<SignalSnapshot> = signalBody(json) { body ->
    body.items { item ->
        val signals = Compass8.entries.flatMap { approach ->
            Movement.entries.mapNotNull { movement ->
                val prefix = approach.prefix + movement.code
                val state = item.text("${prefix}SttsNm")
                if (state.isBlank()) null else DirectionSignal(
                    approach, movement, colorOf(state), item.text("${prefix}RmndCs").toLongOrNull()
                )
            }
        }
        SignalSnapshot(item.getValue("stdgCd").jsonPrimitive.content, item.getValue("crsrdId").jsonPrimitive.content,
            LocalDateTime.parse(item.text("totDt"), signalTime), receivedMonoMs, signals)
    }
}

fun parseIntersectionPage(json: String): List<Intersection> = signalBody(json) { body ->
    body.items { item ->
        Intersection(item.getValue("stdgCd").jsonPrimitive.content, item.getValue("crsrdId").jsonPrimitive.content,
            item.text("crsrdNm"), GeoPoint(item.text("mapCtptIntLat").toDouble(), item.text("mapCtptIntLot").toDouble()))
    }
}

fun parseTotalCount(json: String): Int = signalBody(json) { it.text("totalCount").toIntOrNull() ?: 0 }

fun colorOf(stateName: String): LightColor = when (stateName) {
    "stop-Then-Proceed", "stop-And-Remain" -> LightColor.RED
    "pre-Movement", "permissive-Movement-Allowed", "protected-Movement-Allowed" -> LightColor.GREEN
    "permissive-clearance", "protected-clearance", "caution-Conflicting-Traffic" -> LightColor.YELLOW
    else -> LightColor.UNKNOWN
}
