package com.levelup.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.levelup.app.R
import com.levelup.app.domain.ActivityChartMath
import com.levelup.app.ui.theme.LvAccent
import com.levelup.app.ui.theme.LvBg
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvSurface
import com.levelup.app.ui.theme.LvText
import com.levelup.app.ui.theme.Mono
import kotlin.math.roundToInt

/**
 * Curved activity chart used by the app. Values are percentages (0..100), labels
 * are real weekday initials or W1..W4. Draws in left to right and supports a
 * press/drag tooltip without introducing any Material chrome.
 */
@Composable
fun ActivityLineChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var selected by remember(values, labels) { mutableStateOf<Int?>(null) }
    val drawProgress = remember { Animatable(0f) }
    val typeface = remember {
        ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) ?: Typeface.MONOSPACE
    }
    val labelPaint = remember(density, typeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = with(density) { 11.sp.toPx() }
            color = LvMuted.toArgb()
        }
    }
    val tooltipPaint = remember(density, typeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = with(density) { 12.sp.toPx() }
            color = LvText.toArgb()
        }
    }
    val emptyMessage = androidx.compose.ui.res.stringResource(R.string.activity_empty)

    LaunchedEffect(values, labels) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    fun chartRect(width: Float, heightPx: Float): Rect {
        val left = with(density) { 26.dp.toPx() }
        val right = (width - with(density) { 12.dp.toPx() }).coerceAtLeast(left + 1f)
        val top = with(density) { 14.dp.toPx() }
        val bottom = (heightPx - with(density) { 20.dp.toPx() }).coerceAtLeast(top + 1f)
        return Rect(left, top, right, bottom)
    }

    fun nearest(x: Float, width: Float, heightPx: Float): Int {
        val rect = chartRect(width, heightPx)
        return ActivityChartMath.nearestIndex(x, values.size, rect.left, rect.right)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(values, labels) {
                detectTapGestures(
                    onPress = { offset ->
                        selected = nearest(offset.x, size.width.toFloat(), size.height.toFloat())
                        tryAwaitRelease()
                        selected = null
                    },
                )
            }
            .pointerInput(values, labels) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selected = nearest(offset.x, size.width.toFloat(), size.height.toFloat())
                    },
                    onDrag = { change, _ ->
                        selected = nearest(
                            change.position.x,
                            size.width.toFloat(),
                            size.height.toFloat(),
                        )
                        change.consume()
                    },
                    onDragEnd = { selected = null },
                    onDragCancel = { selected = null },
                )
            },
    ) {
        val rect = chartRect(size.width, size.height)
        drawGrid(rect, labelPaint, values.isNotEmpty())
        if (values.size < 2) {
            drawEmptyChart(rect, labelPaint, emptyMessage)
            return@Canvas
        }

        val segments = ActivityChartMath.segments(
            values = values,
            left = rect.left,
            right = rect.right,
            top = rect.top,
            bottom = rect.bottom,
        )
        val linePath = Path().apply {
            moveTo(segments.first().startX, segments.first().startY)
            segments.forEach { segment ->
                cubicTo(
                    segment.control1X, segment.control1Y,
                    segment.control2X, segment.control2Y,
                    segment.endX, segment.endY,
                )
            }
        }
        val areaPath = Path().apply {
            moveTo(segments.first().startX, rect.bottom)
            lineTo(segments.first().startX, segments.first().startY)
            segments.forEach { segment ->
                cubicTo(
                    segment.control1X, segment.control1Y,
                    segment.control2X, segment.control2Y,
                    segment.endX, segment.endY,
                )
            }
            lineTo(segments.last().endX, rect.bottom)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(LvAccent.copy(alpha = 0.28f), Color.Transparent),
                startY = rect.top,
                endY = rect.bottom,
            ),
            alpha = drawProgress.value,
        )

        if (drawProgress.value > 0f) {
            val measure = PathMeasure()
            measure.setPath(linePath, false)
            val visible = Path()
            val length = measure.length
            if (length > 0f) {
                measure.getSegment(0f, length * drawProgress.value, visible, true)
                drawPath(
                    path = visible,
                    color = LvAccent,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        val revealIndex = (drawProgress.value * (values.size - 1)).toInt()
        values.forEachIndexed { index, value ->
            if (index > revealIndex + 0) return@forEachIndexed
            val point = Offset(
                ActivityChartMath.xFor(index, values.size, rect.left, rect.right),
                ActivityChartMath.yFor(value, rect.top, rect.bottom),
            )
            val latest = index == values.lastIndex
            if (latest) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(LvAccent.copy(alpha = 0.3f), Color.Transparent),
                        center = point,
                        radius = 9.dp.toPx(),
                    ),
                    radius = 9.dp.toPx(),
                    center = point,
                )
                drawCircle(color = LvText, radius = 3.5.dp.toPx(), center = point)
            } else {
                drawCircle(color = LvBg, radius = 2.5.dp.toPx(), center = point)
            }
            drawCircle(
                color = LvAccent,
                radius = if (latest) 3.5.dp.toPx() else 2.5.dp.toPx(),
                center = point,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        drawXLabels(rect, values, labels, labelPaint)
        selected?.let { index ->
            drawSelection(rect, values, labels, index, tooltipPaint)
        }
    }
}

private fun DrawScope.drawGrid(rect: Rect, paint: Paint, hasData: Boolean) {
    val canvas = drawContext.canvas.nativeCanvas
    val rows = listOf(0f, 50f, 100f)
    rows.forEach { value ->
        val y = ActivityChartMath.yFor(value, rect.top, rect.bottom)
        drawLine(
            color = LvBorder,
            start = Offset(rect.left, y),
            end = Offset(rect.right, y),
            strokeWidth = 0.5.dp.toPx(),
        )
        paint.textAlign = Paint.Align.RIGHT
        val baseline = y - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(value.roundToInt().toString(), rect.left - 4.dp.toPx(), baseline, paint)
    }
}

private fun DrawScope.drawXLabels(
    rect: Rect,
    values: List<Float>,
    labels: List<String>,
    paint: Paint,
) {
    if (values.isEmpty()) return
    val canvas = drawContext.canvas.nativeCanvas
    paint.textAlign = Paint.Align.CENTER
    labels.forEachIndexed { index, label ->
        if (index >= values.size) return@forEachIndexed
        val x = ActivityChartMath.xFor(index, values.size, rect.left, rect.right)
        canvas.drawText(label, x, size.height - 4.dp.toPx(), paint)
    }
}

private fun DrawScope.drawEmptyChart(rect: Rect, paint: Paint, message: String) {
    val y = (rect.top + rect.bottom) / 2f
    drawLine(
        color = LvMuted,
        start = Offset(rect.left, y),
        end = Offset(rect.right, y),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(4.dp.toPx(), 5.dp.toPx()),
        ),
    )
    paint.textAlign = Paint.Align.CENTER
    drawContext.canvas.nativeCanvas.drawText(
        message,
        (rect.left + rect.right) / 2f,
        y - 10.dp.toPx(),
        paint,
    )
}

private fun DrawScope.drawSelection(
    rect: Rect,
    values: List<Float>,
    labels: List<String>,
    index: Int,
    textPaint: Paint,
) {
    val x = ActivityChartMath.xFor(index, values.size, rect.left, rect.right)
    val y = ActivityChartMath.yFor(values[index], rect.top, rect.bottom)
    drawLine(
        color = LvAccent.copy(alpha = 0.45f),
        start = Offset(x, rect.top),
        end = Offset(x, rect.bottom),
        strokeWidth = 0.5.dp.toPx(),
    )
    drawCircle(color = LvText, radius = 4.dp.toPx(), center = Offset(x, y))
    drawCircle(
        color = LvAccent,
        radius = 4.dp.toPx(),
        center = Offset(x, y),
        style = Stroke(width = 1.5.dp.toPx()),
    )

    val text = "${labels.getOrElse(index) { "" }}  ${values[index].roundToInt()}%"
    val horizontalPadding = 6.dp.toPx()
    val pillHeight = 20.dp.toPx()
    val textWidth = textPaint.measureText(text)
    val pillWidth = textWidth + horizontalPadding * 2f
    val pillLeft = (x - pillWidth / 2f).coerceIn(0f, size.width - pillWidth)
    val above = y - pillHeight - 6.dp.toPx()
    val pillTop = (if (above >= 0f) above else y + 6.dp.toPx())
        .coerceIn(0f, (size.height - pillHeight).coerceAtLeast(0f))
    val pillRect = Rect(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight)
    drawRoundRect(
        color = LvSurface,
        topLeft = Offset(pillRect.left, pillRect.top),
        size = Size(pillRect.width, pillRect.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
    )
    drawRoundRect(
        color = LvBorder,
        topLeft = Offset(pillRect.left, pillRect.top),
        size = Size(pillRect.width, pillRect.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
        style = Stroke(width = 0.5.dp.toPx()),
    )
    val baseline = pillRect.top + (pillRect.height - (textPaint.descent() + textPaint.ascent())) / 2f
    textPaint.textAlign = Paint.Align.CENTER
    drawContext.canvas.nativeCanvas.drawText(
        text,
        pillRect.center.x,
        baseline,
        textPaint,
    )
}
