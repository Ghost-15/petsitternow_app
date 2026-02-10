package www.com.petsitternow_app.view.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.UserRepository
import javax.inject.Inject

data class EditProfileState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    fun updateProfile(
        firstName: String,
        lastName: String,
        phone: String,
        gender: String,
        dateOfBirth: String,
        address: String,
        city: String,
        codePostal: String
    ) {
        val validationError = validate(firstName, lastName, phone, gender, dateOfBirth, address, city, codePostal)
        if (validationError != null) {
            _state.value = _state.value.copy(error = validationError)
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.value = _state.value.copy(error = "Utilisateur non connecté")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            userRepository.updateProfileData(
                userId = userId,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phone = phone.trim(),
                gender = gender,
                dateOfBirth = dateOfBirth,
                address = address.trim(),
                city = city.trim(),
                codePostal = codePostal.trim()
            ).collect { result ->
                result.onSuccess {
                    _state.value = _state.value.copy(isLoading = false, isSuccess = true)
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Erreur lors de la sauvegarde: ${e.message}"
                    )
                }
            }
        }
    }

    private fun validate(
        firstName: String,
        lastName: String,
        phone: String,
        gender: String,
        dateOfBirth: String,
        address: String,
        city: String,
        codePostal: String
    ): String? {
        if (firstName.trim().length < 2) return "Le prénom doit contenir au moins 2 caractères"
        if (lastName.trim().length < 2) return "Le nom doit contenir au moins 2 caractères"
        if (!Regex("^0[1-9][0-9]{8}$").matches(phone.trim())) return "Le numéro doit commencer par 0 et contenir 10 chiffres"
        if (gender !in listOf("homme", "femme", "autre")) return "Genre invalide"
        if (dateOfBirth.isBlank()) return "La date de naissance est requise"
        if (address.trim().length < 5) return "L'adresse doit contenir au moins 5 caractères"
        if (city.trim().length < 2) return "La ville doit contenir au moins 2 caractères"
        if (!Regex("^[0-9]{5}$").matches(codePostal.trim())) return "Le code postal doit contenir 5 chiffres"
        return null
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
