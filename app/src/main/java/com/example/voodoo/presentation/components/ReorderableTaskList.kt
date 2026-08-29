package com.example.voodoo.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.voodoo.data.Task
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableTaskList(
    tasks: List<Task>,
    onReorder: (from: Int, to: Int) -> Unit,
    taskContent: @Composable (Task, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
            ReorderableItem(
                state = reorderableState,
                key = task.id
            ) { isDragging ->
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 16.dp else 2.dp,
                    label = "elevation"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            if (isDragging) {
                                shadowElevation = 24f
                                scaleX = 1.02f
                                scaleY = 1.02f
                            }
                        }
                        .zIndex(if (isDragging) 1f else 0f),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                ) {
                    taskContent(task, index, isDragging)
                }
            }
        }
    }
}