package com.example.voodoo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.voodoo.presentation.MainViewModel
import com.example.voodoo.presentation.screens.*
import com.example.voodoo.ui.theme.VooDooTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    VooDooNavHost(mainViewModel)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun VooDooNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    val exportResult by viewModel.exportResult.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val context = LocalContext.current

    // Лаунчер для экспорта CSV
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportData(it)
        }
    }

    // Лаунчер для импорта CSV
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importData(it)
        }
    }

    // Обработка результата экспорта
    LaunchedEffect(exportResult) {
        exportResult?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearExportResult()
        }
    }

    // Обработка результата импорта
    LaunchedEffect(importResult) {
        importResult?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearImportResult()
        }
    }

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
                onPriorityClick = { navController.navigate("priority") },
                onRoutineClick = { navController.navigate("routine") }
            )
        }
        composable("context_no") {
            TaskListScreen(
                contextId = null,
                contextName = "Без контекста",
                onBackClick = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task/$taskId") },
                onPriorityClick = { navController.navigate("priority") },
                onRoutineClick = { navController.navigate("routine") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onContextsClick = { navController.navigate("contexts") },
                onICalSyncClick = { navController.navigate("ical_sync") },
                onExportClick = {
                    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val fileName = "voodoo_export_${dateFormat.format(Date())}.csv"
                    exportLauncher.launch(fileName)
                },
                onImportClick = {
                    importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                }
            )
        }
        composable("contexts") {
            ContextsManagementScreen(
                onBackClick = { navController.popBackStack() },
                onRoutineClick = { navController.navigate("routine") }
            )
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
        composable("routine") {
            RoutineScreen(
                onBackClick = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task/$taskId") }
            )
        }
        composable("ical_sync") {
            ICalSyncScreen(onBackClick = { navController.popBackStack() })
        }
    }
}