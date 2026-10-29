package com.roadahead.core.drive

enum class Motion { MOVING, STOPPED, UNKNOWN }

class StopDetector(
    private val stopBelowMps: Double = 0.1,
    private val stopForMs: Long = 2_000,
    private val moveAboveMps: Double = 0.5,
    private val staleMs: Long = 1_000
) {
    private var lastSampleMs: Long? = null
    private var slowSinceMs: Long? = null
    private var state = Motion.UNKNOWN

    fun onSpeed(mps: Double, monoMs: Long) {
        if (lastSampleMs?.let { monoMs - it > staleMs } == true) {
            state = Motion.UNKNOWN
            slowSinceMs = null
        }
        lastSampleMs = monoMs
        if (mps <= stopBelowMps) {
            val since = slowSinceMs ?: monoMs.also { slowSinceMs = it }
            if (monoMs - since >= stopForMs) state = Motion.STOPPED
        } else {
            slowSinceMs = null
            if (mps >= moveAboveMps) state = Motion.MOVING
        }
    }

    fun motion(nowMonoMs: Long): Motion =
        if (lastSampleMs?.let { nowMonoMs - it <= staleMs } == true) state else Motion.UNKNOWN
}
