package com.example.voodoo.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voodoo.presentation.ContextListViewModel
import com.example.voodoo.presentation.MainViewModel

val CONTEXT_COLORS = listOf(
    0xFFEF5350L, 0xFFEC407AL, 0xFFAB47BCL, 0xFF7E57C2L, 0xFF5C6BC0L,
    0xFF42A5F5L, 0xFF29B6F6L, 0xFF26C6DAL, 0xFF26A69AL, 0xFF66BB6AL,
    0xFF9CCC65L, 0xFFD4E157L, 0xFFFFEE58L, 0xFFFFCA28L, 0xFFFFA726L,
    0xFFFF7043L, 0xFF8D6E63L, 0xFF78909CL, 0xFFE57373L, 0xFFF06292L,
    0xFFBA68C8L, 0xFF9575CDL, 0xFF7986CBL, 0xFF64B5F6L, 0xFF4FC3F7L,
    0xFF4DD0E1L, 0xFF4DB6ACL, 0xFF81C784L, 0xFFAED581L, 0xFFFFB74DL
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onContextClick: (Long?) -> Unit,
    onSettingsClick: () -> Unit,
    contextViewModel: ContextListViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val contexts by contextViewModel.contexts.collectAsState()
    val settings by mainViewModel.settings.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "VooDoo",
                        modifier = Modifier.clickable { onSettingsClick() },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить контекст")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ContextCard(
                    name = settings.noContextName,
                    color = Color.Gray,
                    onClick = { onContextClick(null) }
                )
            }
            items(contexts) { context ->
                ContextCard(
                    name = context.name,
                    color = Color(context.color),
                    onClick = { onContextClick(context.id) }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateContextDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, color ->
                contextViewModel.createContext(name, color)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun ContextCard(
    name: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun CreateContextDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(CONTEXT_COLORS[5]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый контекст") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Цвет фона экрана контекста:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CONTEXT_COLORS.chunked(6).forEach { colorRow ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            colorRow.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(color), MaterialTheme.shapes.small)
                                        .clickable { selectedColor = color }
                                        .then(
                                            if (selectedColor == color) {
                                                Modifier.background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}