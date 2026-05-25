package com.autopilot.agent.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.autopilot.agent.MainActivity
import com.autopilot.agent.R
import com.autopilot.agent.util.Constants
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service for running agent tasks in the background.
 * Shows a persistent notification with task progress.
 */
@AndroidEntryPoint
class AgentForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.autopilot.agent.ACTION_START"
        const val ACTION_STOP = "com.autopilot.agent.ACTION_STOP"
        const val ACTION_PAUSE = "com.autopilot.agent.ACTION_PAUSE"
        const val ACTION_CANCEL = "com.autopilot.agent.ACTION_CANCEL"
        const val EXTRA_TASK_NAME = "task_name"
        const val EXTRA_TASK_STEP = "task_step"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val taskName = intent.getStringExtra(EXTRA_TASK_NAME) ?: "Running task"
                val notification = buildNotification(taskName, "Starting...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        Constants.FOREGROUND_SERVICE_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(Constants.FOREGROUND_SERVICE_ID, notification)
                }
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_PAUSE, ACTION_CANCEL -> {
                // These are handled via broadcast to the orchestrator
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Update the notification with current progress.
     */
    fun updateNotification(taskName: String, currentStep: String, progress: Int = -1) {
        val notification = buildNotification(taskName, currentStep, progress)
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(Constants.FOREGROUND_SERVICE_ID, notification)
    }

    private fun buildNotification(
        taskName: String,
        currentStep: String,
        progress: Int = -1
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AgentForegroundService::class.java).apply {
                action = ACTION_CANCEL
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(taskName)
            .setSubText(currentStep)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, getString(R.string.notification_cancel), cancelIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}
