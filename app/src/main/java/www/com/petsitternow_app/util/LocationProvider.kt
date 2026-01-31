package www.com.petsitternow_app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.Handler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import www.com.petsitternow_app.domain.model.WalkLocation
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Location provider using FusedLocationProviderClient.
 * Provides single location requests and continuous location updates.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val DEFAULT_UPDATE_INTERVAL_MS = 30_000L // 30 seconds
        const val FASTEST_UPDATE_INTERVAL_MS = 10_000L // 10 seconds
        const val LOCATION_TIMEOUT_MS = 15_000L // 15 seconds
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Check if location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request a single location update.
     * @return WalkLocation with lat/lng, or throws exception on failure
     */
    suspend fun requestSingleLocation(): WalkLocation = withTimeout(LOCATION_TIMEOUT_MS + 5000) {
        suspendCancellableCoroutine { continuation ->
            if (!hasLocationPermission()) {
                continuation.resumeWithException(
                    SecurityException("Permission de localisation non accordée")
                )
                return@suspendCancellableCoroutine
            }

            try {
                @Suppress("MissingPermission")
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null && continuation.isActive) {
                            continuation.resume(
                                WalkLocation(
                                    lat = location.latitude,
                                    lng = location.longitude,
                                    address = ""
                                )
                            )
                        } else if (continuation.isActive) {
                            // No cached location, request fresh one
                            requestFreshLocation(continuation)
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                Exception("Erreur de géolocalisation: ${exception.message}")
                            )
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        Exception("Erreur de géolocalisation: ${e.message}")
                    )
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun requestFreshLocation(continuation: kotlinx.coroutines.CancellableContinuation<WalkLocation>) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_TIMEOUT_MS
        )
            .setWaitForAccurateLocation(false)
            .setMaxUpdates(1)
            .build()

        var callback: LocationCallback? = null
        val handler = Handler(Looper.getMainLooper())
        var timeoutRunnable: Runnable? = null

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                callback?.let { fusedLocationClient.removeLocationUpdates(it) }
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                val location = result.lastLocation
                if (location != null && continuation.isActive) {
                    continuation.resume(
                        WalkLocation(
                            lat = location.latitude,
                            lng = location.longitude,
                            address = ""
                        )
                    )
                } else if (continuation.isActive) {
                    continuation.resumeWithException(
                        Exception("Impossible d'obtenir la localisation")
                    )
                }
            }
        }

        // Set up timeout
        timeoutRunnable = Runnable {
            callback?.let { fusedLocationClient.removeLocationUpdates(it) }
            if (continuation.isActive) {
                continuation.resumeWithException(
                    Exception("Timeout: La localisation n'a pas pu être obtenue dans les délais")
                )
            }
        }
        handler.postDelayed(timeoutRunnable, LOCATION_TIMEOUT_MS)

        // Cleanup on cancellation
        continuation.invokeOnCancellation {
            callback?.let { fusedLocationClient.removeLocationUpdates(it) }
            timeoutRunnable?.let { handler.removeCallbacks(it) }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            timeoutRunnable?.let { handler.removeCallbacks(it) }
            if (continuation.isActive) {
                continuation.resumeWithException(
                    Exception("Erreur lors de la demande de localisation: ${e.message}")
                )
            }
        }
    }

    /**
     * Observe continuous location updates.
     * @param intervalMs Update interval in milliseconds (default: 30 seconds)
     * @return Flow of WalkLocation updates
     */
    fun observeLocationUpdates(
        intervalMs: Long = DEFAULT_UPDATE_INTERVAL_MS
    ): Flow<WalkLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            throw SecurityException("Permission de localisation non accordée")
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        )
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        WalkLocation(
                            lat = location.latitude,
                            lng = location.longitude,
                            address = ""
                        )
                    )
                }
            }
        }

        @Suppress("MissingPermission")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Stop all location updates.
     * Note: This is handled automatically via callbackFlow's awaitClose,
     * but provided for explicit control if needed.
     */
    fun stopLocationUpdates(callback: LocationCallback) {
        fusedLocationClient.removeLocationUpdates(callback)
    }
}
