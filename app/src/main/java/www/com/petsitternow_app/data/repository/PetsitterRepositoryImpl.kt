package www.com.petsitternow_app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.data.datasource.WalkFirestoreDataSource
import www.com.petsitternow_app.data.datasource.WalkRealtimeDataSource
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.PetsitterProfile
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.PetsitterRepository
import www.com.petsitternow_app.util.DistanceCalculator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PetsitterRepository for petsitter-side operations.
 */
@Singleton
class PetsitterRepositoryImpl @Inject constructor(
    private val firestoreDataSource: WalkFirestoreDataSource,
    private val realtimeDataSource: WalkRealtimeDataSource,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PetsitterRepository {

    companion object {
        private const val COLLECTION_PETSITTERS_PROFILES = "petsitters_profiles"
    }

    override fun goOnline(): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        try {
            // Update Firestore profile
            firestore.collection(COLLECTION_PETSITTERS_PROFILES)
                .document(currentUserId)
                .set(
                    mapOf(
                        "userId" to currentUserId,
                        "isOnline" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun goOffline(): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        try {
            // Update Firestore profile
            firestore.collection(COLLECTION_PETSITTERS_PROFILES)
                .document(currentUserId)
                .set(
                    mapOf(
                        "isOnline" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()

            // Remove from RTDB available list
            realtimeDataSource.removePetsitterAvailable(currentUserId)

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun observePendingMission(petsitterId: String): Flow<PetsitterMission?> {
        return realtimeDataSource.observePendingMission(petsitterId)
    }

    override fun acceptMission(requestId: String): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        val result = firestoreDataSource.createMissionResponse(
            requestId = requestId,
            petsitterId = currentUserId,
            accepted = true
        )

        emit(result)
    }

    override fun declineMission(requestId: String): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        val result = firestoreDataSource.createMissionResponse(
            requestId = requestId,
            petsitterId = currentUserId,
            accepted = false
        )

        emit(result)
    }

    override fun updateLocation(lat: Double, lng: Double): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Update in petsitters_available
        val result = realtimeDataSource.updatePetsitterLocation(currentUserId, lat, lng)
        emit(result)
    }

    override fun startWalk(requestId: String): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Get request to verify petsitter is assigned
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }

        val request = requestResult.getOrNull()
        if (request == null) {
            emit(Result.failure(Exception("Mission introuvable")))
            return@flow
        }

        if (request.assignedPetsitterId != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }

        if (request.status != WalkStatus.ASSIGNED) {
            emit(Result.failure(Exception("Cette action n'est pas disponible dans l'état actuel de la mission")))
            return@flow
        }

        // Update Firestore status
        val firestoreResult = firestoreDataSource.updateWalkRequestStatus(requestId, WalkStatus.IN_PROGRESS)
        if (firestoreResult.isFailure) {
            emit(firestoreResult)
            return@flow
        }

        // Update RTDB with walking status and start time
        val rtdbResult = realtimeDataSource.setWalkStarted(requestId)
        emit(rtdbResult)
    }

    override fun markReturning(requestId: String): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Verify petsitter is assigned
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }

        val request = requestResult.getOrNull()
        if (request == null) {
            emit(Result.failure(Exception("Mission introuvable")))
            return@flow
        }

        if (request.assignedPetsitterId != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }

        // Check status in RTDB
        val activeWalkResult = realtimeDataSource.getActiveWalk(requestId)
        val activeWalk = activeWalkResult.getOrNull()

        if (activeWalk?.status != WalkStatus.WALKING) {
            emit(Result.failure(Exception("Cette action n'est pas disponible dans l'état actuel de la mission")))
            return@flow
        }

        // Update RTDB with returning status
        val rtdbResult = realtimeDataSource.setWalkReturning(requestId)
        emit(rtdbResult)
    }

    override fun completeWalk(requestId: String, petsitterLocation: WalkLocation): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Verify petsitter is assigned
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }

        val request = requestResult.getOrNull()
        if (request == null) {
            emit(Result.failure(Exception("Mission introuvable")))
            return@flow
        }

        if (request.assignedPetsitterId != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }

        // Check status in RTDB
        val activeWalkResult = realtimeDataSource.getActiveWalk(requestId)
        val activeWalk = activeWalkResult.getOrNull()
        val currentStatus = activeWalk?.status ?: request.status

        if (!currentStatus.isWalkingPhase()) {
            emit(Result.failure(Exception("Cette action n'est pas disponible dans l'état actuel de la mission")))
            return@flow
        }

        // Validate distance from owner location
        val ownerLocation = request.location
        val distance = DistanceCalculator.calculateDistanceMeters(
            petsitterLocation.lat,
            petsitterLocation.lng,
            ownerLocation.lat,
            ownerLocation.lng
        )

        if (distance > DistanceCalculator.COMPLETION_DISTANCE_THRESHOLD_METERS) {
            emit(Result.failure(Exception(
                "Vous devez être à proximité du propriétaire pour terminer la mission. Distance actuelle : ${distance.toInt()}m (maximum 100m)"
            )))
            return@flow
        }

        // Update Firestore status to COMPLETED
        val firestoreResult = firestoreDataSource.updateWalkRequestStatus(requestId, WalkStatus.COMPLETED)
        if (firestoreResult.isFailure) {
            emit(firestoreResult)
            return@flow
        }

        // Remove RTDB active walk data
        realtimeDataSource.removeActiveWalk(requestId)

        emit(Result.success(Unit))
    }

    override fun observeMissionHistory(petsitterId: String): Flow<List<WalkRequest>> {
        return firestoreDataSource.observeMissionHistory(petsitterId)
    }

    override fun observeActiveMission(petsitterId: String): Flow<WalkRequest?> {
        return firestoreDataSource.observeActiveMission(petsitterId)
    }

    override fun observeProfile(petsitterId: String): Flow<PetsitterProfile?> = callbackFlow {
        val listener = firestore.collection(COLLECTION_PETSITTERS_PROFILES)
            .document(petsitterId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                } else {
                    val data = snapshot.data
                    trySend(PetsitterProfile.fromMap(data))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun cancelMission(requestId: String): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Get the request to verify petsitter is assigned
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }

        val request = requestResult.getOrNull()
        if (request == null) {
            emit(Result.failure(Exception("Mission introuvable")))
            return@flow
        }

        if (request.assignedPetsitterId != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }

        if (request.status.isFinal()) {
            emit(Result.failure(Exception("Cette mission ne peut plus être annulée")))
            return@flow
        }

        // Update status
        try {
            firestore.collection("walk_requests")
                .document(requestId)
                .update(
                    mapOf(
                        "status" to WalkStatus.CANCELLED.value,
                        "cancelledBy" to "petsitter",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            // Clean up RTDB
            realtimeDataSource.removeActiveWalk(requestId)

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
