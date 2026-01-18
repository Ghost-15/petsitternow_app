package www.com.petsitternow_app.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distance calculator using Haversine formula.
 * Matches web implementation in lib/geo/utils.ts (uses geofire-common's distanceBetween).
 */
object DistanceCalculator {

    /**
     * Maximum distance from owner location to complete a walk (in meters).
     */
    const val COMPLETION_DISTANCE_THRESHOLD_METERS = 100.0

    /**
     * Earth's radius in kilometers.
     */
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculate distance between two coordinates using Haversine formula.
     *
     * @param lat1 Latitude of first point
     * @param lng1 Longitude of first point
     * @param lat2 Latitude of second point
     * @param lng2 Longitude of second point
     * @return Distance in meters
     */
    fun calculateDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c * 1000 // Convert km to meters
    }

    /**
     * Calculate distance between two coordinates.
     *
     * @return Distance in kilometers (matches web geofire-common behavior)
     */
    fun calculateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        return calculateDistanceMeters(lat1, lng1, lat2, lng2) / 1000
    }

    /**
     * Check if a location is within range of another location.
     *
     * @param fromLat Latitude of first point
     * @param fromLng Longitude of first point
     * @param toLat Latitude of second point
     * @param toLng Longitude of second point
     * @param maxMeters Maximum distance in meters (default: 100m as per business rules)
     * @return true if distance <= maxMeters
     */
    fun isWithinRange(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double,
        maxMeters: Double = COMPLETION_DISTANCE_THRESHOLD_METERS
    ): Boolean {
        return calculateDistanceMeters(fromLat, fromLng, toLat, toLng) <= maxMeters
    }
}
