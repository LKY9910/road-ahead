package com.roadahead.core.drive

import com.roadahead.core.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.*
import java.io.ByteArrayOutputStream
import java.util.zip.*

class StopDetectorTest {
    @Test fun `two seconds below threshold means stopped`() {
        val d = StopDetector(); d.onSpeed(3.0, 0)
        (1..10).forEach { d.onSpeed(0.0, it * 200L) }
        assertEquals(Motion.MOVING, d.motion(2_100))
        d.onSpeed(0.0, 2_200); assertEquals(Motion.STOPPED, d.motion(2_200))
    }
    @Test fun `half a meter per second moves at once`() {
        val d = StopDetector(); (0..10).forEach { d.onSpeed(0.0, it * 200L) }
        d.onSpeed(0.6, 2_300); assertEquals(Motion.MOVING, d.motion(2_300))
    }
    @Test fun `between thresholds keeps previous state`() {
        val d = StopDetector(); d.onSpeed(3.0, 0); d.onSpeed(0.3, 200); assertEquals(Motion.MOVING, d.motion(200))
    }
    @Test fun `no sample for over a second is unknown`() {
        val d = StopDetector(); d.onSpeed(3.0, 0); assertEquals(Motion.UNKNOWN, d.motion(1_001))
    }
    @Test fun `first slow sample alone is unknown`() {
        val d = StopDetector(); d.onSpeed(0.3, 0); assertEquals(Motion.UNKNOWN, d.motion(0))
    }
}
