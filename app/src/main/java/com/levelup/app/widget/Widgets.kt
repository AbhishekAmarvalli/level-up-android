package com.levelup.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.levelup.app.LevelUpApp
import com.levelup.app.MainActivity
import com.levelup.app.R
import com.levelup.app.data.WidgetSnapshot
import com.levelup.app.domain.ActivityRange
import com.levelup.app.domain.TierProgress
import com.levelup.app.domain.displayTitle
import com.levelup.app.ui.theme.C
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val BG = ColorProvider(C.Bg)
private val SURFACE = ColorProvider(C.Surface)
private val BORDER = ColorProvider(C.Border)
private val TEXT = ColorProvider(C.Text)
private val MUTED = ColorProvider(C.Muted)
private val ACCENT = ColorProvider(C.Red)

private val TaskIdKey = ActionParameters.Key<Long>("task_id")

object Widgets {
    suspend fun refreshAll(context: Context) {
        withContext(Dispatchers.Main.immediate) {
            runCatching { ComingUpWidget().updateAll(context) }
            runCatching { TodayWidget().updateAll(context) }
            runCatching { ActivityWidget().updateAll(context) }
            runCatching { StreakNextWidget().updateAll(context) }
        }
    }
}

private suspend fun snapshot(context: Context): WidgetSnapshot =
    (context.applicationContext as LevelUpApp).container.repo.widgetSnapshot()

@Composable
private fun WidgetFrame(tab: String, content: @Composable ColumnScope.() -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BORDER)
            .cornerRadius(20.dp)
            .padding(0.5.dp)
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_TAB, tab)
                )
            ),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(BG)
                .cornerRadius(20.dp)
                .padding(14.dp),
            content = content,
        )
    }
}

@Composable
private fun WidgetText(
    text: String,
    fontSize: Int,
    color: ColorProvider = TEXT,
    mono: Boolean = true,
    maxLines: Int = 1,
    strikethrough: Boolean = false,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        style = TextStyle(
            color = color,
            fontSize = fontSize.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
        ),
    )
}

@Composable
private fun VSpace(value: Int) {
    Spacer(GlanceModifier.height(value.dp))
}

@Composable
private fun HSpace(value: Int) {
    Spacer(GlanceModifier.width(value.dp))
}

@Composable
private fun ThinDivider() {
    Box(
        GlanceModifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(BORDER),
    ) {}
}

@Composable
private fun GlanceChip(label: String, active: Boolean, action: Action) {
    Box(
        modifier = GlanceModifier
            .background(if (active) TEXT else BORDER)
            .cornerRadius(12.dp)
            .padding(0.5.dp)
            .clickable(action),
    ) {
        Box(
            GlanceModifier
                .background(BG)
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            WidgetText(
                text = label,
                fontSize = 12,
                color = if (active) TEXT else MUTED,
                mono = false,
            )
        }
    }
}

@Composable
private fun GlanceProgress(fraction: Float) {
    Image(
        provider = ImageProvider(WidgetProgressBitmap.render(fraction)),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(5.dp),
    )
}

class ComingUpWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = snapshot(context)
        val noDeadline = context.getString(R.string.widget_no_deadline)
        provideContent {
            WidgetFrame("deadlines") {
                WidgetText(
                    text = context.getString(R.string.widget_coming_up_label),
                    fontSize = 11,
                    color = MUTED,
                )
                if (data.upcoming.isEmpty()) {
                    VSpace(4)
                    WidgetText(noDeadline, 14, TEXT, mono = false)
                } else {
                    val first = data.upcoming.first()
                    Row(
                        modifier = GlanceModifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        WidgetText(first.daysLeft.toString(), 56, ACCENT)
                        WidgetText(
                            text = context.getString(R.string.days),
                            fontSize = 13,
                            color = MUTED,
                            modifier = GlanceModifier.padding(start = 8.dp, bottom = 7.dp),
                        )
                    }
                    WidgetText(first.title, 15, TEXT, mono = false)
                    WidgetText(
                        text = first.dateText,
                        fontSize = 12,
                        color = MUTED,
                        modifier = GlanceModifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                    data.upcoming.drop(1).take(2).forEach { item ->
                        ThinDivider()
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            WidgetText(
                                text = item.title,
                                fontSize = 12,
                                color = TEXT,
                                mono = false,
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                            )
                            HSpace(6)
                            WidgetText("${item.daysLeft}d", 12, ACCENT)
                        }
                    }
                }
            }
        }
    }
}

class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = snapshot(context)
        provideContent {
            WidgetFrame("today") {
                WidgetText(
                    text = context.getString(R.string.tab_today).uppercase(),
                    fontSize = 11,
                    color = MUTED,
                )
                VSpace(4)
                if (data.visibleTasks.isEmpty()) {
                    WidgetText(context.getString(R.string.widget_no_tasks), 12, MUTED, mono = false)
                } else {
                    data.visibleTasks.take(7).forEach { task ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(
                                    actionRunCallback<ToggleTaskAction>(
                                        actionParametersOf(TaskIdKey to task.task.id)
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (task.done) R.drawable.ic_checkbox_checked
                                    else R.drawable.ic_checkbox_unchecked
                                ),
                                contentDescription = context.getString(
                                    if (task.done) R.string.widget_task_done
                                    else R.string.widget_task_open,
                                    task.task.title,
                                ),
                                modifier = GlanceModifier.size(16.dp),
                            )
                            HSpace(8)
                            WidgetText(
                                text = displayTitle(task.task, data.examMode),
                                fontSize = 13,
                                color = if (task.done) MUTED else TEXT,
                                mono = false,
                                strikethrough = task.done,
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

class ActivityWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = snapshot(context)
        val sevenDays = data.range == ActivityRange.WEEK
        val chart = withContext(Dispatchers.Default) {
            ActivityChartBitmap.render(
                context = context,
                values = data.activity.values.map { it.toFloat() },
                labels = data.activity.labels,
            )
        }
        provideContent {
            WidgetFrame("today") {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetText(
                        text = context.getString(R.string.widget_activity_label),
                        fontSize = 11,
                        color = MUTED,
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    GlanceChip(
                        label = context.getString(R.string.widget_7days),
                        active = sevenDays,
                        action = actionRunCallback<ToggleRangeAction>(),
                    )
                    HSpace(4)
                    GlanceChip(
                        label = context.getString(R.string.widget_4weeks),
                        active = !sevenDays,
                        action = actionRunCallback<ToggleRangeAction>(),
                    )
                }
                Row(
                    modifier = GlanceModifier.padding(top = 6.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    WidgetText("${data.activity.average}%", 28, ACCENT)
                    WidgetText(
                        text = context.getString(R.string.of_tasks_done_average),
                        fontSize = 12,
                        color = MUTED,
                        mono = false,
                        modifier = GlanceModifier.padding(start = 6.dp),
                    )
                }
                Image(
                    provider = ImageProvider(chart),
                    contentDescription = context.getString(R.string.widget_activity_label),
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(126.dp),
                )
            }
        }
    }
}

class StreakNextWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = snapshot(context)
        provideContent {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(BORDER)
                        .cornerRadius(20.dp)
                        .padding(0.5.dp),
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(BG)
                            .cornerRadius(20.dp)
                            .padding(12.dp),
                    ) {
                        WidgetText(context.getString(R.string.streak), 11, MUTED)
                        WidgetText(data.streak.toString(), 30, ACCENT)
                        Row(
                            modifier = GlanceModifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            data.dots.forEachIndexed { index, active ->
                                Box(
                                    GlanceModifier
                                        .size(9.dp)
                                        .cornerRadius(5.dp)
                                        .background(if (active) ACCENT else BORDER)
                                ) {}
                                if (index < data.dots.lastIndex) HSpace(3)
                            }
                        }
                        data.tierProgress.forEach { tier -> TierBar(tier) }
                    }
                }
                HSpace(10)
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(BORDER)
                        .cornerRadius(20.dp)
                        .padding(0.5.dp),
                ) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(BG)
                            .cornerRadius(20.dp)
                            .padding(12.dp),
                    ) {
                        WidgetText(context.getString(R.string.widget_next_step), 11, MUTED)
                        val next = data.nextLesson
                        if (next == null) {
                            WidgetText(
                                text = context.getString(R.string.widget_all_done),
                                fontSize = 12,
                                color = TEXT,
                                mono = false,
                                modifier = GlanceModifier.padding(top = 4.dp),
                            )
                        } else {
                            WidgetText(
                                text = next.lesson.title,
                                fontSize = 13,
                                color = TEXT,
                                maxLines = 2,
                                modifier = GlanceModifier.padding(top = 6.dp, bottom = 4.dp),
                            )
                            WidgetText(
                                text = next.phase.phase.name,
                                fontSize = 11,
                                color = MUTED,
                                mono = false,
                                maxLines = 2,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TierBar(tier: TierProgress) {
    val context = LocalContext.current
    val label = when (tier.tier) {
        0 -> context.getString(R.string.widget_min)
        1 -> context.getString(R.string.widget_norm)
        else -> context.getString(R.string.widget_bonus)
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetText(
            text = label,
            fontSize = 11,
            color = MUTED,
            mono = false,
            modifier = GlanceModifier.width(34.dp),
        )
        HSpace(6)
        Box(GlanceModifier.defaultWeight()) {
            GlanceProgress(tier.ratio)
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val repo = (context.applicationContext as LevelUpApp).container.repo
        repo.toggleTask(taskId, LocalDate.now())
    }
}

class ToggleRangeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val repo = (context.applicationContext as LevelUpApp).container.repo
        repo.toggleWidgetActivityRange()
    }
}

class ComingUpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ComingUpWidget()
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

class ActivityWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActivityWidget()
}

class StreakNextWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakNextWidget()
}
