package www.com.petsitternow_app.domain.repository

import kotlinx.coroutines.flow.Flow
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.PetsitterProfile
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest

/**
 * Repository for petsitter-related operations.
 */
interface PetsitterRepository {

    /**
     * Set petsitter status to online.
     * Writes to Firestore petsitters_profiles and RTDB petsitters_available.
     *
     * @return Flow emitting Result success or failure
     */
    fun goOnline(): Flow<Result<Unit>>

    /**
     * Set petsitter status to offline.
     * Updates Firestore and removes from RTDB petsitters_available.
     *
     * @return Flow emitting Result success or failure
     */
    fun goOffline(): Flow<Result<Unit>>

    /**
     * Observe pending mission notification for petsitter.
     * Listens to RTDB petsitter_missions/{userId}.
     *
     * @param petsitterId The petsitter's user ID
     * @return Flow of pending mission or null
     */
    fun observePendingMission(petsitterId: String): Flow<PetsitterMission?>

    /**
     * Accept a mission (walk request).
     * Creates response document in walk_requests/{requestId}/responses.
     *
     * @param requestId The walk request ID
     * @return Flow emitting Result success or failure
     */
    fun acceptMission(requestId: String): Flow<Result<Unit>>

    /**
     * Decline a mission (walk request).
     * Creates response document with accepted=false.
     *
     * @param requestId The walk request ID
     * @return Flow emitting Result success or failure
     */
    fun declineMission(requestId: String): Flow<Result<Unit>>

    /**
     * Update petsitter's current location.
     * Writes to RTDB petsitters_available/{userId}.
     *
     * @param lat Latitude
     * @param lng Longitude
     * @return Flow emitting Result success or failure
     */
    fun updateLocation(lat: Double, lng: Double): Flow<Result<Unit>>

    /**
     * Start the walk (transition from ASSIGNED to IN_PROGRESS/WALKING).
     * Updates Firestore status and RTDB walkStartedAt.
     *
     * @param requestId The walk request ID
     * @return Flow emitting Result success or failure
     */
    fun startWalk(requestId: String): Flow<Result<Unit>>

    /**
     * Mark walk as returning (transition from WALKING to RETURNING).
     * Updates RTDB status and walkEndedAt.
     *
     * @param requestId The walk request ID
     * @return Flow emitting Result success or failure
     */
    fun markReturning(requestId: String): Flow<Result<Unit>>

    /**
     * Complete the walk mission.
     * Validates petsitter is within 100m of owner location.
     *
     * @param requestId The walk request ID
     * @param petsitterLocation Current petsitter location for validation
     * @return Flow emitting Result success or failure with distance error if too far
     */
    fun completeWalk(requestId: String, petsitterLocation: WalkLocation): Flow<Result<Unit>>

    /**
     * Observe mission history for a petsitter.
     * Returns last 50 completed or cancelled walks assigned to this petsitter.
     *
     * @param petsitterId The petsitter's user ID
     * @return Flow of walk request list
     */
    fun observeMissionHistory(petsitterId: String): Flow<List<WalkRequest>>

    /**
     * Observe the currently active mission for a petsitter.
     *
     * @param petsitterId The petsitter's user ID
     * @return Flow of active WalkRequest or null
     */
    fun observeActiveMission(petsitterId: String): Flow<WalkRequest?>

    /**
     * Observe petsitter profile (for online status).
     *
     * @param petsitterId The petsitter's user ID
     * @return Flow of profile or null
     */
    fun observeProfile(petsitterId: String): Flow<PetsitterProfile?>

    /**
     * Cancel an assigned mission by petsitter.
     *
     * @param requestId The walk request ID
     * @return Flow emitting Result success or failure
     */
    fun cancelMission(requestId: String): Flow<Result<Unit>>

    /**
     * Update petsitter location in active_walks for owner to see.
     *
     * @param requestId The walk request ID
     * @param lat Latitude
     * @param lng Longitude
     * @return Flow emitting Result success or failure
     */
    fun updateLocationForActiveWalk(requestId: String, lat: Double, lng: Double): Flow<Result<Unit>>
}
