package com.roadahead.core.text

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

fun koDistance(meters: Double): String = when {
    meters < 1000 -> {
        val rounded = (meters / 100).roundToInt().coerceAtLeast(1) * 100
        if (rounded >= 1000) "1km" else "${rounded}m"
    }
    meters < 10_000 -> {
        val tenths = (meters / 100).roundToInt()
        if (tenths % 10 == 0) "${tenths / 10}km" else "${tenths / 10}.${tenths % 10}km"
    }
    else -> "${(meters / 1000).roundToInt()}km"
}

private fun hasFinalConsonant(word: String): Boolean =
    word.lastOrNull()?.let { it in '가'..'힣' && (it.code - '가'.code) % 28 != 0 } == true

fun subject(word: String): String = word + if (hasFinalConsonant(word)) "이" else "가"

fun copula(word: String): String = word + if (hasFinalConsonant(word)) "이에요" else "예요"

fun topic(word: String): String = word + if (hasFinalConsonant(word)) "은" else "는"

fun koFee(minutes: Int?, won: Int?): String? = when {
    won == 0 -> "무료예요"
    minutes != null && won != null -> "${minutes}분에 ${String.format(Locale.ROOT, "%,d", won)}원이에요"
    else -> null
}

fun koFeeShort(minutes: Int?, won: Int?): String? = when {
    won == 0 -> "무료"
    minutes != null && won != null -> "${minutes}분에 ${String.format(Locale.ROOT, "%,d", won)}원"
    else -> null
}

fun koUntil(end: LocalDate, today: LocalDate): String = when (end) {
    today -> "오늘"
    today.plusDays(1) -> "내일"
    else -> "${end.monthValue}월 ${end.dayOfMonth}일"
}
