package www.com.petsitternow_app.ui.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * États possibles pour le genre
 */
enum class Gender(val value: String, val label: String) {
    HOMME("homme", "Homme"),
    FEMME("femme", "Femme"),
    AUTRE("autre", "Autre")
}

/**
 * États possibles pour le type d'utilisateur
 */
enum class UserType(val value: String) {
    OWNER("owner"),
    PETSITTER("petsitter")
}

/**
 * État complet du formulaire d'onboarding
 */
data class OnboardingState(
    // Step 1 - Informations personnelles
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val gender: Gender? = null,
    val dateOfBirth: String = "",
    
    // Step 2 - Type d'utilisateur
    val userType: UserType? = null,
    
    // Step 3 - Adresse
    val address: String = "",
    val city: String = "",
    val codePostal: String = "",
    
    // UI State
    val currentStep: Int = 1,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    // ==================== Step 1 ====================
    
    fun updateFirstName(value: String) {
        _state.value = _state.value.copy(firstName = value, error = null)
    }

    fun updateLastName(value: String) {
        _state.value = _state.value.copy(lastName = value, error = null)
    }

    fun updatePhone(value: String) {
        // Garde seulement les chiffres, max 10
        val sanitized = value.filter { it.isDigit() }.take(10)
        _state.value = _state.value.copy(phone = sanitized, error = null)
    }

    fun updateGender(gender: Gender) {
        _state.value = _state.value.copy(gender = gender, error = null)
    }

    fun updateDateOfBirth(value: String) {
        _state.value = _state.value.copy(dateOfBirth = value, error = null)
    }

    fun validateStep1(): Boolean {
        val s = _state.value
        val error = when {
            s.firstName.length < 2 -> "Le prénom doit contenir au moins 2 caractères"
            s.lastName.length < 2 -> "Le nom doit contenir au moins 2 caractères"
            !s.phone.matches(Regex("^0[1-9][0-9]{8}$")) -> "Numéro invalide (ex: 0612345678)"
            s.gender == null -> "Veuillez sélectionner un genre"
            s.dateOfBirth.isBlank() -> "Veuillez sélectionner une date de naissance"
            else -> null
        }
        
        _state.value = _state.value.copy(error = error)
        return error == null
    }

    // ==================== Step 2 ====================

    fun updateUserType(userType: UserType) {
        _state.value = _state.value.copy(userType = userType, error = null)
    }

    fun validateStep2(): Boolean {
        val error = if (_state.value.userType == null) {
            "Veuillez sélectionner un type d'utilisateur"
        } else null
        
        _state.value = _state.value.copy(error = error)
        return error == null
    }

    // ==================== Step 3 ====================

    fun updateAddress(value: String) {
        _state.value = _state.value.copy(address = value, error = null)
    }

    fun updateCity(value: String) {
        _state.value = _state.value.copy(city = value, error = null)
    }

    fun updateCodePostal(value: String) {
        val sanitized = value.filter { it.isDigit() }.take(5)
        _state.value = _state.value.copy(codePostal = sanitized, error = null)
    }

    fun validateStep3(): Boolean {
        val s = _state.value
        val error = when {
            s.address.length < 5 -> "L'adresse doit contenir au moins 5 caractères"
            s.city.length < 2 -> "La ville doit contenir au moins 2 caractères"
            !s.codePostal.matches(Regex("^[0-9]{5}$")) -> "Le code postal doit contenir 5 chiffres"
            else -> null
        }
        
        _state.value = _state.value.copy(error = error)
        return error == null
    }

    // ==================== Navigation ====================

    fun goToStep(step: Int) {
        _state.value = _state.value.copy(currentStep = step, error = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ==================== Submit ====================

    fun submitOnboarding() {
        if (!validateStep3()) return
        
        _state.value = _state.value.copy(isLoading = true)
        
        // TODO: Appeler le repository pour sauvegarder les données
        // Pour l'instant, on simule le succès
        _state.value = _state.value.copy(
            isLoading = false,
            isCompleted = true
        )
    }
}

