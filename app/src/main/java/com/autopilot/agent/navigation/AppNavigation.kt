package com.autopilot.agent.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.autopilot.agent.R
import com.autopilot.agent.data.repository.AgentRepository
import com.autopilot.agent.data.repository.ModelRepository
import com.autopilot.agent.ui.screens.chat.ChatScreen
import com.autopilot.agent.ui.screens.home.HomeScreen
import com.autopilot.agent.ui.screens.notes.NotesScreen
import com.autopilot.agent.ui.screens.settings.SettingsScreen
import com.autopilot.agent.ui.screens.setup.SetupScreen
import com.autopilot.agent.ui.screens.splash.SplashScreen
import com.autopilot.agent.ui.screens.tasks.TasksScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val SPLASH = "splash"
    const val SETUP = "setup"
    const val HOME = "home"
    const val CHAT = "chat/{conversationId}?prompt={prompt}"
    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val SETTINGS = "settings"

    fun chat(conversationId: Long, prompt: String? = null): String {
        val base = "chat/$conversationId"
        return if (prompt != null) "$base?prompt=${java.net.URLEncoder.encode(prompt, "UTF-8")}"
        else base
    }
}

/**
 * Main navigation composable with bottom navigation bar.
 */
@Composable
fun AppNavigation(
    agentRepository: AgentRepository,
    modelRepository: ModelRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(Routes.HOME, Routes.TASKS, Routes.NOTES)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.HOME,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.TASKS,
                        onClick = {
                            navController.navigate(Routes.TASKS) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.TaskAlt, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_tasks)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.NOTES,
                        onClick = {
                            navController.navigate(Routes.NOTES) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Note, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_notes)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.SETTINGS,
                        onClick = {
                            navController.navigate(Routes.SETTINGS) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    agentRepository = agentRepository,
                    modelRepository = modelRepository,
                    onNavigateToSetup = {
                        navController.navigate(Routes.SETUP) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.SETUP) {
                SetupScreen(
                    onSetupComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SETUP) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToChat = { conversationId, prompt ->
                        navController.navigate(Routes.chat(conversationId, prompt))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }

            composable(
                route = Routes.CHAT,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.LongType },
                    navArgument("prompt") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
                val prompt = backStackEntry.arguments?.getString("prompt")?.let {
                    try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
                }
                ChatScreen(
                    conversationId = conversationId,
                    initialPrompt = prompt,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.TASKS) {
                TasksScreen(
                    onNavigateToChat = { conversationId ->
                        navController.navigate(Routes.chat(conversationId))
                    }
                )
            }

            composable(Routes.NOTES) {
                NotesScreen()
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
