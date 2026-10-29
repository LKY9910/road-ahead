package com.roadahead.feed

import android.util.Log
import com.roadahead.BuildConfig
import com.roadahead.core.hazard.*
import com.roadahead.core.model.*
import com.roadahead.core.signal.parseIntersectionPage
import com.roadahead.core.signal.parseTotalCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.*
import java.time.format.DateTimeFormatter

internal fun requestUrl(endpoint: String, params: Map<String, String>): String = endpoint + "?" +
    params.entries.joinToString("&") { (name, raw) ->
        val value = if (name == "serviceKey" && '%' in raw) URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") else raw
        "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }

internal fun maskedUrl(url: String): String = url.replace(Regex("(?i)(serviceKey|apiKey|authKey)=[^&]*"), "$1=***")
internal fun weatherProduced(slot: String): Instant = LocalDateTime.parse(slot,
    DateTimeFormatter.ofPattern("yyyyMMddHHmm")).toInstant(ZoneOffset.UTC)

class Apis {
    private suspend fun get(endpoint: String, params: Map<String, String>): ByteArray = withContext(Dispatchers.IO) {
        check(params.filterKeys { it in setOf("serviceKey", "apiKey", "authKey") }.values.all { it.isNotBlank() }) { "API key missing" }
        val url = requestUrl(endpoint, params)
        Log.i("RoadAhead", "GET ${maskedUrl(url)}")
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            val status = connection.responseCode
            if (status != 200) throw IOException("HTTP $status")
            connection.inputStream.use { it.readBytes() }
        } finally { connection.disconnect() }
    }

    private fun signalParams(region: String, page: Int) = mapOf("serviceKey" to BuildConfig.SIGNAL_KEY,
        "stdgCd" to region, "pageNo" to page.toString(), "numOfRows" to "500", "type" to "json")

    suspend fun intersections(region: String): List<String> {
        val pages = mutableListOf<String>()
        var page = 1
        do {
            val json = get("https://apis.data.go.kr/B551982/rti/crsrd_map_info", signalParams(region, page)).toString(Charsets.UTF_8)
            val items = parseIntersectionPage(json)
            val total = parseTotalCount(json)
            if (items.isEmpty() && (page - 1) * 500 < total) throw ApiException("incomplete-map")
            pages += json
            page++
        } while ((page - 1) * 500 < total)
        return pages
    }

    suspend fun signals(region: String): String = get("https://apis.data.go.kr/B551982/rti/tl_drct_info",
        signalParams(region, 1)).toString(Charsets.UTF_8)

    suspend fun weather(road: String, now: Instant): List<Hazard> {
        for (slot in weatherRequestTimes(now)) {
            try {
                val zip = get("https://apihub.kma.go.kr/api/typ04/url/road_file_down.php",
                    mapOf("roadNum" to road, "tm" to slot, "authKey" to BuildConfig.WEATHER_KEY))
                if (zip.size >= 2 && zip[0] == 0x50.toByte() && zip[1] == 0x4b.toByte())
                    return parseWeatherZip(zip, weatherProduced(slot))
            } catch (e: IOException) {
                Log.w("RoadAhead", "weather slot=$slot failed ${e.javaClass.simpleName}")
            }
        }
        throw ApiException("weather-unavailable")
    }

    private fun bounds(fix: Fix) = mapOf("minX" to (fix.point.lon - .1).toString(), "maxX" to (fix.point.lon + .1).toString(),
        "minY" to (fix.point.lat - .1).toString(), "maxY" to (fix.point.lat + .1).toString())

    suspend fun events(fix: Fix, now: Instant): List<Hazard> = parseItsEvents(get("https://openapi.its.go.kr:9443/eventInfo",
        bounds(fix) + mapOf("apiKey" to BuildConfig.DISASTER_KEY, "type" to "all", "eventType" to "all", "getType" to "json"))
        .toString(Charsets.UTF_8), now)

    suspend fun disasters(fix: Fix, now: Instant): List<Hazard> {
        val date = now.atZone(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        return parseItsDisasters(get("https://openapi.its.go.kr:9443/disasterInfo", bounds(fix) + mapOf(
            "apiKey" to BuildConfig.DISASTER_KEY, "category" to "D", "eventType" to "all", "startDate" to date,
            "endDate" to date, "getType" to "json")).toString(Charsets.UTF_8), now)
    }
}
