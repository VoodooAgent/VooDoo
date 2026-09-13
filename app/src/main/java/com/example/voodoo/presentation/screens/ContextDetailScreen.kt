package com.example.voodoo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.data.ProjectContext
import com.example.voodoo.presentation.ContextListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextDetailScreen(
    contextId: Long?,
    contextName: String,
    onBackClick: () -> Unit,
    onDeadlineListClick: () -> Unit,
    contextViewModel: ContextListViewModel = viewModel(),
) {
    val contexts by contextViewModel.contexts.collectAsState()
    val context = if (contextId != null) contexts.find { it.id == contextId } else null
    val displayName = context?.name ?: contextName
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(displayName)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onDeadlineListClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Задачи по дедлайну")
            }
        }
    }

    if (showEditDialog) {
        ContextEditDialog(
            context = context,
            onDismiss = { showEditDialog = false },
            onSave = { name, color ->
                context?.let { ctx ->
                    contextViewModel.updateContext(ctx.copy(name = name, color = color))
                }
                showEditDialog = false
            }
        )
    }
}