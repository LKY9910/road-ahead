package com.roadahead.core.advice

import com.roadahead.core.geo.*
import com.roadahead.core.model.*
import com.roadahead.core.text.koDistance
import java.time.Duration
import java.time.Instant

data class HazardConfig(val announceM: Double = 3_000.0, val coneDeg: Double = 30.0, val alignDeg: Double = 45.0,
    val weatherMaxAge: Duration = Duration.ofMinutes(10))

class HazardAdvisor(private val cfg: HazardConfig = HazardConfig()) {
    fun advices(hazards: List<Hazard>, fix: Fix?, badWeather: Boolean, now: Instant): List<Advice> {
        if (fix == null) return emptyList()
        return hazards.mapNotNull { hazard ->
            val weather = hazard.kind == HazardKind.ICE || hazard.kind == HazardKind.FOG
            if (weather && (Duration.between(hazard.produced, now) > cfg.weatherMaxAge ||
                    (hazard.severity == Severity.INTEREST && !badWeather))) return@mapNotNull null
            val distance = if (weather) {
                segmentAheadDistance(fix, hazard.from, hazard.to, cfg.coneDeg, cfg.alignDeg)
            } else {
                listOfNotNull(aheadDistance(fix, hazard.from, cfg.coneDeg), aheadDistance(fix, hazard.to, cfg.coneDeg)).minOrNull()
            } ?: return@mapNotNull null
            if (distance > cfg.announceM) return@mapNotNull null
            val d = koDistance(distance)
            val text = when (hazard.kind) {
                HazardKind.ICE -> "$d 앞 살얼음 ${hazard.severity.label} 구간이에요. 속도를 줄이세요."
                HazardKind.FOG -> when (hazard.severity) {
                    Severity.INTEREST -> "$d 앞 옅은 안개 구간이에요. 앞이 1km도 안 보여요."
                    Severity.CAUTION -> "$d 앞 안개 구간이에요. 앞이 500m도 안 보여요. 속도를 줄이세요."
                    Severity.DANGER -> "$d 앞 짙은 안개 구간이에요. 앞이 200m도 안 보여요. 속도를 줄이세요."
                }
                HazardKind.CONSTRUCTION -> when {
                    hazard.fullClosure -> "$d 앞 도로가 전면 통제됐어요."
                    hazard.detail.isNotBlank() -> "$d 앞 공사 구간이에요. ${hazard.detail}."
                    else -> "$d 앞 공사 구간이에요."
                }
                HazardKind.ACCIDENT -> "$d 앞에서 사고가 났어요."
                HazardKind.DISASTER -> "$d 앞 ${hazard.detail} 위험이 있어요."
            }
            val priority = if ((weather && hazard.severity == Severity.DANGER) || hazard.fullClosure || hazard.kind == HazardKind.DISASTER) {
                Priority.HIGH
            } else Priority.NORMAL
            Advice("${hazard.key}:${hazard.severity}", priority, text)
        }
    }
}
