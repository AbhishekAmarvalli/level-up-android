package com.levelup.app.ui.settings

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.BuildConfig
import com.levelup.app.R
import com.levelup.app.domain.UploadNetworkMode
import com.levelup.app.ui.components.AppDialog
import com.levelup.app.ui.components.AppText
import com.levelup.app.ui.components.AppTextField
import com.levelup.app.ui.components.Card
import com.levelup.app.ui.components.Chip
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.DialogOption
import com.levelup.app.ui.components.HtmlSwitch
import com.levelup.app.ui.components.IconButton
import com.levelup.app.ui.components.OptionsDialog
import com.levelup.app.ui.components.SecondaryButton
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.TiIcon
import com.levelup.app.ui.components.appClickable
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import java.util.Calendar

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var capDialog by remember { mutableStateOf(false) }
    var promptDialog by remember { mutableStateOf<PromptEdit?>(null) }

    val current = state
    Box(modifier = Modifier.fillMaxSize()) {
        if (current != null) {
            val settings = current.settings
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        iconRes = R.drawable.ti_chevron_down,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = C.Text,
                        onClick = onBack,
                        iconSize = 16.dp,
                        rotation = 90f,
                    )
                    AppText(
                        text = stringResource(R.string.settings),
                        style = T.Title16,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                SectionLabel(stringResource(R.string.reminders), first = true)
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appClickable {
                                vm.setDailyReminderEnabled(!settings.dailyReminderEnabled)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppText(text = stringResource(R.string.daily_reminder), style = T.Body13)
                        HtmlSwitch(
                            on = settings.dailyReminderEnabled,
                            onCheckedChange = vm::setDailyReminderEnabled,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .appClickable {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> vm.setReminderTime(hour * 60 + minute) },
                                    settings.dailyReminderMinutes / 60,
                                    settings.dailyReminderMinutes % 60,
                                    DateFormat.is24HourFormat(context),
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppText(text = stringResource(R.string.reminder_time), style = T.Body13)
                        AppText(
                            text = formatClock(context, settings.dailyReminderMinutes),
                            style = T.Mono13,
                        )
                    }
                }

                SectionLabel(stringResource(R.string.exam_settings))
                Card {
                    AppText(
                        text = stringResource(
                            R.string.exam_auto_threshold,
                            settings.examAutoThresholdDays,
                        ),
                        style = T.Body13,
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(3, 5, 7, 10, 14).forEach { days ->
                            Chip(
                                text = days.toString(),
                                active = settings.examAutoThresholdDays == days,
                                onClick = { vm.setExamThreshold(days) },
                            )
                        }
                    }
                    if (settings.examModeOverride != null) {
                        AppText(
                            text = stringResource(
                                if (settings.examModeOverride == true) {
                                    R.string.exam_override_on
                                } else {
                                    R.string.exam_override_off
                                }
                            ),
                            style = T.Body12Muted,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        SecondaryButton(
                            text = stringResource(R.string.reset_exam_auto),
                            onClick = vm::resetExamOverride,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                SectionLabel(stringResource(R.string.mobile_data))
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appClickable { capDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AppText(
                            text = stringResource(R.string.monthly_cap_row),
                            style = T.Body13,
                        )
                        AppText(
                            text = settings.monthlyMobileCapMb?.let {
                                stringResource(R.string.cap_value, it)
                            } ?: stringResource(R.string.no_cap_row),
                            style = T.Mono13,
                        )
                    }
                    AppText(
                        text = stringResource(
                            R.string.mobile_settings_note,
                            formatUploadMode(settings.uploadNetworkMode),
                        ),
                        style = T.Body11Muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                SectionLabel(stringResource(R.string.prompts_title))
                Card {
                    settings.prompts.forEachIndexed { index, prompt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .appClickable {
                                    promptDialog = PromptEdit(index, prompt, isNew = false)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppText(
                                text = prompt,
                                style = T.Body12,
                                modifier = Modifier.weight(1f),
                            )
                            if (settings.prompts.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .appClickable { vm.deletePrompt(index) },
                                ) {
                                    TiIcon(
                                        iconRes = R.drawable.ti_trash,
                                        size = 15.dp,
                                        tint = C.Muted,
                                        contentDescription = stringResource(R.string.delete),
                                    )
                                }
                            }
                        }
                    }
                    SecondaryButton(
                        text = stringResource(R.string.add_prompt),
                        onClick = { promptDialog = PromptEdit(null, "", isNew = true) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                SectionLabel(stringResource(R.string.about))
                Card {
                    AppText(
                        text = stringResource(R.string.about_text),
                        style = T.Body12Muted,
                    )
                    AppText(
                        text = stringResource(R.string.version_name, BuildConfig.VERSION_NAME),
                        style = T.Label11,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (capDialog) {
        val options = listOf(200, 500, 1024, 2048)
        OptionsDialog(
            title = stringResource(R.string.monthly_cap_row),
            options = options.map { mb ->
                DialogOption(stringResource(R.string.cap_value, mb)) { vm.setMonthlyCap(mb) }
            } + DialogOption(stringResource(R.string.no_cap_row)) { vm.setMonthlyCap(null) },
            onDismiss = { capDialog = false },
        )
    }

    promptDialog?.let { edit ->
        PromptEditorDialog(
            initial = edit.text,
            isNew = edit.isNew,
            onDismiss = { promptDialog = null },
            onSave = { text ->
                vm.savePrompt(edit.index, text)
                promptDialog = null
            },
        )
    }
}

private data class PromptEdit(val index: Int?, val text: String, val isNew: Boolean)

@Composable
private fun PromptEditorDialog(
    initial: String,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AppDialog(
        onDismiss = onDismiss,
        title = stringResource(if (isNew) R.string.add_prompt else R.string.edit_prompt),
    ) {
        AppTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.prompt_text),
            singleLine = false,
        )
        DialogActions(
            confirmLabel = stringResource(R.string.save),
            onConfirm = if (text.isNotBlank()) {
                { onSave(text.trim()) }
            } else {
                null
            },
            onDismiss = onDismiss,
        )
    }
}

private fun formatClock(context: android.content.Context, minutesOfDay: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        set(Calendar.MINUTE, minutesOfDay % 60)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

@Composable
private fun formatUploadMode(mode: UploadNetworkMode): String = stringResource(
    when (mode) {
        UploadNetworkMode.WIFI_ONLY -> R.string.upload_wifi_only
        UploadNetworkMode.WIFI_AND_MOBILE -> R.string.upload_wifi_mobile
        UploadNetworkMode.ASK_BEFORE_MOBILE -> R.string.upload_ask_mobile
    }
)
