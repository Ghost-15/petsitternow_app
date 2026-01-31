package www.com.petsitternow_app.domain.model

/**
 * Route information from Mapbox Directions API.
 */
data class RouteInfo(
    val coordinates: List<Pair<Double, Double>>, // List of (lng, lat) pairs
    val distance: Double, // in meters
    val duration: Double // in seconds
) {
    companion object {
        /**
         * Format distance for display.
         */
        fun formatDistance(meters: Double): String {
            return if (meters < 1000) {
                "${meters.toInt()} m"
            } else {
                String.format("%.1f km", meters / 1000)
            }
        }

        /**
         * Format duration for display.
         */
        fun formatDuration(seconds: Double): String {
            val minutes = (seconds / 60).toInt()
            return when {
                minutes < 1 -> "< 1 min"
                minutes == 1 -> "1 min"
                else -> "$minutes min"
            }
        }
    }
}
