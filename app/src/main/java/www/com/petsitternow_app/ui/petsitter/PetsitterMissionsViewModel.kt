package www.com.petsitternow_app.ui.petsitter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.repository.PetsitterRepository
import www.com.petsitternow_app.domain.repository.WalkRepository
import www.com.petsitternow_app.util.LocationProvider
import www.com.petsitternow_app.util.TimeFormatter
import javax.inject.Inject

/**
 * UI state for petsitter missions screen.
 */
data class PetsitterMissionsUiState(
    val isOnline: Boolean = false,
    val isTogglingOnline: Boolean = false,
    val pendingMission: PetsitterMission? = null,
    val missionCountdown: Int = 30,
    val activeMission: WalkRequest? = null,
    val currentLocation: WalkLocation? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val walkElapsedTime: String = "",
    val distanceToOwner: Double? = null,
    val isWithinCompletionRange: Boolean = false
)

/**
 * ViewModel for petsitter missions.
 * Handles online/offline toggle, mission acceptance, and walk progression.
 */
@HiltViewModel
class PetsitterMissionsViewModel @Inject constructor(
    private val petsitterRepository: PetsitterRepository,
    private val walkRepository: WalkRepository,
    private val locationProvider: LocationProvider,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetsitterMissionsUiState())
    val uiState: StateFlow<PetsitterMissionsUiState> = _uiState.asStateFlow()

    private var locationUpdateJob: Job? = null
    private var countdownJob: Job? = null
    private var timerJob: Job? = null
    private var activeWalkJob: Job? = null
    private var currentWalkStartedAt: Long? = null

    init {
        observeProfile()
        observePendingMission()
        observeActiveMission()
    }

    private fun observeProfile() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            petsitterRepository.observeProfile(userId)
                .catch { /* Ignore */ }
                .collectLatest { profile ->
                    _uiState.value = _uiState.value.copy(
                        isOnline = profile?.isOnline ?: false
                    )

                    // Start/stop location updates based on online status OR active mission
                    val hasActiveMission = _uiState.value.activeMission != null
                    if (profile?.isOnline == true || hasActiveMission) {
                        startLocationUpdates()
                    } else {
                        stopLocationUpdates()
                    }
                }
        }
    }

    private fun observePendingMission() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            petsitterRepository.observePendingMission(userId)
                .catch { /* Ignore */ }
                .collectLatest { mission ->
                    _uiState.value = _uiState.value.copy(
                        pendingMission = mission,
                        missionCountdown = mission?.remainingSeconds() ?: 30
                    )

                    // Start countdown if we have a pending mission
                    if (mission != null) {
                        startMissionCountdown(mission)
                    } else {
                        stopMissionCountdown()
                    }
                }
        }
    }

    private fun observeActiveMission() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            petsitterRepository.observeActiveMission(userId)
                .catch { /* Ignore */ }
                .collectLatest { mission ->
                    _uiState.value = _uiState.value.copy(activeMission = mission)

                    if (mission != null) {
                        observeActiveWalkData(mission.id)
                        startLocationUpdates()
                    } else {
                        stopActiveWalkObservation()
                        stopWalkTimer()
                        if (!_uiState.value.isOnline) {
                            stopLocationUpdates()
                        }
                    }
                }
        }
    }

    private fun observeActiveWalkData(requestId: String) {
        activeWalkJob?.cancel()
        activeWalkJob = viewModelScope.launch {
            walkRepository.observeActiveWalk(requestId)
                .catch { /* Ignore */ }
                .collectLatest { activeWalk ->
                    currentWalkStartedAt = activeWalk?.walkStartedAt

                    // Start timer during walking phases
                    if (activeWalk?.status?.isWalkingPhase() == true && currentWalkStartedAt != null) {
                        startWalkTimer()
                    } else {
                        stopWalkTimer()
                    }
                }
        }
    }

    private fun stopActiveWalkObservation() {
        activeWalkJob?.cancel()
        activeWalkJob = null
        currentWalkStartedAt = null
    }

    /**
     * Toggle online/offline status.
     */
    fun toggleOnline(wantsOnline: Boolean? = null) {
        val currentOnline = _uiState.value.isOnline
        val targetOnline = wantsOnline ?: !currentOnline

        // If already in the desired state, do nothing
        if (targetOnline == currentOnline) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTogglingOnline = true, error = null)

            val result = if (targetOnline) {
                petsitterRepository.goOnline()
            } else {
                petsitterRepository.goOffline()
            }

            result.catch { e ->
                _uiState.value = _uiState.value.copy(
                    isTogglingOnline = false,
                    error = "Erreur: ${e.message}"
                )
            }.collectLatest { res ->
                res.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isTogglingOnline = false,
                            isOnline = targetOnline
                        )

                        // If going online, update location immediately
                        if (targetOnline) {
                            updateCurrentLocation()
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isTogglingOnline = false,
                            error = e.message ?: "Erreur"
                        )
                    }
                )
            }
        }
    }

    /**
     * Accept a pending mission.
     */
    fun acceptMission() {
        val requestId = _uiState.value.pendingMission?.requestId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            petsitterRepository.acceptMission(requestId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    result.fold(
                        onSuccess = {
                            android.util.Log.d("PetsitterVM", "acceptMission success, clearing pendingMission")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                pendingMission = null
                            )
                        },
                        onFailure = { e ->
                            android.util.Log.e("PetsitterVM", "acceptMission failed: ${e.message}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = e.message ?: "Erreur"
                            )
                        }
                    )
                }
        }
    }

    /**
     * Decline a pending mission.
     */
    fun declineMission() {
        val requestId = _uiState.value.pendingMission?.requestId ?: return

        viewModelScope.launch {
            petsitterRepository.declineMission(requestId)
                .catch { /* Ignore */ }
                .collectLatest {
                    _uiState.value = _uiState.value.copy(pendingMission = null)
                }
        }
    }

    /**
     * Start the walk (pickup complete).
     */
    fun startWalk() {
        val requestId = _uiState.value.activeMission?.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            petsitterRepository.startWalk(requestId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    result.onFailure { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
        }
    }

    /**
     * Mark walk as returning (walk complete, heading back).
     */
    fun markReturning() {
        val requestId = _uiState.value.activeMission?.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            petsitterRepository.markReturning(requestId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    result.onFailure { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
        }
    }

    /**
     * Complete the mission (returned to owner).
     */
    fun completeMission() {
        val requestId = _uiState.value.activeMission?.id ?: return
        val currentLocation = _uiState.value.currentLocation

        if (currentLocation == null) {
            _uiState.value = _uiState.value.copy(
                error = "Position actuelle indisponible"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            petsitterRepository.completeWalk(requestId, currentLocation)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    result.onFailure { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
        }
    }

    /**
     * Cancel the active mission.
     */
    fun cancelMission() {
        val requestId = _uiState.value.activeMission?.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            petsitterRepository.cancelMission(requestId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur d'annulation: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    result.onFailure { e ->
                        _uiState.value = _uiState.value.copy(error = e.message)
                    }
                }
        }
    }

    private fun startLocationUpdates() {
        if (locationUpdateJob?.isActive == true) return

        locationUpdateJob = viewModelScope.launch {
            locationProvider.observeLocationUpdates()
                .catch { /* Ignore errors */ }
                .collectLatest { location ->
                    _uiState.value = _uiState.value.copy(currentLocation = location)

                    // Update location in Firebase if online
                    if (_uiState.value.isOnline) {
                        petsitterRepository.updateLocation(location.lat, location.lng)
                            .catch { /* Ignore */ }
                            .collectLatest { /* Updated */ }
                    }

                    // Update location in active_walks for owner to see during active mission
                    _uiState.value.activeMission?.let { mission ->
                        petsitterRepository.updateLocationForActiveWalk(mission.id, location.lat, location.lng)
                            .catch { /* Ignore */ }
                            .collectLatest { /* Updated */ }
                    }

                    // Calculate distance to owner if in active mission
                    _uiState.value.activeMission?.location?.let { ownerLocation ->
                        val distance = www.com.petsitternow_app.util.DistanceCalculator
                            .calculateDistanceMeters(
                                location.lat, location.lng,
                                ownerLocation.lat, ownerLocation.lng
                            )
                        val isWithinRange = distance <= www.com.petsitternow_app.util.DistanceCalculator.COMPLETION_DISTANCE_THRESHOLD_METERS
                        android.util.Log.d("PetsitterVM", "Distance to owner: ${distance.toInt()}m, isWithinRange: $isWithinRange, petsitter: (${location.lat}, ${location.lng}), owner: (${ownerLocation.lat}, ${ownerLocation.lng})")
                        _uiState.value = _uiState.value.copy(
                            distanceToOwner = distance,
                            isWithinCompletionRange = isWithinRange
                        )
                    } ?: run {
                        android.util.Log.w("PetsitterVM", "No owner location found in active mission")
                    }
                }
        }
    }

    private fun stopLocationUpdates() {
        locationUpdateJob?.cancel()
        locationUpdateJob = null
    }

    private fun startMissionCountdown(mission: PetsitterMission) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = mission.remainingSeconds()
            while (isActive && remaining > 0) {
                _uiState.value = _uiState.value.copy(missionCountdown = remaining)
                delay(1000)
                remaining--
            }

            // Auto-decline when countdown reaches 0
            if (remaining <= 0 && _uiState.value.pendingMission != null) {
                declineMission()
            }
        }
    }

    private fun stopMissionCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun startWalkTimer() {
        if (timerJob?.isActive == true) return

        timerJob = viewModelScope.launch {
            while (isActive) {
                val startTime = currentWalkStartedAt
                if (startTime != null && startTime > 0) {
                    val formattedTime = TimeFormatter.formatElapsedTime(startTime)
                    _uiState.value = _uiState.value.copy(
                        walkElapsedTime = formattedTime
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        walkElapsedTime = "0s"
                    )
                }
                delay(1000)
            }
        }
    }

    private fun stopWalkTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateCurrentLocation() {
        viewModelScope.launch {
            try {
                val location = locationProvider.requestSingleLocation()
                _uiState.value = _uiState.value.copy(currentLocation = location)

                // Update in Firebase
                petsitterRepository.updateLocation(location.lat, location.lng)
                    .catch { /* Ignore */ }
                    .collectLatest { /* Updated */ }
            } catch (ignored: Exception) {
                // Ignore location errors on initial update
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Check if location permission is available.
     */
    fun hasLocationPermission(): Boolean {
        return locationProvider.hasLocationPermission()
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        stopMissionCountdown()
        stopWalkTimer()
        stopActiveWalkObservation()
    }
}
