package com.roadahead.core.speak

import com.roadahead.core.model.Advice

class Announcer(private val naviGapMs: Long = 200, private val forgetAfterMs: Long = 60_000) {
    private val spoken = mutableMapOf<String, Long>()
    private var lastNaviActiveMs: Long? = null

    fun pick(valid: List<Advice>, naviSpeaking: Boolean, selfSpeaking: Boolean, nowMonoMs: Long): Advice? {
        for (advice in valid) if (advice.key in spoken) spoken[advice.key] = nowMonoMs
        spoken.entries.removeAll { nowMonoMs - it.value > forgetAfterMs }
        if (naviSpeaking) lastNaviActiveMs = nowMonoMs
        if (naviSpeaking || selfSpeaking || lastNaviActiveMs?.let { nowMonoMs - it < naviGapMs } == true) return null
        val next = valid.filter { it.key !in spoken }.minByOrNull { it.priority } ?: return null
        spoken[next.key] = nowMonoMs
        return next
    }
}
