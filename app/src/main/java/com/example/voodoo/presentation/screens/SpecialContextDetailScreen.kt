package com.example.voodoo.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialContextDetailScreen(
    label: String,
    emoji: String,
    groupEnabled: Boolean,
    onBackClick: () -> Unit,
    onToggleGroup: (Boolean) -> Unit,
    onStatsClick: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$emoji $label") },
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Группировать по проектам", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = groupEnabled,
                        onCheckedChange = onToggleGroup
                    )
                }
            }

            if (onStatsClick != null) {
                Button(
                    onClick = onStatsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Статистика рутины")
                }
            }
        }
    }
}