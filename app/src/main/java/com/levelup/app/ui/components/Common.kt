package com.levelup.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.levelup.app.ui.theme.LvAccent
import com.levelup.app.ui.theme.LvBg
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvBorderStrong
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvSurface
import com.levelup.app.ui.theme.LvText
import com.levelup.app.ui.theme.Mono
import kotlinx.coroutines.delay

/** Section label: mono 11, muted, 0.5 letter spacing, 12 top / 6 bottom, same as lab(). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp),
        color = LvMuted,
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
    )
}

/** Label inside a card (lab0): no vertical margins. */
@Composable
fun CardLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = LvMuted,
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp,
    )
}

@Composable
fun LvCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LvSurface)
            .padding(padding),
        content = content,
    )
}

@Composable
fun MonoText(
    text: String,
    fontSize: TextUnit,
    color: Color = LvText,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = Mono,
        fontSize = fontSize,
        fontWeight = weight,
        lineHeight = lineHeight,
        textAlign = textAlign,
        maxLines = maxLines,
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}

/** Smooth, quick press feedback without a Material ripple. */
fun Modifier.pressScale(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(90, easing = FastOutSlowInEasing),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Clickable with no Material ripple, plus the shared 0.98 press feedback. */
fun Modifier.lvClickable(onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    clickable(
        interactionSource = source,
        indication = null,
        onClick = onClick,
    ).pressScale(source)
}

/** First-display fade-in-up with a 30 ms stagger, capped so long lists stay snappy. */
fun Modifier.staggerIn(index: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtLeast(0) * 30).toLong())
        progress.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 8.dp.toPx()
    }
}

/** Counts up on first appearance and whenever the value changes. */
@Composable
fun animatedCount(value: Int, durationMillis: Int = 600): Int {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }
    val animated by androidx.compose.animation.core.animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "countUp",
    )
    return animated
}

/** 34 x 20 pill switch with a 16 dp knob and the exact HTML colors. */
@Composable
fun LvSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 16.dp else 2.dp,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "switchKnob",
    )
    val track by animateColorAsState(
        targetValue = if (checked) LvText else LvBorder,
        animationSpec = tween(150),
        label = "switchTrack",
    )
    val knob by animateColorAsState(
        targetValue = if (checked) LvBg else LvMuted,
        animationSpec = tween(150),
        label = "switchKnobColor",
    )
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val clickModifier = if (onCheckedChange != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
        }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(width = 34.dp, height = 20.dp)
            .then(clickModifier)
            .pressScale(interaction),
    ) {
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(track),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = knobOffset, top = 2.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(knob),
            )
        }
    }
}

/** 20 dp square checkbox: red fill with a white check when checked. */
@Composable
fun LvCheckbox(
    checked: Boolean,
    onToggle: (() -> Unit)? = null,
    size: Dp = 20.dp,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val fill by animateColorAsState(
        targetValue = if (checked) LvAccent else Color.Transparent,
        animationSpec = tween(150),
        label = "checkboxFill",
    )
    val border by animateColorAsState(
        targetValue = if (checked) LvAccent else LvMuted,
        animationSpec = tween(150),
        label = "checkboxBorder",
    )
    val pop = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(checked) }
    LaunchedEffect(checked) {
        if (checked != previous) {
            if (checked) {
                pop.snapTo(0.85f)
                pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 620f))
            }
            previous = checked
        }
    }
    val clickModifier = if (onToggle != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .then(clickModifier)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .clip(RoundedCornerShape(6.dp))
            .background(fill)
            .border(1.5.dp, border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.7f),
            exit = fadeOut(tween(80)) + scaleOut(tween(80), targetScale = 0.7f),
        ) {
            Icon(
                painter = painterResource(com.levelup.app.R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.72f),
            )
        }
    }
}

@Composable
fun LvChip(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 4.dp,
    radius: Dp = 12.dp,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = if (active) LvText else LvBorder,
        animationSpec = tween(150),
        label = "chipBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (active) LvText else LvMuted,
        animationSpec = tween(150),
        label = "chipText",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .border(0.5.dp, borderColor, RoundedCornerShape(radius))
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .pressScale(interaction)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            maxLines = 1,
        )
    }
}

@Composable
fun LvPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) Color(0xFFF5F6F8) else LvText,
        animationSpec = tween(90),
        label = "primaryPress",
    )
    Box(
        modifier = modifier
            .heightIn(min = 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .pressScale(interaction)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = LvBg, fontSize = 12.sp)
    }
}

@Composable
fun LvSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background by animateColorAsState(
        targetValue = if (pressed) LvSurface.copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = tween(90),
        label = "secondaryPress",
    )
    Box(
        modifier = modifier
            .heightIn(min = 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(0.5.dp, LvBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pressScale(interaction)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = LvMuted, fontSize = 12.sp)
    }
}

/** Compact icon button with no default ripple. */
@Composable
fun LvIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    tint: Color = LvMuted,
    rotationDegrees: Float = 0f,
    touchSize: Dp = 36.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(touchSize)
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pressScale(interaction),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .rotate(rotationDegrees),
        )
    }
}

@Composable
fun LvProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = LvBorder,
    fillColor: Color = LvText,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val target = if (entered) fraction.coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2f))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2f))
                .background(fillColor),
        )
    }
}

/** 9 dp streak dots with a 4 dp gap. */
@Composable
fun StreakDots(
    dots: List<Boolean>,
    modifier: Modifier = Modifier,
    dotSize: Dp = 9.dp,
    gap: Dp = 4.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dots.forEach { active ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (active) LvAccent else LvBorder),
            )
        }
    }
}

/** Small outlined text field matching the card style. */
@Composable
fun LvTextField(
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
        textStyle = TextStyle(color = LvText, fontSize = 14.sp),
        cursorBrush = SolidColor(LvAccent),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LvBg)
                    .border(0.5.dp, LvBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 11.dp),
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = LvMuted, fontSize = 13.sp)
                }
                inner()
            }
        },
    )
}

@Composable
fun LvDialog(
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
                    .background(LvSurface)
                    .border(0.5.dp, LvBorderStrong, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        color = LvText,
                        fontFamily = Mono,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                content()
            }
        }
    }
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
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (extra != null) {
            extra()
            Spacer(Modifier.weight(1f))
        }
        LvSecondaryButton(text = androidx.compose.ui.res.stringResource(com.levelup.app.R.string.cancel), onClick = onDismiss)
        if (onConfirm != null) {
            LvPrimaryButton(text = confirmLabel, onClick = onConfirm)
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
    LvDialog(onDismiss = onDismiss, title = title) {
        Column(modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(interactionSource = interaction, indication = null) {
                            onDismiss()
                            option.onClick()
                        }
                        .pressScale(interaction)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = option.label,
                        color = if (option.destructive) LvAccent else LvText,
                        fontSize = 14.sp,
                    )
                }
            }
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
    LvDialog(onDismiss = onDismiss, title = title) {
        Text(text = message, color = LvMuted, fontSize = 13.sp, lineHeight = 19.sp)
        if (extraContent != null) {
            Spacer(Modifier.height(8.dp))
            extraContent()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            LvSecondaryButton(text = androidx.compose.ui.res.stringResource(com.levelup.app.R.string.cancel), onClick = onDismiss)
            LvPrimaryButton(text = confirmLabel, onClick = onConfirm)
        }
    }
}
