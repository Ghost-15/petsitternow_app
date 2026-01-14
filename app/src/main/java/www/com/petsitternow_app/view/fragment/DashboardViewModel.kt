package www.com.petsitternow_app.view.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

data class DashboardState(
    val isLoading: Boolean = true,
    val userType: String? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadUserType()
    }

    private fun loadUserType() {
        viewModelScope.launch {
            try {
                authRepository.refreshToken()
                val userType = authRepository.getUserType()
                _state.value = _state.value.copy(
                    isLoading = false,
                    userType = userType
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erreur lors du chargement"
                )
            }
        }
    }

    fun isPetsitter(): Boolean {
        return _state.value.userType == "petsitter"
    }

    fun isOwner(): Boolean {
        return _state.value.userType == "owner"
    }
}
