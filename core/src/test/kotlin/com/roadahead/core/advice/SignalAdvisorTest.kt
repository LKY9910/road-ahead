package com.roadahead.core.advice

import com.roadahead.core.drive.Motion
import com.roadahead.core.model.*
import com.roadahead.core.signal.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SignalAdvisorTest {
    private val intersection = Intersection("3100000000", "100", "울산대학교", GeoPoint(35.543293, 129.259894))
    private val t0 = LocalDateTime.of(2026, 10, 28, 16, 6, 47)
    private val advisor = SignalAdvisor()
    private fun north(m: Double) = GeoPoint(intersection.point.lat + m / 111_195.0, intersection.point.lon)
    private fun tracker(seconds: Double = 40.0, color: LightColor = LightColor.RED,
        approach: Compass8 = Compass8.N, samples: Int = 3, places: List<Intersection> = listOf(intersection)) =
        SignalTracker().also { tracker ->
            repeat(samples) { i -> places.forEach { place ->
                tracker.accept(SignalSnapshot(place.region, place.id, t0.plusSeconds(i.toLong()), i * 1000L,
                    Movement.entries.map { DirectionSignal(approach, it, color, (seconds * 1000).toLong() + (2 - i) * 1000) }))
            } }
        }
    private fun view(mode: NavMode = NavMode.ROUTE, turn: Turn? = Turn(Movement.LEFT, 205.0),
        meters: Double = 200.0, tracker: SignalTracker = tracker(), advisor: SignalAdvisor = this.advisor) =
        advisor.view(mode, turn, Fix(north(meters), 180.0, 2000), listOf(intersection), tracker, 2000)

    @Test fun `route red approach asks to slow down`() {
        assertEquals(listOf(Advice("sig:3100000000:100:red", Priority.SIGNAL, "앞 신호 빨간불이에요. 속도를 줄이세요.")),
            advisor.advices(view(), NavMode.ROUTE, Motion.MOVING, 10.0))
    }
    @Test fun `free red approach only names the red light`() {
        assertEquals(listOf(Advice("sig:3100000000:100:red", Priority.SIGNAL, "앞 신호 빨간불이에요.")),
            advisor.advices(view(NavMode.FREE), NavMode.FREE, Motion.MOVING, 10.0))
    }
    @Test fun `route green ending before arrival warns soon red`() {
        assertEquals(listOf(Advice("sig:3100000000:100:soonred", Priority.SIGNAL, "앞 신호가 곧 빨간불로 바뀌어요. 속도를 줄이세요.")),
            advisor.advices(view(tracker = tracker(5.0, LightColor.GREEN)), NavMode.ROUTE, Motion.MOVING, 10.0))
    }
    @Test fun `red that turns green before arrival is not announced`() {
        assertEquals(emptyList<Advice>(), advisor.advices(view(NavMode.FREE, tracker = tracker(10.0)), NavMode.FREE, Motion.MOVING, 10.0))
    }
    @Test fun `stopped at red tells seconds to the left signal`() {
        assertEquals(listOf(Advice("sig:3100000000:100:wait", Priority.SIGNAL, "좌회전 신호까지 25초 남았어요.")),
            advisor.advices(view(turn = Turn(Movement.LEFT, 30.0), meters = 30.0, tracker = tracker(25.4)), NavMode.ROUTE, Motion.STOPPED, 0.0))
    }
    @Test fun `stopped near change says it will change soon`() {
        assertEquals(listOf(Advice("sig:3100000000:100:soon", Priority.SIGNAL, "곧 좌회전 신호로 바뀌어요.")),
            advisor.advices(view(turn = Turn(Movement.LEFT, 30.0), meters = 30.0, tracker = tracker(4.0)), NavMode.ROUTE, Motion.STOPPED, 0.0))
    }
    @Test fun `free stopped stays quiet`() {
        assertEquals(emptyList<Advice>(), advisor.advices(view(NavMode.FREE), NavMode.FREE, Motion.STOPPED, 0.0))
    }
    @Test fun `right turn at the intersection gets no signal`() {
        assertNull(view(turn = Turn(null, 200.0)))
    }
    @Test fun `turn far beyond means straight here`() {
        assertEquals(Movement.STRAIGHT, view(turn = Turn(Movement.LEFT, 900.0))!!.movement)
    }
    @Test fun `route mode without turn uses straight`() {
        assertEquals(Movement.STRAIGHT, view(turn = null)!!.movement)
    }
    @Test fun `uncalibrated red still names the color but no seconds`() {
        val tracker = tracker(samples = 2)
        assertEquals(listOf(Advice("sig:3100000000:100:red", Priority.SIGNAL, "앞 신호 빨간불이에요.")),
            advisor.advices(view(NavMode.FREE, tracker = tracker), NavMode.FREE, Motion.MOVING, 10.0))
        assertEquals(emptyList<Advice>(), advisor.advices(
            view(turn = Turn(Movement.LEFT, 30.0), meters = 30.0, tracker = tracker), NavMode.ROUTE, Motion.STOPPED, 0.0))
    }
    @Test fun `nearest of two intersections ahead wins`() {
        val near = intersection.copy(id = "near", point = north(80.0))
        val far = intersection.copy(id = "far", point = north(-50.0))
        val places = listOf(far, near)
        val view = advisor.view(NavMode.FREE, null, Fix(north(200.0), 180.0, 2000), places, tracker(places = places), 2000)
        assertEquals("near", view!!.intersection.id)
    }
    @Test fun `heading convention reads the travel direction`() {
        val headingAdvisor = SignalAdvisor(SignalConfig(convention = DirectionConvention.HEADING))
        assertEquals(SignalReading(LightColor.RED, 40.0),
            view(tracker = tracker(approach = Compass8.S), advisor = headingAdvisor)!!.reading)
    }
}
