package www.com.petsitternow_app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DistanceCalculator.
 * Uses known geographic coordinates to verify Haversine formula accuracy.
 */
class DistanceCalculatorTest {

    @Test
    fun `calculateDistanceMeters returns 0 for same location`() {
        val distance = DistanceCalculator.calculateDistanceMeters(
            lat1 = 48.8566,
            lng1 = 2.3522,
            lat2 = 48.8566,
            lng2 = 2.3522
        )
        
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `calculateDistanceMeters returns correct distance for Paris to Lyon`() {
        // Paris to Lyon is approximately 392 km
        val distance = DistanceCalculator.calculateDistanceMeters(
            lat1 = 48.8566, // Paris
            lng1 = 2.3522,
            lat2 = 45.7640, // Lyon
            lng2 = 4.8357
        )
        
        // Should be around 392,000 meters (with some tolerance for formula precision)
        val distanceKm = distance / 1000
        assertTrue("Distance should be around 392km, was $distanceKm km", distanceKm in 390.0..395.0)
    }

    @Test
    fun `calculateDistanceMeters returns correct short distance`() {
        // Two points about 100m apart in Paris
        val distance = DistanceCalculator.calculateDistanceMeters(
            lat1 = 48.8566,
            lng1 = 2.3522,
            lat2 = 48.8575,  // About 100m north
            lng2 = 2.3522
        )
        
        // Should be approximately 100 meters
        assertTrue("Distance should be around 100m, was $distance m", distance in 95.0..105.0)
    }

    @Test
    fun `isWithinRange returns true for distance under threshold`() {
        // Two points 50m apart
        val result = DistanceCalculator.isWithinRange(
            fromLat = 48.8566,
            fromLng = 2.3522,
            toLat = 48.85705,  // About 50m north
            toLng = 2.3522,
            maxMeters = 100.0
        )
        
        assertTrue("50m should be within 100m range", result)
    }

    @Test
    fun `isWithinRange returns false for distance over threshold`() {
        // Two points 200m apart
        val result = DistanceCalculator.isWithinRange(
            fromLat = 48.8566,
            fromLng = 2.3522,
            toLat = 48.8584,  // About 200m north
            toLng = 2.3522,
            maxMeters = 100.0
        )
        
        assertFalse("200m should NOT be within 100m range", result)
    }

    @Test
    fun `isWithinRange uses default threshold of 100m`() {
        // Two points 50m apart - should be within default 100m threshold
        val result = DistanceCalculator.isWithinRange(
            fromLat = 48.8566,
            fromLng = 2.3522,
            toLat = 48.85705,
            toLng = 2.3522
        )
        
        assertTrue("50m should be within default 100m threshold", result)
    }

    @Test
    fun `COMPLETION_DISTANCE_THRESHOLD_METERS is 100`() {
        assertEquals(100.0, DistanceCalculator.COMPLETION_DISTANCE_THRESHOLD_METERS, 0.0)
    }

    @Test
    fun `calculateDistanceKm returns kilometers`() {
        // Paris to Lyon is approximately 392 km
        val distanceKm = DistanceCalculator.calculateDistanceKm(
            lat1 = 48.8566,
            lng1 = 2.3522,
            lat2 = 45.7640,
            lng2 = 4.8357
        )
        
        assertTrue("Distance should be around 392km, was $distanceKm km", distanceKm in 390.0..395.0)
    }
}
