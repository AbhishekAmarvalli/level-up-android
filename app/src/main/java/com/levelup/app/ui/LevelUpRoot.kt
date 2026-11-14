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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.levelup.app.LevelUpApp
import com.levelup.app.R
import com.levelup.app.ui.components.DialogActions
import com.levelup.app.ui.components.LvDialog
import com.levelup.app.ui.components.pressScale
import com.levelup.app.ui.deadlines.DeadlinesScreen
import com.levelup.app.ui.roadmap.RoadmapScreen
import com.levelup.app.ui.settings.SettingsScreen
import com.levelup.app.ui.speaking.SpeakingScreen
import com.levelup.app.ui.theme.LvAccent
import com.levelup.app.ui.theme.LvBg
import com.levelup.app.ui.theme.LvBorder
import com.levelup.app.ui.theme.LvMuted
import com.levelup.app.ui.theme.LvText
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

    // Keeps the app on the right date if it stays open past midnight.
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LvBg,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        bottomBar = {
            if (!showSettings) {
                BottomTabBar(selected = selectedTab, onSelected = onTabSelected)
            }
        },
    ) { padding ->
        val density = LocalDensity.current
        val pixelSlide = with(density) { 8.dp.roundToPx() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = if (showSettings) null else selectedTab,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(
                        animationSpec = tween(200),
                        initialOffsetY = { pixelSlide },
                    )) togetherWith (fadeOut(tween(150)) + slideOutVertically(
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
    }

    if (showNotificationDialog) {
        LvDialog(
            onDismiss = { showNotificationDialog = false },
            title = stringResource(R.string.notifications_title),
        ) {
            Text(
                text = stringResource(R.string.notifications_message),
                color = LvMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
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

@Composable
private fun BottomTabBar(selected: Tab, onSelected: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LvBg)
            .drawBehind {
                drawLine(
                    color = LvBorder,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 12.dp),
    ) {
        val haptic = LocalHapticFeedback.current
        Tab.entries.forEach { tab ->
            val interaction = remember(tab) { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = tab == selected,
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Tab,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelected(tab)
                        },
                    )
                    .pressScale(interaction),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(tab.iconRes),
                    contentDescription = null,
                    tint = if (tab == selected) LvAccent else LvMuted,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(tab.labelRes),
                    color = if (tab == selected) LvAccent else LvMuted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
