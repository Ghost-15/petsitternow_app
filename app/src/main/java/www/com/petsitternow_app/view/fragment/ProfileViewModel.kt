package www.com.petsitternow_app.view.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
    val isPasswordUser: Boolean = false,
    
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
                val doc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                val tokenResult = user.getIdToken(true).await()
                val claims = tokenResult.claims

                val role = claims["role"]?.toString()
                val onboardingCompleted = when (val value = claims["onboardingCompleted"]) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> null
                }

                val isPasswordUser = user.providerData.any { it.providerId == "password" }

                _state.value = ProfileState(
                    email = user.email ?: "-",
                    isPasswordUser = isPasswordUser,
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
            notificationRepository.clearFcmToken().collect { }
            auth.signOut()
            _logoutEvent.emit(Unit)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val user = auth.currentUser
            if (user == null) {
                _state.value = _state.value.copy(error = "Non connecte", isLoading = false)
                return@launch
            }
            try {
                val uid = user.uid
                val db = firestore
                val rtdb = FirebaseDatabase.getInstance()

                val petsSnapshot = db.collection("pets")
                    .whereEqualTo("ownerId", uid).get().await()
                for (doc in petsSnapshot.documents) {
                    doc.reference.delete().await()
                }

                db.collection("users").document(uid).delete().await()
                try { db.collection("petsitters_profiles").document(uid).delete().await() } catch (ignored: Exception) { }
                try { db.collection("rating_users").document(uid).delete().await() } catch (ignored: Exception) { }

                try { rtdb.reference.child("petsitters_available").child(uid).removeValue().await() } catch (ignored: Exception) { }

                notificationRepository.clearFcmToken().collect { }

                user.delete().await()

                Log.d("ProfileVM", "Account deleted for $uid")
                _logoutEvent.emit(Unit)
            } catch (e: Exception) {
                Log.e("ProfileVM", "Error deleting account", e)
                _state.value = _state.value.copy(
                    error = "Erreur lors de la suppression: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}


