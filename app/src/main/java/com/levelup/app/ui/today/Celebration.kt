package com.levelup.app.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.levelup.app.R
import com.levelup.app.ui.components.AppText
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val duration: Float,
    val spin: Float,
    val sway: Float,
    val color: androidx.compose.ui.graphics.Color,
)

@Composable
fun CelebrationOverlay(streak: Int, onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val pieces = remember {
        val palette = listOf(C.Red, C.Text, C.Muted)
        List(20) { index ->
            ConfettiPiece(
                x = Random.nextFloat(),
                delay = Random.nextFloat() * 0.22f,
                duration = 0.55f + Random.nextFloat() * 0.3f,
                spin = if (index % 2 == 0) 540f else -540f,
                sway = 14f + Random.nextFloat() * 30f,
                color = palette[index % palette.size],
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 2000, easing = LinearEasing))
        onFinished()
    }

    val p = progress.value
    val glow = sin(p * PI).toFloat().coerceIn(0f, 1f)
    val textAlpha = when {
        p < 0.12f -> p / 0.12f
        p < 0.82f -> 1f
        else -> ((1f - p) / 0.18f).coerceIn(0f, 1f)
    }

    Box(modifier = Modifier.fillMaxSize().zIndex(10f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(C.Red.copy(alpha = 0.55f * glow), C.Red.copy(alpha = 0f)),
                    center = center,
                    radius = size.maxDimension * 0.78f,
                )
            )
            pieces.forEach { piece ->
                val local = ((p - piece.delay) / piece.duration).coerceIn(0f, 1f)
                if (local <= 0f || local >= 1f) return@forEach
                val y = -20f + local * (size.height + 60f)
                val x = piece.x * size.width +
                    sin((local * 3f + piece.x * 4f) * PI).toFloat() * piece.sway
                val alpha = if (local > 0.8f) (1f - local) / 0.2f else 1f
                rotate(degrees = local * piece.spin, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                        topLeft = Offset(x, y),
                        size = Size(6.dp.toPx(), 10.dp.toPx()),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { this.alpha = textAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(
                text = stringResource(R.string.minimum_done),
                style = T.Body14,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                AppText(text = streak.toString(), style = T.Big30Red)
                Spacer(Modifier.width(4.dp))
                AppText(
                    text = stringResource(R.string.day_streak),
                    style = T.Body14,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}
