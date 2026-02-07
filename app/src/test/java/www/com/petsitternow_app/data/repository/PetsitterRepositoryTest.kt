package www.com.petsitternow_app.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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
 * Unit tests for PetsitterRepository.
 */
class PetsitterRepositoryTest {

    private lateinit var repository: FakePetsitterRepository

    @Before
    fun setup() {
        repository = FakePetsitterRepository()
    }

    // ===== goOnline tests =====

    @Test
    fun `goOnline returns success and sets online status`() = runTest {
        val result = repository.goOnline().first()

        assertTrue(result.isSuccess)
        assertTrue(repository.isCurrentlyOnline())
    }

    @Test
    fun `goOnline updates profile flow`() = runTest {
        repository.goOnline().first()

        val profile = repository.observeProfile("test").first()
        assertNotNull(profile)
        assertTrue(profile!!.isOnline)
    }

    @Test
    fun `goOnline when shouldFail returns failure`() = runTest {
        repository.shouldFail = true
        repository.failureMessage = "Location permission denied"

        val result = repository.goOnline().first()

        assertTrue(result.isFailure)
        assertEquals("Location permission denied", result.exceptionOrNull()?.message)
    }

    // ===== goOffline tests =====

    @Test
    fun `goOffline returns success and clears online status`() = runTest {
        repository.goOnline().first()

        val result = repository.goOffline().first()

        assertTrue(result.isSuccess)
        assertFalse(repository.isCurrentlyOnline())
    }

    @Test
    fun `goOffline updates profile flow`() = runTest {
        repository.setProfile(PetsitterProfile(userId = "test", isOnline = true))

        repository.goOffline().first()

        val profile = repository.observeProfile("test").first()
        assertNotNull(profile)
        assertFalse(profile!!.isOnline)
    }

    // ===== acceptMission tests =====

    @Test
    fun `acceptMission with valid mission returns success`() = runTest {
        val mission = PetsitterMission(
            requestId = "mission1",
            ownerId = "owner1",
            ownerName = "John Doe",
            petNames = listOf("Max"),
            duration = "30",
            distance = 1.5,
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            expiresAt = System.currentTimeMillis() + 30000
        )
        repository.setPendingMission(mission)

        val result = repository.acceptMission("mission1").first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `acceptMission clears pending mission and sets active mission`() = runTest {
        val mission = PetsitterMission(
            requestId = "mission1",
            ownerId = "owner1",
            ownerName = "John Doe",
            petNames = listOf("Max"),
            duration = "30",
            distance = 1.5,
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            expiresAt = System.currentTimeMillis() + 30000
        )
        repository.setPendingMission(mission)

        repository.acceptMission("mission1").first()

        assertNull(repository.observePendingMission("test").first())
        val activeMission = repository.observeActiveMission("test").first()
        assertNotNull(activeMission)
        assertEquals("mission1", activeMission!!.id)
        assertEquals(WalkStatus.ASSIGNED, activeMission.status)
    }

    @Test
    fun `acceptMission with wrong requestId returns failure`() = runTest {
        val mission = PetsitterMission(
            requestId = "mission1",
            ownerId = "owner1",
            ownerName = "John Doe",
            petNames = listOf("Max"),
            duration = "30",
            distance = 1.5,
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            expiresAt = System.currentTimeMillis() + 30000
        )
        repository.setPendingMission(mission)

        val result = repository.acceptMission("wrong_id").first()

        assertTrue(result.isFailure)
    }

    // ===== declineMission tests =====

    @Test
    fun `declineMission clears pending mission`() = runTest {
        val mission = PetsitterMission(
            requestId = "mission1",
            ownerId = "owner1",
            ownerName = "John Doe",
            petNames = listOf("Max"),
            duration = "30",
            distance = 1.5,
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            expiresAt = System.currentTimeMillis() + 30000
        )
        repository.setPendingMission(mission)

        repository.declineMission("mission1").first()

        assertNull(repository.observePendingMission("test").first())
    }

    // ===== startWalk tests =====

    @Test
    fun `startWalk with assigned mission returns success`() = runTest {
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.ASSIGNED,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        val result = repository.startWalk("mission1").first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `startWalk updates status to WALKING`() = runTest {
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.ASSIGNED,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        repository.startWalk("mission1").first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.WALKING, updated?.status)
    }

    @Test
    fun `startWalk with wrong status returns failure`() = runTest {
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.WALKING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        val result = repository.startWalk("mission1").first()

        assertTrue(result.isFailure)
    }

    // ===== markReturning tests =====

    @Test
    fun `markReturning with walking mission returns success`() = runTest {
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.WALKING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        val result = repository.markReturning("mission1").first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `markReturning updates status to RETURNING`() = runTest {
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.WALKING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        repository.markReturning("mission1").first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.RETURNING, updated?.status)
    }

    // ===== completeWalk tests =====

    @Test
    fun `completeWalk within range returns success`() = runTest {
        val ownerLocation = WalkLocation(lat = 48.8566, lng = 2.3522)
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = ownerLocation,
            duration = "30",
            status = WalkStatus.RETURNING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        // Current location is very close to owner
        val currentLocation = WalkLocation(lat = 48.8566, lng = 2.3523)

        val result = repository.completeWalk("mission1", currentLocation).first()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `completeWalk updates status to COMPLETED`() = runTest {
        val ownerLocation = WalkLocation(lat = 48.8566, lng = 2.3522)
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = ownerLocation,
            duration = "30",
            status = WalkStatus.RETURNING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        repository.completeWalk("mission1", ownerLocation).first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.COMPLETED, updated?.status)
        assertNotNull(updated?.completedAt)
    }

    @Test
    fun `completeWalk outside range returns failure`() = runTest {
        val ownerLocation = WalkLocation(lat = 48.8566, lng = 2.3522)
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = ownerLocation,
            duration = "30",
            status = WalkStatus.RETURNING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        // Current location is far from owner (about 10km away)
        val currentLocation = WalkLocation(lat = 48.9566, lng = 2.3522)

        val result = repository.completeWalk("mission1", currentLocation).first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("100m") == true)
    }

    @Test
    fun `completeWalk with wrong status returns failure`() = runTest {
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.WALKING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        val result = repository.completeWalk(
            "mission1",
            WalkLocation(lat = 48.8566, lng = 2.3522)
        ).first()

        assertTrue(result.isFailure)
    }

    // ===== updateLocation tests =====

    @Test
    fun `updateLocation stores current location`() = runTest {
        repository.updateLocation(48.8566, 2.3522).first()

        val location = repository.getCurrentLocation()
        assertNotNull(location)
        assertEquals(48.8566, location!!.lat, 0.0001)
        assertEquals(2.3522, location.lng, 0.0001)
    }

    // ===== cancelMission tests =====

    @Test
    fun `cancelMission updates status to CANCELLED`() = runTest {
        val activeMission = WalkRequest(
            id = "mission1",
            owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
            location = WalkLocation(lat = 48.8566, lng = 2.3522),
            duration = "30",
            status = WalkStatus.WALKING,
            petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
        )
        repository.setActiveMission(activeMission)

        repository.cancelMission("mission1").first()

        val updated = repository.observeActiveMission("test").first()
        assertEquals(WalkStatus.CANCELLED, updated?.status)
    }

    // ===== observeMissionHistory tests =====

    @Test
    fun `observeMissionHistory returns set missions`() = runTest {
        val missions = listOf(
            WalkRequest(
                id = "m1",
                owner = OwnerInfo(id = "owner1", firstName = "Test", lastName = "Owner", name = "Test Owner", pets = listOf(PetInfo(id = "pet1", name = "Rex"))),
                location = WalkLocation(lat = 48.8566, lng = 2.3522),
                duration = "30",
                status = WalkStatus.COMPLETED,
                petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
            ),
            WalkRequest(
                id = "m2",
                owner = OwnerInfo(id = "owner2", firstName = "Test2", lastName = "Owner2", name = "Test2 Owner2", pets = listOf(PetInfo(id = "pet2", name = "Buddy"))),
                location = WalkLocation(lat = 48.8566, lng = 2.3522),
                duration = "45",
                status = WalkStatus.COMPLETED,
                petsitter = PetsitterInfo(id = "petsitter1", name = "Test Petsitter")
            )
        )
        repository.setMissionHistory(missions)

        val history = repository.observeMissionHistory("test").first()

        assertEquals(2, history.size)
    }

    // ===== clear tests =====

    @Test
    fun `clear resets all state`() = runTest {
        repository.goOnline().first()
        repository.updateLocation(48.8566, 2.3522).first()
        repository.shouldFail = true

        repository.clear()

        assertFalse(repository.isCurrentlyOnline())
        assertNull(repository.getCurrentLocation())
        assertFalse(repository.shouldFail)
    }
}
