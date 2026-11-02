package com.roadahead.core.advice

import com.roadahead.core.geo.distanceMeters
import com.roadahead.core.model.*
import com.roadahead.core.text.*
import java.time.Duration
import java.time.Instant

data class ParkingConfig(val triggerM: Double = 1_000.0, val triggerSec: Int = 180, val radiusM: Double = 500.0,
    val maxAge: Duration = Duration.ofMinutes(10), val dropWithinM: Double = 100.0)

fun lotLabel(name: String): String = if ("주차" in name) name else "$name 주차장"

class ParkingAdvisor(private val cfg: ParkingConfig = ParkingConfig()) {
    fun due(dest: Destination?, navMode: NavMode): Boolean {
        if (navMode != NavMode.ROUTE || dest == null) return false
        if (dest.remainDistanceM?.let { it <= cfg.dropWithinM } == true) return false
        return dest.remainDistanceM?.let { it <= cfg.triggerM } == true ||
            dest.remainTimeSec?.let { it <= cfg.triggerSec } == true
    }

    fun pick(dest: Destination, lots: List<ParkingLot>, now: Instant): Pair<ParkingCard, Advice>? {
        fun fresh(lot: ParkingLot) = lot.availableProduced?.let { Duration.between(it, now) <= cfg.maxAge } == true
        val nearby = lots.map { it to distanceMeters(dest.point, it.point) }.filter { it.second <= cfg.radiusM }
        val chosen = nearby.filter { fresh(it.first) && (it.first.available ?: 0) >= 1 }.minByOrNull { it.second }
            ?: nearby.filter { !fresh(it.first) }.minByOrNull { it.second }
            ?: return null
        val (lot, distance) = chosen
        val available = lot.available.takeIf { fresh(lot) }
        val fee = koFee(lot.basicMinutes, lot.basicFeeWon)
        val label = lotLabel(lot.name)
        val text = if (available != null) "목적지 근처 ${label}에 빈자리 ${available}면이 있어요."
            else "목적지 근처에 ${subject(label)} 있어요."
        return ParkingCard(label, available, "목적지에서 " + koDistance(distance), koFeeShort(lot.basicMinutes, lot.basicFeeWon)) to
            Advice("parking:${dest.key}", Priority.NORMAL, text + (fee?.let { " $it." } ?: ""))
    }
}
