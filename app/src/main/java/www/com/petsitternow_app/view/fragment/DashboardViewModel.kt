package www.com.petsitternow_app.view.fragment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.Pet
import www.com.petsitternow_app.domain.repository.PetRepository
import javax.inject.Inject

data class DashboardState(
    val isLoading: Boolean = true,
    val userType: String? = null,
    val pets: List<Pet> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val auth: FirebaseAuth
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
                Log.d("DashboardVM", "userType: $userType")
                _state.value = _state.value.copy(
                    isLoading = false,
                    userType = userType
                )
                if (userType == "owner") {
                    loadPets()
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error loading userType", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erreur lors du chargement"
                )
            }
        }
    }

    fun loadPets() {
        val userId = auth.currentUser?.uid
        Log.d("DashboardVM", "loadPets called, userId: $userId")
        if (userId == null) return
        viewModelScope.launch {
            petRepository.getPets(userId).collect { result ->
                Log.d("DashboardVM", "getPets result: isSuccess=${result.isSuccess}, pets=${result.getOrNull()?.size}")
                if (result.isSuccess) {
                    _state.value = _state.value.copy(pets = result.getOrNull() ?: emptyList())
                } else {
                    Log.e("DashboardVM", "getPets error", result.exceptionOrNull())
                }
            }
        }
    }
}
