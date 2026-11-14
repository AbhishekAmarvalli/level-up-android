package com.levelup.app.ui.today

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.R
import com.levelup.app.data.TaskEntity
import com.levelup.app.domain.DateText
import com.levelup.app.domain.TaskState
import com.levelup.app.domain.displayTitle
import com.levelup.app.ui.components.CardLabel
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.LvCard
import com.levelup.app.ui.components.LvCheckbox
import com.levelup.app.ui.components.LvChip
import com.levelup.app.ui.components.LvDialog
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

    LaunchedEffect(Unit) {
        vm.celebration.collect { streak -> celebrationStreak = streak }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val current = state
        if (current != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 16.dp),
            ) {
                item(key = "header") {
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
                        LvCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            CardLabel(stringResource(R.string.streak))
                            MonoText(
                                text = current.streak.toString(),
                                fontSize = 30.sp,
                                color = LvAccent,
                                lineHeight = 33.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                current.dots.forEach { active ->
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(if (active) LvAccent else LvBorder),
                                    )
                                }
                            }
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

                item(key = "modes") {
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
                            modifier = Modifier.padding(top = 4.dp),
                        )
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
                            SectionLabel(stringResource(groupLabels[tier]))
                        }
                        items(items = group, key = { it.task.id }) { item ->
                            TaskRow(
                                state = item,
                                examMode = current.modes.examMode,
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

                item(key = "bottom") { Spacer(Modifier.height(8.dp)) }
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
                            .clickable { data.performAction() }
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
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoText(text = dateLabel, fontSize = 16.sp)
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
        )
        LvIconButton(
            iconRes = R.drawable.ic_settings,
            contentDescription = stringResource(R.string.cd_settings),
            onClick = onOpenSettings,
            iconSize = 18.dp,
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onValueChange = { onToggle() },
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
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> onToggle()
                SwipeToDismissBoxValue.EndToStart -> onSkip()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
        positionalThreshold = { distance -> distance * 0.35f },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.padding(bottom = 6.dp),
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
                .background(LvSurface)
                .combinedClickable(onClick = onToggle, onLongClick = onLongPress)
                .padding(vertical = 11.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LvCheckbox(checked = state.done)
            Text(
                text = displayTitle(state.task, examMode),
                color = if (state.done) LvMuted else LvText,
                fontSize = 14.sp,
                textDecoration = if (state.done) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f),
            )
        }
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
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                null
            },
        )
    }
}
