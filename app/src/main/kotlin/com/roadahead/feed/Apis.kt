package com.roadahead.feed

import android.util.Log
import com.roadahead.BuildConfig
import com.roadahead.core.hazard.*
import com.roadahead.core.forecast.*
import com.roadahead.core.model.*
import com.roadahead.core.parking.*
import com.roadahead.core.signal.parseIntersectionPage
import com.roadahead.core.signal.parseTotalCount
import com.roadahead.core.tour.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun tourCommonParams() = mapOf("MobileOS" to "AND", "MobileApp" to "RoadAhead", "_type" to "json", "pageNo" to "1")
internal fun tourPlaceParams(point: GeoPoint): Map<String, String> = tourCommonParams() + mapOf(
    "mapX" to String.format(Locale.ROOT, "%.3f", point.lon), "mapY" to String.format(Locale.ROOT, "%.3f", point.lat),
    "radius" to "3000", "arrange" to "E", "numOfRows" to "300")
internal fun tourFestivalParams(arrival: LocalDate): Map<String, String> {
    val date = arrival.format(DateTimeFormatter.BASIC_ISO_DATE)
    return tourCommonParams() + mapOf("eventStartDate" to date, "eventEndDate" to date, "numOfRows" to "1000")
}

internal fun forecastParams(point: GeoPoint, nowKst: LocalDateTime): Map<String, String> {
    val base = forecastBase(nowKst)
    val (x, y) = kmaGrid(point)
    return mapOf("pageNo" to "1", "numOfRows" to "300", "dataType" to "JSON",
        "base_date" to base.format(DateTimeFormatter.BASIC_ISO_DATE),
        "base_time" to base.format(DateTimeFormatter.ofPattern("HHmm")), "nx" to "$x", "ny" to "$y")
}

internal fun seoulUrl(key: String): String =
    "http://openapi.seoul.go.kr:8088/${URLEncoder.encode(key, "UTF-8")}/json/GetParkingInfo/1/1000/"

internal fun maskedSeoulUrl(url: String): String =
    url.replace(Regex("(?i)(https?://openapi\\.seoul\\.go\\.kr(?::[0-9]+)?/)[^/]+"), "$1***")

internal fun requestUrl(endpoint: String, params: Map<String, String>): String = endpoint + "?" +
    params.entries.joinToString("&") { (name, raw) ->
        val value = if (name == "serviceKey" && '%' in raw) URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") else raw
        "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }

internal fun maskedUrl(url: String): String = url.replace(Regex("(?i)(serviceKey|apiKey|authKey)=[^&]*"), "$1=***")
internal fun weatherProduced(slot: String): Instant = LocalDateTime.parse(slot,
    DateTimeFormatter.ofPattern("yyyyMMddHHmm")).toInstant(ZoneOffset.UTC)

class Apis {
    suspend fun tourPlaces(point: GeoPoint): List<TourItem> = parseTourPlaces(get(
        "https://apis.data.go.kr/B551011/KorService2/locationBasedList2",
        tourPlaceParams(point) + ("serviceKey" to BuildConfig.SIGNAL_KEY)).toString(Charsets.UTF_8))

    suspend fun tourFestivals(arrival: LocalDate): List<TourItem> = parseTourFestivals(get(
        "https://apis.data.go.kr/B551011/KorService2/searchFestival2",
        tourFestivalParams(arrival) + ("serviceKey" to BuildConfig.SIGNAL_KEY)).toString(Charsets.UTF_8))

    suspend fun tourPetPlaces(point: GeoPoint): List<TourItem> = parseTourPlaces(get(
        "https://apis.data.go.kr/B551011/KorPetTourService2/locationBasedList2",
        tourPlaceParams(point) + mapOf("numOfRows" to "100", "serviceKey" to BuildConfig.SIGNAL_KEY)).toString(Charsets.UTF_8))

    suspend fun tourPetInfo(id: String): PetInfo? = parsePetInfo(get(
        "https://apis.data.go.kr/B551011/KorPetTourService2/detailPetTour2",
        tourCommonParams() + mapOf("contentId" to id, "serviceKey" to BuildConfig.SIGNAL_KEY)).toString(Charsets.UTF_8))

    suspend fun tourFacilities(id: String): Set<Facility> = parseFacilities(get(
        "https://apis.data.go.kr/B551011/KorWithService2/detailWithTour2",
        tourCommonParams() + mapOf("contentId" to id, "serviceKey" to BuildConfig.SIGNAL_KEY)).toString(Charsets.UTF_8))

    suspend fun forecast(point: GeoPoint, arrival: LocalDateTime): BadWeather? {
        val params = forecastParams(point, LocalDateTime.now(ZoneId.of("Asia/Seoul")))
        val result = arrivalWeather(parseForecast(get(
            "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst",
            params + ("serviceKey" to BuildConfig.SIGNAL_KEY)).toString(Charsets.UTF_8)), arrival)
        Log.i("RoadAhead", "forecast base=${params["base_date"]}/${params["base_time"]} grid=${params["nx"]},${params["ny"]} arrival=$arrival weather=$result")
        return result
    }

    suspend fun seoulParking(): List<SeoulLot> {
        check(BuildConfig.SEOUL_KEY.isNotBlank()) { "API key missing" }
        return parseSeoulParking(getUrl(seoulUrl(BuildConfig.SEOUL_KEY)).toString(Charsets.UTF_8))
    }

    private suspend fun get(endpoint: String, params: Map<String, String>): ByteArray {
        check(params.filterKeys { it in setOf("serviceKey", "apiKey", "authKey") }.values.all { it.isNotBlank() }) { "API key missing" }
        return getUrl(requestUrl(endpoint, params))
    }

    private suspend fun getUrl(url: String): ByteArray = withContext(Dispatchers.IO) {
        Log.i("RoadAhead", "GET ${maskedSeoulUrl(maskedUrl(url))}")
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
