package www.com.petsitternow_app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.PetsitterProfile
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.PetsitterRepository
import www.com.petsitternow_app.util.DistanceCalculator

/**
 * Fake implementation of PetsitterRepository for testing ViewModels.
 */
class FakePetsitterRepository : PetsitterRepository {

    private val profileFlow = MutableStateFlow<PetsitterProfile?>(null)
    private val pendingMissionFlow = MutableStateFlow<PetsitterMission?>(null)
    private val activeMissionFlow = MutableStateFlow<WalkRequest?>(null)
    private val missionHistoryFlow = MutableStateFlow<List<WalkRequest>>(emptyList())

    private var isOnline = false
    private var currentLocation: WalkLocation? = null

    var shouldFail = false
    var failureMessage = "Test error"
    var completionDistanceThreshold = DistanceCalculator.COMPLETION_DISTANCE_THRESHOLD_METERS

    fun setProfile(profile: PetsitterProfile?) {
        profileFlow.value = profile
    }

    fun setPendingMission(mission: PetsitterMission?) {
        pendingMissionFlow.value = mission
    }

    fun setActiveMission(mission: WalkRequest?) {
        activeMissionFlow.value = mission
    }

    fun setMissionHistory(missions: List<WalkRequest>) {
        missionHistoryFlow.value = missions
    }

    override fun observeProfile(petsitterId: String): Flow<PetsitterProfile?> = profileFlow

    override fun observePendingMission(petsitterId: String): Flow<PetsitterMission?> = pendingMissionFlow

    override fun observeActiveMission(petsitterId: String): Flow<WalkRequest?> = activeMissionFlow

    override fun observeMissionHistory(petsitterId: String): Flow<List<WalkRequest>> = missionHistoryFlow

    override fun goOnline(): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        isOnline = true
        val currentProfile = profileFlow.value
        profileFlow.value = currentProfile?.copy(isOnline = true)
            ?: PetsitterProfile(isOnline = true)
        emit(Result.success(Unit))
    }

    override fun goOffline(): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        isOnline = false
        val currentProfile = profileFlow.value
        profileFlow.value = currentProfile?.copy(isOnline = false)
        emit(Result.success(Unit))
    }

    override fun acceptMission(requestId: String): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        val mission = pendingMissionFlow.value
        if (mission == null || mission.requestId != requestId) {
            emit(Result.failure(Exception("Mission non trouvee")))
            return@flow
        }

        // Create active mission from pending
        val activeRequest = WalkRequest(
            id = requestId,
            ownerId = mission.ownerId,
            petIds = emptyList(),
            location = mission.location,
            duration = mission.duration,
            status = WalkStatus.ASSIGNED,
            assignedPetsitterId = "test_petsitter",
            createdAt = System.currentTimeMillis()
        )

        pendingMissionFlow.value = null
        activeMissionFlow.value = activeRequest
        emit(Result.success(Unit))
    }

    override fun declineMission(requestId: String): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        pendingMissionFlow.value = null
        emit(Result.success(Unit))
    }

    override fun updateLocation(lat: Double, lng: Double): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        currentLocation = WalkLocation(lat = lat, lng = lng)
        emit(Result.success(Unit))
    }

    override fun startWalk(requestId: String): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        val current = activeMissionFlow.value
        if (current == null || current.id != requestId) {
            emit(Result.failure(Exception("Mission non trouvee")))
            return@flow
        }

        if (current.status != WalkStatus.ASSIGNED) {
            emit(Result.failure(Exception("Status invalide pour demarrer la promenade")))
            return@flow
        }

        activeMissionFlow.value = current.copy(status = WalkStatus.WALKING)
        emit(Result.success(Unit))
    }

    override fun markReturning(requestId: String): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        val current = activeMissionFlow.value
        if (current == null || current.id != requestId) {
            emit(Result.failure(Exception("Mission non trouvee")))
            return@flow
        }

        if (current.status != WalkStatus.WALKING) {
            emit(Result.failure(Exception("Status invalide pour marquer le retour")))
            return@flow
        }

        activeMissionFlow.value = current.copy(status = WalkStatus.RETURNING)
        emit(Result.success(Unit))
    }

    override fun completeWalk(requestId: String, petsitterLocation: WalkLocation): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        val current = activeMissionFlow.value
        if (current == null || current.id != requestId) {
            emit(Result.failure(Exception("Mission non trouvee")))
            return@flow
        }

        if (current.status != WalkStatus.RETURNING) {
            emit(Result.failure(Exception("Status invalide pour terminer la mission")))
            return@flow
        }

        // Validate distance
        val ownerLocation = current.location
        val distance = DistanceCalculator.calculateDistanceMeters(
            petsitterLocation.lat, petsitterLocation.lng,
            ownerLocation.lat, ownerLocation.lng
        )

        if (distance > completionDistanceThreshold) {
            emit(Result.failure(Exception("Vous devez etre a moins de ${completionDistanceThreshold.toInt()}m du proprietaire")))
            return@flow
        }

        activeMissionFlow.value = current.copy(
            status = WalkStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        emit(Result.success(Unit))
    }

    override fun cancelMission(requestId: String): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        val current = activeMissionFlow.value
        if (current == null || current.id != requestId) {
            emit(Result.failure(Exception("Mission non trouvee")))
            return@flow
        }

        activeMissionFlow.value = current.copy(status = WalkStatus.CANCELLED)
        emit(Result.success(Unit))
    }

    fun isCurrentlyOnline(): Boolean = isOnline

    fun getCurrentLocation(): WalkLocation? = currentLocation

    fun clear() {
        profileFlow.value = null
        pendingMissionFlow.value = null
        activeMissionFlow.value = null
        missionHistoryFlow.value = emptyList()
        isOnline = false
        currentLocation = null
        shouldFail = false
    }
}
