package www.com.petsitternow_app.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import www.com.petsitternow_app.data.FakeWalkRepository
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus

/**
 * Unit tests for WalkRepository.
 */
class WalkRepositoryTest {

    private lateinit var repository: FakeWalkRepository

    @Before
    fun setup() {
        repository = FakeWalkRepository()
    }

    // ===== createWalkRequest tests =====

    @Test
    fun `createWalkRequest with valid data returns success`() = runTest {
        val petIds = listOf("pet1", "pet2")
        val location = WalkLocation(lat = 48.8566, lng = 2.3522, address = "Paris")
        val duration = "30"

        val result = repository.createWalkRequest(petIds, duration, location).first()

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertTrue(result.getOrNull()!!.startsWith("walk_"))
    }

    @Test
    fun `createWalkRequest with empty petIds returns failure`() = runTest {
        val petIds = emptyList<String>()
        val location = WalkLocation(lat = 48.8566, lng = 2.3522)
        val duration = "30"

        val result = repository.createWalkRequest(petIds, duration, location).first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("animal") == true)
    }

    @Test
    fun `createWalkRequest when shouldFail is true returns failure`() = runTest {
        repository.shouldFail = true
        repository.failureMessage = "Network error"

        val result = repository.createWalkRequest(
            listOf("pet1"),
            "30",
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `createWalkRequest updates activeWalkRequest flow`() = runTest {
        val petIds = listOf("pet1")
        val location = WalkLocation(lat = 48.8566, lng = 2.3522)

        repository.createWalkRequest(petIds, "30", location).first()

        val activeWalk = repository.observeActiveWalkRequest("test").first()
        assertNotNull(activeWalk)
        assertEquals(petIds, activeWalk!!.petIds)
        assertEquals(WalkStatus.PENDING, activeWalk.status)
    }

    // ===== cancelWalkRequest tests =====

    @Test
    fun `cancelWalkRequest with valid pending request returns success`() = runTest {
        // Create a walk first
        val createResult = repository.createWalkRequest(
            listOf("pet1"),
            "30",
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()
        val requestId = createResult.getOrNull()!!

        val result = repository.cancelWalkRequest(requestId).first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `cancelWalkRequest clears activeWalkRequest flow`() = runTest {
        val createResult = repository.createWalkRequest(
            listOf("pet1"),
            "30",
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()
        val requestId = createResult.getOrNull()!!

        repository.cancelWalkRequest(requestId).first()

        val activeWalk = repository.observeActiveWalkRequest("test").first()
        assertNull(activeWalk)
    }

    @Test
    fun `cancelWalkRequest with non-existent request returns failure`() = runTest {
        val result = repository.cancelWalkRequest("non_existent_id").first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("non trouvee") == true)
    }

    @Test
    fun `cancelWalkRequest with completed request returns failure`() = runTest {
        val completedWalk = WalkRequest(
            id = "completed_walk",
            ownerId = "owner1",
            petIds = listOf("pet1"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.COMPLETED
        )
        repository.setActiveWalkRequest(completedWalk)

        val result = repository.cancelWalkRequest("completed_walk").first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("terminee") == true)
    }

    // ===== dismissWalkRequest tests =====

    @Test
    fun `dismissWalkRequest with failed request returns success`() = runTest {
        val failedWalk = WalkRequest(
            id = "failed_walk",
            ownerId = "owner1",
            petIds = listOf("pet1"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.FAILED
        )
        repository.setActiveWalkRequest(failedWalk)

        val result = repository.dismissWalkRequest("failed_walk").first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `dismissWalkRequest with non-failed request returns failure`() = runTest {
        val pendingWalk = WalkRequest(
            id = "pending_walk",
            ownerId = "owner1",
            petIds = listOf("pet1"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.PENDING
        )
        repository.setActiveWalkRequest(pendingWalk)

        val result = repository.dismissWalkRequest("pending_walk").first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("echouees") == true)
    }

    // ===== observeWalkHistory tests =====

    @Test
    fun `observeWalkHistory returns empty list initially`() = runTest {
        val history = repository.observeWalkHistory("test").first()

        assertTrue(history.isEmpty())
    }

    @Test
    fun `observeWalkHistory returns set walks`() = runTest {
        val walks = listOf(
            WalkRequest(
                id = "walk1",
                ownerId = "owner1",
                petIds = listOf("pet1"),
                location = WalkLocation(lat = 48.8566, lng = 2.3522),
                duration = "30",
                status = WalkStatus.COMPLETED
            ),
            WalkRequest(
                id = "walk2",
                ownerId = "owner1",
                petIds = listOf("pet2"),
                location = WalkLocation(lat = 48.8566, lng = 2.3522),
                duration = "45",
                status = WalkStatus.CANCELLED
            )
        )
        repository.setWalkHistory(walks)

        val history = repository.observeWalkHistory("test").first()

        assertEquals(2, history.size)
        assertEquals("walk1", history[0].id)
        assertEquals("walk2", history[1].id)
    }

    // ===== clear tests =====

    @Test
    fun `clear resets all state`() = runTest {
        repository.createWalkRequest(
            listOf("pet1"),
            "30",
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()
        repository.shouldFail = true

        repository.clear()

        assertNull(repository.observeActiveWalkRequest("test").first())
        assertTrue(repository.observeWalkHistory("test").first().isEmpty())
        assertFalse(repository.shouldFail)
    }
}
