package com.autopilot.agent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.autopilot.agent.data.repository.AgentRepository
import com.autopilot.agent.data.repository.ModelRepository
import com.autopilot.agent.navigation.AppNavigation
import com.autopilot.agent.ui.theme.AutoPilotAITheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for AutoPilot AI.
 * Sets up Compose UI with theming and navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var agentRepository: AgentRepository

    @Inject
    lateinit var modelRepository: ModelRepository

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val theme by agentRepository.theme.collectAsState(initial = "system")

            AutoPilotAITheme(themeMode = theme) {
                AppNavigation(
                    agentRepository = agentRepository,
                    modelRepository = modelRepository
                )
            }
        }
    }
}
