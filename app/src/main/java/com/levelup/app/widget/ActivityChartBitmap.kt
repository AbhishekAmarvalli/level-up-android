package com.levelup.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.levelup.app.R
import com.levelup.app.domain.ActivityChartMath
import com.levelup.app.ui.theme.C
import kotlin.math.roundToInt

/**
 * Glance cannot draw a Compose Canvas, so the widget chart uses the same
 * monotone geometry and exact colors at 3x density: 110 dp plot + 16 dp labels.
 */
object ActivityChartBitmap {
    fun render(
        context: Context,
        values: List<Float>,
        labels: List<String>,
        widthPx: Int = 900,
        heightPx: Int = 378,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = heightPx / 126f
        val left = 22f * density
        val right = widthPx - 8f * density
        val top = 6f * density
        val bottom = 106f * density
        val mono = ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular)
            ?: Typeface.MONOSPACE
        val sans = ResourcesCompat.getFont(context, R.font.inter_regular)
            ?: Typeface.SANS_SERIF

        val yPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = mono
            textSize = 11f * density
            color = C.Muted.toArgb()
            fontFeatureSettings = "tnum"
        }
        val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 11f * density
            color = C.Muted.toArgb()
        }
        val xLastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 11f * density
            color = C.Text.toArgb()
        }
        val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sans
            textSize = 12f * density
            color = C.Muted.toArgb()
            textAlign = Paint.Align.CENTER
        }
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = C.Border.toArgb()
            strokeWidth = 0.5f * density
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = C.Red.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        listOf(0f, 50f, 100f).forEach { value ->
            val y = ActivityChartMath.yFor(value, top, bottom)
            canvas.drawLine(left, y, right, y, grid)
            yPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                value.roundToInt().toString(),
                left - 4f * density,
                y - (yPaint.descent() + yPaint.ascent()) / 2f,
                yPaint,
            )
        }

        if (values.size < 2) {
            val emptyPaintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = C.Muted.toArgb()
                strokeWidth = 1.5f * density
                pathEffect = DashPathEffect(floatArrayOf(4f * density, 5f * density), 0f)
            }
            val y = ActivityChartMath.yFor(0f, top, bottom)
            canvas.drawLine(left, y, right, y, emptyPaintLine)
            canvas.drawText(
                context.getString(R.string.activity_empty),
                (left + right) / 2f,
                y - 10f * density,
                emptyPaint,
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
            shader = LinearGradient(
                0f, top, 0f, bottom,
                C.Red.copy(alpha = 0.28f).toArgb(),
                C.Red.copy(alpha = 0f).toArgb(),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(area, areaPaint)
        canvas.drawPath(line, stroke)

        values.forEachIndexed { index, value ->
            val x = ActivityChartMath.xFor(index, values.size, left, right)
            val y = ActivityChartMath.yFor(value, top, bottom)
            val last = index == values.lastIndex
            if (last) {
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = C.Red.toArgb()
                    alpha = 64
                }
                canvas.drawCircle(x, y, 9f * density, glow)
                canvas.drawCircle(
                    x,
                    y,
                    4f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.Text.toArgb() },
                )
                canvas.drawCircle(
                    x,
                    y,
                    4f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = C.Red.toArgb()
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * density
                    },
                )
            } else {
                canvas.drawCircle(
                    x,
                    y,
                    2.5f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.Bg.toArgb() },
                )
                canvas.drawCircle(
                    x,
                    y,
                    2.5f * density,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = C.Red.toArgb()
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * density
                    },
                )
            }
        }

        labels.forEachIndexed { index, label ->
            if (index >= values.size) return@forEachIndexed
            val x = ActivityChartMath.xFor(index, values.size, left, right)
            val paint = if (index == values.lastIndex) xLastPaint else xPaint
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, x, heightPx - 4f * density, paint)
        }
        return bitmap
    }
}
