package com.example.voodoo.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.voodoo.data.Task

object ReminderScheduler {

    private const val ACTION_REMINDER = "com.example.voodoo.REMINDER"

    fun scheduleReminder(context: Context, task: Task) {
        if (task.plannedStart == null || task.reminderMinutesBefore == null) return

        val reminderTime = task.plannedStart - (task.reminderMinutesBefore * 60 * 1000L)
        val now = System.currentTimeMillis()

        if (reminderTime <= now) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
        }

        val requestCode = (task.id % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }

        val requestCode = (taskId % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}