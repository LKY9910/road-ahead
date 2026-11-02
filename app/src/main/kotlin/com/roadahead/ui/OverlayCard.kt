package com.roadahead.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.text.LineBreakConfig
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.roadahead.core.model.Card
import com.roadahead.core.model.LightColor
import com.roadahead.core.model.TourKind
import java.util.Locale
import kotlin.math.roundToInt

class OverlayCard(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(WindowManager::class.java)
    private var view: LinearLayout? = null
    private var lastCard: Card? = null
    private var failed = false
    private val params = WindowManager.LayoutParams(dp(560), WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT).apply {
        gravity = Gravity.BOTTOM or Gravity.END
        x = dp(176)
        y = dp(40)
        alpha = 0.8f
        setTitle("RoadAheadCard")
    }

    fun show(card: Card?) {
        if (failed) return
        if (!Settings.canDrawOverlays(context)) { close(); return }
        try {
            if (card == null) {
                view?.takeIf { it.visibility != View.GONE }?.let {
                    it.visibility = View.GONE
                    manager.updateViewLayout(it, params)
                }
                lastCard = null
                return
            }
            if (lastCard == card) return
            val root = view ?: LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(40), dp(28), dp(40), dp(36))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F0111418"))
                    cornerRadius = dp(32).toFloat()
                }
            }
            root.removeAllViews()
            render(root, card)
            root.visibility = View.VISIBLE
            if (view == null) {
                manager.addView(root, params)
                view = root
            } else {
                manager.updateViewLayout(root, params)
            }
            lastCard = card
        } catch (error: Exception) {
            failed = true
            Log.w("RoadAhead", "overlay stopped ${error.javaClass.simpleName}")
            close()
        }
    }

    fun close() {
        val attached = view
        view = null
        lastCard = null
        if (attached != null) {
            try { manager.removeView(attached) }
            catch (error: Exception) { Log.w("RoadAhead", "overlay remove failed ${error.javaClass.simpleName}") }
        }
    }

    private fun render(root: LinearLayout, card: Card) {
        if (card is Card.Travel) { renderTravel(root, card); return }
        if (card is Card.Parking) { renderParking(root, card); return }
        val kind = when (card) {
            is Card.Signal -> "신호"
            is Card.Hazard -> "도로 위험"
            is Card.Parking -> "주차"
            is Card.Travel -> "여행"
        }
        root.addView(text("앞길 안내 · $kind", 24f, "#A7B0BA"),
            LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
        val (color, description) = when (card) {
            is Card.Signal -> when (card.signal.color) {
                LightColor.RED -> "#E5484D" to "빨간불"
                LightColor.GREEN -> "#30A46C" to "초록불"
                LightColor.YELLOW -> "#F5C518" to "노란불"
                LightColor.UNKNOWN -> "#6B7280" to "신호 정보 없음"
            }
            is Card.Hazard -> "#F5A524" to "도로 위험"
            is Card.Parking -> "#3E7BFA" to "주차"
            is Card.Travel -> "#30A46C" to "여행"
        }
        val body = run {
            val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(View(context).apply {
                contentDescription = description
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(color)) }
            }, LinearLayout.LayoutParams(dp(88), dp(88)).apply { marginEnd = dp(24) })
            val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            row.addView(column, LinearLayout.LayoutParams(0, -2, 1f))
            root.addView(row, LinearLayout.LayoutParams(-1, -2))
            column
        }
        when (card) {
            is Card.Signal -> {
                body.addView(text("${card.signal.movement.label} 신호", 34f))
                body.addView(text(card.signal.remainingSec?.let { "${it}초" } ?: "—", 96f))
            }
            is Card.Hazard -> {
                val sentenceEnd = card.text.indexOf(". ")
                val title = if (sentenceEnd < 0) card.text else card.text.substring(0, sentenceEnd + 1)
                body.addView(text(title, 34f, lines = 2))
                if (sentenceEnd >= 0) {
                    val detail = card.text.substring(sentenceEnd + 2)
                    if (detail.isNotBlank()) body.addView(text(detail, 26f, "#C9D1D9", lines = 2))
                }
            }
            else -> Unit
        }
    }

    private fun renderTravel(root: LinearLayout, card: Card.Travel) {
        val lines = card.lines.take(5).toMutableList()
        do {
            root.removeAllViews()
            val weather = card.weatherNote?.takeIf { lines.any { it.kind == TourKind.CULTURE } }
            root.addView(flowText("앞길 안내 · 여행", 24f, 34, "#A7B0BA"), margins(bottom = if (weather == null) 20 else 4))
            if (weather != null) root.addView(flowText(weather, 20f, 29, "#C9D1D9"), margins(bottom = 20))
            lines.forEachIndexed { index, line ->
                root.addView(flowText(line.title, 26f, 38), margins(top = if (index == 0) 0 else 12))
                val meta = flowText("", 20f, 29, "#C9D1D9")
                val base = "${line.kind.label} · ${line.distanceText}"
                meta.text = line.periodText?.let { period ->
                    if (meta.paint.measureText("$base · $period") <= dp(480)) "$base · $period" else "$base\n$period"
                } ?: base
                root.addView(meta, margins())
                if (line.kind != TourKind.FESTIVAL) {
                    val reason = flowText("", 20f, 29, "#B8D4FF")
                    val labels = line.reasons.take(2).toMutableList()
                    while (labels.isNotEmpty() && reason.paint.measureText(labels.joinToString(" · ")) > dp(480)) labels.removeAt(labels.lastIndex)
                    if (labels.isNotEmpty()) {
                        reason.text = labels.joinToString(" · ")
                        root.addView(reason, margins(top = 4))
                    }
                }
            }
            root.measure(View.MeasureSpec.makeMeasureSpec(dp(560), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            if (root.measuredHeight <= dp(880) || lines.isEmpty()) break
            lines.removeAt(lines.lastIndex)
        } while (true)
    }

    private fun renderParking(root: LinearLayout, card: Card.Parking) {
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(View(context).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#3E7BFA")) }
        }, LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(12) })
        header.addView(flowText("앞길 안내 · 주차", 24f, 34, "#A7B0BA"))
        root.addView(header, margins(bottom = 20))
        val parking = card.parking
        root.addView(flowText(parking.name, 34f, 48, medium = true), margins(bottom = if (parking.available == null) 20 else 16))
        parking.available?.let { available ->
            val label = flowText("빈자리", 32f, 44)
            val unit = flowText("면", 32f, 44)
            var size = 96f
            val number = flowText(available.toString(), size, 144, medium = true)
            val fullWidth = label.paint.measureText("빈자리") + number.paint.measureText(number.text.toString()) + unit.paint.measureText("면") + dp(24)
            val split = fullWidth > dp(480)
            if (split) {
                for (candidate in listOf(96f, 80f, 72f)) {
                    number.textSize = candidate
                    size = candidate
                    if (number.paint.measureText(number.text.toString()) + unit.paint.measureText("면") + dp(12) <= dp(480)) break
                }
            }
            val fits = number.paint.measureText(number.text.toString()) + unit.paint.measureText("면") + dp(12) <= dp(480)
            if (fits) {
                if (split) root.addView(label, margins())
                setFlowHeight(number, if (size == 96f) 144 else (size * 1.5f).roundToInt())
                val row = LinearLayout(context).apply { isBaselineAligned = true }
                if (!split) row.addView(label, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(12) })
                row.addView(number, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(12) })
                row.addView(unit)
                root.addView(row, margins(bottom = 20))
            } else Log.w("RoadAhead", "parking count does not fit")
        }
        val meta = flowText("", 30f, 44, "#C9D1D9")
        meta.text = parking.feeText?.let { fee ->
            val joined = "${parking.distanceText} · $fee"
            if (meta.paint.measureText(joined) <= dp(480)) joined else "${parking.distanceText}\n$fee"
        } ?: parking.distanceText
        root.addView(meta, margins())
    }

    private fun margins(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(top); bottomMargin = dp(bottom)
    }

    private fun flowText(value: String, size: Float, height: Int, color: String = "#FFFFFF", medium: Boolean = false) = TextView(context).apply {
        applyKoreanWordBreak()
        text = value
        textSize = size
        includeFontPadding = false
        if (medium) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(Color.parseColor(color))
        setFlowHeight(this, height)
    }

    private fun setFlowHeight(view: TextView, height: Int) {
        val fontHeight = view.paint.fontMetricsInt.let { it.descent - it.ascent }
        val lineHeight = maxOf(dp(height), fontHeight)
        view.setLineHeight(lineHeight)
        // TextView omits extra line spacing after its last line.
        view.setPadding(0, 0, 0, lineHeight - fontHeight)
    }

    private fun text(value: String, size: Float, color: String = "#FFFFFF", lines: Int = 1) = TextView(context).apply {
        applyKoreanWordBreak()
        text = value
        textSize = size
        setTextColor(Color.parseColor(color))
        maxLines = lines
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun TextView.applyKoreanWordBreak() {
        if (Build.VERSION.SDK_INT >= 33) {
            textLocale = Locale.KOREAN
            lineBreakWordStyle = LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
        }
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
}
