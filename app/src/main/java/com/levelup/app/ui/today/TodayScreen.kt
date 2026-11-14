package com.levelup.app.ui.today

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.R
import com.levelup.app.data.TaskEntity
import com.levelup.app.domain.ActivityModel
import com.levelup.app.domain.ActivityRange
import com.levelup.app.domain.DateText
import com.levelup.app.domain.TaskState
import com.levelup.app.domain.activityModel
import com.levelup.app.domain.displayTitle
import com.levelup.app.ui.components.ActivityLineChart
import com.levelup.app.ui.components.AppDialog
import com.levelup.app.ui.components.AppText
import com.levelup.app.ui.components.AppTextField
import com.levelup.app.ui.components.AutoDismiss
import com.levelup.app.ui.components.Card
import com.levelup.app.ui.components.Chip
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.HtmlSwitch
import com.levelup.app.ui.components.PrimaryButton
import com.levelup.app.ui.components.RedCheckbox
import com.levelup.app.ui.components.SecondaryButton
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.StreakDots
import com.levelup.app.ui.components.UndoSnackbar
import com.levelup.app.ui.components.WidgetCard
import com.levelup.app.ui.components.appClickable
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    vm: TodayViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var celebrationStreak by remember { mutableStateOf<Int?>(null) }
    var editorTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var activityRange by rememberSaveable { mutableStateOf("7d") }
    var skippedTask by remember { mutableStateOf<TaskEntity?>(null) }

    LaunchedEffect(Unit) {
        vm.celebration.collect { celebrationStreak = it }
    }

    val current = state
    Box(modifier = Modifier.fillMaxSize()) {
        if (current != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 4.dp,
                    bottom = 12.dp,
                ),
            ) {
                item(key = "header") {
                    TodayHeader(
                        dateLabel = DateText.header(current.today),
                        examActive = current.modes.examMode,
                        onOpenSettings = onOpenSettings,
                    )
                }

                item(key = "next") {
                    NextStepCard(
                        title = current.nextLesson?.lesson?.title,
                        phase = current.nextLesson?.phase?.phase?.name,
                        onMarkDone = vm::markNextLessonDone,
                        onSkip = vm::skipNextLesson,
                    )
                }

                item(key = "summary") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Card(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AppText(text = stringResource(R.string.streak), style = T.Label11)
                            AppText(
                                text = current.streak.toString(),
                                style = T.Big30Red,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            StreakDots(
                                dots = current.dots,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        Card(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AppText(
                                text = stringResource(R.string.next_deadline),
                                style = T.Label11,
                            )
                            AppText(
                                text = current.nextDeadlineDays?.let { "${it}d" } ?: "—",
                                style = T.Big30Red,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            AppText(
                                text = current.nextDeadlineTitle
                                    ?: stringResource(R.string.no_deadlines),
                                style = T.Body12,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }

                item(key = "modes") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ModeRow(
                            label = stringResource(R.string.bad_day_mode),
                            checked = current.modes.badDay,
                            onToggle = vm::toggleBadDay,
                        )
                        ModeRow(
                            label = if (current.modes.examDays != null) {
                                stringResource(R.string.exam_mode_auto, current.modes.examDays)
                            } else {
                                stringResource(R.string.exam_mode)
                            },
                            checked = current.modes.examMode,
                            onToggle = vm::toggleExamMode,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                val labels = listOf(
                    R.string.minimum_upper,
                    R.string.normal_upper,
                    R.string.bonus_upper,
                )
                (0..2).forEach { tier ->
                    val group = current.visibleTasks.filter { it.task.tier == tier }
                    if (group.isNotEmpty()) {
                        item(key = "label-$tier") {
                            SectionLabel(
                                text = stringResource(labels[tier]),
                                modifier = Modifier.appClickable(
                                    onClick = {},
                                    onLongClick = {
                                        editorTask = null
                                        showEditor = true
                                    },
                                ),
                            )
                        }
                        itemsIndexed(group, key = { _, item -> item.task.id }) { _, item ->
                            SwipeTaskRow(
                                state = item,
                                examMode = current.modes.examMode,
                                onToggle = { vm.toggleTask(item.task.id) },
                                onSkip = {
                                    vm.skipTask(item.task.id)
                                    skippedTask = item.task
                                },
                                onLongPress = {
                                    editorTask = item.task
                                    showEditor = true
                                },
                            )
                        }
                    }
                }

                item(key = "activity") {
                    val activity = activityModel(
                        records = current.records,
                        today = current.today,
                        range = if (activityRange == "4w") {
                            ActivityRange.MONTH
                        } else {
                            ActivityRange.WEEK
                        },
                    )
                    ActivityCard(
                        activity = activity,
                        range = activityRange,
                        onRange = { activityRange = it },
                    )
                }
            }
        }

        skippedTask?.let { task ->
            AutoDismiss(active = true, durationMillis = 3000) {
                skippedTask = null
            }
            UndoSnackbar(
                message = stringResource(R.string.skip_task_hint),
                action = stringResource(R.string.undo),
                onAction = {
                    vm.unskipTask(task.id)
                    skippedTask = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        celebrationStreak?.let { streak ->
            CelebrationOverlay(
                streak = streak,
                onFinished = { celebrationStreak = null },
            )
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            initial = editorTask,
            onDismiss = { showEditor = false },
            onSave = { title, tier, ai ->
                val editing = editorTask
                if (editing == null) vm.addTask(title, tier, ai)
                else vm.updateTask(editing, title, tier, ai)
                showEditor = false
            },
            onDelete = editorTask?.let { task ->
                {
                    showEditor = false
                    taskToDelete = task
                }
            },
        )
    }

    taskToDelete?.let { task ->
        ConfirmDialog(
            title = stringResource(R.string.delete_task_title),
            message = stringResource(R.string.delete_task_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.deleteTask(task)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null },
        )
    }
}

@Composable
private fun TodayHeader(
    dateLabel: String,
    examActive: Boolean,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(
            text = dateLabel,
            style = T.Title16,
            modifier = Modifier.appClickable(
                onClick = {},
                onLongClick = onOpenSettings,
            ),
        )
        if (examActive) {
            AppText(
                text = stringResource(R.string.exam_mode_pill),
                style = T.Label11,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(0.5.dp, C.Border, RoundedCornerShape(6.dp))
                    .appClickable(onClick = onOpenSettings)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun NextStepCard(
    title: String?,
    phase: String?,
    onMarkDone: () -> Unit,
    onSkip: () -> Unit,
) {
    Card {
        AppText(text = stringResource(R.string.next_step), style = T.Label11)
        if (title == null) {
            AppText(
                text = stringResource(R.string.all_lessons_done),
                style = T.Body14,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            AppText(
                text = title,
                style = T.Mono15,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            )
            AppText(text = phase.orEmpty(), style = T.Body12Muted)
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.mark_done),
                    onClick = onMarkDone,
                )
                SecondaryButton(
                    text = stringResource(R.string.skip_for_now),
                    onClick = onSkip,
                )
            }
        }
    }
}

@Composable
private fun ModeRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(text = label, style = T.Body13)
        HtmlSwitch(on = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SwipeTaskRow(
    state: TaskState,
    examMode: Boolean,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onLongPress: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val limit = with(density) { 72.dp.toPx() }
    val trigger = with(density) { 70.dp.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    drag > 0f -> C.Red
                    drag < 0f -> C.BorderStrong
                    else -> C.Surface
                }
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = drag }
                .background(C.Surface)
                .appClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
                    onLongClick = onLongPress,
                )
                .pointerInput(state.task.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                drag > trigger -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggle()
                                }
                                drag < -trigger -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSkip()
                                }
                            }
                            drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                        onHorizontalDrag = { change, amount ->
                            drag = (drag + amount).coerceIn(-limit, limit)
                            if (abs(drag) > 1f) change.consume()
                        },
                    )
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RedCheckbox(done = state.done)
            AppText(
                text = displayTitle(state.task, examMode),
                style = if (state.done) T.Body14Muted else T.Body14,
                textDecoration = if (state.done) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityModel,
    range: String,
    onRange: (String) -> Unit,
) {
    WidgetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(text = stringResource(R.string.widget_activity_label), style = T.Label11)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Chip(
                    text = stringResource(R.string.widget_7days),
                    active = range == "7d",
                    onClick = { onRange("7d") },
                )
                Chip(
                    text = stringResource(R.string.widget_4weeks),
                    active = range == "4w",
                    onClick = { onRange("4w") },
                )
            }
        }
        Row(modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)) {
            AppText(
                text = "${activity.average}%",
                style = T.Big28Red,
                modifier = Modifier.alignByBaseline(),
            )
            AppText(
                text = stringResource(R.string.of_tasks_done_average),
                style = T.Body12Muted,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .alignByBaseline(),
            )
        }
        ActivityLineChart(
            values = activity.values.map { it.toFloat() },
            labels = activity.labels,
        )
    }
}

@Composable
private fun TaskEditorDialog(
    initial: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var tier by remember(initial?.id) { mutableStateOf(initial?.tier ?: 0) }
    var aiTagged by remember(initial?.id) { mutableStateOf(initial?.isAiTagged ?: false) }

    AppDialog(
        onDismiss = onDismiss,
        title = stringResource(if (initial == null) R.string.add_task else R.string.edit_task),
    ) {
        AppTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.task_title),
            keyboardType = KeyboardType.Text,
        )
        AppText(
            text = stringResource(R.string.task_tier),
            style = T.Label11,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Chip(stringResource(R.string.tier_minimum), tier == 0) { tier = 0 }
            Chip(stringResource(R.string.tier_normal), tier == 1) { tier = 1 }
            Chip(stringResource(R.string.tier_bonus), tier == 2) { tier = 2 }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(text = stringResource(R.string.ai_tagged), style = T.Body13)
            HtmlSwitch(on = aiTagged, onCheckedChange = { aiTagged = it })
        }
        DialogActions(
            confirmLabel = stringResource(R.string.save),
            onConfirm = if (title.isNotBlank()) {
                { onSave(title.trim(), tier, aiTagged) }
            } else {
                null
            },
            onDismiss = onDismiss,
            extra = onDelete?.let {
                {
                    AppText(
                        text = stringResource(R.string.delete),
                        style = T.Body12,
                        color = C.Red,
                        modifier = Modifier
                            .appClickable(onClick = it)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            },
        )
    }
}
