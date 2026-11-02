package com.roadahead.spike.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.roundToInt

object OverlayCard {
    private var view: TextView? = null
    private var manager: WindowManager? = null

    fun show(context: Context, alpha: Float) {
        check(Settings.canDrawOverlays(context)) { "Overlay permission is not granted" }
        hide()
        val app = context.applicationContext
        val density = app.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).roundToInt()
        val wm = app.getSystemService(WindowManager::class.java)
        val card = TextView(app).apply {
            text = "오버레이 시험\n300 × 150 dp"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.rgb(28, 44, 68)); cornerRadius = dp(16).toFloat() }
        }
        val params = WindowManager.LayoutParams(dp(300), dp(150),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(24)
            y = dp(24)
            this.alpha = alpha.coerceIn(0f, 1f)
            setTitle("T21Overlay")
        }
        wm.addView(card, params)
        manager = wm
        view = card
        card.post {
            val xy = IntArray(2)
            card.getLocationOnScreen(xy)
            Log.i("OverlaySpike", "overlay xy=${xy.toList()} size=${card.width}x${card.height} density=$density alpha=${params.alpha} flags=${params.flags}")
        }
    }

    fun hide() {
        view?.let { manager?.removeView(it) }
        view = null
        manager = null
        Log.i("OverlaySpike", "overlay removed")
    }
}
