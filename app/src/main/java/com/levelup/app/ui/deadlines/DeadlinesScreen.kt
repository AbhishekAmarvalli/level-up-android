package com.levelup.app.ui.deadlines

import android.app.DatePickerDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.R
import com.levelup.app.data.DeadlineEntity
import com.levelup.app.domain.DateText
import com.levelup.app.domain.DeadlineType
import com.levelup.app.domain.daysLeft
import com.levelup.app.domain.typeEnum
import com.levelup.app.ui.components.AppDialog
import com.levelup.app.ui.components.AppText
import com.levelup.app.ui.components.AppTextField
import com.levelup.app.ui.components.Card
import com.levelup.app.ui.components.Chip
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.DialogOption
import com.levelup.app.ui.components.OptionsDialog
import com.levelup.app.ui.components.RedCheckbox
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.appClickable
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import java.time.LocalDate
import java.time.YearMonth

private sealed interface DeadlineDialog {
    data class Editor(val deadline: DeadlineEntity?) : DeadlineDialog
    data class Menu(val deadline: DeadlineEntity) : DeadlineDialog
    data class Delete(val deadline: DeadlineEntity) : DeadlineDialog
}

@Composable
fun DeadlinesScreen(vm: DeadlinesViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf("list") }
    var dialog by remember { mutableStateOf<DeadlineDialog?>(null) }

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
                            .padding(top = 2.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText(
                            text = stringResource(R.string.deadlines),
                            style = T.Title16,
                            modifier = Modifier.appClickable(
                                onClick = {},
                                onLongClick = { dialog = DeadlineDialog.Editor(null) },
                            ),
                        )
                    }
                }

                item(key = "calendar") {
                    CalendarCard(
                        month = YearMonth.from(current.today),
                        today = current.today,
                        deadlines = current.all,
                    )
                }

                item(key = "modes") {
                    Row(
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Chip(
                            text = stringResource(R.string.list),
                            active = mode == "list",
                            onClick = { mode = "list" },
                        )
                        Chip(
                            text = stringResource(R.string.timeline),
                            active = mode == "timeline",
                            onClick = { mode = "timeline" },
                        )
                    }
                }

                if (mode == "list") {
                    itemsIndexed(
                        items = current.upcoming,
                        key = { _, deadline -> deadline.id },
                    ) { _, deadline ->
                        DeadlineListRow(
                            deadline = deadline,
                            today = current.today,
                            onClick = { dialog = DeadlineDialog.Editor(deadline) },
                            onLongClick = { dialog = DeadlineDialog.Menu(deadline) },
                        )
                    }
                } else {
                    itemsIndexed(
                        items = current.upcoming,
                        key = { _, deadline -> deadline.id },
                    ) { _, deadline ->
                        DeadlineTimelineRow(
                            deadline = deadline,
                            today = current.today,
                            onClick = { dialog = DeadlineDialog.Editor(deadline) },
                            onLongClick = { dialog = DeadlineDialog.Menu(deadline) },
                        )
                    }
                }

                if (current.done.isNotEmpty()) {
                    item(key = "done-label") {
                        SectionLabel(stringResource(R.string.done_upper))
                    }
                    itemsIndexed(
                        items = current.done,
                        key = { _, deadline -> deadline.id },
                    ) { _, deadline ->
                        DoneDeadlineRow(
                            deadline = deadline,
                            onToggleDone = { vm.setDone(deadline, false) },
                            onLongClick = { dialog = DeadlineDialog.Menu(deadline) },
                        )
                    }
                }
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
            onDelete = d.deadline?.let { { dialog = DeadlineDialog.Delete(it) } },
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
) {
    val deadlineDates = remember(deadlines) { deadlines.mapNotNull { it.date }.toSet() }
    val leading = month.atDay(1).dayOfWeek.value % 7
    val cells: List<Int?> = List(leading) { null } + (1..month.lengthOfMonth()).toList()

    Card {
        AppText(
            text = DateText.monthYear(month.atDay(1)),
            style = T.Mono13,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            "SMTWTFS".forEach { letter ->
                AppText(
                    text = letter.toString(),
                    style = T.Label11,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.weight(1f).height(30.dp))
                    } else {
                        val date = month.atDay(day)
                        val isToday = date == today
                        val hasDeadline = deadlineDates.contains(date.toString())
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .then(
                                    if (isToday) {
                                        Modifier.border(1.dp, C.Muted, RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppText(text = day.toString(), style = T.Body12)
                                Box(
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (hasDeadline) C.Red else C.Red.copy(alpha = 0f)
                                        ),
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) {
                    Box(modifier = Modifier.weight(1f).height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun DeadlineListRow(
    deadline: DeadlineEntity,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val date = DateText.parse(deadline.date)
    val days = (deadline.daysLeft(today) ?: 0L).coerceAtLeast(0L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .appClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                text = deadline.title,
                style = T.Body14,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (date != null) {
                AppText(text = DateText.header(date), style = T.Mono12Muted)
            }
        }
        Spacer(Modifier.width(10.dp))
        Countdown(days)
    }
}

@Composable
private fun DeadlineTimelineRow(
    deadline: DeadlineEntity,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val date = DateText.parse(deadline.date)
    val days = (deadline.daysLeft(today) ?: 0L).coerceAtLeast(0L)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp)
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
                .offset(x = (-5).dp, y = 3.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(C.Red),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (date != null) {
                    AppText(text = DateText.header(date), style = T.Mono12Muted)
                }
                AppText(
                    text = deadline.title,
                    style = T.Body14,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Countdown(days)
        }
    }
}

@Composable
private fun DoneDeadlineRow(
    deadline: DeadlineEntity,
    onToggleDone: () -> Unit,
    onLongClick: () -> Unit,
) {
    val doneDate = deadline.doneDate?.let { DateText.parse(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .appClickable(onClick = {}, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RedCheckbox(
            done = true,
            size = 18.dp,
            onToggle = onToggleDone,
        )
        AppText(
            text = deadline.title,
            style = T.Body13Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (doneDate != null) {
            AppText(text = DateText.short(doneDate), style = T.Mono12Muted)
        }
    }
}

@Composable
private fun Countdown(days: Long) {
    Column(horizontalAlignment = Alignment.End) {
        AppText(text = days.toString(), style = T.Big24Red, maxLines = 1)
        AppText(text = stringResource(R.string.days_left), style = T.Body11Muted)
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
            { _, year, month, day -> date = LocalDate.of(year, month + 1, day) },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).show()
    }

    AppDialog(
        onDismiss = onDismiss,
        title = stringResource(if (initial == null) R.string.add_deadline else R.string.edit_deadline),
    ) {
        AppTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.deadline_title),
        )
        AppText(
            text = stringResource(R.string.deadline_date),
            style = T.Label11,
            modifier = Modifier.padding(top = 10.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, C.Border, RoundedCornerShape(8.dp))
                .appClickable(onClick = ::pickDate)
                .padding(horizontal = 10.dp, vertical = 9.dp),
        ) {
            AppText(text = DateText.header(date), style = T.Mono13)
        }
        AppText(
            text = stringResource(R.string.deadline_type),
            style = T.Label11,
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DeadlineType.entries.forEach { option ->
                Chip(
                    text = typeLabel(option),
                    active = type == option,
                    onClick = { type = option },
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
