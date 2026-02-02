package www.com.petsitternow_app.view.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.NotificationRepository
import javax.inject.Inject

data class ProfileState(
    // Firebase Auth
    val email: String = "",
    
    // Firestore (document users/{uid})
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val userType: String = "",
    val address: String = "",
    val city: String = "",
    val codePostal: String = "",
    
    // Custom Claims
    val role: String? = null,
    val onboardingCompleted: Boolean? = null,
    
    // UI State
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val user = auth.currentUser
            if (user == null) {
                _state.value = _state.value.copy(
                    error = "Utilisateur non connecté",
                    isLoading = false
                )
                return@launch
            }

            try {
                // 1. Récupérer les données Firestore
                val doc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                // 2. Récupérer les Custom Claims
                val tokenResult = user.getIdToken(true).await()
                val claims = tokenResult.claims

                val role = claims["role"]?.toString()
                val onboardingCompleted = when (val value = claims["onboardingCompleted"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> null
                }

                // 3. Mettre à jour l'état
                _state.value = ProfileState(
                    email = user.email ?: "-",
                    firstName = doc.getString("firstName") ?: "-",
                    lastName = doc.getString("lastName") ?: "-",
                    phone = doc.getString("phone") ?: "-",
                    gender = doc.getString("gender") ?: "-",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "-",
                    userType = doc.getString("userType") ?: "-",
                    address = doc.getString("address") ?: "-",
                    city = doc.getString("city") ?: "-",
                    codePostal = doc.getString("codePostal") ?: "-",
                    role = role,
                    onboardingCompleted = onboardingCompleted,
                    isLoading = false
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Erreur: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Supprimer le token FCM AVANT de se déconnecter
            notificationRepository.clearFcmToken().collect { }
            auth.signOut()
            _logoutEvent.emit(Unit)
        }
    }
}


