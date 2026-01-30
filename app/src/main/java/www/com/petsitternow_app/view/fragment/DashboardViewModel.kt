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
import kotlinx.coroutines.flow.catch
import www.com.petsitternow_app.domain.model.PetsitterProfile
import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.FeatureFlagRepository
import www.com.petsitternow_app.domain.repository.Pet
import www.com.petsitternow_app.domain.repository.PetRepository
import www.com.petsitternow_app.domain.repository.PetsitterRepository
import javax.inject.Inject

data class DashboardState(
    val isLoading: Boolean = true,
    val userType: String? = null,
    val pets: List<Pet> = emptyList(),
    val petsitterProfile: PetsitterProfile? = null,
    val completedMissionsCount: Int = 0,
    val error: String? = null,
    val ownerPathEnabled: Boolean = true,
    val petsitterPathEnabled: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository,
    private val petsitterRepository: PetsitterRepository,
    private val featureFlagRepository: FeatureFlagRepository,
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
                featureFlagRepository.fetchAndActivate()
                val userType = authRepository.getUserType()
                val ownerPathEnabled = featureFlagRepository.isOwnerPathEnabled()
                val petsitterPathEnabled = featureFlagRepository.isPetsitterPathEnabled()
                Log.d("DashboardVM", "userType: $userType (raw), ownerPath: $ownerPathEnabled, petsitterPath: $petsitterPathEnabled")
                if (userType == null) {
                    Log.w("DashboardVM", "getUserType() returned null - check Firebase custom claims for userType")
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    userType = userType,
                    ownerPathEnabled = ownerPathEnabled,
                    petsitterPathEnabled = petsitterPathEnabled
                )
                when (userType) {
                    "owner" -> loadPets()
                    "petsitter" -> observePetsitterProfile()
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

    private fun observePetsitterProfile() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            petsitterRepository.observeProfile(userId)
                .catch { e -> Log.e("DashboardVM", "observeProfile error", e) }
                .collect { profile ->
                    _state.value = _state.value.copy(petsitterProfile = profile)
                }
        }
        // Also load completed missions count
        loadCompletedMissionsCount(userId)
    }
    
    private fun loadCompletedMissionsCount(userId: String) {
        viewModelScope.launch {
            petsitterRepository.observeMissionHistory(userId)
                .catch { e -> Log.e("DashboardVM", "observeMissionHistory error", e) }
                .collect { missions ->
                    val completedCount = missions.count { it.status == www.com.petsitternow_app.domain.model.WalkStatus.COMPLETED }
                    Log.d("DashboardVM", "Completed missions count: $completedCount")
                    _state.value = _state.value.copy(completedMissionsCount = completedCount)
                }
        }
    }
}
