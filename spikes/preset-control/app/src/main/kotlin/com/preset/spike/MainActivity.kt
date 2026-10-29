package com.preset.spike

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var output: TextView
    private lateinit var probe: VehicleProbe
    private lateinit var speech: SpeechProbe
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        output = TextView(this).apply { textSize = 17f; setTextIsSelectable(true) }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (op in listOf("capabilities", "motion", "target", "state", "tts")) {
            layout.addView(Button(this).apply { text = op; setOnClickListener { run(op) } })
        }
        layout.addView(output)
        setContentView(ScrollView(this).apply { addView(layout) })
        probe = VehicleProbe(this, ::record)
        speech = SpeechProbe(this, ::record)
        probe.initialize()
        scope.launch { delay(1500); run(intent.getStringExtra("op") ?: "capabilities") }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        run(intent.getStringExtra("op") ?: "state")
    }

    private fun record(message: String) {
        val line = "${SystemClock.elapsedRealtime()} $message"
        Log.i("T13", line)
        runOnUiThread { output.append("$line\n") }
    }

    private fun run(op: String) {
        if (job?.isActive == true) { record("BUSY op=$op"); return }
        job = scope.launch {
            record("BEGIN $op")
            try {
                when (op) {
                    "capabilities" -> probe.capabilities()
                    "motion" -> probe.motion()
                    "target" -> probe.targets()
                    "state" -> probe.state()
                    "tts" -> speech.tts()
                    else -> record("UNKNOWN op=$op")
                }
            } catch (e: Exception) { record("FAIL $op ${e.javaClass.simpleName}: ${e.message}") }
            finally { record("END $op") }
        }
    }

    override fun onDestroy() {
        job?.cancel()
        probe.close()
        speech.close()
        scope.cancel()
        super.onDestroy()
    }
}
