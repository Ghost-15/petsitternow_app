package www.com.petsitternow_app.domain.repository

import kotlinx.coroutines.flow.Flow
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest

/**
 * Repository for walk-related operations (owner side).
 */
interface WalkRepository {

    /**
     * Create a new walk request.
     *
     * @param petIds List of pet IDs to include in the walk
     * @param duration Duration value ("30", "45", or "60")
     * @param location Pickup location
     * @return Flow emitting Result with request ID on success
     */
    fun createWalkRequest(
        petIds: List<String>,
        duration: String,
        location: WalkLocation
    ): Flow<Result<String>>

    /**
     * Cancel an active walk request.
     * Only allowed for non-final statuses.
     *
     * @param requestId The walk request ID
     * @return Flow emitting Result success or failure
     */
    fun cancelWalkRequest(requestId: String): Flow<Result<Unit>>

    /**
     * Dismiss a failed walk request.
     * Only allowed for FAILED status.
     *
     * @param requestId The walk request ID
     * @return Flow emitting Result success or failure
     */
    fun dismissWalkRequest(requestId: String): Flow<Result<Unit>>

    /**
     * Observe the current active walk request for an owner.
     * Returns null when no active request exists.
     *
     * @param ownerId The owner's user ID
     * @return Flow of the active WalkRequest or null
     */
    fun observeActiveWalkRequest(ownerId: String): Flow<WalkRequest?>

    /**
     * Observe real-time data for an active walk (from RTDB).
     *
     * @param requestId The walk request ID
     * @return Flow of ActiveWalk data or null
     */
    fun observeActiveWalk(requestId: String): Flow<ActiveWalk?>

    /**
     * Observe walk history for an owner.
     * Returns last 50 completed or cancelled walks.
     *
     * @param ownerId The owner's user ID
     * @return Flow of walk request list
     */
    fun observeWalkHistory(ownerId: String): Flow<List<WalkRequest>>
}
