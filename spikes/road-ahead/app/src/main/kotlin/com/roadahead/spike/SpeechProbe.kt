package com.roadahead.spike

import android.content.Context
import android.os.SystemClock
import android.media.AudioAttributes
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import kotlinx.coroutines.*
import java.util.Locale

class SpeechProbe(private val context: Context, private val log: (String) -> Unit) {
    private var tts: TextToSpeech? = null
    private var ttsListener: EventListener? = null

    suspend fun tts(navigationUsage: Boolean = false) {
        tts?.release()
        val engine = TextToSpeech(context, Mode.ON_DEVICE).also { tts = it }
        val ready = CompletableDeferred<Boolean>()
        val initAt = SystemClock.elapsedRealtime()
        engine.initialize(object : OnInitializeListener {
            override fun onSuccess() { log("08 TTS initialize success latencyMs=${SystemClock.elapsedRealtime()-initAt}"); ready.complete(true) }
            override fun onFailure(errorType: ErrorType) { log("08 TTS initialize failure=$errorType"); ready.complete(false) }
        })
        if (withTimeoutOrNull(8000) { ready.await() } != true) { log("08 TTS unavailable"); return }
        val done = CompletableDeferred<Unit>()
        var requestAt = 0L
        val listener = object : EventListener {
            override fun onReady() { log("08 TTS onReady") }
            override fun onStart() { log("08 TTS onStart latencyMs=${SystemClock.elapsedRealtime()-requestAt}") }
            override fun onDone() { log("08 TTS onDone elapsedMs=${SystemClock.elapsedRealtime()-requestAt}"); done.complete(Unit) }
            override fun onStop() { log("08 TTS onStop") }
            override fun onUpdatedRms(rms: Double) { log("08 TTS RMS=$rms") }
            override fun onError(errorType: ErrorType) { log("08 TTS onError=$errorType elapsedMs=${SystemClock.elapsedRealtime()-requestAt}"); done.complete(Unit) }
        }
        ttsListener = listener
        engine.addEventListener(listener)
        engine.setLocale(Locale.KOREA)
        val attributes = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(if (navigationUsage) AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE else AudioAttributes.USAGE_MEDIA).build()
        engine.setAudioAttributes(attributes)
        requestAt = SystemClock.elapsedRealtime()
        log("08 TTS speak mode=ON_DEVICE locale=ko-KR usage=${attributes.usage} text=앞 신호 빨간불이에요. 속도를 줄이세요.")
        engine.speak("앞 신호 빨간불이에요. 속도를 줄이세요.")
        if (withTimeoutOrNull(15000) { done.await() } == null) log("08 TTS callback timeout 15000ms")
        engine.stop()
        if (navigationUsage) engine.setAudioAttributes(AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build())
        engine.removeEventListener(listener)
        engine.release(); tts = null; ttsListener = null
    }

    fun close() {
        ttsListener?.let { tts?.removeEventListener(it) }; tts?.stop(); tts?.release()
    }
}
