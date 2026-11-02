package com.roadahead.feed

import com.roadahead.core.advice.TravelAdvisor
import com.roadahead.core.forecast.BadWeather
import com.roadahead.core.model.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime

interface TourSource {
    suspend fun places(point: GeoPoint): List<TourItem>
    suspend fun festivals(date: LocalDate): List<TourItem>
    suspend fun petPlaces(point: GeoPoint): List<TourItem>
    suspend fun petInfo(id: String): PetInfo?
    suspend fun facilities(id: String): Set<Facility>
    suspend fun weather(point: GeoPoint, arrival: LocalDateTime): BadWeather?
}

private suspend fun <T> available(block: suspend () -> T): T? = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (_: Exception) {
    null
}

suspend fun collectTour(source: TourSource, dest: Destination, arrival: LocalDateTime, companions: Set<Companion>,
    advisor: TravelAdvisor = TravelAdvisor()): TourData = coroutineScope {
    val placesRequest = async { available { source.places(dest.point) }.orEmpty() }
    val festivalsRequest = async { available { source.festivals(arrival.toLocalDate()) }.orEmpty() }
    val weatherRequest = async { available { source.weather(dest.point, arrival) } }
    val petRequest = if (Companion.PET in companions) async { available { source.petPlaces(dest.point) } } else null
    val places = placesRequest.await()
    val festivals = festivalsRequest.await()
    val weather = weatherRequest.await()
    val petPlaces = petRequest?.await()
    val petInfo = advisor.petTargets(dest, petPlaces.orEmpty()).mapNotNull { id ->
        available { source.petInfo(id) }?.let { id to it }
    }.toMap()
    val confirmedPet = petPlaces?.filter { it.id in petInfo }
    val facilities = advisor.facilityTargets(dest, places, companions, weather, confirmedPet).mapNotNull { id ->
        available { source.facilities(id) }?.let { id to it }
    }.toMap()
    TourData(places, festivals, companions, petPlaces, petInfo, facilities, weather)
}
