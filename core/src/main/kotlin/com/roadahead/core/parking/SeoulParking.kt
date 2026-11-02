package com.roadahead.core.parking

import com.roadahead.core.model.ApiException
import com.roadahead.core.model.ParkingLot
import kotlinx.serialization.json.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class SeoulLot(val code: String, val totalSpaces: Int?, val occupied: Int?, val linked: Boolean,
    val updated: LocalDateTime?, val busOnly: Boolean, val weekdayHours: IntRange?, val weekendHours: IntRange?,
    val saturdayFree: Boolean, val basicMinutes: Int?, val basicFeeWon: Int?)

val SEOUL_TO_TS: Map<String, String> = mapOf(
    "1425816" to "04540-11291-00001-00-1", "171721" to "03172-11291-00001-00-1")

private fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull.orEmpty().trim()

private fun JsonObject.integer(key: String): Int? =
    text(key).toBigDecimalOrNull()?.let { runCatching { it.intValueExact() }.getOrNull() }

private fun minute(text: String): Int? {
    if (text == "2400") return 1440
    if (!text.matches(Regex("\\d{4}"))) return null
    val hour = text.take(2).toInt()
    val minute = text.takeLast(2).toInt()
    return (hour * 60 + minute).takeIf { hour in 0..23 && minute in 0..59 }
}

private fun JsonObject.hours(prefix: String): IntRange? {
    val start = minute(text("${prefix}_OPER_BGNG_TM")) ?: return null
    val end = minute(text("${prefix}_OPER_END_TM")) ?: return null
    return start..end
}

fun parseSeoulParking(body: String): List<SeoulLot> = try {
    val root = Json.parseToJsonElement(body).jsonObject
    val response = root["GetParkingInfo"]?.jsonObject ?: root
    when (response.getValue("RESULT").jsonObject.text("CODE")) {
        "INFO-200" -> emptyList()
        "INFO-000" -> response.getValue("row").jsonArray.map {
            val row = it.jsonObject
            val code = row.text("PKLT_CD").also { require(it.isNotEmpty()) }
            SeoulLot(code, row.integer("TPKCT"), row.integer("NOW_PRK_VHCL_CNT"), row.text("PRK_STTS_YN") == "1",
                runCatching { LocalDateTime.parse(row.text("NOW_PRK_VHCL_UPDT_TM"),
                    DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)) }.getOrNull(),
                row.text("OPER_SE") == "4", row.hours("WD"), row.hours("WE"), row.text("SAT_CHGD_FREE_SE") == "N",
                row.integer("BSC_PRK_HR"), row.integer("BSC_PRK_CRG"))
        }
        else -> throw ApiException("result")
    }
} catch (error: ApiException) {
    throw error
} catch (_: Exception) {
    throw ApiException("parse")
}

fun applySeoul(lots: List<ParkingLot>, seoul: List<SeoulLot>, now: Instant,
    zone: ZoneId = ZoneId.of("Asia/Seoul")): List<ParkingLot> {
    val mapped = seoul.mapNotNull { row -> SEOUL_TO_TS[row.code]?.let { it to row } }.toMap()
    val local = now.atZone(zone)
    val minute = local.hour * 60 + local.minute
    return lots.mapNotNull { lot ->
        val row = mapped[lot.id] ?: return@mapNotNull lot
        val hours = if (local.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) row.weekendHours else row.weekdayHours
        val open = hours == null || if (hours.last > hours.first) minute >= hours.first && minute < hours.last
            else minute >= hours.first || minute < hours.last
        if (row.busOnly || !open) return@mapNotNull null
        val produced = row.updated?.atZone(zone)?.toInstant()
        val age = produced?.let { Duration.between(it, now) }
        val fresh = row.linked && row.totalSpaces != null && row.totalSpaces > 0 &&
            row.occupied != null && row.occupied in 0..row.totalSpaces && age != null &&
            !age.isNegative && age <= Duration.ofSeconds(600)
        val hasFee = row.basicMinutes != null && row.basicFeeWon != null
        lot.copy(available = if (fresh) row.totalSpaces!! - row.occupied!! else null,
            availableProduced = produced.takeIf { fresh },
            basicMinutes = if (hasFee) row.basicMinutes else lot.basicMinutes,
            basicFeeWon = if (local.dayOfWeek == DayOfWeek.SATURDAY && row.saturdayFree) 0
                else if (hasFee) row.basicFeeWon else lot.basicFeeWon)
    }
}
