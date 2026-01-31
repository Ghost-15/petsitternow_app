package www.com.petsitternow_app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.WalkRepository

/**
 * Fake implementation of WalkRepository for testing ViewModels.
 */
class FakeWalkRepository : WalkRepository {

    private val walkRequests = mutableMapOf<String, WalkRequest>()
    private val activeWalks = mutableMapOf<String, ActiveWalk>()

    private val activeWalkRequestFlow = MutableStateFlow<WalkRequest?>(null)
    private val walkHistoryFlow = MutableStateFlow<List<WalkRequest>>(emptyList())
    private val activeWalkFlows = mutableMapOf<String, MutableStateFlow<ActiveWalk?>>()

    var shouldFail = false
    var failureMessage = "Test error"
    var createWalkDelay = 0L

    fun setActiveWalkRequest(walk: WalkRequest?) {
        activeWalkRequestFlow.value = walk
        walk?.let { walkRequests[it.id] = it }
    }

    fun setWalkHistory(walks: List<WalkRequest>) {
        walkHistoryFlow.value = walks
        walks.forEach { walkRequests[it.id] = it }
    }

    fun setActiveWalk(requestId: String, walk: ActiveWalk?) {
        if (walk != null) {
            activeWalks[requestId] = walk
        } else {
            activeWalks.remove(requestId)
        }
        getActiveWalkFlow(requestId).value = walk
    }

    override fun createWalkRequest(
        petIds: List<String>,
        duration: String,
        location: WalkLocation
    ): Flow<Result<String>> = flow {
        if (createWalkDelay > 0) {
            kotlinx.coroutines.delay(createWalkDelay)
        }

        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        if (petIds.isEmpty()) {
            emit(Result.failure(IllegalArgumentException("Au moins un animal doit etre selectionne")))
            return@flow
        }

        val id = "walk_${System.currentTimeMillis()}"
        val request = WalkRequest(
            id = id,
            ownerId = "test_owner",
            petIds = petIds,
            location = location,
            duration = duration,
            status = WalkStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        walkRequests[id] = request
        activeWalkRequestFlow.value = request
        emit(Result.success(id))
    }

    override fun cancelWalkRequest(requestId: String): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        val request = walkRequests[requestId]
        if (request == null) {
            emit(Result.failure(Exception("Demande non trouvee")))
            return@flow
        }

        val finalStatuses = listOf(
            WalkStatus.COMPLETED,
            WalkStatus.CANCELLED,
            WalkStatus.FAILED,
            WalkStatus.DISMISSED
        )
        if (request.status in finalStatuses) {
            emit(Result.failure(Exception("Impossible d'annuler une demande terminee")))
            return@flow
        }

        val updated = request.copy(status = WalkStatus.CANCELLED, updatedAt = System.currentTimeMillis())
        walkRequests[requestId] = updated
        activeWalkRequestFlow.value = null
        emit(Result.success(Unit))
    }

    override fun dismissWalkRequest(requestId: String): Flow<Result<Unit>> = flow {
        if (shouldFail) {
            emit(Result.failure(Exception(failureMessage)))
            return@flow
        }

        val request = walkRequests[requestId]
        if (request == null) {
            emit(Result.failure(Exception("Demande non trouvee")))
            return@flow
        }

        if (request.status != WalkStatus.FAILED) {
            emit(Result.failure(Exception("Seules les demandes echouees peuvent etre fermees")))
            return@flow
        }

        val updated = request.copy(status = WalkStatus.DISMISSED, updatedAt = System.currentTimeMillis())
        walkRequests[requestId] = updated
        activeWalkRequestFlow.value = null
        emit(Result.success(Unit))
    }

    override fun observeActiveWalkRequest(ownerId: String): Flow<WalkRequest?> = activeWalkRequestFlow

    override fun observeWalkHistory(ownerId: String): Flow<List<WalkRequest>> = walkHistoryFlow

    override fun observeActiveWalk(requestId: String): Flow<ActiveWalk?> = getActiveWalkFlow(requestId)

    private fun getActiveWalkFlow(requestId: String): MutableStateFlow<ActiveWalk?> {
        return activeWalkFlows.getOrPut(requestId) {
            MutableStateFlow(activeWalks[requestId])
        }
    }

    fun clear() {
        walkRequests.clear()
        activeWalks.clear()
        activeWalkRequestFlow.value = null
        walkHistoryFlow.value = emptyList()
        activeWalkFlows.clear()
        shouldFail = false
        createWalkDelay = 0L
    }
}
