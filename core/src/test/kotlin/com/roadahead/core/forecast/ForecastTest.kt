package com.roadahead.core.forecast

import com.roadahead.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ForecastTest {
    @Test fun `grid for seoul city hall`() {
        assertEquals(60 to 127, kmaGrid(GeoPoint(37.5663, 126.9779)))
        // KMA grid latitude/longitude table (2610), Busan Metropolitan City, https://www.data.go.kr/data/15084084/openapi.do
        assertEquals(98 to 76, kmaGrid(GeoPoint(35.17701944, 129.0769528)))
    }

    @Test fun `forecast base time`() {
        for ((now, expected) in listOf(
            "2026-10-29T19:36" to "2026-10-29T17:00",
            "2026-10-29T17:09" to "2026-10-29T14:00",
            "2026-10-29T17:10" to "2026-10-29T17:00",
            "2026-10-30T02:05" to "2026-10-29T23:00"
        )) assertEquals(LocalDateTime.parse(expected), forecastBase(LocalDateTime.parse(now)))
    }

    @Test fun `rain at arrival from recorded forecast`() {
        val map = parseForecast(javaClass.getResource("/fixtures/forecast-20261029-1700.json")!!.readText())
        assertEquals(BadWeather.RAIN, arrivalWeather(map, LocalDateTime.parse("2026-10-29T19:40")))
        assertNull(arrivalWeather(map, LocalDateTime.parse("2026-10-28T19:40")))
    }

    @Test fun `bad weather thresholds`() {
        assertEquals(BadWeather.RAIN, badWeather(HourForecast(0, 60, 20.0)))
        assertNull(badWeather(HourForecast(0, 59, 32.9)))
        assertEquals(BadWeather.HEAT, badWeather(HourForecast(0, 10, 33.0)))
        assertEquals(BadWeather.COLD, badWeather(HourForecast(0, 10, -5.0)))
        assertEquals(BadWeather.SNOW, badWeather(HourForecast(3, 90, -6.0)))
        assertEquals(BadWeather.RAIN_SNOW, badWeather(HourForecast(2, 90, -6.0)))
        assertEquals(BadWeather.SHOWER, badWeather(HourForecast(4, 90, 34.0)))
        assertNull(badWeather(null))
    }

    @Test fun `forecast error throws`() {
        assertThrows(ApiException::class.java) {
            parseForecast("""{"response":{"header":{"resultCode":"03"}}}""")
        }
        assertThrows(ApiException::class.java) { parseForecast("not json") }
    }
}
