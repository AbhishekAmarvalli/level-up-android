package com.levelup.app.ui.settings

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.levelup.app.BuildConfig
import com.levelup.app.R
import com.levelup.app.domain.UploadNetworkMode
import com.levelup.app.ui.components.CardLabel
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.DialogOption
import com.levelup.app.ui.components.LvCard
import com.levelup.app.ui.components.LvChip
import com.levelup.app.ui.components.LvDialog
import com.levelup.app.ui.components.LvIconButton
import com.levelup.app.ui.components.LvSecondaryButton
import com.levelup.app.ui.components.LvSwitch
import com.levelup.app.ui.components.LvTextField
import com.levelup.app.ui.components.MonoText
import com.levelup.app.ui.components.OptionsDialog
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvText
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

    Box(modifier = Modifier.fillMaxSize()) {
        val current = state
        if (current != null) {
            val settings = current.settings
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp)
                    .padding(top = 4.dp, bottom = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp)
                        .heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LvIconButton(
                        iconRes = R.drawable.ic_arrow_back,
                        contentDescription = stringResource(R.string.cd_back),
                        onClick = onBack,
                        iconSize = 18.dp,
                        tint = LvText,
                    )
                    Spacer(Modifier.fillMaxWidth(0.02f))
                    MonoText(text = stringResource(R.string.settings), fontSize = 16.sp)
                }

                SectionLabel(stringResource(R.string.reminders))
                LvCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .toggleable(
                                value = settings.dailyReminderEnabled,
                                role = Role.Switch,
                                indication = null,
                                interactionSource = remember {
                                    androidx.compose.foundation.interaction.MutableInteractionSource()
                                },
                                onValueChange = vm::setDailyReminderEnabled,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.daily_reminder),
                            color = LvText,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        LvSwitch(
                            checked = settings.dailyReminderEnabled,
                            onCheckedChange = null,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(LvBorder),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        vm.setReminderTime(hour * 60 + minute)
                                    },
                                    settings.dailyReminderMinutes / 60,
                                    settings.dailyReminderMinutes % 60,
                                    DateFormat.is24HourFormat(context),
                                ).show()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_time),
                            color = LvText,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        MonoText(
                            text = formatClock(context, settings.dailyReminderMinutes),
                            fontSize = 13.sp,
                        )
                    }
                }

                SectionLabel(stringResource(R.string.exam_settings))
                LvCard {
                    Text(
                        text = stringResource(
                            R.string.exam_auto_threshold,
                            settings.examAutoThresholdDays,
                        ),
                        color = LvText,
                        fontSize = 13.sp,
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(3, 5, 7, 10, 14).forEach { days ->
                            LvChip(
                                text = days.toString(),
                                active = settings.examAutoThresholdDays == days,
                                onClick = { vm.setExamThreshold(days) },
                                fontSize = 11.sp,
                            )
                        }
                    }
                    if (settings.examModeOverride != null) {
                        Text(
                            text = stringResource(
                                if (settings.examModeOverride == true) R.string.exam_override_on
                                else R.string.exam_override_off
                            ),
                            color = LvMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        LvSecondaryButton(
                            text = stringResource(R.string.reset_exam_auto),
                            onClick = vm::resetExamOverride,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                SectionLabel(stringResource(R.string.mobile_data))
                LvCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable { capDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.monthly_cap_row),
                            color = LvText,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        MonoText(
                            text = settings.monthlyMobileCapMb?.let {
                                stringResource(R.string.cap_value, it)
                            } ?: stringResource(R.string.no_cap_row),
                            fontSize = 13.sp,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.mobile_settings_note,
                            formatUploadMode(settings.uploadNetworkMode),
                        ),
                        color = LvMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                SectionLabel(stringResource(R.string.prompts_title))
                LvCard {
                    settings.prompts.forEachIndexed { index, prompt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable { promptDialog = PromptEdit(index, prompt, isNew = false) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = prompt,
                                color = LvText,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                            )
                            if (settings.prompts.size > 1) {
                                Text(
                                    text = stringResource(R.string.delete),
                                    color = LvMuted,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clickable { vm.deletePrompt(index) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                    LvSecondaryButton(
                        text = stringResource(R.string.add_prompt),
                        onClick = { promptDialog = PromptEdit(null, "", isNew = true) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                SectionLabel(stringResource(R.string.about))
                LvCard {
                    Text(
                        text = stringResource(R.string.about_text),
                        color = LvMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    CardLabel(
                        text = stringResource(R.string.version_name, BuildConfig.VERSION_NAME),
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
    LvDialog(
        onDismiss = onDismiss,
        title = stringResource(if (isNew) R.string.add_prompt else R.string.edit_prompt),
    ) {
        LvTextField(
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
