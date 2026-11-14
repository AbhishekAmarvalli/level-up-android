package com.levelup.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.levelup.app.R

enum class Tab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    TODAY(R.string.tab_today, R.drawable.ic_tab_home),
    ROADMAP(R.string.tab_roadmap, R.drawable.ic_tab_route),
    DEADLINES(R.string.tab_deadlines, R.drawable.ic_tab_calendar),
    SPEAKING(R.string.tab_speaking, R.drawable.ic_tab_video),
}
