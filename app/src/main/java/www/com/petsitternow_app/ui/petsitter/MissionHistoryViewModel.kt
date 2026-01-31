package www.com.petsitternow_app.ui.petsitter

import android.util.Log
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
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.PetsitterRepository
import www.com.petsitternow_app.domain.repository.WalkRepository
import javax.inject.Inject

/**
 * UI state for mission history screen.
 */
data class MissionHistoryUiState(
    val isLoading: Boolean = false,
    val missions: List<WalkRequest> = emptyList(),
    val error: String? = null,
    val userRole: String? = null
)

/**
 * ViewModel for history - works for both petsitter and owner.
 */
@HiltViewModel
class MissionHistoryViewModel @Inject constructor(
    private val petsitterRepository: PetsitterRepository,
    private val walkRepository: WalkRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionHistoryUiState())
    val uiState: StateFlow<MissionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Get user role to determine which history to load
            val userRole = authRepository.getUserType()
            Log.d("MissionHistoryVM", "Loading history for role: $userRole, userId: $userId")
            _uiState.value = _uiState.value.copy(userRole = userRole)
            
            when (userRole) {
                "petsitter" -> loadPetsitterHistory(userId)
                "owner" -> loadOwnerHistory(userId)
                else -> {
                    Log.w("MissionHistoryVM", "Unknown role: $userRole, defaulting to owner history")
                    loadOwnerHistory(userId)
                }
            }
        }
    }
    
    private suspend fun loadPetsitterHistory(userId: String) {
        petsitterRepository.observeMissionHistory(userId)
            .catch { e ->
                Log.e("MissionHistoryVM", "Error loading petsitter history", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement: ${e.message}"
                )
            }
            .collectLatest { missions ->
                Log.d("MissionHistoryVM", "Loaded ${missions.size} petsitter missions")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    missions = missions,
                    error = null
                )
            }
    }
    
    private suspend fun loadOwnerHistory(userId: String) {
        walkRepository.observeWalkHistory(userId)
            .catch { e ->
                Log.e("MissionHistoryVM", "Error loading owner history", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erreur de chargement: ${e.message}"
                )
            }
            .collectLatest { walks ->
                Log.d("MissionHistoryVM", "Loaded ${walks.size} owner walks")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    missions = walks,
                    error = null
                )
            }
    }

    fun refresh() {
        loadHistory()
    }
}
