package www.com.petsitternow_app.data.datasource

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.PetsitterAvailability
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.WalkStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firebase Realtime Database operations.
 * Paths:
 * - petsitters_available/{userId} - online petsitters with location
 * - active_walks/{requestId} - real-time walk tracking data
 * - petsitter_missions/{userId} - pending mission notifications
 */
@Singleton
class WalkRealtimeDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    companion object {
        private const val PATH_PETSITTERS_AVAILABLE = "petsitters_available"
        private const val PATH_ACTIVE_WALKS = "active_walks"
        private const val PATH_PETSITTER_MISSIONS = "petsitter_missions"
    }

    /**
     * Set petsitter as available (online) with location.
     */
    suspend fun setPetsitterOnline(
        userId: String,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        return try {
            val data = mapOf(
                "lat" to lat,
                "lng" to lng,
                "lastUpdate" to ServerValue.TIMESTAMP,
                "isOnline" to true
            )

            database.getReference(PATH_PETSITTERS_AVAILABLE)
                .child(userId)
                .setValue(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove petsitter from available list (offline).
     */
    suspend fun removePetsitterAvailable(userId: String): Result<Unit> {
        return try {
            database.getReference(PATH_PETSITTERS_AVAILABLE)
                .child(userId)
                .removeValue()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update petsitter location.
     */
    suspend fun updatePetsitterLocation(
        userId: String,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "lat" to lat,
                "lng" to lng,
                "lastUpdate" to ServerValue.TIMESTAMP
            )

            database.getReference(PATH_PETSITTERS_AVAILABLE)
                .child(userId)
                .updateChildren(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe active walk data for real-time tracking.
     */
    @Suppress("UNCHECKED_CAST")
    fun observeActiveWalk(requestId: String): Flow<ActiveWalk?> = callbackFlow {
        val ref = database.getReference(PATH_ACTIVE_WALKS).child(requestId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                val data = snapshot.value as? Map<String, Any?>
                trySend(ActiveWalk.fromMap(data))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Update active walk status.
     */
    suspend fun updateActiveWalkStatus(requestId: String, status: WalkStatus): Result<Unit> {
        return try {
            database.getReference(PATH_ACTIVE_WALKS)
                .child(requestId)
                .child("status")
                .setValue(status.value)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set walk started timestamp and status.
     */
    suspend fun setWalkStarted(requestId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to WalkStatus.WALKING.value,
                "walkStartedAt" to ServerValue.TIMESTAMP
            )

            database.getReference(PATH_ACTIVE_WALKS)
                .child(requestId)
                .updateChildren(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set walk returning status and timestamp.
     */
    suspend fun setWalkReturning(requestId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to WalkStatus.RETURNING.value,
                "walkEndedAt" to ServerValue.TIMESTAMP
            )

            database.getReference(PATH_ACTIVE_WALKS)
                .child(requestId)
                .updateChildren(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update petsitter location in active walk.
     */
    suspend fun updateActiveWalkPetsitterLocation(
        requestId: String,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        return try {
            val locationData = mapOf(
                "lat" to lat,
                "lng" to lng
            )

            database.getReference(PATH_ACTIVE_WALKS)
                .child(requestId)
                .child("petsitterLocation")
                .setValue(locationData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove active walk data (on completion).
     */
    suspend fun removeActiveWalk(requestId: String): Result<Unit> {
        return try {
            database.getReference(PATH_ACTIVE_WALKS)
                .child(requestId)
                .removeValue()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe pending mission notification for a petsitter.
     */
    @Suppress("UNCHECKED_CAST")
    fun observePendingMission(petsitterId: String): Flow<PetsitterMission?> = callbackFlow {
        val ref = database.getReference(PATH_PETSITTER_MISSIONS).child(petsitterId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }

                val data = snapshot.value as? Map<String, Any?>
                val mission = PetsitterMission.fromMap(data)

                // Check if mission is expired
                if (mission != null && mission.isExpired()) {
                    trySend(null)
                } else {
                    trySend(mission)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Get active walk data (one-time read).
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun getActiveWalk(requestId: String): Result<ActiveWalk?> {
        return try {
            val snapshot = database.getReference(PATH_ACTIVE_WALKS)
                .child(requestId)
                .get()
                .await()

            if (!snapshot.exists()) {
                Result.success(null)
            } else {
                val data = snapshot.value as? Map<String, Any?>
                Result.success(ActiveWalk.fromMap(data))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
