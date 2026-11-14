package com.levelup.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.levelup.app.LevelUpApp
import com.levelup.app.R
import com.levelup.app.ui.components.AppDialog
import com.levelup.app.ui.components.AppText
import com.levelup.app.ui.components.BottomTabBar
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.deadlines.DeadlinesScreen
import com.levelup.app.ui.roadmap.RoadmapScreen
import com.levelup.app.ui.settings.SettingsScreen
import com.levelup.app.ui.speaking.SpeakingScreen
import com.levelup.app.ui.theme.C
import com.levelup.app.ui.theme.T
import com.levelup.app.ui.today.TodayScreen
import com.levelup.app.widget.Widgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun LevelUpRoot(selectedTab: Tab, onTabSelected: (Tab) -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as LevelUpApp).container
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        val asked = container.settings.current().notificationsAsked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED &&
            !asked
        ) {
            showNotificationDialog = true
            container.settings.setNotificationsAsked()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            if (container.repo.todayFlow.value != LocalDate.now()) {
                container.repo.refreshToday()
                container.appScope.launch {
                    container.repo.ensureDay(LocalDate.now())
                    Widgets.refreshAll(context)
                }
            }
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            val pixelSlide = with(LocalDensity.current) { 8.dp.roundToPx() }
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = if (showSettings) null else selectedTab,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically(
                            animationSpec = tween(200),
                            initialOffsetY = { pixelSlide },
                        )) togetherWith (fadeOut(tween(200)) + slideOutVertically(
                            animationSpec = tween(200),
                            targetOffsetY = { -pixelSlide },
                        ))
                    },
                    label = "tabTransition",
                ) { target ->
                    when (target) {
                        null -> SettingsScreen(onBack = { showSettings = false })
                        Tab.TODAY -> TodayScreen(onOpenSettings = { showSettings = true })
                        Tab.ROADMAP -> RoadmapScreen()
                        Tab.DEADLINES -> DeadlinesScreen()
                        Tab.SPEAKING -> SpeakingScreen()
                    }
                }
            }
            if (!showSettings) {
                BottomTabBar(selected = selectedTab, onSelected = onTabSelected)
            }
        }
    }

    if (showNotificationDialog) {
        AppDialog(
            onDismiss = { showNotificationDialog = false },
            title = stringResource(R.string.notifications_title),
        ) {
            AppText(
                text = stringResource(R.string.notifications_message),
                style = T.Body13Muted,
            )
            DialogActions(
                confirmLabel = stringResource(R.string.allow),
                onConfirm = {
                    showNotificationDialog = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onDismiss = { showNotificationDialog = false },
            )
        }
    }
}
