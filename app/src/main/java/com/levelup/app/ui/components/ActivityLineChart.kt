package com.levelup.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.levelup.app.R
import com.levelup.app.domain.ActivityChartMath
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import kotlin.math.roundToInt

private const val PLOT_HEIGHT_DP = 110f
private const val TOTAL_HEIGHT_DP = 126f

/**
 * Monotone curved activity graph. The plot is 110 dp high with another 16 dp
 * below it for labels. Geometry comes from ActivityChartMath, which clamps all
 * Fritsch-Carlson control points to the plot.
 */
@Composable
fun ActivityLineChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var selected by remember(values, labels) { mutableStateOf<Int?>(null) }
    val reveal = remember { Animatable(0f) }
    val monoTypeface = remember {
        ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) ?: Typeface.MONOSPACE
    }
    val sansTypeface = remember {
        ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.SANS_SERIF
    }
    val yPaint = remember(density, monoTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = monoTypeface
            textSize = with(density) { T.Label11.fontSize.toPx() }
            color = C.Muted.toArgb()
            fontFeatureSettings = "tnum"
        }
    }
    val xPaint = remember(density, sansTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansTypeface
            textSize = with(density) { T.Body11Muted.fontSize.toPx() }
            color = C.Muted.toArgb()
        }
    }
    val xLastPaint = remember(density, sansTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansTypeface
            textSize = with(density) { T.Body11Muted.fontSize.toPx() }
            color = C.Text.toArgb()
        }
    }
    val emptyPaint = remember(density, sansTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansTypeface
            textSize = with(density) { T.Body12Muted.fontSize.toPx() }
            color = C.Muted.toArgb()
            textAlign = Paint.Align.CENTER
        }
    }
    val tooltipPaint = remember(density, monoTypeface) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = monoTypeface
            textSize = with(density) { T.Mono12.fontSize.toPx() }
            color = C.Text.toArgb()
            fontFeatureSettings = "tnum"
        }
    }
    val emptyText = stringResource(R.string.activity_empty)

    LaunchedEffect(values, labels) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(800))
    }

    fun plotRect(width: Float, heightPx: Float): Rect {
        val left = with(density) { 22.dp.toPx() }
        val right = (width - with(density) { 8.dp.toPx() }).coerceAtLeast(left + 1f)
        val top = with(density) { 6.dp.toPx() }
        val bottom = (with(density) { PLOT_HEIGHT_DP.dp.toPx() } - with(density) { 4.dp.toPx() })
            .coerceAtLeast(top + 1f)
        return Rect(left, top, right, bottom)
    }

    fun nearest(x: Float, width: Float, heightPx: Float): Int {
        val rect = plotRect(width, heightPx)
        return ActivityChartMath.nearestIndex(x, values.size, rect.left, rect.right)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TOTAL_HEIGHT_DP.dp)
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
        val rect = plotRect(size.width, size.height)
        drawGrid(rect, yPaint)
        if (values.size < 2) {
            drawEmpty(rect, emptyPaint, emptyText)
            return@Canvas
        }

        val segments = ActivityChartMath.segments(
            values = values,
            left = rect.left,
            right = rect.right,
            top = rect.top,
            bottom = rect.bottom,
        )
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
            path = area,
            brush = Brush.verticalGradient(
                colors = listOf(C.Red.copy(alpha = 0.28f), Color.Transparent),
                startY = rect.top,
                endY = rect.bottom,
            ),
            alpha = reveal.value,
        )

        if (reveal.value > 0f) {
            val source = PathMeasure().apply { setPath(line, false) }
            val visible = Path()
            source.getSegment(0f, source.length * reveal.value, visible, true)
            drawPath(
                path = visible,
                color = C.Red,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        val shown = reveal.value * (values.lastIndex.toFloat())
        values.forEachIndexed { index, value ->
            if (index.toFloat() > shown + 0.001f) return@forEachIndexed
            val offset = Offset(
                ActivityChartMath.xFor(index, values.size, rect.left, rect.right),
                ActivityChartMath.yFor(value, rect.top, rect.bottom),
            )
            if (index == values.lastIndex) {
                drawCircle(
                    color = C.Red.copy(alpha = 0.25f),
                    radius = 9.dp.toPx(),
                    center = offset,
                )
                drawCircle(color = C.Text, radius = 4.dp.toPx(), center = offset)
                drawCircle(
                    color = C.Red,
                    radius = 4.dp.toPx(),
                    center = offset,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            } else {
                drawCircle(color = C.Bg, radius = 2.5.dp.toPx(), center = offset)
                drawCircle(
                    color = C.Red,
                    radius = 2.5.dp.toPx(),
                    center = offset,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }

        drawXLabels(rect, values, labels, xPaint, xLastPaint)
        selected?.let { index ->
            if (index in values.indices) {
                drawSelection(rect, values, labels, index, tooltipPaint)
            }
        }
    }
}

private fun DrawScope.drawGrid(rect: Rect, paint: Paint) {
    val canvas = drawContext.canvas.nativeCanvas
    listOf(0f, 50f, 100f).forEach { value ->
        val y = ActivityChartMath.yFor(value, rect.top, rect.bottom)
        drawLine(
            color = C.Border,
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
    lastPaint: Paint,
) {
    val canvas = drawContext.canvas.nativeCanvas
    val baseline = size.height - 4.dp.toPx()
    labels.forEachIndexed { index, label ->
        if (index >= values.size) return@forEachIndexed
        val x = ActivityChartMath.xFor(index, values.size, rect.left, rect.right)
        val labelPaint = if (index == values.lastIndex) lastPaint else paint
        labelPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, x, baseline, labelPaint)
    }
}

private fun DrawScope.drawEmpty(rect: Rect, paint: Paint, message: String) {
    val y = ActivityChartMath.yFor(0f, rect.top, rect.bottom)
    drawLine(
        color = C.Muted,
        start = Offset(rect.left, y),
        end = Offset(rect.right, y),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(4.dp.toPx(), 5.dp.toPx()),
        ),
    )
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
        color = C.Muted,
        start = Offset(x, rect.top),
        end = Offset(x, rect.bottom),
        strokeWidth = 0.5.dp.toPx(),
    )
    drawCircle(color = C.Text, radius = 4.dp.toPx(), center = Offset(x, y))
    drawCircle(
        color = C.Red,
        radius = 4.dp.toPx(),
        center = Offset(x, y),
        style = Stroke(width = 1.5.dp.toPx()),
    )

    val text = "${labels.getOrElse(index) { "" }}  ${values[index].roundToInt()}%"
    val horizontal = 8.dp.toPx()
    val vertical = 4.dp.toPx()
    val textWidth = textPaint.measureText(text)
    val pillWidth = textWidth + horizontal * 2f
    val textHeight = textPaint.descent() - textPaint.ascent()
    val pillHeight = textHeight + vertical * 2f
    val left = (x - pillWidth / 2f).coerceIn(0f, (size.width - pillWidth).coerceAtLeast(0f))
    val above = y - pillHeight - 4.dp.toPx()
    val top = (if (above >= 0f) above else y + 4.dp.toPx())
        .coerceIn(0f, (size.height - pillHeight).coerceAtLeast(0f))
    val corner = CornerRadius(8.dp.toPx())
    drawRoundRect(
        color = C.Surface,
        topLeft = Offset(left, top),
        size = Size(pillWidth, pillHeight),
        cornerRadius = corner,
    )
    drawRoundRect(
        color = C.Border,
        topLeft = Offset(left, top),
        size = Size(pillWidth, pillHeight),
        cornerRadius = corner,
        style = Stroke(width = 0.5.dp.toPx()),
    )
    textPaint.textAlign = Paint.Align.CENTER
    val baseline = top + vertical - textPaint.ascent()
    drawIntoCanvas { _ ->
        drawContext.canvas.nativeCanvas.drawText(
            text,
            left + pillWidth / 2f,
            baseline,
            textPaint,
        )
    }
}
