package com.levelup.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.levelup.app.ui.Tab
import com.levelup.app.ui.LevelUpRoot
import com.levelup.app.ui.theme.LevelUpTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_TAB = "tab"
    }

    private val selectedTab = MutableStateFlow(Tab.TODAY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        selectedTab.value = tabFrom(intent)
        setContent {
            val tab by selectedTab.collectAsState()
            LevelUpTheme {
                LevelUpRoot(
                    selectedTab = tab,
                    onTabSelected = { selectedTab.value = it },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedTab.value = tabFrom(intent)
    }

    override fun onResume() {
        super.onResume()
        val container = (application as LevelUpApp).container
        container.repo.refreshToday()
        container.appScope.launch {
            container.repo.ensureDay(LocalDate.now())
        }
    }

    private fun tabFrom(intent: Intent?): Tab =
        when (intent?.getStringExtra(EXTRA_TAB)?.lowercase()) {
            "roadmap" -> Tab.ROADMAP
            "deadlines" -> Tab.DEADLINES
            "speaking" -> Tab.SPEAKING
            else -> Tab.TODAY
        }
}
