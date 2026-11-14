package com.levelup.app.ui.speaking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.levelup.app.R
import com.levelup.app.data.ClipEntity
import com.levelup.app.drive.DriveClient
import com.levelup.app.domain.UploadNetworkMode
import com.levelup.app.domain.UploadState
import com.levelup.app.domain.stateEnum
import com.levelup.app.recording.CameraRecorder
import com.levelup.app.ui.components.CardLabel
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.DialogOption
import com.levelup.app.ui.components.LvCard
import com.levelup.app.ui.components.LvCheckbox
import com.levelup.app.ui.components.LvChip
import com.levelup.app.ui.components.LvIconButton
import com.levelup.app.ui.components.LvPrimaryButton
import com.levelup.app.ui.components.LvProgressBar
import com.levelup.app.ui.components.LvSecondaryButton
import com.levelup.app.ui.components.LvSwitch
import com.levelup.app.ui.components.MonoText
import com.levelup.app.ui.components.OptionsDialog
import com.levelup.app.ui.components.SectionLabel
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_SECONDS = 300

@Composable
fun SpeakingScreen(vm: SpeakingViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val recorder = remember { CameraRecorder(context) }
    val hasPermissions = rememberCameraPermissions()
    var requestedOnce by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { requestedOnce = true }

    var recording by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableIntStateOf(0) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var barPhase by remember { mutableFloatStateOf(0f) }
    var playerPath by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<ClipEntity?>(null) }
    var deleteOnDrive by remember { mutableStateOf(false) }
    var showCapDialog by remember { mutableStateOf(false) }

    val googleOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveClient.SCOPE))
            .build()
    }
    val signInClient = remember { GoogleSignIn.getClient(context, googleOptions) }
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val email = account?.email
            if (email.isNullOrBlank()) {
                Toast.makeText(context, R.string.drive_signin_failed, Toast.LENGTH_LONG).show()
            } else {
                vm.enableBackup(email)
            }
        } catch (error: ApiException) {
            if (error.statusCode != GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                Toast.makeText(context, R.string.drive_signin_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) { vm.refreshAll() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(80)
            barPhase += 0.35f
        }
    }

    val smoothAmplitude by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = tween(120),
        label = "amplitude",
    )

    LaunchedEffect(recording, startedAt) {
        if (!recording) return@LaunchedEffect
        while (true) {
            val seconds = ((SystemClock.elapsedRealtime() - startedAt) / 1000L).toInt()
            elapsed = seconds
            if (seconds >= MAX_SECONDS) {
                recorder.stop()
                break
            }
            delay(200)
        }
    }

    fun startRecording() {
        val file = vm.newClipFile()
        amplitude = 0f
        elapsed = 0
        val started = recorder.start(
            file = file,
            onStarted = { startedAt = it; recording = true },
            onAmplitude = { db -> amplitude = normalizeAmplitude(db) },
            onFinished = { seconds ->
                recording = false
                elapsed = 0
                amplitude = 0f
                vm.saveClip(file, seconds)
            },
            onError = {
                recording = false
                Toast.makeText(context, R.string.recording_error, Toast.LENGTH_LONG).show()
            },
        )
        if (!started) {
            Toast.makeText(context, R.string.recording_error, Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val current = state
        if (current != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 10.dp)
                        .heightIn(min = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoText(text = stringResource(R.string.speaking), fontSize = 16.sp)
                }

                LvCard {
                    CardLabel(stringResource(R.string.todays_prompt))
                    Text(
                        text = current.prompt,
                        color = LvText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                    )
                    MonoText(
                        text = formatTime(if (recording) (MAX_SECONDS - elapsed) else MAX_SECONDS),
                        fontSize = 26.sp,
                        color = LvAccent,
                    )
                }

                if (!hasPermissions) {
                    LvCard(
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        MonoText(
                            text = stringResource(R.string.camera_rationale_title),
                            fontSize = 14.sp,
                        )
                        Text(
                            text = stringResource(R.string.camera_rationale_message),
                            color = LvMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            LvPrimaryButton(
                                text = stringResource(R.string.enable_camera),
                                onClick = {
                                    requestedOnce = true
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO,
                                        )
                                    )
                                },
                            )
                            if (requestedOnce) {
                                LvSecondaryButton(
                                    text = stringResource(R.string.open_settings),
                                    onClick = {
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.parse("package:${context.packageName}"),
                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LvSurface),
                    ) {
                        CameraPreview(recorder = recorder, modifier = Modifier.fillMaxSize())
                        Text(
                            text = stringResource(R.string.front_camera_preview),
                            color = LvMuted,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 10.dp, top = 8.dp),
                        )
                        if (recording) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LvAccent),
                                )
                                Spacer(Modifier.width(5.dp))
                                MonoText(text = stringResource(R.string.rec), fontSize = 12.sp)
                            }
                        }
                    }

                    val pulse = rememberInfiniteTransition(label = "recordPulse")
                    val ringScale by pulse.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.16f,
                        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
                        label = "recordRingScale",
                    )
                    val ringAlpha by pulse.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
                        label = "recordRingAlpha",
                    )
                    val recordInteraction = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (recording) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .graphicsLayer {
                                        scaleX = ringScale
                                        scaleY = ringScale
                                        alpha = ringAlpha
                                    }
                                    .border(1.5.dp, LvAccent, CircleShape),
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(LvSurface)
                                .border(1.5.dp, LvBorder, CircleShape)
                                .clickable(
                                    interactionSource = recordInteraction,
                                    indication = null,
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (recording) recorder.stop() else startRecording()
                                }
                                .pressScale(recordInteraction),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (recording) R.drawable.ic_player_stop else R.drawable.ic_video
                                ),
                                contentDescription = stringResource(
                                    if (recording) R.string.cd_stop else R.string.cd_record
                                ),
                                tint = LvAccent,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(30) { index ->
                        val multiplier = 0.35f + 0.65f * abs(sin(barPhase + index * 0.55f))
                        val target = if (recording) {
                            (6f + smoothAmplitude * 30f * multiplier).dp
                        } else {
                            6.dp
                        }
                        val barHeight by animateDpAsState(
                            targetValue = target,
                            animationSpec = tween(120),
                            label = "waveBar$index",
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(if (recording) LvText else LvMuted),
                        )
                        if (index < 29) Spacer(Modifier.width(3.dp))
                    }
                }

                Text(
                    text = stringResource(
                        if (recording) R.string.recording_hint else R.string.record_hint
                    ),
                    color = LvMuted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                SectionLabel(stringResource(R.string.saved_clips))
                if (current.clips.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_clips),
                        color = LvMuted,
                        fontSize = 12.sp,
                    )
                } else {
                    current.clips.forEachIndexed { index, clip ->
                        ClipRow(
                            clip = clip,
                            backupEnabled = current.settings.backupEnabled,
                            modifier = Modifier.staggerIn(index),
                            onPlay = {
                                if (File(clip.filePath).exists()) {
                                    playerPath = clip.filePath
                                } else {
                                    Toast.makeText(context, R.string.clip_missing, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                deleteTarget = clip
                                deleteOnDrive = clip.stateEnum() == UploadState.UPLOADED
                            },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.storage))
                StorageCard(
                    state = current,
                    onToggleBackup = { enabled ->
                        if (enabled) signInLauncher.launch(signInClient.signInIntent)
                        else vm.disableBackup()
                    },
                    onModeSelected = vm::setUploadMode,
                    onCapClick = { showCapDialog = true },
                    onRefresh = vm::refreshStorage,
                )

                SectionLabel(stringResource(R.string.weekly_review))
                WeeklyChecklist(
                    selected = current.settings.checklistItems,
                    onToggle = vm::toggleChecklist,
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    playerPath?.let { path ->
        VideoPlayerDialog(path = path, onDismiss = { playerPath = null })
    }

    deleteTarget?.let { clip ->
        ConfirmDialog(
            title = stringResource(R.string.delete_clip_title),
            message = stringResource(R.string.delete_clip_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.deleteClip(clip, deleteOnDrive)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
            extraContent = if (state?.settings?.backupEnabled == true && clip.driveFileId != null) {
                {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LvCheckbox(
                            checked = deleteOnDrive,
                            onToggle = { deleteOnDrive = !deleteOnDrive },
                            size = 18.dp,
                        )
                        Text(
                            text = stringResource(R.string.delete_also_drive),
                            color = LvMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                null
            },
        )
    }

    if (showCapDialog) {
        val options = listOf(200, 500, 1024, 2048)
        OptionsDialog(
            title = stringResource(R.string.monthly_cap),
            options = options.map { mb ->
                DialogOption(stringResource(R.string.cap_value, mb)) {
                    vm.setMonthlyCap(mb)
                }
            } + DialogOption(stringResource(R.string.no_cap)) { vm.setMonthlyCap(null) },
            onDismiss = { showCapDialog = false },
        )
    }
}

@Composable
private fun CameraPreview(recorder: CameraRecorder, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        androidx.camera.view.PreviewView(context).apply {
            scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
            implementationMode = androidx.camera.view.PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(Unit) {
        recorder.bind(lifecycleOwner, previewView) { }
        onDispose { recorder.release() }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun rememberCameraPermissions(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    var granted by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = permissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

@Composable
private fun ClipRow(
    clip: ClipEntity,
    backupEnabled: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnail by produceState<Bitmap?>(initialValue = null, clip.filePath) {
        value = withContext(Dispatchers.IO) { extractThumbnail(clip.filePath) }
    }
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LvSurface)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onPlay,
            )
            .pressScale(interaction)
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LvBg),
            contentAlignment = Alignment.Center,
        ) {
            thumbnail?.let { bitmap ->
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_player_play),
                contentDescription = stringResource(R.string.cd_play),
                tint = LvMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = stringResource(R.string.clip_name, clip.id),
            color = LvText,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 6.dp),
        )
        MonoText(
            text = formatTime(clip.durationSeconds),
            fontSize = 12.sp,
            color = LvMuted,
        )
        if (backupEnabled) {
            CloudStatus(clip = clip)
        }
        LvIconButton(
            iconRes = R.drawable.ic_trash,
            contentDescription = stringResource(R.string.cd_delete),
            onClick = onDelete,
            iconSize = 17.dp,
            touchSize = 28.dp,
        )
    }
}

@Composable
private fun CloudStatus(clip: ClipEntity) {
    val state = clip.stateEnum()
    val label = when (state) {
        UploadState.QUEUED -> stringResource(R.string.queued)
        UploadState.UPLOADING -> stringResource(R.string.uploading)
        UploadState.WAITING_FOR_WIFI -> stringResource(R.string.waiting_wifi_label)
        UploadState.PAUSED_STORAGE -> stringResource(R.string.paused_storage)
        UploadState.FAILED -> stringResource(R.string.upload_failed)
        else -> null
    }
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(
                if (state == UploadState.UPLOADED) R.drawable.ic_cloud_check
                else R.drawable.ic_cloud_off
            ),
            contentDescription = null,
            tint = LvMuted,
            modifier = Modifier.size(16.dp),
        )
        if (label != null) {
            Text(
                text = label,
                color = LvMuted,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyChecklist(selected: Set<String>, onToggle: (String) -> Unit) {
    val items = listOf(
        R.string.check_eye_contact,
        R.string.check_posture,
        R.string.check_hand_movement,
        R.string.check_facial_expression,
        R.string.check_filler_words,
        R.string.check_pace,
    ).map { stringResource(it) }

    LvCard {
        Text(
            text = stringResource(R.string.weekly_review_hint),
            color = LvMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { item ->
                LvChip(
                    text = item,
                    active = selected.contains(item),
                    onClick = { onToggle(item) },
                )
            }
        }
    }
}

@Composable
private fun StorageCard(
    state: SpeakingUiState,
    onToggleBackup: (Boolean) -> Unit,
    onModeSelected: (UploadNetworkMode) -> Unit,
    onCapClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    val settings = state.settings
    val capBytes = settings.monthlyMobileCapMb?.toLong()?.times(1024L * 1024L)
    val capReached = capBytes != null && state.mobileBytes >= capBytes
    val quota = state.quota
    val percent = quota?.percent ?: 0

    LvCard {            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.backup_to_drive),
                color = LvText,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            LvSwitch(
                checked = settings.backupEnabled,
                onCheckedChange = onToggleBackup,
            )
        }

        if (!settings.backupEnabled) {
            Text(
                text = stringResource(R.string.clips_local_only),
                color = LvMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            return@LvCard
        }

        Text(
            text = stringResource(R.string.upload_over),
            color = LvMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LvChip(
                text = stringResource(R.string.upload_wifi_only),
                active = settings.uploadNetworkMode == UploadNetworkMode.WIFI_ONLY,
                onClick = { onModeSelected(UploadNetworkMode.WIFI_ONLY) },
                fontSize = 11.sp,
            )
            LvChip(
                text = stringResource(R.string.upload_wifi_mobile),
                active = settings.uploadNetworkMode == UploadNetworkMode.WIFI_AND_MOBILE,
                onClick = { onModeSelected(UploadNetworkMode.WIFI_AND_MOBILE) },
                fontSize = 11.sp,
            )
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            LvChip(
                text = stringResource(R.string.upload_ask_mobile),
                active = settings.uploadNetworkMode == UploadNetworkMode.ASK_BEFORE_MOBILE,
                onClick = { onModeSelected(UploadNetworkMode.ASK_BEFORE_MOBILE) },
                fontSize = 11.sp,
            )
        }

        if (settings.uploadNetworkMode != UploadNetworkMode.WIFI_ONLY) {
            Text(
                text = stringResource(
                    R.string.mobile_used,
                    String.format(Locale.US, "%.1f", state.mobileBytes / (1024.0 * 1024.0)),
                ),
                color = LvMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .lvClickable(onClick = onCapClick)
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.monthly_cap),
                    color = LvMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                MonoText(
                    text = settings.monthlyMobileCapMb?.let {
                        stringResource(R.string.cap_value, it)
                    } ?: stringResource(R.string.no_cap),
                    fontSize = 12.sp,
                )
            }
            if (capReached) {
                Text(
                    text = stringResource(R.string.cap_reached),
                    color = LvAccent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            Text(
                text = stringResource(R.string.google_storage_used),
                color = LvMuted,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            MonoText(
                text = when {
                    quota == null -> "—"
                    quota.limitBytes == null -> stringResource(
                        R.string.storage_used_only,
                        formatGigabytes(quota.usageBytes),
                    )

                    else -> stringResource(
                        R.string.storage_of_limit,
                        formatGigabytes(quota.usageBytes),
                        formatGigabytes(quota.limitBytes),
                    )
                },
                fontSize = 12.sp,
            )
        }
        LvProgressBar(
            fraction = quota?.fraction ?: 0f,
            modifier = Modifier.padding(top = 5.dp),
            height = 5.dp,
        )
        Text(
            text = stringResource(
                when {
                    percent >= 90 -> R.string.storage_status_paused
                    percent >= 80 -> R.string.storage_status_warn
                    else -> R.string.storage_status_ok
                }
            ),
            color = LvMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (state.quotaLoading) {
            Text(
                text = stringResource(R.string.storage_loading),
                color = LvMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (state.quotaError) {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.storage_unknown),
                    color = LvMuted,
                    fontSize = 11.sp,
                )
                Text(
                    text = stringResource(R.string.refresh),
                    color = LvText,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .lvClickable(onClick = onRefresh)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerDialog(path: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoPath(path)
                        setOnPreparedListener { mediaPlayer ->
                            mediaPlayer.isLooping = false
                            start()
                        }
                        setOnErrorListener { _, _, _ -> true }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            LvIconButton(
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.close),
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                iconSize = 20.dp,
                tint = LvText,
            )
        }
    }
}

private fun normalizeAmplitude(db: Double): Float {
    if (db.isNaN() || db <= -60.0) return 0f
    return ((db + 60.0) / 60.0).toFloat().coerceIn(0f, 1f)
}

private fun extractThumbnail(path: String): Bitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    val bitmap = retriever.getFrameAtTime(500_000L)
    retriever.release()
    bitmap
}.getOrNull()

private fun formatTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", safe / 60, safe % 60)
}

private fun formatGigabytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 10.0) String.format(Locale.US, "%.0f", gb)
    else String.format(Locale.US, "%.1f", gb)
}
