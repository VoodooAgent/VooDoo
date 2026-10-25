package com.example.voodoo.presentation

import com.example.voodoo.data.Task
import com.example.voodoo.presentation.components.describeDeletion
import com.example.voodoo.presentation.components.formatTrackedTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteTaskDialogTextTest {

    private val task = Task(id = 1, title = "Задача")

    @Test
    fun formatTrackedTime_formatsHoursAndMinutes() {
        assertEquals("меньше минуты", formatTrackedTime(30_000))
        assertEquals("5 мин", formatTrackedTime(5 * 60_000L))
        assertEquals("2 ч", formatTrackedTime(2 * 3_600_000L))
        assertEquals("1 ч 30 мин", formatTrackedTime(90 * 60_000L))
    }

    @Test
    fun describeDeletion_plainTask_onlyWarnsItCannotBeUndone() {
        val text = describeDeletion(PendingDeletion(task, 0, 0, 0, 0))
        assertEquals("Отменить удаление нельзя.", text)
    }

    @Test
    fun describeDeletion_mentionsSubtasksTimeAndRunningTimer() {
        val text = describeDeletion(PendingDeletion(task, descendantCount = 3, sessionCount = 4,
            trackedMillis = 90 * 60_000L, activeTimerCount = 1))
        assertTrue(text.contains("подзадачи: 3"))
        assertTrue(text.contains("1 ч 30 мин"))
        assertTrue(text.contains("сессий: 4"))
        assertTrue(text.contains("таймер"))
    }

    @Test
    fun describeDeletion_withoutSessions_doesNotMentionTime() {
        val text = describeDeletion(PendingDeletion(task, 2, 0, 0, 0))
        assertFalse(text.contains("время"))
    }
}
