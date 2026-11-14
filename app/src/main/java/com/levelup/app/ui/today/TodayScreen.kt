package com.levelup.app.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.levelup.app.ui.components.CardLabel
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.LvCard
import com.levelup.app.ui.components.LvCheckbox
import com.levelup.app.ui.components.LvChip
import com.levelup.app.ui.components.LvDialog
import com.levelup.app.ui.components.StreakDots
import com.levelup.app.ui.components.animatedCount
import com.levelup.app.ui.components.pressScale
import com.levelup.app.ui.components.staggerIn
import com.levelup.app.ui.components.LvIconButton
import com.levelup.app.ui.components.LvPrimaryButton
import com.levelup.app.ui.components.LvSecondaryButton
import com.levelup.app.ui.components.LvSwitch
import com.levelup.app.ui.components.LvTextField
import com.levelup.app.ui.components.MonoText
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.theme.LvAccent
import com.levelup.app.ui.theme.LvBg
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvSurface
import com.levelup.app.ui.theme.LvText
import com.levelup.app.ui.theme.Mono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(
    onOpenSettings: () -> Unit,
    vm: TodayViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.undo)
    val skippedLabel = stringResource(R.string.skip_task_hint)

    var celebrationStreak by remember { mutableStateOf<Int?>(null) }
    var editorTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var activityRange by rememberSaveable { mutableStateOf("7d") }

    LaunchedEffect(Unit) {
        vm.celebration.collect { streak -> celebrationStreak = streak }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val current = state
        if (current != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
            ) {
                item(key = "header") {
                    Box(Modifier.staggerIn(0)) {
                        TodayHeader(
                            dateLabel = DateText.header(current.today),
                            examActive = current.modes.examMode,
                            onAddTask = {
                                editorTask = null
                                showEditor = true
                            },
                            onOpenSettings = onOpenSettings,
                        )
                    }
                }

                item(key = "next") {
                    Box(Modifier.staggerIn(1)) {
                        NextStepCard(
                            title = current.nextLesson?.lesson?.title,
                            phase = current.nextLesson?.phase?.phase?.name,
                            onMarkDone = vm::markNextLessonDone,
                            onSkip = vm::skipNextLesson,
                        )
                    }
                }

                item(key = "summary") {
                    Box(Modifier.staggerIn(2)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LvCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CardLabel(stringResource(R.string.streak))
                            MonoText(
                                text = animatedCount(current.streak).toString(),
                                fontSize = 30.sp,
                                color = LvAccent,
                                lineHeight = 33.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            StreakDots(
                                dots = current.dots,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        LvCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CardLabel(stringResource(R.string.next_deadline))
                            MonoText(
                                text = current.nextDeadlineDays?.let { "${it}d" } ?: "—",
                                fontSize = 30.sp,
                                color = LvAccent,
                                lineHeight = 33.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                text = current.nextDeadlineTitle ?: stringResource(R.string.no_deadlines),
                                color = LvText,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                    }
                }

                item(key = "modes") {
                    Box(Modifier.staggerIn(3)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ModeSwitchRow(
                                label = stringResource(R.string.bad_day_mode),
                                checked = current.modes.badDay,
                                onToggle = vm::toggleBadDay,
                            )
                            ModeSwitchRow(
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
                }

                val groupLabels = listOf(
                    R.string.minimum_upper,
                    R.string.normal_upper,
                    R.string.bonus_upper,
                )
                (0..2).forEach { tier ->
                    val group = current.visibleTasks.filter { it.task.tier == tier }
                    if (group.isNotEmpty()) {
                        item(key = "label-$tier") {
                            SectionLabel(
                                text = stringResource(groupLabels[tier]),
                                modifier = Modifier.staggerIn(4 + tier),
                            )
                        }
                        itemsIndexed(items = group, key = { _, item -> item.task.id }) { index, item ->
                            TaskRow(
                                state = item,
                                examMode = current.modes.examMode,
                                modifier = Modifier.staggerIn(index + 4 + tier),
                                onToggle = { vm.toggleTask(item.task.id) },
                                onSkip = {
                                    vm.skipTask(item.task.id)
                                    scope.launch {
                                        val result = snackbar.showSnackbar(
                                            message = skippedLabel,
                                            actionLabel = undoLabel,
                                            withDismissAction = false,
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            vm.unskipTask(item.task.id)
                                        }
                                    }
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
                        range = if (activityRange == "4w") ActivityRange.MONTH else ActivityRange.WEEK,
                    )
                    Box(Modifier.staggerIn(8)) {
                        ActivityCard(
                            activity = activity,
                            range = activityRange,
                            onRange = { activityRange = it },
                        )
                    }
                }

                item(key = "bottom") { Spacer(Modifier.height(4.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) { data ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LvSurface)
                    .border(0.5.dp, LvBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(data.visuals.message, color = LvText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                data.visuals.actionLabel?.let { label ->
                    Text(
                        text = label,
                        color = LvAccent,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember {
                                    androidx.compose.foundation.interaction.MutableInteractionSource()
                                },
                                indication = null,
                            ) { data.performAction() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        celebrationStreak?.let { streak ->
            CelebrationOverlay(streak = streak, onFinished = { celebrationStreak = null })
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
    onAddTask: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 10.dp)
            .heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoText(text = dateLabel, fontSize = 16.sp, maxLines = 1)
        Spacer(Modifier.weight(1f))
        if (examActive) {
            Text(
                text = stringResource(R.string.exam_mode_pill),
                color = LvMuted,
                fontFamily = Mono,
                fontSize = 11.sp,
                modifier = Modifier
                    .border(0.5.dp, LvBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(2.dp))
        }
        LvIconButton(
            iconRes = R.drawable.ic_plus,
            contentDescription = stringResource(R.string.add_task),
            onClick = onAddTask,
            iconSize = 18.dp,
            touchSize = 32.dp,
        )
        LvIconButton(
            iconRes = R.drawable.ic_settings,
            contentDescription = stringResource(R.string.cd_settings),
            onClick = onOpenSettings,
            iconSize = 18.dp,
            touchSize = 32.dp,
        )
    }
}

@Composable
private fun NextStepCard(
    title: String?,
    phase: String?,
    onMarkDone: () -> Unit,
    onSkip: () -> Unit,
) {
    LvCard {
        CardLabel(stringResource(R.string.next_step))
        if (title == null) {
            Text(
                text = stringResource(R.string.all_lessons_done),
                color = LvText,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            MonoText(
                text = title,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
            )
            Text(text = phase.orEmpty(), color = LvMuted, fontSize = 12.sp)
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LvPrimaryButton(text = stringResource(R.string.mark_done), onClick = onMarkDone)
                LvSecondaryButton(text = stringResource(R.string.skip_for_now), onClick = onSkip)
            }
        }
    }
}

/** A row that behaves like a switch with a 44 dp touch target. */
@Composable
private fun ModeSwitchRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onValueChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = LvText, fontSize = 13.sp, modifier = Modifier.weight(1f))
        LvSwitch(checked = checked, onCheckedChange = null)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    state: TaskState,
    examMode: Boolean,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var flash by remember { mutableStateOf(false) }
    var wasDone by remember { mutableStateOf(state.done) }
    LaunchedEffect(state.done) {
        if (state.done && !wasDone) {
            flash = true
            delay(260)
            flash = false
        }
        wasDone = state.done
    }
    val rowBackground by animateColorAsState(
        targetValue = if (flash) lerp(LvSurface, LvAccent, 0.12f) else LvSurface,
        animationSpec = tween(220),
        label = "taskFlash",
    )
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSkip()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
        positionalThreshold = { distance -> distance * 0.35f },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(bottom = 6.dp),
        enableDismissFromStartToEnd = !state.done,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (direction == SwipeToDismissBoxValue.StartToEnd) LvAccent else LvSurface)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Arrangement.End
                } else {
                    Arrangement.Start
                },
            ) {
                Text(
                    text = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        stringResource(R.string.mark_done)
                    } else {
                        stringResource(R.string.skip_for_now)
                    },
                    color = if (direction == SwipeToDismissBoxValue.StartToEnd) LvBg else LvMuted,
                    fontFamily = Mono,
                    fontSize = 11.sp,
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(rowBackground)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
                    onLongClick = onLongPress,
                )
                .pressScale(interaction)
                .padding(vertical = 11.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LvCheckbox(checked = state.done)
            TaskTitle(
                text = displayTitle(state.task, examMode),
                done = state.done,
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
    LvCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardLabel(
                text = stringResource(R.string.widget_activity_label),
                modifier = Modifier.weight(1f),
            )
            LvChip(
                text = stringResource(R.string.widget_7days),
                active = range == "7d",
                onClick = { onRange("7d") },
                fontSize = 11.sp,
                horizontalPadding = 8.dp,
                verticalPadding = 2.dp,
                radius = 10.dp,
            )
            Spacer(Modifier.width(4.dp))
            LvChip(
                text = stringResource(R.string.widget_4weeks),
                active = range == "4w",
                onClick = { onRange("4w") },
                fontSize = 11.sp,
                horizontalPadding = 8.dp,
                verticalPadding = 2.dp,
                radius = 10.dp,
            )
        }
        val average = animatedCount(activity.average)
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            MonoText(
                text = "$average%",
                fontSize = 28.sp,
                color = LvAccent,
                lineHeight = 30.sp,
            )
            Text(
                text = stringResource(R.string.of_tasks_done_average),
                color = LvMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 3.dp),
            )
        }
        ActivityLineChart(
            values = activity.values.map { it.toFloat() },
            labels = activity.labels,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TaskTitle(
    text: String,
    done: Boolean,
    modifier: Modifier = Modifier,
) {
    var textWidth by remember { mutableIntStateOf(0) }
    val lineProgress by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = tween(220),
        label = "taskStrike",
    )
    Text(
        text = text,
        color = if (done) LvMuted else LvText,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout ->
            if (layout.lineCount > 0) textWidth = layout.getLineRight(0).toInt()
        },
        modifier = modifier.drawBehind {
            if (textWidth > 0 && lineProgress > 0f) {
                drawLine(
                    color = LvMuted,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(textWidth * lineProgress, size.height / 2f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        },
    )
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

    LvDialog(
        onDismiss = onDismiss,
        title = stringResource(if (initial == null) R.string.add_task else R.string.edit_task),
    ) {
        LvTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.task_title),
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.task_tier), color = LvMuted, fontSize = 11.sp, fontFamily = Mono)
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LvChip(stringResource(R.string.tier_minimum), tier == 0, { tier = 0 })
            LvChip(stringResource(R.string.tier_normal), tier == 1, { tier = 1 })
            LvChip(stringResource(R.string.tier_bonus), tier == 2, { tier = 2 })
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ai_tagged),
                color = LvText,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            LvSwitch(checked = aiTagged, onCheckedChange = { aiTagged = it })
        }
        DialogActions(
            confirmLabel = stringResource(R.string.save),
            onConfirm = if (title.isNotBlank()) {
                { onSave(title.trim(), tier, aiTagged) }
            } else {
                null
            },
            onDismiss = onDismiss,
            extra = if (onDelete != null) {
                {
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onDelete)
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.delete),
                            color = LvAccent,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}
