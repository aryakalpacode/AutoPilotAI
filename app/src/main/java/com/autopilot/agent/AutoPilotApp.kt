package com.autopilot.agent

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.autopilot.agent.util.Constants
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for AutoPilot AI.
 * Initializes Hilt dependency injection and notification channels.
 */
@HiltAndroidApp
class AutoPilotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val agentChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }

        val reminderChannel = NotificationChannel(
            "reminders",
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminder notifications from AutoPilot AI"
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(agentChannel)
        nm.createNotificationChannel(reminderChannel)
    }
}
