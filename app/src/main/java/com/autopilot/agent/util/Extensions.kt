package com.autopilot.agent.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Extension functions used across the application.
 */

/** Format a timestamp to a human-readable date-time string. */
fun Long.toFormattedDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

/** Format a timestamp to a short time string. */
fun Long.toFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

/** Format a timestamp to a date string. */
fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

/** Get the current ISO8601 datetime string. */
fun currentIso8601(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(Date())
}

/** Truncate a string to a given max length, appending ellipsis if truncated. */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this
    else substring(0, maxLength - 3) + "..."
}

/** Estimate token count for a string (approximately 1 token per 4 characters). */
fun String.estimateTokens(): Int {
    return (length + Constants.TOKEN_CHAR_RATIO - 1) / Constants.TOKEN_CHAR_RATIO
}

/** Safely parse a string to JSON-safe string (escape special chars). */
fun String.escapeJson(): String {
    return this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

/** Get relative time description from timestamp. */
fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> toFormattedDate()
    }
}
