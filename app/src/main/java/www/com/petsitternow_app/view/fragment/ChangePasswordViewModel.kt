package www.com.petsitternow_app.view.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChangePasswordState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        // Validation locale
        val validationError = validate(currentPassword, newPassword, confirmPassword)
        if (validationError != null) {
            _state.value = _state.value.copy(error = validationError)
            return
        }

        val user = auth.currentUser
        if (user == null || user.email == null) {
            _state.value = _state.value.copy(error = "Utilisateur non connecté")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                // 1. Ré-authentification
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).await()

                // 2. Mise à jour du mot de passe
                user.updatePassword(newPassword).await()

                _state.value = _state.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                val message = mapFirebaseError(e)
                _state.value = _state.value.copy(isLoading = false, error = message)
            }
        }
    }

    private fun validate(currentPassword: String, newPassword: String, confirmPassword: String): String? {
        if (currentPassword.isBlank()) return "Le mot de passe actuel est requis"
        if (newPassword.length < 6) return "Le nouveau mot de passe doit contenir au moins 6 caractères"
        if (newPassword != confirmPassword) return "Les mots de passe ne correspondent pas"
        return null
    }

    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: ""
        return when {
            message.contains("INVALID_LOGIN_CREDENTIALS") || message.contains("wrong-password") || message.contains("invalid-credential") ->
                "Mot de passe actuel incorrect"
            message.contains("too-many-requests") ->
                "Trop de tentatives. Veuillez réessayer plus tard."
            message.contains("requires-recent-login") ->
                "Veuillez vous reconnecter avant de changer le mot de passe."
            message.contains("weak-password") ->
                "Le mot de passe doit contenir au moins 6 caractères."
            message.contains("network") ->
                "Erreur de connexion réseau."
            else ->
                "Erreur: ${e.message}"
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
