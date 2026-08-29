package com.example.voodoo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.voodoo.data.AppDatabase
import com.example.voodoo.service.TimerServiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VooDooApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannels()

        applicationScope.launch {
            TimerServiceManager.restoreTimers(this@VooDooApp)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о запланированных задачах"
            }
            notificationManager.createNotificationChannel(reminderChannel)

            val timerChannel = NotificationChannel(
                CHANNEL_ID_TIMERS,
                "Работающие таймеры",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о работающих таймерах задач"
            }
            notificationManager.createNotificationChannel(timerChannel)
        }
    }

    companion object {
        lateinit var instance: VooDooApp
            private set

        const val CHANNEL_ID_REMINDERS = "voodoo_reminders"
        const val CHANNEL_ID_TIMERS = "voodoo_timers"
    }
}