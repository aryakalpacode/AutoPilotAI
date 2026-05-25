package com.autopilot.agent.agent.tools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.autopilot.agent.domain.model.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Device information tool providing system-level details to the agent.
 */
class DeviceInfoTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {

    override val name = "DEVICE_INFO"
    override val description = "Get device information: battery, network, storage, date/time"
    override val requiresConfirmation = false

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()

        return try {
            val info = buildString {
                appendLine("=== Device Information ===")
                appendLine()

                // Device
                appendLine("📱 Device:")
                appendLine("  Model: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("  Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("  Brand: ${Build.BRAND}")
                appendLine()

                // Battery
                appendLine("🔋 Battery:")
                val batteryLevel = getBatteryLevel()
                val isCharging = isCharging()
                appendLine("  Level: ${batteryLevel}%")
                appendLine("  Charging: ${if (isCharging) "Yes" else "No"}")
                appendLine()

                // Network
                appendLine("🌐 Network:")
                val networkType = getNetworkType()
                appendLine("  Status: $networkType")
                appendLine()

                // Storage
                appendLine("💾 Storage:")
                val (totalGb, availGb) = getStorageInfo()
                appendLine("  Total: %.1f GB".format(totalGb))
                appendLine("  Available: %.1f GB".format(availGb))
                appendLine("  Used: %.1f GB".format(totalGb - availGb))
                appendLine()

                // Date/Time
                appendLine("🕐 Date/Time:")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                appendLine("  Current: ${sdf.format(Date())}")
                appendLine("  Timezone: ${TimeZone.getDefault().id} (${TimeZone.getDefault().displayName})")
                appendLine("  Locale: ${Locale.getDefault()}")
            }

            ToolResult(
                toolName = name,
                success = true,
                output = info.trim(),
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ToolResult(
                toolName = name,
                success = false,
                output = "",
                error = "Failed to get device info: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Offline"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Offline"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }
    }

    private fun getStorageInfo(): Pair<Double, Double> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalBytes = stat.totalBytes
        val availBytes = stat.availableBytes
        return Pair(totalBytes / 1_073_741_824.0, availBytes / 1_073_741_824.0)
    }
}
