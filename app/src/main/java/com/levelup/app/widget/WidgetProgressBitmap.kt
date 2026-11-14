package com.levelup.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.toArgb
import com.levelup.app.ui.theme.C

/** Glance has no percentage-width modifier, so tier bars use a proportional bitmap. */
object WidgetProgressBitmap {
    fun render(
        fraction: Float,
        widthPx: Int = 300,
        heightPx: Int = 10,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = heightPx / 2f
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.Border.toArgb() }
        canvas.drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radius, radius, track)
        val safe = fraction.coerceIn(0f, 1f)
        if (safe > 0f) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = C.Text.toArgb() }
            canvas.drawRoundRect(
                RectF(0f, 0f, widthPx * safe, heightPx.toFloat()),
                radius,
                radius,
                fill,
            )
        }
        return bitmap
    }
}
