package www.com.petsitternow_app.ui.petsitter

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
import www.com.petsitternow_app.domain.repository.PetsitterRepository
import javax.inject.Inject

/**
 * UI state for mission history screen.
 */
data class MissionHistoryUiState(
    val isLoading: Boolean = false,
    val missions: List<WalkRequest> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for petsitter mission history.
 */
@HiltViewModel
class MissionHistoryViewModel @Inject constructor(
    private val petsitterRepository: PetsitterRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionHistoryUiState())
    val uiState: StateFlow<MissionHistoryUiState> = _uiState.asStateFlow()

    init {
        loadMissionHistory()
    }

    fun loadMissionHistory() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            petsitterRepository.observeMissionHistory(userId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur de chargement: ${e.message}"
                    )
                }
                .collectLatest { missions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        missions = missions,
                        error = null
                    )
                }
        }
    }

    fun refresh() {
        loadMissionHistory()
    }
}
