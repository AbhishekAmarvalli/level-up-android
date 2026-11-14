package com.levelup.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.levelup.app.R
import com.levelup.app.ui.Tab
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import kotlinx.coroutines.delay

@Composable
fun AppText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textDecoration: TextDecoration? = null,
    softWrap: Boolean = true,
) {
    var resolved = style
    if (color != Color.Unspecified) resolved = resolved.copy(color = color)
    if (textAlign != null) resolved = resolved.copy(textAlign = textAlign)
    if (textDecoration != null) resolved = resolved.copy(textDecoration = textDecoration)
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
    )
}

/**
 * Shared click target. There is deliberately no indication, so Compose never
 * draws a Material ripple; pressed feedback is the specified 0.7 alpha.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.appClickable(
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    this
        .graphicsLayer { alpha = if (pressed && enabled) 0.7f else 1f }
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = role,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
            } else {
                Modifier.clickable(
                    interactionSource = source,
                    indication = null,
                    enabled = enabled,
                    role = role,
                    onClick = onClick,
                )
            }
        )
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.Surface)
            .padding(12.dp),
        content = content,
    )
}

@Composable
fun RowItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    var row = modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(C.Surface)
    if (onClick != null) {
        row = row.appClickable(onClick = onClick, onLongClick = onLongClick)
    }
    Row(
        modifier = row.padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun WidgetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(C.Bg)
            .border(0.5.dp, C.Border, RoundedCornerShape(20.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    first: Boolean = false,
) {
    AppText(
        text = text,
        style = T.Label11,
        modifier = modifier.padding(top = if (first) 0.dp else 12.dp, bottom = 6.dp),
    )
}

@Composable
fun RedCheckbox(
    done: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    onToggle: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val scale = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done != previous) {
            scale.snapTo(0.85f)
            scale.animateTo(1f, tween(120))
            previous = done
        }
    }
    var box = modifier
        .size(size)
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clip(RoundedCornerShape(6.dp))
        .then(
            if (done) Modifier.background(C.Red)
            else Modifier.border(1.5.dp, C.Muted, RoundedCornerShape(6.dp))
        )
    if (onToggle != null) {
        box = box.appClickable(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
            role = Role.Checkbox,
        )
    }
    Box(modifier = box, contentAlignment = Alignment.Center) {
        if (done) {
            TiIcon(
                iconRes = R.drawable.ti_check,
                size = size - 6.dp,
                tint = Color.White,
            )
        }
    }
}

@Composable
fun HtmlSwitch(
    on: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val knobX by animateDpAsState(
        targetValue = if (on) 16.dp else 2.dp,
        animationSpec = tween(150),
        label = "switchKnob",
    )
    val track by animateColorAsState(
        targetValue = if (on) C.Text else C.Border,
        animationSpec = tween(150),
        label = "switchTrack",
    )
    val knob by animateColorAsState(
        targetValue = if (on) C.Bg else C.Muted,
        animationSpec = tween(150),
        label = "switchKnobColor",
    )
    var box = modifier.size(width = 34.dp, height = 20.dp)
    if (onCheckedChange != null) {
        box = box.appClickable(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!on)
            },
            role = Role.Switch,
        )
    }
    Box(
        modifier = box
            .clip(RoundedCornerShape(10.dp))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(knob),
        )
    }
}

@Composable
fun Chip(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                0.5.dp,
                if (active) C.Text else C.Border,
                RoundedCornerShape(12.dp),
            )
            .appClickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        AppText(
            text = text,
            style = if (active) T.Body12 else T.Body12Muted,
            maxLines = 1,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(C.Text)
            .appClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = T.BtnDark12)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, C.Border, RoundedCornerShape(8.dp))
            .appClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppText(text = text, style = T.Body12Muted)
    }
}

@Composable
fun ProgressBar(
    fraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(C.Border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(C.Text),
        )
    }
}

@Composable
fun StreakDot(on: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (on) C.Red else C.Border),
    )
}

@Composable
fun StreakDots(
    dots: List<Boolean>,
    modifier: Modifier = Modifier,
    gap: Dp = 4.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dots.forEach { StreakDot(on = it) }
    }
}

@Composable
fun TiIcon(
    iconRes: Int,
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun IconButton(
    iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
    rotation: Float = 0f,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .appClickable(onClick = onClick)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center,
    ) {
        TiIcon(
            iconRes = iconRes,
            size = iconSize,
            tint = tint,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun BottomTabBar(
    selected: Tab,
    onSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(C.Bg)
            .drawBehind {
                drawLine(
                    color = C.Border,
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .appClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelected(tab)
                        },
                        role = Role.Tab,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TiIcon(
                    iconRes = tab.iconRes,
                    size = 22.dp,
                    tint = if (active) C.Red else C.Muted,
                )
                AppText(
                    text = stringResource(tab.labelRes),
                    style = T.Body11Muted,
                    color = if (active) C.Red else C.Muted,
                )
            }
        }
    }
}

@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.Surface)
                    .border(0.5.dp, C.BorderStrong, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                if (title != null) {
                    AppText(text = title, style = T.Mono14)
                    Spacer(Modifier.height(10.dp))
                }
                content()
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = T.Body13,
        cursorBrush = SolidColor(C.Red),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.Bg)
                    .border(0.5.dp, C.Border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                if (value.isEmpty()) {
                    AppText(text = placeholder, style = T.Body13Muted)
                }
                inner()
            }
        },
    )
}

@Composable
fun DialogActions(
    confirmLabel: String,
    onConfirm: (() -> Unit)?,
    onDismiss: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (extra != null) {
            extra()
            Spacer(Modifier.weight(1f))
        }
        SecondaryButton(text = stringResource(R.string.cancel), onClick = onDismiss)
        if (onConfirm != null) {
            PrimaryButton(text = confirmLabel, onClick = onConfirm)
        }
    }
}

data class DialogOption(
    val label: String,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun OptionsDialog(
    title: String?,
    options: List<DialogOption>,
    onDismiss: () -> Unit,
) {
    AppDialog(onDismiss = onDismiss, title = title) {
        options.forEach { option ->
            AppText(
                text = option.label,
                style = T.Body14,
                color = if (option.destructive) C.Red else C.Text,
                modifier = Modifier
                    .fillMaxWidth()
                    .appClickable {
                        onDismiss()
                        option.onClick()
                    }
                    .padding(vertical = 9.dp),
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    AppDialog(onDismiss = onDismiss, title = title) {
        AppText(text = message, style = T.Body13Muted)
        if (extraContent != null) {
            Spacer(Modifier.height(8.dp))
            extraContent()
        }
        DialogActions(
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun UndoSnackbar(
    message: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .border(0.5.dp, C.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = message,
            style = T.Body13,
            modifier = Modifier.weight(1f),
        )
        AppText(
            text = action,
            style = T.Body13,
            color = C.Red,
            modifier = Modifier
                .appClickable(onClick = onAction)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Utility for one-shot delayed clearing in the custom snackbar. */
@Composable
fun AutoDismiss(active: Boolean, durationMillis: Long, onDone: () -> Unit) {
    LaunchedEffect(active) {
        if (active) {
            delay(durationMillis)
            onDone()
        }
    }
}
