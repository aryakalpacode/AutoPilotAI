package com.autopilot.agent.agent.tools

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.autopilot.agent.domain.model.ToolResult
import com.autopilot.agent.service.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Reminder tool for scheduling local notifications using AlarmManager.
 */
class ReminderTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "REMINDER"
    override val description = "Schedule local notification reminders"
    override val requiresConfirmation = true

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val title = input["title"]?.toString() ?: "Reminder"
        val message = input["message"]?.toString() ?: ""
        val timeStr = input["time"]?.toString()

        if (timeStr.isNullOrBlank()) {
            return ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Time is required (ISO8601 format, e.g., 2024-12-25T10:00:00)",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }

        return try {
            val triggerTime = parseTime(timeStr)
            if (triggerTime <= System.currentTimeMillis()) {
                return ToolResult(
                    toolName = name,
                    success = false,
                    output = "",
                    error = "Reminder time must be in the future",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }

            scheduleAlarm(title, message, triggerTime)

            val formattedTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(Date(triggerTime))

            ToolResult(
                toolName = name,
                success = true,
                output = "Reminder set:\n  Title: $title\n  Message: $message\n  Time: $formattedTime",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Failed to set reminder: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun parseTime(timeStr: String): Long {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.timeZone = TimeZone.getDefault()
                val date = sdf.parse(timeStr)
                if (date != null) return date.time
            } catch (_: Exception) { /* try next format */ }
        }

        throw IllegalArgumentException("Cannot parse time: $timeStr. Use ISO8601 format.")
    }

    private fun scheduleAlarm(title: String, message: String, triggerTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("request_code", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
