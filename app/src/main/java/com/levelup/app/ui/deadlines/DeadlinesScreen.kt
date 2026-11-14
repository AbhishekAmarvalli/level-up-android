package com.levelup.app.ui.deadlines

import android.app.DatePickerDialog
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.R
import com.levelup.app.data.DeadlineEntity
import com.levelup.app.domain.DateText
import com.levelup.app.domain.DeadlineType
import com.levelup.app.domain.daysLeft
import com.levelup.app.domain.typeEnum
import com.levelup.app.ui.components.CardLabel
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.DialogOption
import com.levelup.app.ui.components.LvCard
import com.levelup.app.ui.components.LvCheckbox
import com.levelup.app.ui.components.LvChip
import com.levelup.app.ui.components.LvDialog
import com.levelup.app.ui.components.LvIconButton
import com.levelup.app.ui.components.LvTextField
import com.levelup.app.ui.components.MonoText
import com.levelup.app.ui.components.OptionsDialog
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.animatedCount
import com.levelup.app.ui.components.lvClickable
import com.levelup.app.ui.components.pressScale
import com.levelup.app.ui.components.staggerIn
import com.levelup.app.ui.theme.LvAccent
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvSurface
import com.levelup.app.ui.theme.LvText
import com.levelup.app.ui.theme.Mono
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.max

private sealed interface DeadlineDialog {
    data class Editor(val deadline: DeadlineEntity?) : DeadlineDialog
    data class Menu(val deadline: DeadlineEntity) : DeadlineDialog
    data class Delete(val deadline: DeadlineEntity) : DeadlineDialog
}

@Composable
fun DeadlinesScreen(vm: DeadlinesViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf("list") }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var dialog by remember { mutableStateOf<DeadlineDialog?>(null) }
    val month = runCatching { YearMonth.parse(monthKey) }.getOrDefault(YearMonth.now())

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
                            .padding(top = 2.dp, bottom = 10.dp)
                            .heightIn(min = 36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoText(text = stringResource(R.string.deadlines), fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        LvIconButton(
                            iconRes = R.drawable.ic_plus,
                            contentDescription = stringResource(R.string.add_deadline),
                            onClick = { dialog = DeadlineDialog.Editor(null) },
                            iconSize = 18.dp,
                            touchSize = 32.dp,
                        )
                    }
                }

                item(key = "calendar") {
                    CalendarCard(
                        month = month,
                        today = current.today,
                        deadlines = current.all,
                        onPrev = { monthKey = month.minusMonths(1).toString() },
                        onNext = { monthKey = month.plusMonths(1).toString() },
                    )
                }

                item(key = "toggle") {
                    Row(
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        LvChip(
                            text = stringResource(R.string.list),
                            active = mode == "list",
                            onClick = { mode = "list" },
                        )
                        LvChip(
                            text = stringResource(R.string.timeline),
                            active = mode == "timeline",
                            onClick = { mode = "timeline" },
                        )
                    }
                }

                if (current.upcoming.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.no_deadlines),
                            color = LvMuted,
                            fontSize = 12.sp,
                        )
                    }
                } else if (mode == "list") {
                    itemsIndexed(items = current.upcoming, key = { _, deadline -> deadline.id }) { index, deadline ->
                        DeadlineListRow(
                            deadline = deadline,
                            today = current.today,
                            modifier = Modifier.staggerIn(index),
                            onClick = { dialog = DeadlineDialog.Editor(deadline) },
                            onLongClick = { dialog = DeadlineDialog.Menu(deadline) },
                            onToggleDone = { vm.setDone(deadline, true) },
                        )
                    }
                } else {
                    itemsIndexed(items = current.upcoming, key = { _, deadline -> deadline.id }) { index, deadline ->
                        DeadlineTimelineRow(
                            deadline = deadline,
                            today = current.today,
                            modifier = Modifier.staggerIn(index),
                            onClick = { dialog = DeadlineDialog.Editor(deadline) },
                            onLongClick = { dialog = DeadlineDialog.Menu(deadline) },
                        )
                    }
                }

                if (current.done.isNotEmpty()) {
                    item(key = "done-label") { SectionLabel(stringResource(R.string.done_upper)) }
                    itemsIndexed(items = current.done, key = { _, deadline -> deadline.id }) { index, deadline ->
                        DoneDeadlineRow(
                            deadline = deadline,
                            modifier = Modifier.staggerIn(index),
                            onToggleDone = { vm.setDone(deadline, false) },
                            onLongClick = { dialog = DeadlineDialog.Menu(deadline) },
                        )
                    }
                }

                item(key = "bottom") { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    when (val d = dialog) {
        is DeadlineDialog.Editor -> DeadlineEditorDialog(
            initial = d.deadline,
            today = state?.today ?: LocalDate.now(),
            onDismiss = { dialog = null },
            onSave = { title, date, type ->
                val deadline = d.deadline
                if (deadline == null) vm.add(title, date, type)
                else vm.update(deadline, title, date, type)
                dialog = null
            },
            onDelete = d.deadline?.let { deadline ->
                { dialog = DeadlineDialog.Delete(deadline) }
            },
        )

        is DeadlineDialog.Menu -> OptionsDialog(
            title = d.deadline.title,
            options = listOf(
                DialogOption(stringResource(R.string.edit_deadline)) {
                    dialog = DeadlineDialog.Editor(d.deadline)
                },
                DialogOption(
                    if (d.deadline.done) stringResource(R.string.mark_deadline_undone)
                    else stringResource(R.string.mark_deadline_done)
                ) {
                    vm.setDone(d.deadline, !d.deadline.done)
                    dialog = null
                },
                DialogOption(stringResource(R.string.delete), destructive = true) {
                    dialog = DeadlineDialog.Delete(d.deadline)
                },
            ),
            onDismiss = { dialog = null },
        )

        is DeadlineDialog.Delete -> ConfirmDialog(
            title = stringResource(R.string.delete_deadline_title),
            message = stringResource(R.string.delete_deadline_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.delete(d.deadline)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    today: LocalDate,
    deadlines: List<DeadlineEntity>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val deadlineDates = remember(deadlines) { deadlines.mapNotNull { it.date }.toSet() }
    val leading = month.atDay(1).dayOfWeek.value % 7
    val cells: List<Int?> = List(leading) { null } + (1..month.lengthOfMonth()).toList()

    LvCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LvIconButton(
                iconRes = R.drawable.ic_chevron_up,
                contentDescription = stringResource(R.string.cd_prev_month),
                onClick = onPrev,
                iconSize = 16.dp,
                rotationDegrees = -90f,
                touchSize = 28.dp,
            )
            MonoText(
                text = DateText.monthYear(month.atDay(1)),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            LvIconButton(
                iconRes = R.drawable.ic_chevron_down,
                contentDescription = stringResource(R.string.cd_next_month),
                onClick = onNext,
                iconSize = 16.dp,
                rotationDegrees = 90f,
                touchSize = 28.dp,
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            "SMTWTFS".forEach { letter ->
                Text(
                    text = letter.toString(),
                    color = LvMuted,
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f).height(30.dp))
                    } else {
                        val date = month.atDay(day)
                        val isToday = date == today
                        val hasDeadline = deadlineDates.contains(date.toString())
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .padding(horizontal = 1.dp)
                                .then(
                                    if (isToday) {
                                        Modifier
                                            .border(1.dp, LvMuted, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(text = day.toString(), color = LvText, fontSize = 12.sp)
                            Box(
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (hasDeadline) LvAccent else Color.Transparent),
                            )
                        }
                    }
                }
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeadlineListRow(
    deadline: DeadlineEntity,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = max(0L, deadline.daysLeft(today) ?: 0L)
    val date = DateText.parse(deadline.date)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LvSurface)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .pressScale(interaction)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LvCheckbox(checked = false, onToggle = onToggleDone, size = 18.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(text = deadline.title, color = LvText, fontSize = 14.sp)
            if (date != null) {
                Text(
                    text = DateText.header(date),
                    color = LvMuted,
                    fontFamily = Mono,
                    fontSize = 12.sp,
                )
            }
        }
        Countdown(days)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeadlineTimelineRow(
    deadline: DeadlineEntity,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = max(0L, deadline.daysLeft(today) ?: 0L)
    val date = DateText.parse(deadline.date)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .pressScale(interaction),
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 4.dp)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(LvBorder),
            )
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 3.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(LvAccent),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (date != null) {
                    MonoText(text = DateText.header(date), fontSize = 12.sp, color = LvMuted)
                }
                Text(text = deadline.title, color = LvText, fontSize = 14.sp)
            }
            Countdown(days)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DoneDeadlineRow(
    deadline: DeadlineEntity,
    onToggleDone: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val doneDate = deadline.doneDate?.let { DateText.parse(it) }
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LvSurface)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onToggleDone,
                onLongClick = onLongClick,
            )
            .pressScale(interaction)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LvCheckbox(checked = true, onToggle = onToggleDone, size = 18.dp)
        Text(
            text = deadline.title,
            color = LvMuted,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        if (doneDate != null) {
            MonoText(text = DateText.short(doneDate), fontSize = 12.sp, color = LvMuted)
        }
    }
}

@Composable
private fun Countdown(days: Long) {
    Column(horizontalAlignment = Alignment.End) {
        MonoText(
            text = days.toString(),
            fontSize = 24.sp,
            color = LvAccent,
            lineHeight = 24.sp,
            maxLines = 1,
        )
        Text(text = stringResource(R.string.days_left), color = LvMuted, fontSize = 11.sp)
    }
}

@Composable
private fun typeLabel(type: DeadlineType): String = stringResource(
    when (type) {
        DeadlineType.EXAM -> R.string.type_exam
        DeadlineType.ASSIGNMENT -> R.string.type_assignment
        DeadlineType.PROJECT -> R.string.type_project
        DeadlineType.OTHER -> R.string.type_other
    }
)

@Composable
private fun DeadlineEditorDialog(
    initial: DeadlineEntity?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, DeadlineType) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember(initial?.id) {
        mutableStateOf(initial?.date?.let { DateText.parse(it) } ?: today)
    }
    var type by remember(initial?.id) { mutableStateOf(initial?.typeEnum() ?: DeadlineType.OTHER) }

    fun pickDate() {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                date = LocalDate.of(year, month + 1, day)
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).show()
    }

    LvDialog(
        onDismiss = onDismiss,
        title = stringResource(if (initial == null) R.string.add_deadline else R.string.edit_deadline),
    ) {
        LvTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.deadline_title),
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.deadline_date), color = LvMuted, fontSize = 11.sp, fontFamily = Mono)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, LvBorder, RoundedCornerShape(8.dp))
                .lvClickable { pickDate() }
                .padding(horizontal = 10.dp, vertical = 11.dp),
        ) {
            MonoText(text = DateText.header(date), fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.deadline_type), color = LvMuted, fontSize = 11.sp, fontFamily = Mono)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DeadlineType.entries.forEach { option ->
                LvChip(
                    text = typeLabel(option),
                    active = type == option,
                    onClick = { type = option },
                    fontSize = 11.sp,
                )
            }
        }
        DialogActions(
            confirmLabel = stringResource(R.string.save),
            onConfirm = if (title.isNotBlank()) {
                { onSave(title.trim(), date, type) }
            } else {
                null
            },
            onDismiss = onDismiss,
            extra = if (onDelete != null) {
                {
                    Box(
                        modifier = Modifier
                            .lvClickable(onClick = onDelete)
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
