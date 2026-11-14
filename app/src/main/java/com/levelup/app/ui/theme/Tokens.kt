package com.levelup.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

object C {
    val Bg = Color(0xFF0F1115)
    val Surface = Color(0xFF171A21)
    val Border = Color(0xFF2A2F3A)
    val BorderStrong = Color(0xFF3A3F4B)
    val Text = Color(0xFFE8EAED)
    val Muted = Color(0xFF9AA0A6)
    val Red = Color(0xFFFF3B3B)
    val BarInactive = Color(0xFF5F6673)
}

private val NoPad = PlatformTextStyle(includeFontPadding = false)
private val Trim = LineHeightStyle(
    LineHeightStyle.Alignment.Center,
    LineHeightStyle.Trim.Both,
)

private fun ts(f: FontFamily, size: Int, color: Color, lh: Float) = TextStyle(
    fontFamily = f,
    fontSize = size.sp,
    lineHeight = (size * lh).sp,
    color = color,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    platformStyle = NoPad,
    lineHeightStyle = Trim,
)

private fun mono(size: Int, color: Color, lh: Float) =
    ts(Mono, size, color, lh).copy(fontFeatureSettings = "tnum")

object T {
    val Title16 = mono(16, C.Text, 1.3f)
    val Label11 = mono(11, C.Muted, 1.3f)
    val Mono12Muted = mono(12, C.Muted, 1.3f)
    val Mono12 = mono(12, C.Text, 1.3f)
    val Mono13 = mono(13, C.Text, 1.3f)
    val Mono13Muted = mono(13, C.Muted, 1.3f)
    val Mono13Red = mono(13, C.Red, 1.3f)
    val Mono14 = mono(14, C.Text, 1.3f)
    val Mono15 = mono(15, C.Text, 1.3f)
    val Big30Red = mono(30, C.Red, 1.1f)
    val Big24Red = mono(24, C.Red, 1.0f)
    val Big26Red = mono(26, C.Red, 1.0f)
    val Big28Red = mono(28, C.Red, 1.0f)
    val Big56Red = mono(56, C.Red, 1.0f)
    val Body11Muted = ts(Sans, 11, C.Muted, 1.3f)
    val Body12 = ts(Sans, 12, C.Text, 1.3f)
    val Body12Muted = ts(Sans, 12, C.Muted, 1.35f)
    val Body13 = ts(Sans, 13, C.Text, 1.3f)
    val Body13Muted = ts(Sans, 13, C.Muted, 1.3f)
    val Body14 = ts(Sans, 14, C.Text, 1.3f)
    val Body14Muted = ts(Sans, 14, C.Muted, 1.3f)
    val Body15 = ts(Sans, 15, C.Text, 1.3f)
    val BtnDark12 = ts(Sans, 12, C.Bg, 1.2f)
}
