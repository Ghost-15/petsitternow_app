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
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.repository.WalkRepository
import javax.inject.Inject

/**
 * UI state for walk history screen.
 */
data class WalkHistoryUiState(
    val isLoading: Boolean = false,
    val walks: List<WalkRequest> = emptyList(),
    val error: String? = null
)

/**
 * ViewModel for owner walk history.
 */
@HiltViewModel
class WalkHistoryViewModel @Inject constructor(
    private val walkRepository: WalkRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalkHistoryUiState())
    val uiState: StateFlow<WalkHistoryUiState> = _uiState.asStateFlow()

    init {
        loadWalkHistory()
    }

    fun loadWalkHistory() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            walkRepository.observeWalkHistory(userId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erreur de chargement: ${e.message}"
                    )
                }
                .collectLatest { walks ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        walks = walks,
                        error = null
                    )
                }
        }
    }

    fun refresh() {
        loadWalkHistory()
    }
}
