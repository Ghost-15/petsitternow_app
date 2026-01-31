package www.com.petsitternow_app.ui.walk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import www.com.petsitternow_app.data.FakeWalkRepository
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus

/**
 * Unit tests for OwnerWalkViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OwnerWalkViewModelTest {

    private lateinit var repository: FakeWalkRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWalkRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        repository.clear()
    }

    // ===== requestWalk tests =====

    @Test
    fun `requestWalk with valid data emits success`() = runTest {
        val petIds = listOf("pet1", "pet2")
        val location = WalkLocation(lat = 48.8566, lng = 2.3522)
        val duration = "30"

        val result = repository.createWalkRequest(petIds, duration, location).first()

        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `requestWalk with empty pets emits failure`() = runTest {
        val result = repository.createWalkRequest(
            emptyList(),
            "30",
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()

        assertTrue(result.isFailure)
    }

    @Test
    fun `requestWalk updates activeWalk flow`() = runTest {
        repository.createWalkRequest(
            listOf("pet1"),
            "30",
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()

        val activeWalk = repository.observeActiveWalkRequest("test").first()
        assertNotNull(activeWalk)
        assertEquals(WalkStatus.PENDING, activeWalk?.status)
    }

    // ===== cancelRequest tests =====

    @Test
    fun `cancelRequest clears active walk`() = runTest {
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
    fun `cancelRequest with completed walk fails`() = runTest {
        val completedWalk = WalkRequest(
            id = "completed",
            ownerId = "owner",
            petIds = listOf("pet1"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.COMPLETED
        )
        repository.setActiveWalkRequest(completedWalk)

        val result = repository.cancelWalkRequest("completed").first()

        assertTrue(result.isFailure)
    }

    // ===== dismissRequest tests =====

    @Test
    fun `dismissRequest with failed walk succeeds`() = runTest {
        val failedWalk = WalkRequest(
            id = "failed",
            ownerId = "owner",
            petIds = listOf("pet1"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.FAILED
        )
        repository.setActiveWalkRequest(failedWalk)

        val result = repository.dismissWalkRequest("failed").first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `dismissRequest with pending walk fails`() = runTest {
        val pendingWalk = WalkRequest(
            id = "pending",
            ownerId = "owner",
            petIds = listOf("pet1"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.PENDING
        )
        repository.setActiveWalkRequest(pendingWalk)

        val result = repository.dismissWalkRequest("pending").first()

        assertTrue(result.isFailure)
    }

    // ===== observeActiveWalkRequest tests =====

    @Test
    fun `observeActiveWalkRequest initially returns null`() = runTest {
        val activeWalk = repository.observeActiveWalkRequest("test").first()

        assertNull(activeWalk)
    }

    @Test
    fun `observeActiveWalkRequest returns set walk`() = runTest {
        val walk = WalkRequest(
            id = "walk1",
            ownerId = "owner",
            petIds = listOf("pet1"),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.MATCHING
        )
        repository.setActiveWalkRequest(walk)

        val activeWalk = repository.observeActiveWalkRequest("test").first()

        assertNotNull(activeWalk)
        assertEquals("walk1", activeWalk?.id)
        assertEquals(WalkStatus.MATCHING, activeWalk?.status)
    }

    // ===== observeWalkHistory tests =====

    @Test
    fun `observeWalkHistory initially returns empty`() = runTest {
        val history = repository.observeWalkHistory("test").first()

        assertTrue(history.isEmpty())
    }

    @Test
    fun `observeWalkHistory returns set walks`() = runTest {
        val walks = listOf(
            WalkRequest(
                id = "w1",
                ownerId = "owner",
                petIds = listOf("pet1"),
                location = WalkLocation(lat = 48.8566, lng = 2.3522),
                duration = "30",
                status = WalkStatus.COMPLETED
            ),
            WalkRequest(
                id = "w2",
                ownerId = "owner",
                petIds = listOf("pet2"),
                location = WalkLocation(lat = 48.8566, lng = 2.3522),
                duration = "45",
                status = WalkStatus.CANCELLED
            )
        )
        repository.setWalkHistory(walks)

        val history = repository.observeWalkHistory("test").first()

        assertEquals(2, history.size)
        assertEquals("w1", history[0].id)
        assertEquals("w2", history[1].id)
    }

    // ===== error handling tests =====

    @Test
    fun `repository failure propagates error`() = runTest {
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
}
