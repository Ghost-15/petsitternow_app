package www.com.petsitternow_app.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import www.com.petsitternow_app.data.datasource.WalkFirestoreDataSource
import www.com.petsitternow_app.data.datasource.WalkRealtimeDataSource
import www.com.petsitternow_app.domain.model.ActiveWalk
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

        val result = firestoreDataSource.createWalkRequest(
            ownerId = currentUserId,
            petIds = petIds,
            duration = duration,
            location = location
        )

        emit(result)
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
        if (request.ownerId != currentUserId) {
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
        if (request.ownerId != currentUserId) {
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
}
