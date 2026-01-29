package www.com.petsitternow_app.ui.walk

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import www.com.petsitternow_app.data.remote.MapboxDirectionsService
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.RouteInfo
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.WalkRepository
import www.com.petsitternow_app.util.MapboxConfig
import www.com.petsitternow_app.util.TimeFormatter
import javax.inject.Inject

/**
 * UI state for walk tracking.
 */
data class WalkTrackingUiState(
    val activeWalk: ActiveWalk? = null,
    val route: RouteInfo? = null,
    val elapsedTimeSeconds: Long = 0,
    val formattedTime: String = "",
    val isLoading: Boolean = true
)

/**
 * ViewModel for real-time walk tracking.
 * Observes active walk data and manages timer display and route calculation.
 */
@HiltViewModel
class WalkTrackingViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val savedStateHandle: SavedStateHandle,
    private val directionsService: MapboxDirectionsService,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    companion object {
        const val KEY_REQUEST_ID = "requestId"
        private const val ROUTE_UPDATE_DEBOUNCE_MS = 1000L // 1 second
    }

    private val _uiState = MutableStateFlow(WalkTrackingUiState())
    val uiState: StateFlow<WalkTrackingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var routeJob: Job? = null
    private var requestId: String? = null
    private var lastRouteUpdateTime = 0L

    init {
        // Get request ID from saved state or arguments
        savedStateHandle.get<String>(KEY_REQUEST_ID)?.let { id ->
            setRequestId(id)
        }
    }

    /**
     * Set the request ID to track.
     */
    fun setRequestId(id: String) {
        if (requestId == id) return

        requestId = id
        observeActiveWalk(id)
    }

    private fun observeActiveWalk(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            walkRepository.observeActiveWalk(id)
                .catch { /* Ignore errors */ }
                .collectLatest { activeWalk ->
                    _uiState.value = _uiState.value.copy(
                        activeWalk = activeWalk,
                        isLoading = false
                    )

                    // Start/stop timer based on status
                    if (activeWalk?.status?.isWalkingPhase() == true) {
                        startTimer(activeWalk.walkStartedAt)
                    } else {
                        stopTimer()
                    }

                    // Calculate route if needed
                    updateRoute(activeWalk)
                }
        }
    }

    private fun startTimer(walkStartedAt: Long?) {
        if (timerJob?.isActive == true) return
        if (walkStartedAt == null || walkStartedAt == 0L) return

        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - walkStartedAt) / 1000
                _uiState.value = _uiState.value.copy(
                    elapsedTimeSeconds = elapsed,
                    formattedTime = TimeFormatter.formatDurationSeconds(elapsed.toInt())
                )
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Update route based on active walk status and locations.
     */
    private fun updateRoute(activeWalk: ActiveWalk?) {
        if (activeWalk == null) {
            _uiState.value = _uiState.value.copy(route = null)
            return
        }

        val status = activeWalk.status
        val shouldShowRoute = status == WalkStatus.GOING_TO_OWNER ||
                status == WalkStatus.ASSIGNED ||
                status == WalkStatus.RETURNING

        if (!shouldShowRoute) {
            _uiState.value = _uiState.value.copy(route = null)
            return
        }

        val petsitterLocation = activeWalk.petsitterLocation
        val ownerLocation = activeWalk.ownerLocation

        if (petsitterLocation == null || ownerLocation == null) {
            _uiState.value = _uiState.value.copy(route = null)
            return
        }

        // Debounce route updates to avoid too many API calls
        val now = System.currentTimeMillis()
        if (now - lastRouteUpdateTime < ROUTE_UPDATE_DEBOUNCE_MS) {
            return
        }
        lastRouteUpdateTime = now

        // Cancel previous route calculation
        routeJob?.cancel()

        routeJob = viewModelScope.launch {
            val accessToken = MapboxConfig.getAccessToken(context)
            if (accessToken.isEmpty()) {
                _uiState.value = _uiState.value.copy(route = null)
                return@launch
            }

            directionsService.getRoute(petsitterLocation, ownerLocation, accessToken)
                .fold(
                    onSuccess = { route ->
                        _uiState.value = _uiState.value.copy(route = route)
                    },
                    onFailure = {
                        // Silently fail - route is optional
                        _uiState.value = _uiState.value.copy(route = null)
                    }
                )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        routeJob?.cancel()
    }
}
