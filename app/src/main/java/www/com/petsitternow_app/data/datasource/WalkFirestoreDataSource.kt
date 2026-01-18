package www.com.petsitternow_app.data.datasource

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for Firestore operations related to walk requests.
 * Collection: walk_requests
 */
@Singleton
class WalkFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val COLLECTION_WALK_REQUESTS = "walk_requests"
        private const val COLLECTION_RESPONSES = "responses"
        private const val COLLECTION_PETS = "pets"
        private const val FIELD_OWNER_ID = "ownerId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_ASSIGNED_PETSITTER_ID = "assignedPetsitterId"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val HISTORY_LIMIT = 50L
    }

    /**
     * Create a new walk request.
     */
    suspend fun createWalkRequest(
        ownerId: String,
        petIds: List<String>,
        duration: String,
        location: WalkLocation
    ): Result<String> {
        return try {
            val requestData = mapOf(
                FIELD_OWNER_ID to ownerId,
                "petIds" to petIds,
                "location" to location.toMap(),
                "duration" to duration,
                FIELD_STATUS to WalkStatus.PENDING.value,
                FIELD_CREATED_AT to FieldValue.serverTimestamp()
            )

            val docRef = firestore.collection(COLLECTION_WALK_REQUESTS)
                .add(requestData)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update walk request status.
     */
    suspend fun updateWalkRequestStatus(requestId: String, status: WalkStatus): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_WALK_REQUESTS)
                .document(requestId)
                .update(
                    mapOf(
                        FIELD_STATUS to status.value,
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get walk request by ID.
     */
    suspend fun getWalkRequest(requestId: String): Result<WalkRequest?> {
        return try {
            val doc = firestore.collection(COLLECTION_WALK_REQUESTS)
                .document(requestId)
                .get()
                .await()

            if (!doc.exists()) {
                Result.success(null)
            } else {
                val data = doc.data ?: return Result.success(null)
                Result.success(WalkRequest.fromMap(doc.id, data))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe active walk request for an owner.
     * Active means status is not in FINAL_STATUSES and not DISMISSED.
     */
    fun observeActiveWalkRequest(ownerId: String): Flow<WalkRequest?> = callbackFlow {
        val activeStatuses = listOf(
            WalkStatus.PENDING.value,
            WalkStatus.MATCHING.value,
            WalkStatus.ASSIGNED.value,
            WalkStatus.GOING_TO_OWNER.value,
            WalkStatus.IN_PROGRESS.value,
            WalkStatus.WALKING.value,
            WalkStatus.RETURNING.value,
            WalkStatus.FAILED.value // Show FAILED until dismissed
        )

        val listener: ListenerRegistration = firestore.collection(COLLECTION_WALK_REQUESTS)
            .whereEqualTo(FIELD_OWNER_ID, ownerId)
            .whereIn(FIELD_STATUS, activeStatuses)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val doc = snapshot?.documents?.firstOrNull()
                if (doc == null || !doc.exists()) {
                    trySend(null)
                } else {
                    val data = doc.data
                    if (data != null) {
                        trySend(WalkRequest.fromMap(doc.id, data))
                    } else {
                        trySend(null)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe walk history for an owner.
     */
    fun observeWalkHistory(ownerId: String): Flow<List<WalkRequest>> = callbackFlow {
        val finalStatuses = listOf(
            WalkStatus.COMPLETED.value,
            WalkStatus.CANCELLED.value,
            WalkStatus.DISMISSED.value
        )

        val listener: ListenerRegistration = firestore.collection(COLLECTION_WALK_REQUESTS)
            .whereEqualTo(FIELD_OWNER_ID, ownerId)
            .whereIn(FIELD_STATUS, finalStatuses)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(HISTORY_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { WalkRequest.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe active mission for a petsitter.
     */
    fun observeActiveMission(petsitterId: String): Flow<WalkRequest?> = callbackFlow {
        val activeStatuses = listOf(
            WalkStatus.ASSIGNED.value,
            WalkStatus.GOING_TO_OWNER.value,
            WalkStatus.IN_PROGRESS.value,
            WalkStatus.WALKING.value,
            WalkStatus.RETURNING.value
        )

        val listener: ListenerRegistration = firestore.collection(COLLECTION_WALK_REQUESTS)
            .whereEqualTo(FIELD_ASSIGNED_PETSITTER_ID, petsitterId)
            .whereIn(FIELD_STATUS, activeStatuses)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val doc = snapshot?.documents?.firstOrNull()
                if (doc == null || !doc.exists()) {
                    trySend(null)
                } else {
                    val data = doc.data
                    if (data != null) {
                        trySend(WalkRequest.fromMap(doc.id, data))
                    } else {
                        trySend(null)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe mission history for a petsitter.
     */
    fun observeMissionHistory(petsitterId: String): Flow<List<WalkRequest>> = callbackFlow {
        val finalStatuses = listOf(
            WalkStatus.COMPLETED.value,
            WalkStatus.CANCELLED.value
        )

        val listener: ListenerRegistration = firestore.collection(COLLECTION_WALK_REQUESTS)
            .whereEqualTo(FIELD_ASSIGNED_PETSITTER_ID, petsitterId)
            .whereIn(FIELD_STATUS, finalStatuses)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(HISTORY_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { WalkRequest.fromMap(doc.id, it) }
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Create mission response (accept or decline).
     */
    suspend fun createMissionResponse(
        requestId: String,
        petsitterId: String,
        accepted: Boolean
    ): Result<Unit> {
        return try {
            val responseData = mapOf(
                "petsitterId" to petsitterId,
                "accepted" to accepted,
                "respondedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_WALK_REQUESTS)
                .document(requestId)
                .collection(COLLECTION_RESPONSES)
                .add(responseData)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
