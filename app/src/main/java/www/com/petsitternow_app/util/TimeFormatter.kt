package www.com.petsitternow_app.util

/**
 * Time formatter for walk duration display.
 * Format matches web implementation: Xh Xm Xs, Xm Xs, or Xs.
 */
object TimeFormatter {

    /**
     * Format elapsed time from a start timestamp to now.
     *
     * @param startTimestamp Start time in milliseconds (epoch time)
     * @return Formatted string like "1h 23m 45s", "5m 30s", or "45s"
     */
    fun formatElapsedTime(startTimestamp: Long?): String {
        if (startTimestamp == null || startTimestamp == 0L) return ""

        val elapsedMs = System.currentTimeMillis() - startTimestamp
        if (elapsedMs < 0) return ""

        return formatDurationMs(elapsedMs)
    }

    /**
     * Format a duration in milliseconds.
     *
     * @param durationMs Duration in milliseconds
     * @return Formatted string like "1h 23m 45s", "5m 30s", or "45s"
     */
    fun formatDurationMs(durationMs: Long): String {
        if (durationMs < 0) return ""

        val totalSeconds = (durationMs / 1000).toInt()
        return formatDurationSeconds(totalSeconds)
    }

    /**
     * Format a duration in seconds.
     *
     * @param totalSeconds Duration in seconds
     * @return Formatted string like "1h 23m 45s", "5m 30s", or "45s"
     */
    fun formatDurationSeconds(totalSeconds: Int): String {
        if (totalSeconds < 0) return ""

        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Format a duration option for display.
     *
     * @param minutes Duration in minutes (30, 45, or 60)
     * @return Formatted string like "30 min", "45 min", "1h"
     */
    fun formatDurationOption(minutes: Int): String {
        return when (minutes) {
            60 -> "1h"
            else -> "$minutes min"
        }
    }

    /**
     * Format a duration value from string (as stored in database).
     *
     * @param durationValue Duration value string ("30", "45", "60")
     * @return Formatted string like "30 min", "45 min", "1h"
     */
    fun formatDurationValue(durationValue: String?): String {
        val minutes = durationValue?.toIntOrNull() ?: 30
        return formatDurationOption(minutes)
    }
}
