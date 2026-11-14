package com.levelup.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.levelup.app.R
import com.levelup.app.domain.ActivityChartMath
import com.levelup.app.ui.theme.LvAccent
import com.levelup.app.ui.theme.LvBg
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvText
import kotlin.math.roundToInt

/**
 * Glance cannot draw a Compose Canvas, so the widget chart is rendered with the
 * same monotone geometry into a transparent bitmap at roughly 3x density.
 */
object ActivityChartBitmap {
    fun render(
        context: Context,
        values: List<Float>,
        labels: List<String>,
        widthPx: Int = 900,
        heightPx: Int = 330,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = 3f
        val left = 27f * density
        val right = widthPx - 12f * density
        val top = 14f * density
        val bottom = heightPx - 20f * density
        val typeface = ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular)
            ?: Typeface.MONOSPACE

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 11f * density
            color = LvMuted.toArgb()
        }
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LvBorder.toArgb()
            strokeWidth = 0.5f * density
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = LvAccent.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        listOf(0f, 50f, 100f).forEach { value ->
            val y = ActivityChartMath.yFor(value, top, bottom)
            canvas.drawLine(left, y, right, y, gridPaint)
            labelPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                value.roundToInt().toString(),
                left - 4f * density,
                y - (labelPaint.descent() + labelPaint.ascent()) / 2f,
                labelPaint,
            )
        }

        if (values.size < 2) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = LvMuted.toArgb()
                strokeWidth = 1.5f * density
                pathEffect = DashPathEffect(floatArrayOf(4f * density, 5f * density), 0f)
            }
            val middle = (top + bottom) / 2f
            canvas.drawLine(left, middle, right, middle, emptyPaint)
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(
                context.getString(R.string.activity_empty),
                (left + right) / 2f,
                middle - 10f * density,
                labelPaint,
            )
            return bitmap
        }

        val segments = ActivityChartMath.segments(values, left, right, top, bottom)
        val line = Path().apply {
            moveTo(segments.first().startX, segments.first().startY)
            segments.forEach { segment ->
                cubicTo(
                    segment.control1X, segment.control1Y,
                    segment.control2X, segment.control2Y,
                    segment.endX, segment.endY,
                )
            }
        }
        val area = Path().apply {
            moveTo(segments.first().startX, bottom)
            lineTo(segments.first().startX, segments.first().startY)
            segments.forEach { segment ->
                cubicTo(
                    segment.control1X, segment.control1Y,
                    segment.control2X, segment.control2Y,
                    segment.endX, segment.endY,
                )
            }
            lineTo(segments.last().endX, bottom)
            close()
        }
        val areaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // 28% alpha, preserving the RGB of #FF3B3B.
            shader = LinearGradient(
                0f, top, 0f, bottom,
                android.graphics.Color.argb(72, 255, 59, 59),
                android.graphics.Color.TRANSPARENT,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(area, areaPaint)
        canvas.drawPath(line, accentPaint)

        values.forEachIndexed { index, value ->
            val x = ActivityChartMath.xFor(index, values.size, left, right)
            val y = ActivityChartMath.yFor(value, top, bottom)
            val latest = index == values.lastIndex
            if (latest) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        x, y, 9f * density,
                        android.graphics.Color.argb(80, 255, 59, 59),
                        android.graphics.Color.TRANSPARENT,
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawCircle(x, y, 9f * density, glowPaint)
                canvas.drawCircle(
                    x, y, 3.5f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LvText.toArgb() },
                )
                canvas.drawCircle(
                    x, y, 3.5f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = LvAccent.toArgb()
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * density
                    },
                )
            } else {
                canvas.drawCircle(
                    x, y, 2.5f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = LvBg.toArgb() },
                )
                canvas.drawCircle(
                    x, y, 2.5f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = LvAccent.toArgb()
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * density
                    },
                )
            }
        }

        labelPaint.textAlign = Paint.Align.CENTER
        labels.forEachIndexed { index, label ->
            if (index >= values.size) return@forEachIndexed
            val x = ActivityChartMath.xFor(index, values.size, left, right)
            canvas.drawText(label, x, heightPx - 4f * density, labelPaint)
        }
        return bitmap
    }
}
