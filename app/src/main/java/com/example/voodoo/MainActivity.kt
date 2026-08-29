package com.example.voodoo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voodoo.presentation.MainViewModel
import com.example.voodoo.presentation.screens.*
import com.example.voodoo.ui.theme.VooDooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val isDarkTheme by mainViewModel.isDarkTheme.collectAsState()

            VooDooTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VooDooNavHost()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun VooDooNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onContextClick = { contextId ->
                    if (contextId != null) {
                        navController.navigate("context/$contextId")
                    } else {
                        navController.navigate("context_no")
                    }
                },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable(
            route = "context/{contextId}",
            arguments = listOf(navArgument("contextId") { type = NavType.LongType })
        ) { backStackEntry ->
            val contextId = backStackEntry.arguments?.getLong("contextId")
            TaskListScreen(
                contextId = contextId,
                contextName = "Контекст",
                onBackClick = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task/$taskId") },
                onPriorityClick = { navController.navigate("priority") }
            )
        }

        composable("context_no") {
            TaskListScreen(
                contextId = null,
                contextName = "Без контекста",
                onBackClick = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task/$taskId") },
                onPriorityClick = { navController.navigate("priority") }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onContextsClick = { navController.navigate("contexts") },
                onICalSyncClick = { navController.navigate("ical_sync") },
                onExportClick = { /* TODO */ },
                onImportClick = { /* TODO */ }
            )
        }

        composable("contexts") {
            ContextsManagementScreen(onBackClick = { navController.popBackStack() })
        }

        composable(
            route = "task/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: return@composable
            TaskDetailScreen(
                taskId = taskId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("priority") {
            PriorityScreen(
                onBackClick = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task/$taskId") }
            )
        }

        composable("ical_sync") {
            ICalSyncScreen(onBackClick = { navController.popBackStack() })
        }
    }
}