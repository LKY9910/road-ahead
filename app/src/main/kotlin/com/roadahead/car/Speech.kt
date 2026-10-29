package com.roadahead.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class Speech(context: Context) {
    private val tts = TextToSpeech(context, Mode.ON_DEVICE)
    private val handler = Handler(Looper.getMainLooper())
    private var closed = false
    private val _available = MutableStateFlow(false)
    private val _speaking = MutableStateFlow(false)
    val available = _available.asStateFlow()
    val speaking = _speaking.asStateFlow()
    private val timeout = Runnable {
        if (_speaking.value) { tts.stop(); _speaking.value = false; Log.w("RoadAhead", "TTS callback timeout") }
    }
    private fun ended() { _speaking.value = false; handler.removeCallbacks(timeout) }
    private val listener = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() { if (!closed) _speaking.value = true; Log.i("RoadAhead", "TTS onStart") }
        override fun onDone() { ended(); Log.i("RoadAhead", "TTS onDone") }
        override fun onStop() = ended()
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onError(errorType: ErrorType) { ended(); Log.w("RoadAhead", "TTS error=$errorType") }
    }
    init {
        try {
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    if (!closed) {
                        tts.setLocale(Locale.KOREA)
                        tts.addEventListener(listener)
                        _available.value = true
                        Log.i("RoadAhead", "TTS available=true")
                    }
                }
                override fun onFailure(errorType: ErrorType) { _available.value = false; Log.w("RoadAhead", "TTS initialization=$errorType") }
            })
        } catch (e: Exception) { Log.w("RoadAhead", "TTS initialization failed ${e.javaClass.simpleName}") }
    }
    fun speak(text: String) {
        if (closed || !_available.value || _speaking.value) return
        try {
            _speaking.value = true
            handler.postDelayed(timeout, 15_000)
            tts.speak(text)
        } catch (e: Exception) { ended(); _available.value = false; Log.w("RoadAhead", "TTS failed ${e.javaClass.simpleName}") }
    }
    fun release() {
        closed = true
        _available.value = false
        ended()
        tts.removeEventListener(listener)
        tts.stop()
        tts.release()
    }
}
