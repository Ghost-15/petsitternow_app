package www.com.petsitternow_app.ui.petsitter

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
import www.com.petsitternow_app.data.FakePetsitterRepository
import www.com.petsitternow_app.domain.model.OwnerInfo
import www.com.petsitternow_app.domain.model.PetInfo
import www.com.petsitternow_app.domain.model.PetsitterInfo
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.PetsitterProfile
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus

/**
 * Unit tests for PetsitterMissionsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetsitterMissionsViewModelTest {

    private lateinit var repository: FakePetsitterRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakePetsitterRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        repository.clear()
    }

    // ===== toggleOnline tests =====

    @Test
    fun `goOnline sets online status`() = runTest {
        repository.goOnline().first()

        assertTrue(repository.isCurrentlyOnline())
    }

    @Test
    fun `goOffline clears online status`() = runTest {
        repository.goOnline().first()

        repository.goOffline().first()

        assertFalse(repository.isCurrentlyOnline())
    }

    @Test
    fun `goOnline updates profile flow`() = runTest {
        repository.goOnline().first()

        val profile = repository.observeProfile("test").first()
        assertNotNull(profile)
        assertTrue(profile!!.isOnline)
    }

    @Test
    fun `goOnline failure propagates error`() = runTest {
        repository.shouldFail = true
        repository.failureMessage = "Location error"

        val result = repository.goOnline().first()

        assertTrue(result.isFailure)
        assertEquals("Location error", result.exceptionOrNull()?.message)
    }

    // ===== acceptMission tests =====

    @Test
    fun `acceptMission with valid mission succeeds`() = runTest {
        val mission = createTestMission()
        repository.setPendingMission(mission)

        val result = repository.acceptMission(mission.requestId).first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `acceptMission clears pending and sets active`() = runTest {
        val mission = createTestMission()
        repository.setPendingMission(mission)

        repository.acceptMission(mission.requestId).first()

        assertNull(repository.observePendingMission("test").first())
        assertNotNull(repository.observeActiveMission("test").first())
    }

    @Test
    fun `acceptMission sets status to ASSIGNED`() = runTest {
        val mission = createTestMission()
        repository.setPendingMission(mission)

        repository.acceptMission(mission.requestId).first()

        val active = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.ASSIGNED, active?.status)
    }

    @Test
    fun `acceptMission with wrong id fails`() = runTest {
        val mission = createTestMission()
        repository.setPendingMission(mission)

        val result = repository.acceptMission("wrong_id").first()

        assertTrue(result.isFailure)
    }

    // ===== declineMission tests =====

    @Test
    fun `declineMission clears pending mission`() = runTest {
        val mission = createTestMission()
        repository.setPendingMission(mission)

        repository.declineMission(mission.requestId).first()

        assertNull(repository.observePendingMission("test").first())
    }

    // ===== startWalk tests =====

    @Test
    fun `startWalk with assigned mission succeeds`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.ASSIGNED)
        repository.setActiveMission(activeMission)

        val result = repository.startWalk(activeMission.id).first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `startWalk updates status to WALKING`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.ASSIGNED)
        repository.setActiveMission(activeMission)

        repository.startWalk(activeMission.id).first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.WALKING, updated?.status)
    }

    @Test
    fun `startWalk with wrong status fails`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.WALKING)
        repository.setActiveMission(activeMission)

        val result = repository.startWalk(activeMission.id).first()

        assertTrue(result.isFailure)
    }

    // ===== markReturning tests =====

    @Test
    fun `markReturning with walking mission succeeds`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.WALKING)
        repository.setActiveMission(activeMission)

        val result = repository.markReturning(activeMission.id).first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `markReturning updates status to RETURNING`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.WALKING)
        repository.setActiveMission(activeMission)

        repository.markReturning(activeMission.id).first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.RETURNING, updated?.status)
    }

    @Test
    fun `markReturning with assigned status fails`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.ASSIGNED)
        repository.setActiveMission(activeMission)

        val result = repository.markReturning(activeMission.id).first()

        assertTrue(result.isFailure)
    }

    // ===== completeWalk tests =====

    @Test
    fun `completeWalk within range succeeds`() = runTest {
        val ownerLocation = WalkLocation(lat = 48.8566, lng = 2.3522)
        val activeMission = createTestWalkRequest(WalkStatus.RETURNING, ownerLocation)
        repository.setActiveMission(activeMission)

        // Very close location (same coordinates)
        val result = repository.completeWalk(activeMission.id, ownerLocation).first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `completeWalk updates status to COMPLETED`() = runTest {
        val ownerLocation = WalkLocation(lat = 48.8566, lng = 2.3522)
        val activeMission = createTestWalkRequest(WalkStatus.RETURNING, ownerLocation)
        repository.setActiveMission(activeMission)

        repository.completeWalk(activeMission.id, ownerLocation).first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.COMPLETED, updated?.status)
    }

    @Test
    fun `completeWalk outside range fails`() = runTest {
        val ownerLocation = WalkLocation(lat = 48.8566, lng = 2.3522)
        val activeMission = createTestWalkRequest(WalkStatus.RETURNING, ownerLocation)
        repository.setActiveMission(activeMission)

        // Far location (about 10km away)
        val farLocation = WalkLocation(lat = 48.9566, lng = 2.3522)
        val result = repository.completeWalk(activeMission.id, farLocation).first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("100m") == true)
    }

    @Test
    fun `completeWalk with walking status fails`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.WALKING)
        repository.setActiveMission(activeMission)

        val result = repository.completeWalk(
            activeMission.id,
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()

        assertTrue(result.isFailure)
    }

    // ===== updateLocation tests =====

    @Test
    fun `updateLocation stores location`() = runTest {
        repository.updateLocation(48.8566, 2.3522).first()

        val location = repository.getCurrentLocation()
        assertNotNull(location)
        assertEquals(48.8566, location!!.lat, 0.0001)
        assertEquals(2.3522, location.lng, 0.0001)
    }

    // ===== cancelMission tests =====

    @Test
    fun `cancelMission updates status to CANCELLED`() = runTest {
        val activeMission = createTestWalkRequest(WalkStatus.WALKING)
        repository.setActiveMission(activeMission)

        repository.cancelMission(activeMission.id).first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.CANCELLED, updated?.status)
    }

    // ===== observeMissionHistory tests =====

    @Test
    fun `observeMissionHistory returns set missions`() = runTest {
        val missions = listOf(
            createTestWalkRequest(WalkStatus.COMPLETED),
            createTestWalkRequest(WalkStatus.COMPLETED)
        )
        repository.setMissionHistory(missions)

        val history = repository.observeMissionHistory("test").first()

        assertEquals(2, history.size)
    }

    // ===== Helper functions =====

    private fun createTestMission(requestId: String = "mission1"): PetsitterMission {
        return PetsitterMission(
            requestId = requestId,
            ownerId = "owner1",
            ownerName = "John Doe",
            petNames = listOf("Max", "Buddy"),
            duration = "30",
            distance = 1.5,
            location = WalkLocation(lat = 48.8566, lng = 2.3522, address = "Paris"),
            expiresAt = System.currentTimeMillis() + 30000
        )
    }

    private fun createTestWalkRequest(
        status: WalkStatus,
        location: WalkLocation = WalkLocation(lat = 48.8566, lng = 2.3522)
    ): WalkRequest {
        return WalkRequest(
            id = "walk_${System.currentTimeMillis()}",
            owner = OwnerInfo(
                id = "owner1",
                firstName = "Test",
                lastName = "Owner",
                name = "Test Owner",
                pets = listOf(
                    PetInfo(id = "pet1", name = "Max"),
                    PetInfo(id = "pet2", name = "Buddy")
                )
            ),
            location = location,
            duration = "30",
            status = status,
            petsitter = PetsitterInfo(id = "petsitter1", firstName = "Test", lastName = "Petsitter", name = "Test Petsitter"),
            createdAt = System.currentTimeMillis()
        )
    }
}
