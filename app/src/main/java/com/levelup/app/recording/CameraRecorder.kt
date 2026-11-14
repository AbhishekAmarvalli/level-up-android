package com.levelup.app.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

/**
 * Thin CameraX wrapper: front camera preview plus 720p video with audio.
 * VideoRecordEvent.Status carries the real microphone amplitude used by the waveform.
 */
class CameraRecorder(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    fun bind(lifecycleOwner: LifecycleOwner, previewView: PreviewView, onError: (String) -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.HD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD),
                        )
                    )
                    .build()
                val capture = VideoCapture.withOutput(recorder)
                videoCapture = capture
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    capture,
                )
            } catch (error: Throwable) {
                onError(error.message ?: "Camera unavailable")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun start(
        file: File,
        onStarted: (Long) -> Unit,
        onAmplitude: (Double) -> Unit,
        onFinished: (Int) -> Unit,
        onError: (String) -> Unit,
    ): Boolean {
        val capture = videoCapture
        if (capture == null) {
            onError("Camera is not ready")
            return false
        }
        return try {
            val options = FileOutputOptions.Builder(file).build()
            var pending = capture.output.prepareRecording(context, options)
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                pending = pending.withAudioEnabled()
            }
            recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onStarted(SystemClock.elapsedRealtime())
                    is VideoRecordEvent.Status -> {
                        onAmplitude(event.recordingStats.audioStats.audioAmplitude)
                    }

                    is VideoRecordEvent.Finalize -> {
                        val seconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000L)
                            .toInt()
                            .coerceAtLeast(1)
                        if (event.hasError()) {
                            onError("Recording failed (${event.error})")
                        } else {
                            onFinished(seconds)
                        }
                    }

                    else -> Unit
                }
            }
            true
        } catch (error: Throwable) {
            onError(error.message ?: "Could not start recording")
            false
        }
    }

    fun stop() {
        runCatching { recording?.stop() }
        recording = null
    }

    fun release() {
        stop()
        runCatching { provider?.unbindAll() }
        provider = null
        videoCapture = null
    }
}
