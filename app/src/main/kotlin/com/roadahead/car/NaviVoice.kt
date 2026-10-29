package com.roadahead.car

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NaviVoice(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val _speaking = MutableStateFlow(false)
    val speaking = _speaking.asStateFlow()
    private fun update(configs: List<AudioPlaybackConfiguration>) {
        _speaking.value = configs.any { it.audioAttributes.usage == AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE }
    }
    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) = update(configs)
    }
    fun start() {
        audio.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        update(audio.activePlaybackConfigurations)
    }
    fun stop() { audio.unregisterAudioPlaybackCallback(callback); _speaking.value = false }
}
