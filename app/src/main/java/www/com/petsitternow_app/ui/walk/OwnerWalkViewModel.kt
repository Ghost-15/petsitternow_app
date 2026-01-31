package www.com.petsitternow_app.ui.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.Pet
import www.com.petsitternow_app.domain.repository.PetRepository
import www.com.petsitternow_app.domain.repository.WalkRepository
import www.com.petsitternow_app.util.LocationProvider
import javax.inject.Inject

/**
 * UI state for owner walk screen.
 */
data class OwnerWalkUiState(
    val isLoading: Boolean = false,
    val pets: List<Pet> = emptyList(),
    val activeWalk: WalkRequest? = null,
    val activeWalkDetails: ActiveWalk? = null,
    val error: String? = null,
    val isRequestingWalk: Boolean = false
)

/**
 * ViewModel for owner walk operations.
 * Handles creating walk requests, tracking active walks, and history.
 */
@HiltViewModel
class OwnerWalkViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val petRepository: PetRepository,
    private val locationProvider: LocationProvider,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(OwnerWalkUiState())
    val uiState: StateFlow<OwnerWalkUiState> = _uiState.asStateFlow()

    init {
        loadPets()
        observeActiveWalk()
    }

    private fun loadPets() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            petRepository.getPets(userId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur de chargement: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    result.fold(
                        onSuccess = { pets ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                pets = pets,
                                error = null
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Erreur de chargement: ${e.message}"
                            )
                        }
                    )
                }
        }
    }

    private fun observeActiveWalk() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            walkRepository.observeActiveWalkRequest(userId)
                .catch { /* Ignore errors for observation */ }
                .collectLatest { walkRequest ->
                    _uiState.value = _uiState.value.copy(
                        activeWalk = walkRequest,
                        error = null
                    )

                    // If we have an active walk, observe its real-time details
                    walkRequest?.id?.let { requestId ->
                        observeActiveWalkDetails(requestId)
                    }
                }
        }
    }

    private fun observeActiveWalkDetails(requestId: String) {
        viewModelScope.launch {
            walkRepository.observeActiveWalk(requestId)
                .catch { /* Ignore errors */ }
                .collectLatest { activeWalk ->
                    _uiState.value = _uiState.value.copy(
                        activeWalkDetails = activeWalk
                    )
                }
        }
    }

    /**
     * Request a new walk.
     */
    fun requestWalk(petIds: List<String>, duration: String, location: WalkLocation) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRequestingWalk = true, error = null)

            walkRepository.createWalkRequest(petIds, duration, location)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isRequestingWalk = false,
                        error = "Erreur: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isRequestingWalk = false,
                                error = null
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isRequestingWalk = false,
                                error = e.message ?: "Erreur inconnue"
                            )
                        }
                    )
                }
        }
    }

    /**
     * Cancel the active walk request.
     */
    fun cancelWalkRequest() {
        val requestId = _uiState.value.activeWalk?.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            walkRepository.cancelWalkRequest(requestId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur d'annulation: ${e.message}"
                    )
                }
                .collectLatest { result ->
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = null
                            )
                        },
                        onFailure = { e ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = e.message ?: "Erreur d'annulation"
                            )
                        }
                    )
                }
        }
    }

    /**
     * Dismiss a failed walk request.
     */
    fun dismissFailedRequest() {
        val requestId = _uiState.value.activeWalk?.id ?: return

        viewModelScope.launch {
            walkRepository.dismissWalkRequest(requestId)
                .catch { /* Ignore */ }
                .collectLatest { /* Request will be removed from observation */ }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Get current location.
     */
    suspend fun getCurrentLocation(): WalkLocation? {
        return try {
            locationProvider.requestSingleLocation()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Erreur de localisation: ${e.message}"
            )
            null
        }
    }

    /**
     * Check if location permission is available.
     */
    fun hasLocationPermission(): Boolean {
        return locationProvider.hasLocationPermission()
    }
}
