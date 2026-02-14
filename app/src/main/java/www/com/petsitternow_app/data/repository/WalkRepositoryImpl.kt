package www.com.petsitternow_app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.data.datasource.WalkFirestoreDataSource
import www.com.petsitternow_app.data.datasource.WalkRealtimeDataSource
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.OwnerInfo
import www.com.petsitternow_app.domain.model.PetInfo
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.WalkRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of WalkRepository for owner-side operations.
 */
@Singleton
class WalkRepositoryImpl @Inject constructor(
    private val firestoreDataSource: WalkFirestoreDataSource,
    private val realtimeDataSource: WalkRealtimeDataSource,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : WalkRepository {

    override fun createWalkRequest(
        petIds: List<String>,
        duration: String,
        location: WalkLocation
    ): Flow<Result<String>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Validate inputs
        if (petIds.isEmpty()) {
            emit(Result.failure(Exception("Au moins un chien est requis")))
            return@flow
        }

        if (duration !in listOf("30", "45", "60")) {
            emit(Result.failure(Exception("Durée invalide")))
            return@flow
        }

        if (location.lat == 0.0 && location.lng == 0.0) {
            emit(Result.failure(Exception("Position invalide")))
            return@flow
        }

        try {
            // Fetch user profile to build owner info
            val userDoc = firestore.collection("users").document(currentUserId).get().await()
            val firstName = userDoc.getString("firstName") ?: ""
            val lastName = userDoc.getString("lastName") ?: ""
            val name = "$firstName $lastName".trim()

            // Fetch pet docs to build pets list
            val pets = petIds.mapNotNull { petId ->
                try {
                    val petDoc = firestore.collection("pets").document(petId).get().await()
                    val petName = petDoc.getString("name") ?: return@mapNotNull null
                    PetInfo(id = petId, name = petName)
                } catch (e: Exception) {
                    Log.e("WalkRepoImpl", "Error fetching pet $petId", e)
                    null
                }
            }

            if (pets.isEmpty()) {
                emit(Result.failure(Exception("Impossible de récupérer les informations des animaux")))
                return@flow
            }

            val owner = OwnerInfo(
                id = currentUserId,
                firstName = firstName,
                lastName = lastName,
                name = name,
                pets = pets
            )

            val result = firestoreDataSource.createWalkRequest(
                owner = owner,
                duration = duration,
                location = location
            )

            emit(result)
        } catch (e: Exception) {
            Log.e("WalkRepoImpl", "Error creating walk request", e)
            emit(Result.failure(e))
        }
    }

    override fun cancelWalkRequest(requestId: String): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Get the request to verify ownership
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }

        val request = requestResult.getOrNull()
        if (request == null) {
            emit(Result.failure(Exception("Demande introuvable")))
            return@flow
        }

        // Verify ownership
        if (request.owner.id != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }

        // Verify not in final status
        if (request.status.isFinal()) {
            emit(Result.failure(Exception("Cette demande ne peut plus être annulée")))
            return@flow
        }

        // Update status
        val updateResult = firestoreDataSource.updateWalkRequestStatus(requestId, WalkStatus.CANCELLED)
        if (updateResult.isFailure) {
            emit(updateResult)
            return@flow
        }

        // Clean up RTDB active walk
        realtimeDataSource.removeActiveWalk(requestId)

        emit(Result.success(Unit))
    }

    override fun dismissWalkRequest(requestId: String): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        // Get the request to verify ownership and status
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }

        val request = requestResult.getOrNull()
        if (request == null) {
            emit(Result.failure(Exception("Demande introuvable")))
            return@flow
        }

        // Verify ownership
        if (request.owner.id != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }

        // Only FAILED or EXPIRED requests can be dismissed
        if (request.status != WalkStatus.FAILED && request.status != WalkStatus.EXPIRED) {
            emit(Result.failure(Exception("Cette action n'est disponible que pour les demandes en échec")))
            return@flow
        }

        // Update status to DISMISSED
        val updateResult = firestoreDataSource.updateWalkRequestStatus(requestId, WalkStatus.DISMISSED)
        emit(updateResult)
    }

    override fun observeActiveWalkRequest(ownerId: String): Flow<WalkRequest?> {
        return firestoreDataSource.observeActiveWalkRequest(ownerId)
    }

    override fun observeActiveWalk(requestId: String): Flow<ActiveWalk?> {
        return realtimeDataSource.observeActiveWalk(requestId)
    }

    override fun observeWalkHistory(ownerId: String): Flow<List<WalkRequest>> {
        return firestoreDataSource.observeWalkHistory(ownerId)
    }

    override fun submitWalkRating(requestId: String, score: Int, comment: String?): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }
        val request = requestResult.getOrNull() ?: run {
            emit(Result.failure(Exception("Demande introuvable")))
            return@flow
        }
        if (request.owner.id != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }
        if (request.status != WalkStatus.COMPLETED) {
            emit(Result.failure(Exception("Seules les promenades terminées peuvent être notées")))
            return@flow
        }
        if (request.petsitter?.rating != null) {
            emit(Result.failure(Exception("Vous avez déjà noté ce petsitter pour cette promenade")))
            return@flow
        }
        if (request.petsitter?.id.isNullOrBlank()) {
            emit(Result.failure(Exception("Aucun petsitter à noter pour cette promenade")))
            return@flow
        }
        if (score !in 1..5) {
            emit(Result.failure(Exception("La note doit être entre 1 et 5")))
            return@flow
        }
        val commentTrimmed = comment?.take(500)?.trim()
        emit(firestoreDataSource.submitWalkRating(requestId, score, commentTrimmed.let { if (it.isNullOrEmpty()) null else it }))
    }

    override fun submitOwnerRating(requestId: String, score: Int, comment: String?): Flow<Result<Unit>> = flow {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }
        val requestResult = firestoreDataSource.getWalkRequest(requestId)
        if (requestResult.isFailure) {
            emit(Result.failure(requestResult.exceptionOrNull() ?: Exception("Erreur inconnue")))
            return@flow
        }
        val request = requestResult.getOrNull() ?: run {
            emit(Result.failure(Exception("Demande introuvable")))
            return@flow
        }
        if (request.petsitter?.id != currentUserId) {
            emit(Result.failure(Exception("Action non autorisée")))
            return@flow
        }
        if (request.status != WalkStatus.COMPLETED) {
            emit(Result.failure(Exception("Seules les missions terminées peuvent être notées")))
            return@flow
        }
        if (request.owner.rating != null) {
            emit(Result.failure(Exception("Vous avez déjà noté ce propriétaire pour cette mission")))
            return@flow
        }
        if (score !in 1..5) {
            emit(Result.failure(Exception("La note doit être entre 1 et 5")))
            return@flow
        }
        val commentTrimmed = comment?.take(500)?.trim()
        emit(firestoreDataSource.submitOwnerRating(requestId, score, commentTrimmed.let { if (it.isNullOrEmpty()) null else it }))
    }

    override fun observeWalkRequest(requestId: String): Flow<WalkRequest?> = callbackFlow {
        val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("walk_requests")
            .document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                } else {
                    val data = snapshot.data
                    if (data != null) {
                        trySend(WalkRequest.fromMap(snapshot.id, data))
                    } else {
                        trySend(null)
                    }
                }
            }
        awaitClose { listener.remove() }
    }
}
