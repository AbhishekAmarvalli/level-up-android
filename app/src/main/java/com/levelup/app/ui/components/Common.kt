package com.levelup.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

/** Section label: mono 11, muted, 12 top / 6 bottom, same as lab() in the prototype. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp),
        color = LvMuted,
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp,
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
        letterSpacing = 0.2.sp,
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
    )
}

/** 34 x 20 pill switch with a 16 dp knob, wrapped in a 44 dp touch target. */
@Composable
fun LvSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val knobOffset by animateDpAsState(if (checked) 16.dp else 2.dp, label = "switchKnob")
    val track by animateColorAsState(if (checked) LvText else LvBorder, label = "switchTrack")
    val knob by animateColorAsState(if (checked) LvBg else LvMuted, label = "switchKnobColor")
    val interaction = remember { MutableInteractionSource() }
    val clickModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            interactionSource = interaction,
            indication = null,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(44.dp)
            .then(clickModifier),
        contentAlignment = Alignment.Center,
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
    val click = if (onToggle != null) {
        Modifier
            .size(44.dp)
            .clickable(onClick = onToggle)
    } else {
        Modifier.size(size)
    }
    Box(
        modifier = modifier.then(click),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
                .then(
                    if (checked) {
                        Modifier.background(LvAccent)
                    } else {
                        Modifier.border(1.5.dp, LvMuted, RoundedCornerShape(6.dp))
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(com.levelup.app.R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.72f),
                )
            }
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
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, if (active) LvText else LvBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = if (active) LvText else LvMuted,
            fontSize = fontSize,
            maxLines = 1,
        )
    }
}

@Composable
fun LvPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LvText)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = LvBg, fontSize = 12.sp)
    }
}

@Composable
fun LvSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, LvBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = LvMuted, fontSize = 12.sp)
    }
}

/** 44 dp touch target for a 16-22 dp icon. */
@Composable
fun LvIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    tint: Color = LvMuted,
    rotationDegrees: Float = 0f,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2f))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2f))
                .background(fillColor),
        )
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable {
                            onDismiss()
                            option.onClick()
                        }
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

