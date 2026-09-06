package com.example.voodoo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SwipeConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var confirmOffsetX by remember { mutableFloatStateOf(0f) }
    var cancelOffsetX by remember { mutableFloatStateOf(0f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Подтвердите выполнение",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Вместе с родительской задачей будут отмечены как выполненные все её активные подзадачи.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Верхняя полоска: Выполнено (свайп вправо)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF4CAF50), shape = MaterialTheme.shapes.medium)
                        .offset(x = confirmOffsetX.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (confirmOffsetX > 150) {
                                        onConfirm()
                                    }
                                    confirmOffsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    confirmOffsetX += dragAmount
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Выполнено (свайп вправо)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                // Нижняя полоска: Отмена (свайп влево)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.medium)
                        .offset(x = cancelOffsetX.dp)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (cancelOffsetX < -150) {
                                        onCancel()
                                    }
                                    cancelOffsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    cancelOffsetX += dragAmount
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Отмена (свайп влево)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}