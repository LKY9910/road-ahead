package com.roadahead.feed

import com.roadahead.core.RoadAheadEngine
import com.roadahead.core.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface DataEvent {
    data class Signals(val json: String, val receivedMonoMs: Long) : DataEvent
    data class Intersections(val json: String) : DataEvent
    data class Weather(val hazards: List<Hazard>) : DataEvent
    data class Events(val hazards: List<Hazard>) : DataEvent
    data class Disasters(val hazards: List<Hazard>) : DataEvent
}

interface Feed {
    val navMode: StateFlow<NavMode>
    val turn: StateFlow<Turn?>
    val fix: StateFlow<Fix?>
    val speed: SharedFlow<Pair<Double, Long>>
    val wiperOn: StateFlow<Boolean>
    val defrosterOn: StateFlow<Boolean>
    val naviSpeaking: StateFlow<Boolean>
    val data: SharedFlow<DataEvent>
    val label: StateFlow<String?>
    fun start(scope: CoroutineScope, engine: RoadAheadEngine)
    fun stop()
}
