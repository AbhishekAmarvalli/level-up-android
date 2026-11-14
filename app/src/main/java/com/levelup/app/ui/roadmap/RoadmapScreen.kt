package com.levelup.app.ui.roadmap

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.R
import com.levelup.app.data.LessonEntity
import com.levelup.app.data.PhaseEntity
import com.levelup.app.data.PhaseWithLessons
import com.levelup.app.domain.DateText
import com.levelup.app.domain.NextLesson
import com.levelup.app.ui.components.CardLabel
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.DialogOption
import com.levelup.app.ui.components.LvDialog
import com.levelup.app.ui.components.LvIconButton
import com.levelup.app.ui.components.LvProgressBar
import com.levelup.app.ui.components.LvSwitch
import com.levelup.app.ui.components.LvTextField
import com.levelup.app.ui.components.MonoText
import com.levelup.app.ui.components.OptionsDialog
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.animatedCount
import com.levelup.app.ui.components.lvClickable
import com.levelup.app.ui.components.pressScale
import com.levelup.app.ui.components.staggerIn
import com.levelup.app.ui.theme.LvAccent
import com.levelup.app.ui.theme.LvBg
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvSurface
import com.levelup.app.ui.theme.LvText
import com.levelup.app.ui.theme.Mono
import kotlin.math.roundToInt

private sealed interface RoadmapDialog {
    data class PhaseEditor(val phase: PhaseEntity?) : RoadmapDialog
    data class LessonEditor(val phaseId: Long, val lesson: LessonEntity?) : RoadmapDialog
    data class PhaseMenu(val phase: PhaseWithLessons) : RoadmapDialog
    data class LessonMenu(val phase: PhaseWithLessons, val lesson: LessonEntity) : RoadmapDialog
    data class DeletePhase(val phase: PhaseWithLessons) : RoadmapDialog
    data class DeleteLesson(val lesson: LessonEntity) : RoadmapDialog
}

@Composable
fun RoadmapScreen(vm: RoadmapViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var expandAll by rememberSaveable { mutableStateOf(false) }
    val manualOpen = remember { mutableStateMapOf<Long, Boolean>() }
    var dialog by remember { mutableStateOf<RoadmapDialog?>(null) }
    val context = LocalContext.current

    fun togglePhase(phase: PhaseWithLessons, currentlyOpen: Boolean) {
        expandAll = false
        manualOpen[phase.phase.id] = !currentlyOpen
    }

    fun openLink(lesson: LessonEntity) {
        val url = lesson.url
        if (url.isNullOrBlank()) {
            dialog = RoadmapDialog.LessonEditor(lesson.phaseId, lesson)
        } else {
            val fixed = if (url.contains("://")) url else "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixed))
            val opened = runCatching { context.startActivity(intent) }.isSuccess
            if (!opened) {
                Toast.makeText(context, context.getString(R.string.link_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val current = state
        if (current != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
            ) {
                item(key = "title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 12.dp)
                            .heightIn(min = 36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoText(text = stringResource(R.string.roadmap), fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        LvIconButton(
                            iconRes = R.drawable.ic_plus,
                            contentDescription = stringResource(R.string.add_phase),
                            onClick = { dialog = RoadmapDialog.PhaseEditor(null) },
                            iconSize = 18.dp,
                            touchSize = 32.dp,
                        )
                        Text(
                            text = stringResource(R.string.expand_all),
                            color = LvMuted,
                            fontSize = 12.sp,
                        )
                        LvSwitch(checked = expandAll, onCheckedChange = {
                            expandAll = it
                            if (it) manualOpen.clear()
                        })
                    }
                }

                if (current.phases.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.no_phases),
                            color = LvMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                itemsIndexed(items = current.phases, key = { _, phase -> phase.phase.id }) { index, phase ->
                    val next = current.next
                    val isCurrent = next?.phase?.phase?.id == phase.phase.id
                    val open = if (expandAll) true else (manualOpen[phase.phase.id] ?: isCurrent)
                    PhaseBlock(
                        modifier = Modifier.staggerIn(index),
                        phase = phase,
                        next = next,
                        open = open,
                        onToggleOpen = { togglePhase(phase, open) },
                        onMenu = { dialog = RoadmapDialog.PhaseMenu(phase) },
                        onToggleLesson = { lesson ->
                            vm.setLessonDone(lesson, !lesson.done)
                        },
                        onLessonMenu = { lesson ->
                            dialog = RoadmapDialog.LessonMenu(phase, lesson)
                        },
                        onOpenLink = { lesson -> openLink(lesson) },
                    )
                }

                item(key = "bottom") { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    when (val d = dialog) {
        is RoadmapDialog.PhaseEditor -> PhaseEditorDialog(
            initial = d.phase,
            onDismiss = { dialog = null },
            onSave = { name ->
                val phase = d.phase
                if (phase == null) vm.addPhase(name) else vm.renamePhase(phase, name)
                dialog = null
            },
        )

        is RoadmapDialog.LessonEditor -> LessonEditorDialog(
            initial = d.lesson,
            onDismiss = { dialog = null },
            onSave = { title, url ->
                val lesson = d.lesson
                if (lesson == null) vm.addLesson(d.phaseId, title, url)
                else vm.updateLesson(lesson, title, url)
                dialog = null
            },
            onDelete = d.lesson?.let {
                { dialog = RoadmapDialog.DeleteLesson(it) }
            },
        )

        is RoadmapDialog.PhaseMenu -> {
            val next = state?.next?.phase?.phase?.id == d.phase.phase.id
            OptionsDialog(
                title = d.phase.phase.name,
                options = listOfNotNull(
                    DialogOption(stringResource(R.string.add_lesson)) {
                        dialog = RoadmapDialog.LessonEditor(d.phase.phase.id, null)
                    },
                    DialogOption(stringResource(R.string.rename)) {
                        dialog = RoadmapDialog.PhaseEditor(d.phase.phase)
                    },
                    DialogOption(stringResource(R.string.move_up)) {
                        vm.movePhase(d.phase.phase, up = true)
                        dialog = null
                    },
                    DialogOption(stringResource(R.string.move_down)) {
                        vm.movePhase(d.phase.phase, up = false)
                        dialog = null
                    },
                    if (next) {
                        DialogOption(stringResource(R.string.skip_for_now)) {
                            vm.skipNext()
                            dialog = null
                        }
                    } else {
                        null
                    },
                    DialogOption(stringResource(R.string.delete), destructive = true) {
                        dialog = RoadmapDialog.DeletePhase(d.phase)
                    },
                ),
                onDismiss = { dialog = null },
            )
        }

        is RoadmapDialog.LessonMenu -> OptionsDialog(
            title = d.lesson.title,
            options = listOf(
                DialogOption(
                    if (d.lesson.done) stringResource(R.string.mark_lesson_undone)
                    else stringResource(R.string.mark_lesson_done)
                ) {
                    vm.setLessonDone(d.lesson, !d.lesson.done)
                    dialog = null
                },
                DialogOption(stringResource(R.string.rename)) {
                    dialog = RoadmapDialog.LessonEditor(d.phase.phase.id, d.lesson)
                },
                DialogOption(stringResource(R.string.edit_link)) {
                    dialog = RoadmapDialog.LessonEditor(d.phase.phase.id, d.lesson)
                },
                DialogOption(stringResource(R.string.move_up)) {
                    vm.moveLesson(d.lesson, up = true)
                    dialog = null
                },
                DialogOption(stringResource(R.string.move_down)) {
                    vm.moveLesson(d.lesson, up = false)
                    dialog = null
                },
                DialogOption(stringResource(R.string.delete), destructive = true) {
                    dialog = RoadmapDialog.DeleteLesson(d.lesson)
                },
            ),
            onDismiss = { dialog = null },
        )

        is RoadmapDialog.DeletePhase -> ConfirmDialog(
            title = stringResource(R.string.delete_phase_title),
            message = stringResource(R.string.delete_phase_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.deletePhase(d.phase.phase)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is RoadmapDialog.DeleteLesson -> ConfirmDialog(
            title = stringResource(R.string.delete_lesson_title),
            message = stringResource(R.string.lesson_title),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.deleteLesson(d.lesson)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhaseBlock(
    modifier: Modifier = Modifier,
    phase: PhaseWithLessons,
    next: NextLesson?,
    open: Boolean,
    onToggleOpen: () -> Unit,
    onMenu: () -> Unit,
    onToggleLesson: (LessonEntity) -> Unit,
    onLessonMenu: (LessonEntity) -> Unit,
    onOpenLink: (LessonEntity) -> Unit,
) {
    val total = phase.lessons.size
    val doneCount = phase.lessons.count { it.done }
    val fraction = if (total == 0) 0f else doneCount.toFloat() / total.toFloat()
    val percent = (fraction * 100f).roundToInt()
    val complete = total > 0 && doneCount == total
    val isCurrent = next?.phase?.phase?.id == phase.phase.id
    val orderedLessons = phase.lessons.sortedWith(compareBy({ it.sortOrder }, { it.id }))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 9.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(LvBorder),
            )
            Box(
                modifier = Modifier
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(if (complete) LvAccent else LvBg)
                    .border(
                        1.5.dp,
                        if (complete) LvAccent else if (isCurrent) LvText else LvBorder,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (complete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }

        val haptic = LocalHapticFeedback.current
        val interaction = remember { MutableInteractionSource() }
        val chevron by animateFloatAsState(
            targetValue = if (open) 180f else 0f,
            animationSpec = tween(200),
            label = "chevron",
        )
        val shownPercent = animatedCount(percent)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 12.dp)
                .animateContentSize(tween(200)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onToggleOpen,
                        onLongClick = onMenu,
                    )
                    .pressScale(interaction)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonoText(text = phase.phase.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                MonoText(text = "$shownPercent%", fontSize = 13.sp, color = LvAccent)
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = stringResource(R.string.cd_expand_phase),
                    tint = LvMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(chevron),
                )
            }
            LvProgressBar(
                fraction = fraction,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.lessons_count, doneCount, total),
                color = LvMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp),
            )

            AnimatedVisibility(
                visible = open,
                enter = expandVertically(tween(200), expandFrom = Alignment.Top) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(120)),
            ) {
                Column {
                    orderedLessons.forEachIndexed { index, lesson ->
                        val isNext = next?.lesson?.id == lesson.id
                        LessonRow(
                            modifier = Modifier.staggerIn(index),
                            lesson = lesson,
                            isNext = isNext,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleLesson(lesson)
                            },
                            onMenu = { onLessonMenu(lesson) },
                            onOpenLink = { onOpenLink(lesson) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LessonRow(
    lesson: LessonEntity,
    isNext: Boolean,
    onToggle: () -> Unit,
    onMenu: () -> Unit,
    onOpenLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isNext) LvSurface else Color.Transparent)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {},
                onLongClick = onMenu,
            )
            .pressScale(interaction)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (lesson.done) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.lesson_done),
                    tint = LvAccent,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (isNext) LvText else LvBorder,
                            CircleShape,
                        ),
                )
            }
        }
        Text(
            text = lesson.title,
            color = if (lesson.done) LvMuted else LvText,
            fontSize = 13.sp,
            textDecoration = if (lesson.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f),
        )
        if (isNext) {
            Text(
                text = stringResource(R.string.next_tag),
                color = LvMuted,
                fontFamily = Mono,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        LvIconButton(
            iconRes = R.drawable.ic_external_link,
            contentDescription = stringResource(R.string.cd_open_link),
            onClick = onOpenLink,
            iconSize = 15.dp,
            touchSize = 24.dp,
        )
    }
}

@Composable
private fun PhaseEditorDialog(
    initial: PhaseEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    LvDialog(
        onDismiss = onDismiss,
        title = stringResource(if (initial == null) R.string.add_phase else R.string.rename),
    ) {
        LvTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.phase_name),
        )
        DialogActions(
            confirmLabel = stringResource(R.string.save),
            onConfirm = if (name.isNotBlank()) {
                { onSave(name.trim()) }
            } else {
                null
            },
            onDismiss = onDismiss,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LessonEditorDialog(
    initial: LessonEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var url by remember(initial?.id) { mutableStateOf(initial?.url.orEmpty()) }
    LvDialog(
        onDismiss = onDismiss,
        title = stringResource(
            if (initial == null) R.string.add_lesson else R.string.edit_lesson
        ),
    ) {
        LvTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.lesson_title),
        )
        Spacer(Modifier.height(10.dp))
        LvTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = stringResource(R.string.lesson_url),
            keyboardType = KeyboardType.Uri,
        )
        Text(
            text = stringResource(R.string.no_link),
            color = LvMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
        DialogActions(
            confirmLabel = stringResource(R.string.save),
            onConfirm = if (title.isNotBlank()) {
                { onSave(title.trim(), url.trim().takeIf { it.isNotBlank() }) }
            } else {
                null
            },
            onDismiss = onDismiss,
            extra = if (onDelete != null) {
                {
                    Box(
                        modifier = Modifier
                            .combinedClickable(onClick = onDelete, onLongClick = onDelete)
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
