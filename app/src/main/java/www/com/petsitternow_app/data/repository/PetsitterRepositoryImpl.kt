package www.com.petsitternow_app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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

    @Suppress("UNCHECKED_CAST")
    override fun observePendingMission(petsitterId: String): Flow<PetsitterMission?> = callbackFlow {
        Log.d("PetsitterRepo", "observePendingMission: Observing walk_requests with status=matching")
        
        // Observe walk_requests with status == 'matching' from Firestore (like web app)
        val listener = firestore.collection("walk_requests")
            .whereEqualTo("status", "matching")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PetsitterRepo", "observePendingMission error", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                
                if (snapshot == null || snapshot.isEmpty) {
                    Log.d("PetsitterRepo", "observePendingMission: No matching requests found")
                    trySend(null)
                } else {
                    val doc = snapshot.documents.first()
                    val data = doc.data
                    Log.d("PetsitterRepo", "observePendingMission: Found request ${doc.id}")
                    
                    // Convert to PetsitterMission
                    val locationMap = data?.get("location") as? Map<String, Any?>
                    val location = WalkLocation(
                        lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: 0.0,
                        lng = (locationMap?.get("lng") as? Number)?.toDouble() ?: 0.0,
                        address = locationMap?.get("address") as? String ?: ""
                    )
                    
                    val petIds = (data?.get("petIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val ownerId = data?.get("ownerId") as? String ?: ""
                    
                    // Fetch pet names and owner name asynchronously
                    kotlinx.coroutines.GlobalScope.launch {
                        val petNames = mutableListOf<String>()
                        for (petId in petIds) {
                            try {
                                val petDoc = firestore.collection("pets").document(petId).get().await()
                                val name = petDoc.getString("name")
                                if (name != null) petNames.add(name)
                            } catch (e: Exception) {
                                Log.e("PetsitterRepo", "Error fetching pet name for $petId", e)
                            }
                        }
                        
                        var ownerName = ""
                        try {
                            val ownerDoc = firestore.collection("users").document(ownerId).get().await()
                            val firstName = ownerDoc.getString("firstName") ?: ""
                            val lastName = ownerDoc.getString("lastName") ?: ""
                            ownerName = "$firstName $lastName".trim().ifEmpty { "Propriétaire" }
                        } catch (e: Exception) {
                            Log.e("PetsitterRepo", "Error fetching owner name for $ownerId", e)
                        }
                        
                        val mission = PetsitterMission(
                            requestId = doc.id,
                            ownerId = ownerId,
                            ownerName = ownerName,
                            petNames = petNames.ifEmpty { listOf("Animal") },
                            duration = data?.get("duration") as? String ?: "30",
                            distance = 0.0,
                            location = location,
                            expiresAt = Long.MAX_VALUE // Never expires client-side
                        )
                        
                        Log.d("PetsitterRepo", "observePendingMission: Sending mission ${mission.requestId} with petNames=${mission.petNames}, ownerName=${mission.ownerName}")
                        trySend(mission)
                    }
                }
            }
        
        awaitClose { 
            Log.d("PetsitterRepo", "observePendingMission: Closing listener")
            listener.remove() 
        }
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

        Log.d("PetsitterRepo", "Updating location: lat=$lat, lng=$lng, userId=$currentUserId")
        // Use setPetsitterOnline which does setValue() - creates node if not exists
        // This is important because updateChildren() fails silently if parent doesn't exist
        val result = realtimeDataSource.setPetsitterOnline(currentUserId, lat, lng)
        if (result.isSuccess) {
            Log.d("PetsitterRepo", "Location updated successfully in RTDB petsitters_available/$currentUserId")
        } else {
            Log.e("PetsitterRepo", "Failed to update location", result.exceptionOrNull())
        }
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
        Log.d("PetsitterRepo", "markReturning called for requestId=$requestId")
        
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e("PetsitterRepo", "markReturning: Non authentifié")
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Verify petsitter is assigned
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            Log.e("PetsitterRepo", "markReturning: Failed to get walk request", requestResult.exceptionOrNull())
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }

        val request = requestResult.getOrNull()
        if (request == null) {
            Log.e("PetsitterRepo", "markReturning: Mission introuvable")
            emit(Result.failure(Exception("Mission introuvable")))
            return@flow
        }

        Log.d("PetsitterRepo", "markReturning: request.assignedPetsitterId=${request.assignedPetsitterId}, currentUserId=$currentUserId")
        if (request.assignedPetsitterId != currentUserId) {
            Log.e("PetsitterRepo", "markReturning: Action non autorisée")
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }

        // Check status in RTDB
        val activeWalkResult = realtimeDataSource.getActiveWalk(requestId)
        val activeWalk = activeWalkResult.getOrNull()
        
        Log.d("PetsitterRepo", "markReturning: activeWalk=$activeWalk, status=${activeWalk?.status}")

        // Allow both WALKING and IN_PROGRESS statuses
        if (activeWalk?.status != WalkStatus.WALKING && activeWalk?.status != WalkStatus.IN_PROGRESS) {
            Log.e("PetsitterRepo", "markReturning: Invalid status ${activeWalk?.status}, expected WALKING or IN_PROGRESS")
            emit(Result.failure(Exception("Cette action n'est pas disponible dans l'état actuel de la mission (status: ${activeWalk?.status})")))
            return@flow
        }

        // Update RTDB with returning status
        Log.d("PetsitterRepo", "markReturning: Calling setWalkReturning")
        val rtdbResult = realtimeDataSource.setWalkReturning(requestId)
        Log.d("PetsitterRepo", "markReturning: result=${rtdbResult.isSuccess}")
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
        Log.d("PetsitterRepo", "observeProfile started for $petsitterId")
        val listener = firestore.collection(COLLECTION_PETSITTERS_PROFILES)
            .document(petsitterId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PetsitterRepo", "observeProfile error", error)
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    Log.w("PetsitterRepo", "observeProfile: document does not exist for $petsitterId")
                    trySend(null)
                } else {
                    val data = snapshot.data
                    Log.d("PetsitterRepo", "observeProfile data: $data")
                    val profile = PetsitterProfile.fromMap(data)
                    Log.d("PetsitterRepo", "observeProfile parsed: totalWalks=${profile?.totalWalks}, isOnline=${profile?.isOnline}")
                    trySend(profile)
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

    override fun updateLocationForActiveWalk(requestId: String, lat: Double, lng: Double): Flow<Result<Unit>> = flow {
        Log.d("PetsitterRepo", "Updating petsitter location in active_walks/$requestId: lat=$lat, lng=$lng")
        val result = realtimeDataSource.updateActiveWalkPetsitterLocation(requestId, lat, lng)
        if (result.isSuccess) {
            Log.d("PetsitterRepo", "Successfully updated petsitter location in active_walks/$requestId")
        } else {
            Log.e("PetsitterRepo", "Failed to update petsitter location", result.exceptionOrNull())
        }
        emit(result)
    }
}
