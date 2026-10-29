package com.roadahead.car

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.SystemClock
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

class CarSignals(private val context: Context) {
    private val vehicle = Vehicle(context)
    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private var weatherJob: Job? = null
    private var closed = false
    private val _speed = MutableSharedFlow<Pair<Double, Long>>(extraBufferCapacity = 16)
    private val _parked = MutableStateFlow(false)
    private val _wiperOn = MutableStateFlow(false)
    private val _defrosterOn = MutableStateFlow(false)
    val speed = _speed.asSharedFlow()
    val parked = _parked.asStateFlow()
    val wiperOn = _wiperOn.asStateFlow()
    val defrosterOn = _defrosterOn.asStateFlow()
    private val wipers = ConcurrentHashMap<WindowArea, Boolean>()
    private val defrosters = ConcurrentHashMap<WindowArea, Boolean>()
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(value: VehicleGear?) {
            if (!closed) _parked.value = value == VehicleGear.GEAR_PARK
            Log.i("RoadAhead", "parked=${_parked.value} gear=$value")
        }
        override fun onFailed(e: Exception) { _parked.value = false; failed("gear", e) }
    }
    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val mps = (value.value as? Number)?.toDouble() ?: return
            val monoMs = value.timestamp / 1_000_000
            if (!closed && value.status == 0 && mps.isFinite() && SystemClock.elapsedRealtime() - monoMs in 0..1_000)
                _speed.tryEmit(mps to monoMs)
        }
        override fun onErrorEvent(propertyId: Int, areaId: Int) { Log.w("RoadAhead", "speed error property=$propertyId area=$areaId") }
    }

    fun start(scope: CoroutineScope) {
        vehicle.initialize()
        vehicle.getGear().getCurrentGearSelectionState({ gearListener.onSelected(it) }, { gearListener.onFailed(it) })
        vehicle.getGear().registerGearSelection(gearListener)
        try {
            car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (ready && !closed) {
                    try {
                        properties = connected.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                        Log.i("RoadAhead", "speed registered=${properties!!.registerCallback(speedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, 5f)}")
                    } catch (e: Exception) { failed("speed", e) }
                }
            }
        } catch (e: Exception) { failed("car", e) }
        weatherJob = scope.launch {
            while (isActive) {
                readWeather()
                delay(5_000)
            }
        }
    }

    private fun readWeather() {
        for (window in listOf(WindowValue.FRONT_WINDSHIELD, WindowValue.REAR_WINDSHIELD)) {
            val area = WindowArea(window)
            vehicle.getWiper().getCurrentWiperState(area, {
                if (!closed) {
                    wipers[area] = it == WiperState.ON
                    _wiperOn.value = wipers.values.any { on -> on }
                    Log.i("RoadAhead", "wiperOn=${_wiperOn.value}")
                }
            }, { failed("wiper", it) })
            vehicle.getHVAC().getHvacDefrosterState(area, {
                if (!closed) {
                    defrosters[area] = it == true
                    _defrosterOn.value = defrosters.values.any { on -> on }
                    Log.i("RoadAhead", "defrosterOn=${_defrosterOn.value}")
                }
            }, { failed("defroster", it) })
        }
    }

    private fun failed(source: String, e: Exception) { Log.w("RoadAhead", "$source failed ${e.javaClass.simpleName}") }

    fun stop() {
        closed = true
        weatherJob?.cancel()
        properties?.unregisterCallback(speedCallback)
        car?.disconnect()
        vehicle.getGear().unregisterGearSelection(gearListener)
        vehicle.release()
    }
}
