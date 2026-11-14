package com.levelup.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.levelup.app.R

enum class Tab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    TODAY(R.string.tab_today, R.drawable.ti_home),
    ROADMAP(R.string.tab_roadmap, R.drawable.ti_route),
    DEADLINES(R.string.tab_deadlines, R.drawable.ti_calendar),
    SPEAKING(R.string.tab_speaking, R.drawable.ti_video),
}
