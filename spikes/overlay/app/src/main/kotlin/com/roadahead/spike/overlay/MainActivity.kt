package com.roadahead.spike.overlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

open class MainActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(TextView(context).apply { text = "T-21 오버레이 시험 / ${this@MainActivity.javaClass.simpleName}"; textSize = 26f })
        }
        listOf("권한 설정" to "permission", "카드 표시 (서비스 없음)" to "show",
            "카드 표시 (FGS)" to "service", "카드 숨기기" to "hide",
            "화면 종료" to "finish").forEach { (label, op) ->
            layout.addView(Button(this).apply { text = label; setOnClickListener { runOp(op, intent) } })
        }
        setContentView(layout)
        runOp(intent.getStringExtra("op"), intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runOp(intent.getStringExtra("op"), intent)
    }

    override fun onDestroy() {
        Log.i("OverlaySpike", "destroyed=${javaClass.simpleName} finishing=$isFinishing")
        super.onDestroy()
    }

    private fun runOp(op: String?, source: Intent) {
        Log.i("OverlaySpike", "activity=${javaClass.simpleName} op=$op canDraw=${Settings.canDrawOverlays(this)}")
        try {
            when (op) {
                "permission" -> startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                "show" -> OverlayCard.show(this, source.getFloatExtra("alpha", 0.8f))
                "service" -> startForegroundService(Intent(this, OverlayService::class.java))
                "hide" -> { stopService(Intent(this, OverlayService::class.java)); OverlayCard.hide() }
                "finish" -> finishAndRemoveTask()
            }
        } catch (error: Exception) {
            Log.e("OverlaySpike", "op=$op failed", error)
        }
    }
}

class DoActivity : MainActivity()
