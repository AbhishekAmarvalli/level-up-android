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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.levelup.app.ui.components.AppText
import com.levelup.app.ui.components.Card
import com.levelup.app.ui.components.Chip
import com.levelup.app.ui.components.ConfirmDialog
import com.levelup.app.ui.components.HtmlSwitch
import com.levelup.app.ui.components.ProgressBar
import com.levelup.app.ui.components.RedCheckbox
import com.levelup.app.ui.components.SectionLabel
import com.levelup.app.ui.components.TiIcon
import com.levelup.app.ui.components.appClickable
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val MAX_SECONDS = 300
private const val WAVE_BARS = 30

@Composable
fun SpeakingScreen(vm: SpeakingViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val recorder = remember { CameraRecorder(context) }
    val hasPermissions = rememberCameraPermissions()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    var recording by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableIntStateOf(0) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var playerPath by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<ClipEntity?>(null) }
    var deleteOnDrive by remember { mutableStateOf(false) }

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

    fun requestCamera() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )
        )
    }

    fun startRecording() {
        if (!hasPermissions) {
            requestCamera()
            return
        }
        val file = vm.newClipFile()
        amplitude = 0f
        elapsed = 0
        val started = recorder.start(
            file = file,
            onStarted = {
                startedAt = it
                recording = true
            },
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

    val current = state
    Box(modifier = Modifier.fillMaxSize()) {
        if (current != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppText(text = stringResource(R.string.speaking), style = T.Title16)
                }

                Card {
                    AppText(text = stringResource(R.string.todays_prompt), style = T.Label11)
                    AppText(
                        text = current.prompt,
                        style = T.Body14,
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
                    )
                    AppText(
                        text = formatTime(if (recording) MAX_SECONDS - elapsed else MAX_SECONDS),
                        style = T.Big26Red,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(C.Surface),
                ) {
                    if (hasPermissions) {
                        CameraPreview(
                            recorder = recorder,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .appClickable(onClick = ::requestCamera),
                            contentAlignment = Alignment.Center,
                        ) {
                            TiIcon(
                                iconRes = R.drawable.ti_user,
                                size = 64.dp,
                                tint = C.Border,
                                contentDescription = stringResource(R.string.enable_camera),
                            )
                        }
                    }
                    if (recording) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(C.Red),
                            )
                            Spacer(Modifier.width(5.dp))
                            AppText(text = stringResource(R.string.rec), style = T.Mono12)
                        }
                    }
                }

                val pulse = rememberInfiniteTransition(label = "recordPulse")
                val ringScale by pulse.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                    label = "ringScale",
                )
                val ringAlpha by pulse.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
                    label = "ringAlpha",
                )
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
                                .border(1.5.dp, C.Red, CircleShape),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(C.Surface)
                            .border(1.5.dp, C.Border, CircleShape)
                            .appClickable(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (recording) recorder.stop() else startRecording()
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        TiIcon(
                            iconRes = if (recording) {
                                R.drawable.ti_player_stop
                            } else {
                                R.drawable.ti_video
                            },
                            size = 32.dp,
                            tint = C.Red,
                            contentDescription = stringResource(
                                if (recording) R.string.cd_stop else R.string.cd_record
                            ),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        3.dp,
                        Alignment.CenterHorizontally,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(WAVE_BARS) {
                        val target = if (recording) {
                            (6f + amplitude.coerceIn(0f, 1f) * 30f).dp
                        } else {
                            6.dp
                        }
                        val barHeight by animateDpAsState(
                            targetValue = target,
                            animationSpec = tween(100),
                            label = "waveBar",
                        )
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (recording) C.Text else C.Muted),
                        )
                    }
                }

                AppText(
                    text = stringResource(
                        if (recording) R.string.recording_hint else R.string.record_hint
                    ),
                    style = T.Body12Muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                )

                SectionLabel(stringResource(R.string.saved_clips))
                if (current.clips.isEmpty()) {
                    AppText(text = stringResource(R.string.no_clips), style = T.Body12Muted)
                } else {
                    current.clips.forEach { clip ->
                        ClipRow(
                            clip = clip,
                            backupEnabled = current.settings.backupEnabled,
                            onPlay = {
                                if (File(clip.filePath).exists()) {
                                    playerPath = clip.filePath
                                } else {
                                    Toast.makeText(context, R.string.clip_missing, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            },
                            onDelete = {
                                deleteTarget = clip
                                deleteOnDrive =
                                    current.settings.backupEnabled && clip.driveFileId != null
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
                    onRefresh = vm::refreshStorage,
                )

                SectionLabel(stringResource(R.string.weekly_review))
                WeeklyChecklist(
                    selected = current.settings.checklistItems,
                    onToggle = vm::toggleChecklist,
                )
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        RedCheckbox(
                            done = deleteOnDrive,
                            size = 18.dp,
                            onToggle = { deleteOnDrive = !deleteOnDrive },
                        )
                        AppText(
                            text = stringResource(R.string.delete_also_drive),
                            style = T.Body12Muted,
                        )
                    }
                }
            } else {
                null
            },
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
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = permissions.all {
                    ContextCompat.checkSelfPermission(context, it) ==
                        PackageManager.PERMISSION_GRANTED
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
) {
    val thumbnail by produceState<Bitmap?>(initialValue = null, clip.filePath) {
        value = withContext(Dispatchers.IO) { extractThumbnail(clip.filePath) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .appClickable(onClick = onPlay)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(C.Bg),
            contentAlignment = Alignment.Center,
        ) {
            thumbnail?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            TiIcon(
                iconRes = R.drawable.ti_player_play,
                size = 18.dp,
                tint = C.Muted,
                contentDescription = stringResource(R.string.cd_play),
            )
        }
        AppText(
            text = stringResource(R.string.clip_name, clip.id),
            style = T.Body13,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AppText(text = formatTime(clip.durationSeconds), style = T.Mono12Muted)
        if (backupEnabled) {
            TiIcon(
                iconRes = if (clip.stateEnum() == UploadState.UPLOADED) {
                    R.drawable.ti_cloud_check
                } else {
                    R.drawable.ti_cloud_off
                },
                size = 16.dp,
                tint = C.Muted,
            )
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .appClickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            TiIcon(
                iconRes = R.drawable.ti_trash,
                size = 17.dp,
                tint = C.Muted,
                contentDescription = stringResource(R.string.cd_delete),
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

    Card {
        AppText(
            text = stringResource(R.string.weekly_review_hint),
            style = T.Body12Muted,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { item ->
                Chip(
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
    onRefresh: () -> Unit,
) {
    val settings = state.settings
    val quota = state.quota
    val percent = quota?.percent ?: 0

    Card {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(text = stringResource(R.string.backup_to_drive), style = T.Body13)
            HtmlSwitch(
                on = settings.backupEnabled,
                onCheckedChange = onToggleBackup,
            )
        }

        if (!settings.backupEnabled) {
            AppText(
                text = stringResource(R.string.clips_local_only),
                style = T.Body12Muted,
                modifier = Modifier.padding(top = 8.dp),
            )
            return@Card
        }

        AppText(
            text = stringResource(R.string.upload_over),
            style = T.Body13,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Chip(
                text = stringResource(R.string.upload_wifi_only),
                active = settings.uploadNetworkMode == UploadNetworkMode.WIFI_ONLY,
                onClick = { onModeSelected(UploadNetworkMode.WIFI_ONLY) },
            )
            Chip(
                text = stringResource(R.string.upload_wifi_mobile),
                active = settings.uploadNetworkMode == UploadNetworkMode.WIFI_AND_MOBILE,
                onClick = { onModeSelected(UploadNetworkMode.WIFI_AND_MOBILE) },
            )
            Chip(
                text = stringResource(R.string.upload_ask_mobile),
                active = settings.uploadNetworkMode == UploadNetworkMode.ASK_BEFORE_MOBILE,
                onClick = { onModeSelected(UploadNetworkMode.ASK_BEFORE_MOBILE) },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .appClickable(onClick = onRefresh),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AppText(
                text = stringResource(R.string.google_storage_used),
                style = T.Body12Muted,
            )
            AppText(
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
                style = T.Mono12,
            )
        }
        ProgressBar(
            fraction = quota?.fraction ?: 0f,
            height = 5.dp,
            modifier = Modifier.padding(top = 5.dp),
        )
        AppText(
            text = stringResource(
                when {
                    percent >= 90 -> R.string.storage_status_paused
                    percent >= 80 -> R.string.storage_status_warn
                    else -> R.string.storage_status_ok
                }
            ),
            style = T.Body12Muted,
            modifier = Modifier.padding(top = 8.dp),
        )
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
                .background(C.Bg),
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
            AppText(
                text = stringResource(R.string.close),
                style = T.Body13,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .appClickable(onClick = onDismiss)
                    .padding(4.dp),
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
