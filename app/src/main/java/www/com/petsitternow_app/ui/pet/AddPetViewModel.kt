package www.com.petsitternow_app.ui.pet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.AddPetData
import www.com.petsitternow_app.domain.repository.PetRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class AddPetState(
    val name: String = "",
    val breed: String = "",
    val birthDate: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

sealed class AddPetNavigation {
    object GoBack : AddPetNavigation()
}

@HiltViewModel
class AddPetViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(AddPetState())
    val state: StateFlow<AddPetState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<AddPetNavigation>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name, error = null)
    }

    fun updateBreed(breed: String) {
        _state.value = _state.value.copy(breed = breed, error = null)
    }

    fun updateBirthDate(birthDate: String) {
        _state.value = _state.value.copy(birthDate = birthDate, error = null)
    }

    private fun validate(): String? {
        val s = _state.value
        return when {
            s.name.trim().isEmpty() -> "Le nom est requis"
            s.name.trim().length < 2 -> "Le nom doit contenir au moins 2 caractères"
            s.breed.isEmpty() -> "La race est requise"
            s.birthDate.isEmpty() -> "La date de naissance est requise"
            !isValidDate(s.birthDate) -> "La date de naissance n'est pas valide"
            else -> null
        }
    }

    private fun isValidDate(dateString: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString)
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 23)
            today.set(Calendar.MINUTE, 59)
            today.set(Calendar.SECOND, 59)
            date != null && date <= today.time
        } catch (e: Exception) {
            false
        }
    }

    fun submitPet() {
        val error = validate()
        if (error != null) {
            _state.value = _state.value.copy(error = error)
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            _state.value = _state.value.copy(error = "Utilisateur non connecté")
            return
        }

        val s = _state.value
        _state.value = s.copy(isLoading = true, error = null)

        viewModelScope.launch {
            petRepository.addPet(
                ownerId = userId,
                petData = AddPetData(
                    name = s.name.trim(),
                    breed = s.breed,
                    birthDate = s.birthDate,
                    photos = emptyList()
                )
            ).collect { result ->
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    _navigationEvent.emit(AddPetNavigation.GoBack)
                } else {
                    val exception = result.exceptionOrNull()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = exception?.message ?: "Erreur lors de l'ajout de l'animal"
                    )
                }
            }
        }
    }
}
