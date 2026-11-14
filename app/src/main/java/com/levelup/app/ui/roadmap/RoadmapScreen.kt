package com.levelup.app.ui.roadmap

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.R
import com.levelup.app.data.LessonEntity
import com.levelup.app.data.PhaseEntity
import com.levelup.app.data.PhaseWithLessons
import com.levelup.app.domain.NextLesson
import com.levelup.app.ui.components.AppDialog
import com.levelup.app.ui.components.AppText
import com.levelup.app.ui.components.AppTextField
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.DialogOption
import com.levelup.app.ui.components.HtmlSwitch
import com.levelup.app.ui.components.OptionsDialog
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.TiIcon
import com.levelup.app.ui.components.appClickable
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
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
            if (!runCatching { context.startActivity(intent) }.isSuccess) {
                Toast.makeText(context, context.getString(R.string.link_error), Toast.LENGTH_SHORT)
                    .show()
            }
        }
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
                item(key = "title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppText(
                            text = stringResource(R.string.roadmap),
                            style = T.Title16,
                            modifier = Modifier.appClickable(
                                onClick = {},
                                onLongClick = {
                                    dialog = RoadmapDialog.PhaseEditor(null)
                                },
                            ),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppText(
                                text = stringResource(R.string.expand_all),
                                style = T.Body12Muted,
                            )
                            HtmlSwitch(
                                on = expandAll,
                                onCheckedChange = {
                                    expandAll = it
                                    if (it) manualOpen.clear()
                                },
                            )
                        }
                    }
                }

                if (current.phases.isEmpty()) {
                    item(key = "empty") {
                        AppText(text = stringResource(R.string.no_phases), style = T.Body12Muted)
                    }
                }

                itemsIndexed(
                    items = current.phases,
                    key = { _, phase -> phase.phase.id },
                ) { _, phase ->
                    val next = current.next
                    val isCurrent = next?.phase?.phase?.id == phase.phase.id
                    val open = if (expandAll) true else (manualOpen[phase.phase.id] ?: isCurrent)
                    PhaseBlock(
                        phase = phase,
                        next = next,
                        open = open,
                        onToggleOpen = { togglePhase(phase, open) },
                        onMenu = { dialog = RoadmapDialog.PhaseMenu(phase) },
                        onToggleLesson = { lesson -> vm.setLessonDone(lesson, !lesson.done) },
                        onLessonMenu = { lesson ->
                            dialog = RoadmapDialog.LessonMenu(phase, lesson)
                        },
                        onOpenLink = ::openLink,
                    )
                }
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
            onDelete = d.lesson?.let { { dialog = RoadmapDialog.DeleteLesson(it) } },
        )

        is RoadmapDialog.PhaseMenu -> {
            val isNext = state?.next?.phase?.phase?.id == d.phase.phase.id
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
                    if (isNext) {
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

@Composable
private fun PhaseBlock(
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
    val done = phase.lessons.count { it.done }
    val fraction = if (total == 0) 0f else done.toFloat() / total.toFloat()
    val percent = (fraction * 100f).roundToInt()
    val complete = total > 0 && done == total
    val isCurrent = next?.phase?.phase?.id == phase.phase.id
    val lessons = phase.lessons.sortedWith(compareBy({ it.sortOrder }, { it.id }))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 9.dp)
            .drawBehind {
                drawLine(
                    color = C.Border,
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-10).dp)
                .size(19.dp)
                .clip(CircleShape)
                .background(if (complete) C.Red else C.Bg)
                .border(
                    1.5.dp,
                    if (complete) C.Red else if (isCurrent) C.Text else C.Border,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (complete) {
                TiIcon(
                    iconRes = R.drawable.ti_check,
                    size = 12.dp,
                    tint = androidx.compose.ui.graphics.Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 20.dp, bottom = 12.dp)
                .animateContentSize(tween(200)),
        ) {
            val haptic = LocalHapticFeedback.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .appClickable(onClick = onToggleOpen, onLongClick = onMenu),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = phase.phase.name,
                    style = T.Mono14,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AppText(text = "$percent%", style = T.Mono13Red)
                Spacer(Modifier.width(4.dp))
                TiIcon(
                    iconRes = if (open) R.drawable.ti_chevron_up else R.drawable.ti_chevron_down,
                    size = 12.dp,
                    tint = C.Muted,
                )
            }
            ProgressBar(
                fraction = fraction,
                height = 4.dp,
                modifier = Modifier.padding(top = 6.dp),
            )
            AppText(
                text = stringResource(R.string.lessons_count, done, total),
                style = T.Body11Muted,
                modifier = Modifier.padding(top = 3.dp),
            )

            if (open) {
                lessons.forEach { lesson ->
                    val isNext = next?.lesson?.id == lesson.id
                    LessonRow(
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

@Composable
private fun LessonRow(
    lesson: LessonEntity,
    isNext: Boolean,
    onToggle: () -> Unit,
    onMenu: () -> Unit,
    onOpenLink: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isNext) C.Surface else C.Bg.copy(alpha = 0f))
            .appClickable(onClick = {}, onLongClick = onMenu)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .appClickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (lesson.done) {
                TiIcon(
                    iconRes = R.drawable.ti_check,
                    size = 16.dp,
                    tint = C.Red,
                    contentDescription = stringResource(R.string.lesson_done),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (isNext) C.Text else C.Border,
                            CircleShape,
                        ),
                )
            }
        }
        AppText(
            text = lesson.title,
            style = if (lesson.done) T.Body13Muted else T.Body13,
            textDecoration = if (lesson.done) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
        if (isNext) {
            AppText(text = stringResource(R.string.next_tag), style = T.Label11)
        }
        Box(
            modifier = Modifier
                .size(16.dp)
                .appClickable(onClick = onOpenLink),
            contentAlignment = Alignment.Center,
        ) {
            TiIcon(
                iconRes = R.drawable.ti_external_link,
                size = 15.dp,
                tint = C.Muted,
                contentDescription = stringResource(R.string.cd_open_link),
            )
        }
    }
}

@Composable
private fun PhaseEditorDialog(
    initial: PhaseEntity?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    AppDialog(
        onDismiss = onDismiss,
        title = stringResource(if (initial == null) R.string.add_phase else R.string.rename),
    ) {
        AppTextField(
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

@Composable
private fun LessonEditorDialog(
    initial: LessonEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var url by remember(initial?.id) { mutableStateOf(initial?.url.orEmpty()) }
    AppDialog(
        onDismiss = onDismiss,
        title = stringResource(if (initial == null) R.string.add_lesson else R.string.edit_lesson),
    ) {
        AppTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.lesson_title),
        )
        Spacer(Modifier.height(8.dp))
        AppTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = stringResource(R.string.lesson_url),
            keyboardType = KeyboardType.Uri,
        )
        AppText(
            text = stringResource(R.string.no_link),
            style = T.Label11,
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
