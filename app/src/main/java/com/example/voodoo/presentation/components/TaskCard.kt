package com.example.voodoo.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.example.voodoo.data.Task
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCard(
    task: Task,
    pastSessionsDuration: Long = 0L,
    fontSize: Int = 16,
    onToggleDone: () -> Unit,
    onCyclePriority: () -> Unit,
    onToggleTimer: () -> Unit,
    onClick: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var currentTimerDuration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(task.timerActive, task.timerStartedAt, pastSessionsDuration) {
        if (task.timerActive && task.timerStartedAt != null) {
            while (true) {
                currentTimerDuration = (System.currentTimeMillis() - task.timerStartedAt).coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            currentTimerDuration = pastSessionsDuration
        }
    }

    val fontScale = fontSize / 16f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(task.id, task.isDone) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            offsetX > 150 -> {
                                onSwipeRight()
                                offsetX = 0f
                            }
                            offsetX < -150 -> {
                                onSwipeLeft()
                                offsetX = 0f
                            }
                            else -> offsetX = 0f
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    }
                )
            }
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PriorityIndicator(
                priority = task.priority,
                onCycle = onCyclePriority,
                iconSize = (14 * fontScale).dp,
                modifier = Modifier.padding(end = 4.dp)
            )

            Text(
                text = task.title,
                fontSize = TextUnit(15 * fontScale, TextUnitType.Sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (task.isDone) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (task.isDone) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                },
                modifier = Modifier.weight(1f)
            )

            TimerButton(
                isActive = task.timerActive,
                currentDuration = currentTimerDuration,
                fontSize = fontSize,
                onToggle = onToggleTimer,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Вертикальный индикатор приоритета.
 *
 * Состояния (сверху вниз):
 *  0 — все три звезды серые
 *  1 — нижняя звезда закрашена
 *  2 — нижняя и средняя закрашены
 *  3 — все три закрашены
 *  4 — вместо звёзд буква "R"
 */
@Composable
fun PriorityIndicator(
    priority: Int,
    onCycle: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val filledTint = Color(0xFFFFD700)
    val emptyTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

    Column(
        modifier = modifier.clickable(onClick = onCycle),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (priority == 4) {
            // Состояние 4: буква "R" вместо звёзд
            Text(
                text = "R",
                color = MaterialTheme.colorScheme.primary,
                fontSize = TextUnit(iconSize.value * 1.4f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold,
                lineHeight = TextUnit(iconSize.value * 1.4f, TextUnitType.Sp)
            )
        } else {
            // Состояния 0–3: три звезды, заполняются снизу вверх

            // Верхняя звезда (заполнена только при приоритете 3)
            Icon(
                imageVector = if (priority >= 3) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (priority >= 3) filledTint else emptyTint,
                modifier = Modifier.size(iconSize)
            )

            // Средняя звезда (заполнена при приоритете 2 и 3)
            Icon(
                imageVector = if (priority >= 2) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (priority >= 2) filledTint else emptyTint,
                modifier = Modifier.size(iconSize)
            )

            // Нижняя звезда (заполнена при приоритете 1, 2 и 3)
            Icon(
                imageVector = if (priority >= 1) Icons.Default.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (priority >= 1) filledTint else emptyTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun TimerButton(
    isActive: Boolean,
    currentDuration: Long,
    fontSize: Int = 16,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = currentDuration / (1000 * 60 * 60)
    val minutes = (currentDuration % (1000 * 60 * 60)) / (1000 * 60)
    val fontScale = fontSize / 16f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size((24 * fontScale).dp)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isActive) "Пауза" else "Старт",
                tint = if (isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
        Text(
            text = String.format("%02d:%02d", hours, minutes),
            fontSize = TextUnit(9 * fontScale, TextUnitType.Sp),
            color = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )
    }
}