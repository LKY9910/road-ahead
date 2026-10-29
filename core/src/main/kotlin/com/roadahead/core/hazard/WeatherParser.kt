package com.roadahead.core.hazard

import com.roadahead.core.model.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

fun parseWeatherCsv(csv: String, kind: HazardKind, produced: Instant): List<Hazard> = try {
    require(kind == HazardKind.FOG || kind == HazardKind.ICE)
    val lines = csv.removePrefix("\uFEFF").lineSequence().filter { it.isNotBlank() }.iterator()
    val header = lines.next().split(',').map { it.trim() }
    val levelColumn = if (kind == HazardKind.FOG) "FOG" else "B_ICE"
    val required = listOf("LINK_ID", "F_LAT", "F_LON", "T_LAT", "T_LON", levelColumn)
    require(header.containsAll(required))
    var rows = 0
    var failures = 0
    val hazards = lines.asSequence().mapNotNull { line ->
        rows++
        try {
            val fields = line.split(',')
            fun value(name: String) = fields[header.indexOf(name)].trim()
            val severity = when (value(levelColumn)) {
                "1" -> Severity.INTEREST
                "2" -> Severity.CAUTION
                "3" -> Severity.DANGER
                "0", "" -> return@mapNotNull null
                else -> error("Unknown weather level")
            }
            val prefix = if (kind == HazardKind.FOG) "fog" else "ice"
            Hazard("$prefix:${value("LINK_ID")}", kind, severity,
                GeoPoint(value("F_LAT").toDouble(), value("F_LON").toDouble()),
                GeoPoint(value("T_LAT").toDouble(), value("T_LON").toDouble()), "", false, produced)
        } catch (_: Exception) {
            failures++
            null
        }
    }.toList()
    if (rows > 0 && failures == rows) throw ApiException("parse")
    hazards
} catch (e: ApiException) {
    throw e
} catch (_: Exception) {
    throw ApiException("parse")
}

fun parseWeatherZip(zip: ByteArray, produced: Instant): List<Hazard> = try {
    require(zip.size >= 4 && zip[0] == 0x50.toByte() && zip[1] == 0x4b.toByte())
    ZipInputStream(zip.inputStream()).use { input ->
        buildList {
            while (true) {
                val entry = input.nextEntry ?: break
                val kind = when {
                    "_LINK_FOG_" in entry.name -> HazardKind.FOG
                    "_LINK_FRG_" in entry.name -> HazardKind.ICE
                    else -> null
                }
                if (!entry.isDirectory && kind != null) {
                    addAll(parseWeatherCsv(input.readBytes().toString(Charsets.UTF_8), kind, produced))
                }
                input.closeEntry()
            }
        }
    }
} catch (e: ApiException) {
    throw e
} catch (_: Exception) {
    throw ApiException("parse")
}

fun weatherRequestTimes(now: Instant): List<String> {
    val slot = now.epochSecond / 300 * 300
    val format = DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC)
    return (0..2).map { format.format(Instant.ofEpochSecond(slot - it * 300)) }
}
