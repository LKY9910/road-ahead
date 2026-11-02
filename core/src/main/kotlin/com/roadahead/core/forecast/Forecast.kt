package com.roadahead.core.forecast

import com.roadahead.core.model.ApiException
import com.roadahead.core.model.GeoPoint
import kotlinx.serialization.json.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.*

fun kmaGrid(point: GeoPoint): Pair<Int, Int> {
    val rad = PI / 180.0
    val re = 6371.00877 / 5.0
    val slat1 = 30.0 * rad
    val slat2 = 60.0 * rad
    val sn = ln(cos(slat1) / cos(slat2)) /
        ln(tan(PI / 4 + slat2 / 2) / tan(PI / 4 + slat1 / 2))
    val sf = tan(PI / 4 + slat1 / 2).pow(sn) * cos(slat1) / sn
    val ro = re * sf / tan(PI / 4 + 38.0 * rad / 2).pow(sn)
    val ra = re * sf / tan(PI / 4 + point.lat * rad / 2).pow(sn)
    var theta = (point.lon - 126.0) * rad
    if (theta > PI) theta -= 2 * PI
    if (theta < -PI) theta += 2 * PI
    theta *= sn
    return floor(ra * sin(theta) + 43 + 0.5).toInt() to
        floor(ro - ra * cos(theta) + 136 + 0.5).toInt()
}

fun forecastBase(nowKst: LocalDateTime): LocalDateTime {
    val latest = nowKst.minusMinutes(10).truncatedTo(ChronoUnit.HOURS)
    return latest.minusHours(((latest.hour + 1) % 3).toLong())
}

data class HourForecast(val pty: Int?, val pop: Int?, val tmp: Double?)

fun parseForecast(json: String): Map<LocalDateTime, HourForecast> = try {
    val response = Json.parseToJsonElement(json).jsonObject.getValue("response").jsonObject
    if (response.getValue("header").jsonObject.getValue("resultCode").jsonPrimitive.content != "00")
        throw ApiException("result")
    val rows = response.getValue("body").jsonObject.getValue("items").jsonObject.getValue("item").jsonArray
    rows.map { it.jsonObject }.filter { it.getValue("category").jsonPrimitive.content in setOf("PTY", "POP", "TMP") }
        .groupBy {
            LocalDateTime.parse(it.getValue("fcstDate").jsonPrimitive.content +
                it.getValue("fcstTime").jsonPrimitive.content, DateTimeFormatter.ofPattern("uuuuMMddHHmm"))
        }.mapValues { (_, values) ->
            val categories = values.associate { it.getValue("category").jsonPrimitive.content to
                it.getValue("fcstValue").jsonPrimitive.content }
            HourForecast(categories["PTY"]?.toIntOrNull(), categories["POP"]?.toIntOrNull(),
                categories["TMP"]?.toDoubleOrNull()?.takeIf { it.isFinite() })
        }
} catch (error: ApiException) {
    throw error
} catch (_: Exception) {
    throw ApiException("parse")
}

enum class BadWeather(val word: String) {
    RAIN("비"), RAIN_SNOW("비나 눈"), SNOW("눈"), SHOWER("소나기"), HEAT("무더위"), COLD("강추위")
}

fun badWeather(hour: HourForecast?): BadWeather? = when (hour?.pty) {
    1 -> BadWeather.RAIN
    2 -> BadWeather.RAIN_SNOW
    3 -> BadWeather.SNOW
    4 -> BadWeather.SHOWER
    else -> when {
        (hour?.pop ?: 0) >= 60 -> BadWeather.RAIN
        hour?.tmp != null && hour.tmp >= 33 -> BadWeather.HEAT
        hour?.tmp != null && hour.tmp <= -5 -> BadWeather.COLD
        else -> null
    }
}

fun arrivalWeather(forecast: Map<LocalDateTime, HourForecast>, arrival: LocalDateTime): BadWeather? =
    badWeather(forecast[arrival.truncatedTo(ChronoUnit.HOURS)])
