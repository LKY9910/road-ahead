package com.preset.spike

import android.content.Context
import android.os.SystemClock
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

    suspend fun tts() {
        tts?.release()
        val engine = TextToSpeech(context, Mode.ON_DEVICE).also { tts = it }
        val ready = CompletableDeferred<Boolean>()
        val initAt = SystemClock.elapsedRealtime()
        engine.initialize(object : OnInitializeListener {
            override fun onSuccess() { log("10 TTS initialize success latencyMs=${SystemClock.elapsedRealtime()-initAt}"); ready.complete(true) }
            override fun onFailure(errorType: ErrorType) { log("10 TTS initialize failure=$errorType"); ready.complete(false) }
        })
        if (withTimeoutOrNull(8000) { ready.await() } != true) { log("10 TTS unavailable"); return }
        val done = CompletableDeferred<Unit>()
        var requestAt = 0L
        val listener = object : EventListener {
            override fun onReady() { log("10 TTS onReady") }
            override fun onStart() { log("10 TTS onStart latencyMs=${SystemClock.elapsedRealtime()-requestAt}") }
            override fun onDone() { log("10 TTS onDone elapsedMs=${SystemClock.elapsedRealtime()-requestAt}"); done.complete(Unit) }
            override fun onStop() { log("10 TTS onStop") }
            override fun onUpdatedRms(rms: Double) { log("10 TTS RMS=$rms") }
            override fun onError(errorType: ErrorType) { log("10 TTS onError=$errorType elapsedMs=${SystemClock.elapsedRealtime()-requestAt}"); done.complete(Unit) }
        }
        ttsListener = listener
        engine.addEventListener(listener)
        engine.setLocale(Locale.KOREA)
        requestAt = SystemClock.elapsedRealtime()
        log("10 TTS speak mode=ON_DEVICE locale=ko-KR text=운전석과 동승석 프리셋을 적용했습니다.")
        engine.speak("운전석과 동승석 프리셋을 적용했습니다.")
        if (withTimeoutOrNull(15000) { done.await() } == null) log("10 TTS callback timeout 15000ms")
        engine.stop()
        engine.removeEventListener(listener)
        engine.release(); tts = null; ttsListener = null
    }

    fun close() {
        ttsListener?.let { tts?.removeEventListener(it) }; tts?.stop(); tts?.release()
    }
}
