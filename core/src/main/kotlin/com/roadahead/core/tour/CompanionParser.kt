package com.roadahead.core.tour

import com.roadahead.core.model.Facility
import com.roadahead.core.model.PetScope
import com.roadahead.core.model.PetInfo
import kotlinx.serialization.json.*

private fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull.orEmpty().trim()

fun parsePetInfo(json: String): PetInfo? = tourItems(json) { row ->
    val scope = row.text("acmpyTypeCd")
    val parsed = when {
        "전구역" in scope -> PetScope.ALL
        "일부구역" in scope -> PetScope.PARTIAL
        else -> null
    } ?: return@tourItems null
    PetInfo(parsed, row.text("acmpyNeedMtr").takeIf {
        it.isNotEmpty() && it.length <= 10 && '\n' !in it && '\r' !in it
    })
}.firstOrNull()

fun parseFacilities(json: String): Set<Facility> = tourItems(json) { row ->
    val fields = mapOf(
        "stroller" to Facility.STROLLER, "lactationroom" to Facility.LACTATION,
        "babysparechair" to Facility.BABY_CHAIR, "wheelchair" to Facility.WHEELCHAIR,
        "elevator" to Facility.ELEVATOR, "restroom" to Facility.ACCESSIBLE_TOILET
    )
    buildSet {
        for ((field, facility) in fields) {
            val text = row.text(field)
            if (text.isNotEmpty() && "없음" !in text && "불가" !in text) add(facility)
        }
        if (listOf("route", "exit").any {
            val text = row.text(it)
            "없음" !in text && "불가" !in text && ("턱이 없" in text || "경사로" in text)
        }) add(Facility.STEP_FREE)
    }
}.flatten().toSet()
