package com.autopilot.agent.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.autopilot.agent.data.repository.AgentRepository
import com.autopilot.agent.data.repository.ModelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

/**
 * Splash screen with animated logo.
 * Routes to Setup or Home based on whether API key is configured.
 */
@Composable
fun SplashScreen(
    agentRepository: AgentRepository,
    modelRepository: ModelRepository,
    onNavigateToSetup: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate in
        scale.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(1000))
    }

    LaunchedEffect(Unit) {
        delay(1500) // Show splash for 1.5 seconds
        val isSetupComplete = agentRepository.isSetupComplete.firstOrNull() ?: false
        val hasKey = modelRepository.hasApiKey()
        if (isSetupComplete && hasKey) {
            onNavigateToHome()
        } else {
            onNavigateToSetup()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AutoPilot AI",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your Autonomous AI Agent",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
