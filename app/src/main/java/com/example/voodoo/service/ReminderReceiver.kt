package com.example.voodoo.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.voodoo.MainActivity
import com.example.voodoo.VooDooApp

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1)
        val taskTitle = intent.getStringExtra("task_title") ?: "Задача"

        if (taskId == -1L) return

        showNotification(context, taskId, taskTitle)
    }

    private fun showNotification(context: Context, taskId: Long, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_task_id", taskId)
        }

        val requestCode = (taskId % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VooDooApp.CHANNEL_ID_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Напоминание: $title")
            .setContentText("Время начать работу над задачей")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (taskId % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notification)
    }
}