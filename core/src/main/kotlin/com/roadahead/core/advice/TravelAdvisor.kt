package com.roadahead.core.advice

import com.roadahead.core.geo.distanceMeters
import com.roadahead.core.forecast.BadWeather
import com.roadahead.core.model.*
import com.roadahead.core.model.Companion.*
import com.roadahead.core.text.*
import java.time.LocalDate

data class TravelConfig(val festivalM: Double = 5_000.0, val festivalMax: Int = 2,
    val placeM: Double = 3_000.0, val placeMax: Int = 3)

class TravelAdvisor(private val cfg: TravelConfig = TravelConfig()) {
    private fun nearbyPlaces(dest: Destination, items: List<TourItem>) = items
        .filter { it.kind in setOf(TourKind.ATTRACTION, TourKind.CULTURE, TourKind.LEISURE) }
        .map { it to distanceMeters(dest.point, it.point) }.filter { it.second <= cfg.placeM }.sortedBy { it.second }

    fun petTargets(dest: Destination, petPlaces: List<TourItem>): List<String> =
        nearbyPlaces(dest, petPlaces).take(5).map { it.first.id }

    fun facilityTargets(dest: Destination, places: List<TourItem>, companions: Set<Companion>,
        weather: BadWeather?, confirmedPet: List<TourItem>?): List<String> {
        if (CHILD !in companions && ELDER !in companions) return emptyList()
        if (PET in companions) return confirmedPet.orEmpty().take(5).map { it.id }
        return nearbyPlaces(dest, places).sortedBy { weather != null && it.first.kind != TourKind.CULTURE }
            .take(10).map { it.first.id }
    }

    private fun reasons(item: TourItem, data: TourData): List<String> {
        val facilities = Facility.entries.filter { it.companion in data.companions && it in data.facilities[item.id].orEmpty() }
        val child = facilities.filter { it.companion == CHILD }
        val elder = facilities.filter { it.companion == ELDER }
        val human = if (child.isNotEmpty() && elder.isNotEmpty()) listOf(child.first(), elder.first()) else facilities
        val pet = data.petInfo[item.id]?.takeIf { PET in data.companions }
        return (listOfNotNull(pet?.scope?.label, pet?.need) + human.map { it.label }).distinct().take(2)
    }

    fun pick(dest: Destination, data: TourData, arrival: LocalDate, today: LocalDate): TravelResult? {
        val festivals = data.festivals.map { it to distanceMeters(dest.point, it.point) }.filter { (item, distance) ->
            item.kind == TourKind.FESTIVAL && distance <= cfg.festivalM &&
                item.start != null && item.end != null && arrival >= item.start && arrival <= item.end
        }.sortedBy { it.second }.take(cfg.festivalMax)
        val pet = PET in data.companions
        val candidates = if (pet) data.petPlaces.orEmpty().filter { it.id in data.petInfo } else data.places
        val places = nearbyPlaces(dest, candidates).sortedWith(
            compareByDescending<Pair<TourItem, Double>> { (item, _) ->
                data.facilities[item.id].orEmpty().map { it.companion }.distinct()
                    .count { it in data.companions && it != PET }
            }.thenBy { data.weather != null && it.first.kind != TourKind.CULTURE }.thenBy { it.second }
        ).take(cfg.placeMax)
        if (festivals.isEmpty() && places.isEmpty()) return null

        val sentences = mutableListOf<String>()
        festivals.firstOrNull()?.let { (item, distance) ->
            sentences += "목적지 근처에서 ${koUntil(item.end!!, today)}까지 ${subject(item.title)} 열려요. " +
                "목적지에서 ${copula(koDistance(distance))}."
        }
        festivals.drop(1).forEach { (item, _) -> sentences += "${item.title}도 열려요." }
        val weather = data.weather?.takeIf { places.any { it.first.kind == TourKind.CULTURE } }
        if (weather != null) sentences += "도착할 때쯤 ${weather.word} 예보가 있어 실내 명소를 먼저 골랐어요."
        if (places.isNotEmpty()) sentences += (if (pet) "반려동물과 갈 수 있는 가까운 명소는 " else "가까운 명소는 ") +
            "${copula(places.joinToString(", ") { it.first.title })}."
        places.firstOrNull()?.first?.let { first ->
            val reason = reasons(first, data).firstOrNull { it != PetScope.ALL.label && it != data.petInfo[first.id]?.need }
            val sentence = when (reason) {
                Facility.STROLLER.label -> "유모차를 빌릴 수 있어요"
                Facility.LACTATION.label -> "수유실이 있어요"
                Facility.BABY_CHAIR.label -> "유아용 의자가 있어요"
                Facility.WHEELCHAIR.label -> "휠체어를 빌릴 수 있어요"
                Facility.STEP_FREE.label -> "입구에 턱이 없어요"
                Facility.ELEVATOR.label -> "엘리베이터가 있어요"
                Facility.ACCESSIBLE_TOILET.label -> "장애인 화장실이 있어요"
                PetScope.PARTIAL.label -> "일부 구역만 반려동물과 들어갈 수 있어요"
                else -> null
            }
            if (sentence != null) sentences += "${topic(first.title)} $sentence."
        }
        val lines = festivals.map { (item, distance) ->
            TravelLine(item.title, item.kind, koDistance(distance), "${koUntil(item.end!!, today)}까지")
        } + places.map { (item, distance) -> TravelLine(item.title, item.kind, koDistance(distance), null, reasons(item, data)) }
        return TravelResult(lines, Advice("travel:${dest.key}", Priority.NORMAL, sentences.joinToString(" ")),
            weather?.let { "도착 때 ${it.word} 예보 · 실내 명소 우선" })
    }
}
