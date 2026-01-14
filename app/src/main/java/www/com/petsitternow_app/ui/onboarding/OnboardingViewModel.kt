package www.com.petsitternow_app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.UserRepository
import javax.inject.Inject


enum class Gender(val value: String, val label: String) {
    HOMME("male", "Homme"),
    FEMME("female", "Femme"),
    AUTRE("other", "Autre")
}


enum class UserType(val value: String) {
    OWNER("owner"),
    PETSITTER("petsitter")
}


data class OnboardingState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val gender: Gender? = null,
    val dateOfBirth: String = "",
    
    val userType: UserType? = null,
    
    val address: String = "",
    val city: String = "",
    val codePostal: String = "",
    
    val error: String? = null,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()


    fun updateFirstName(value: String) {
        _state.value = _state.value.copy(firstName = value, error = null)
    }

    fun updateLastName(value: String) {
        _state.value = _state.value.copy(lastName = value, error = null)
    }

    fun updatePhone(value: String) {
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


    fun submitOnboarding() {
        if (!validateStep3()) return

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.value = _state.value.copy(error = "Utilisateur non connecté")
            return
        }

        val s = _state.value
        _state.value = s.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                userRepository.saveOnboardingData(
                    userId = userId,
                    firstName = s.firstName,
                    lastName = s.lastName,
                    phone = s.phone,
                    gender = s.gender?.value ?: "",
                    dateOfBirth = s.dateOfBirth,
                    userType = s.userType?.value ?: "",
                    address = s.address,
                    city = s.city,
                    codePostal = s.codePostal
                ).collect { result ->
                    result.onSuccess {
                        auth.currentUser?.getIdToken(true)?.await()

                        val data = hashMapOf("userType" to (s.userType?.value ?: ""))
                        FirebaseFunctions.getInstance()
                            .getHttpsCallable("completeOnboarding")
                            .call(data)
                            .await()

                        auth.currentUser?.getIdToken(true)?.await()
                        _state.value = _state.value.copy(isLoading = false, isCompleted = true)

                    }.onFailure { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = e.message ?: "Erreur lors de la sauvegarde"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erreur inattendue"
                )
            }
        }
    }
}

