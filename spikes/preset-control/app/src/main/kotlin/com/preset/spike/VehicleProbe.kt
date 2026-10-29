package com.preset.spike

import android.content.Context
import android.os.SystemClock
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.*
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.*
import ai.pleos.playground.vehicle.listener.*
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

private typealias Failure = (Exception) -> Unit
private typealias PositionCap = ((CapabilityConfig, Map<SeatArea, AreaBounds<Int>>) -> Unit, Failure) -> Unit
private typealias MoveCap = ((CapabilityConfig, List<SeatArea>) -> Unit, Failure) -> Unit

class VehicleProbe(context: Context, private val log: (String) -> Unit) {
    private val vehicle = Vehicle(context)
    private val seat by lazy { vehicle.getSeat() }
    private val fronts = listOf(SeatArea(SeatValue.ROW_1_LEFT), SeatArea(SeatValue.ROW_1_RIGHT))
    private val cleanups = CopyOnWriteArrayList<() -> Unit>()
    private val bounds = mutableMapOf<String, Map<SeatArea, AreaBounds<Int>>>()
    private val moveAreas = mutableMapOf<String, List<SeatArea>>()
    private fun name(area: SeatArea) = area.values.joinToString("+") { it.name }
    fun initialize() { vehicle.initialize(); log("Vehicle.initialize returned") }

    private suspend fun <T> call(label: String, start: ((T) -> Unit, Failure) -> Unit): T? {
        val answer = CompletableDeferred<T?>()
        try { start({ value -> log("$label = $value"); answer.complete(value) }, {
            log("$label ERROR ${it.javaClass.simpleName}: ${it.message}"); answer.complete(null)
        }) } catch (e: Exception) { log("$label THREW ${e.javaClass.simpleName}: ${e.message}"); answer.complete(null) }
        return withTimeoutOrNull(4000) { answer.await() }.also { if (!answer.isCompleted) log("$label TIMEOUT 4000ms") }
    }

    private data class Axis(
        val key: String, val cap: PositionCap, val moveCap: MoveCap,
        val get: (SeatArea, (Int?) -> Unit, Failure) -> Unit,
        val move: (SeatArea, MoveControl, (SeatArea, MoveControl?) -> Unit, Failure) -> Unit
    )

    private val axes by lazy { listOf(
        Axis("ForeAft", { ok, fail -> seat.checkSeatForeAftPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatForeAftCapabilities(ok, fail) },
            seat::getCurrentSeatForeAftPosition, seat::moveSeatForeAft),
        Axis("BackrestAngle", { ok, fail -> seat.checkSeatBackrestAnglePositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatBackrestAngleCapabilities(ok, fail) },
            seat::getCurrentSeatBackrestAnglePosition, seat::moveSeatBackrestAngle),
        Axis("Height", { ok, fail -> seat.checkSeatHeightPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatHeightCapabilities(ok, fail) },
            seat::getCurrentSeatHeightPosition, seat::moveSeatHeight),
        Axis("Tilt", { ok, fail -> seat.checkSeatTiltPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatTiltCapabilities(ok, fail) },
            seat::getCurrentSeatTiltPosition, seat::moveSeatTilt),
        Axis("Depth", { ok, fail -> seat.checkSeatDepthPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatDepthCapabilities(ok, fail) },
            seat::getCurrentSeatDepthPosition, seat::moveSeatDepth),
        Axis("HeadrestHeight", { ok, fail -> seat.checkSeatHeadrestHeightPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatHeadrestHeightCapabilities(ok, fail) },
            seat::getCurrentSeatHeadrestHeightAftPosition, seat::moveSeatHeadrestHeight),
        Axis("HeadrestAngle", { ok, fail -> seat.checkSeatHeadrestAnglePositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatHeadrestAngleCapabilities(ok, fail) },
            seat::getCurrentSeatHeadrestAnglePosition, seat::moveSeatHeadrestAngle),
        Axis("HeadrestForeAft", { ok, fail -> seat.checkSeatHeadrestForeAftPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatHeadrestForeAftCapabilities(ok, fail) },
            seat::getCurrentSeatHeadrestForeAftPosition, seat::moveSeatHeadrestForeAft),
        Axis("LumbarForeAft", { ok, fail -> seat.checkSeatLumbarForeAftPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatLumbarForeAftCapabilities(ok, fail) },
            seat::getCurrentSeatLumbarForeAftPosition, seat::moveSeatLumbarForeAft),
        Axis("LumbarSideSupport", { ok, fail -> seat.checkSeatLumbarSideSupportPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatLumbarSideSupportCapabilities(ok, fail) },
            seat::getCurrentSeatLumbarSideSupportPosition, seat::moveSeatLumbarSideSupport),
        Axis("BeltHeight", { ok, fail -> seat.checkSeatBeltHeightPositionCapabilities(ok, fail) },
            { ok, fail -> seat.checkMoveSeatBeltHeightCapabilities(ok, fail) },
            seat::getCurrentSeatBeltHeightPosition, seat::moveSeatBeltHeight)
    ) }

    suspend fun capabilities() {
        for (axis in axes) {
            val positions = call<Map<SeatArea, AreaBounds<Int>>>("01 ${axis.key} position capability") { ok, fail ->
                axis.cap({ config, areas ->
                    log("01 ${axis.key} position access=${config.accessType} " + areas.entries.joinToString { "${name(it.key)}:${it.value.minValue}..${it.value.maxValue}" }); ok(areas)
                }, fail)
            }
            if (positions != null) {
                bounds[axis.key] = positions
                for (area in positions.keys) read(axis, area, "01 current")
            }
            val moves = call<List<SeatArea>>("01 ${axis.key} move capability") { ok, fail ->
                axis.moveCap({ config, areas -> log("01 ${axis.key} move access=${config.accessType} areas=${areas.map(::name)}"); ok(areas) }, fail)
            }
            if (moves != null) moveAreas[axis.key] = moves
        }
    }

    private suspend fun read(axis: Axis, area: SeatArea, label: String = "position") =
        call<Int?>("$label ${axis.key} ${name(area)}") { ok, fail -> axis.get(area, ok, fail) }

    private suspend fun move(axis: Axis, area: SeatArea, direction: MoveControl) =
        call<MoveControl?>("move ${axis.key} ${name(area)} $direction") { ok, fail -> axis.move(area, direction, { _, value -> ok(value) }, fail) }

    private fun subscribe(axis: Axis, area: SeatArea, samples: MutableList<Pair<Long, Int>>) {
        fun sample(value: Int?) {
            log("02 callback ${axis.key} ${name(area)} = $value")
            if (value != null) samples.add(SystemClock.elapsedRealtime() to value)
        }
        if (axis.key == "ForeAft") {
            val listener = object : SeatForeAftPositionListener {
                override fun onSeatForeAftPositionUpdated(value: Int?) = sample(value)
                override fun onFailed(e: Exception) { log("02 callback ERROR $e") }
            }
            seat.registerSeatForeAftPosition(area, listener)
            cleanups += { seat.unregisterSeatForeAftPosition(area, listener) }
        } else {
            val listener = object : SeatBackrestAnglePositionListener {
                override fun onSeatBackrestAnglePositionUpdated(value: Int?) = sample(value)
                override fun onFailed(e: Exception) { log("02 callback ERROR $e") }
            }
            seat.registerSeatBackrestAnglePosition(area, listener)
            cleanups += { seat.unregisterSeatBackrestAnglePosition(area, listener) }
        }
    }

    suspend fun motion() {
        if (bounds.isEmpty()) capabilities()
        for (axis in axes.take(2)) for (area in fronts) {
            if (area !in moveAreas[axis.key].orEmpty()) continue
            val original = read(axis, area, "02 baseline") ?: continue
            val samples = CopyOnWriteArrayList<Pair<Long, Int>>()
            subscribe(axis, area, samples)
            try {
                for (direction in listOf(MoveControl.FORWARD, MoveControl.BACKWARD)) {
                    val before = read(axis, area) ?: continue
                    val start = SystemClock.elapsedRealtime()
                    move(axis, area, direction)
                    delay(650)
                    val stopValue = read(axis, area)
                    val stopAt = SystemClock.elapsedRealtime()
                    move(axis, area, MoveControl.STOP)
                    delay(600)
                    val final = read(axis, area)
                    val changed = samples.filter { it.first > stopAt && it.second != stopValue }
                    log("02 METRIC ${axis.key} ${name(area)} $direction before=$before stopValue=$stopValue final=$final runMs=${stopAt-start} speed=${if(stopValue!=null)(stopValue-before)*1000.0/(stopAt-start) else null} over=${if(final!=null&&stopValue!=null)final-stopValue else null} lastChangeAfterStopMs=${changed.lastOrNull()?.first?.minus(stopAt)}")
                }
                val range = bounds[axis.key]?.get(area) ?: continue
                target(axis, area, range.maxValue, "02 endpoint")
                move(axis, area, MoveControl.FORWARD); delay(400); move(axis, area, MoveControl.STOP)
                read(axis, area, "02 endpoint extra FORWARD")
            } finally {
                withContext(NonCancellable) { move(axis, area, MoveControl.STOP); target(axis, area, original, "RESTORE") }
            }
        }
    }

    // shortcut: position feedback is stale on this AVD; do not reuse this loop until the bridge is verified.
    private suspend fun target(axis: Axis, area: SeatArea, goal: Int, label: String) {
        val range = bounds[axis.key]?.get(area) ?: return
        require(goal in range.minValue..range.maxValue)
        val initial = read(axis, area) ?: return
        val direction = if (goal > initial) MoveControl.FORWARD else MoveControl.BACKWARD
        val started = SystemClock.elapsedRealtime()
        var value = initial
        try {
            if (value != goal && move(axis, area, direction) != null) {
                while (SystemClock.elapsedRealtime() - started < 5000) {
                    delay(40)
                    value = read(axis, area) ?: break
                    if ((direction == MoveControl.FORWARD && value >= goal) || (direction == MoveControl.BACKWARD && value <= goal)) break
                }
            }
        } finally { withContext(NonCancellable) { move(axis, area, MoveControl.STOP) } }
        delay(350)
        val final = read(axis, area)
        log("$label TARGET ${axis.key} ${name(area)} initial=$initial goal=$goal final=$final error=${final?.minus(goal)} elapsedMs=${SystemClock.elapsedRealtime()-started}")
    }

    suspend fun targets() = coroutineScope {
        if (bounds.isEmpty()) capabilities()
        for (axis in axes.take(2)) {
            fronts.map { area -> async {
                val original = read(axis, area, "03 baseline") ?: return@async
                val range = bounds[axis.key]?.get(area) ?: return@async
                try { target(axis, area, (range.minValue + range.maxValue) / 2 + 3, "03 simultaneous") }
                finally { withContext(NonCancellable) { target(axis, area, original, "RESTORE") } }
            } }.awaitAll()
        }
    }

    suspend fun state() {
        call<List<SeatArea>>("06 occupancy capability") { ok, fail -> seat.checkSeatOccupancyCapabilities({ config, areas -> log("06 occupancy access=${config.accessType} areas=${areas.map(::name)}"); ok(areas) }, fail) }
        call<List<SeatArea>>("06 belt capability") { ok, fail -> seat.checkSeatBeltBuckledCapabilities({ config, areas -> log("06 belt access=${config.accessType} areas=${areas.map(::name)}"); ok(areas) }, fail) }
        for (value in SeatValue.entries) {
            val area = SeatArea(value)
            call<Boolean?>("06 occupied ${name(area)}") { ok, fail -> seat.getCurrentSeatOccupancy(area, ok, fail) }
            call<Boolean?>("06 buckled ${name(area)}") { ok, fail -> seat.getCurrentSeatBeltBuckledState(area, ok, fail) }
        }
    }

    fun close() {
        cleanups.forEach { runCatching(it) }
        vehicle.release()
    }
}
