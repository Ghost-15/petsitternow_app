package www.com.petsitternow_app.ui.pet

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.AddPetData
import www.com.petsitternow_app.domain.repository.PetRepository
import www.com.petsitternow_app.util.ImageCompressor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class EditPetState(
    val petId: String = "",
    val name: String = "",
    val breed: String = "",
    val birthDate: String = "",
    val existingPhotos: List<String> = emptyList(),
    val newPhotoUris: List<Uri> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val isDeleted: Boolean = false
)

sealed class EditPetNavigation {
    object GoBack : EditPetNavigation()
}

@HiltViewModel
class EditPetViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(EditPetState())
    val state: StateFlow<EditPetState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<EditPetNavigation>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        val petId = savedStateHandle.get<String>("petId") ?: ""
        if (petId.isNotEmpty()) {
            loadPet(petId)
        } else {
            _state.value = _state.value.copy(isLoading = false, error = "ID de l'animal manquant")
        }
    }

    private fun loadPet(petId: String) {
        viewModelScope.launch {
            petRepository.getPetById(petId).collect { result ->
                if (result.isSuccess) {
                    val pet = result.getOrNull()!!
                    _state.value = _state.value.copy(
                        isLoading = false,
                        petId = pet.id,
                        name = pet.name,
                        breed = pet.breed,
                        birthDate = pet.birthDate,
                        existingPhotos = pet.photos
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Erreur lors du chargement"
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name, error = null)
    }

    fun updateBreed(breed: String) {
        _state.value = _state.value.copy(breed = breed, error = null)
    }

    fun updateBirthDate(birthDate: String) {
        _state.value = _state.value.copy(birthDate = birthDate, error = null)
    }

    fun addNewPhotoUris(uris: List<Uri>) {
        val currentUris = _state.value.newPhotoUris.toMutableList()
        currentUris.addAll(uris)
        _state.value = _state.value.copy(newPhotoUris = currentUris, error = null)
    }

    fun removeExistingPhoto(photoUrl: String) {
        val currentPhotos = _state.value.existingPhotos.toMutableList()
        currentPhotos.remove(photoUrl)
        _state.value = _state.value.copy(existingPhotos = currentPhotos)
    }

    fun removeNewPhoto(uri: Uri) {
        val currentUris = _state.value.newPhotoUris.toMutableList()
        currentUris.remove(uri)
        _state.value = _state.value.copy(newPhotoUris = currentUris)
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
        } catch (ignored: Exception) {
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
        _state.value = s.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val photoUrls = s.existingPhotos.toMutableList()

                s.newPhotoUris.forEachIndexed { index, uri ->
                    val compressedBytes = ImageCompressor.compress(context, uri)
                    val timestamp = System.currentTimeMillis()
                    val fileName = "pets/$userId/${timestamp}_${index}_photo.jpg"
                    val storageRef = storage.reference.child(fileName)
                    storageRef.putBytes(compressedBytes).await()
                    val downloadUrl = storageRef.downloadUrl.await()
                    photoUrls.add(downloadUrl.toString())
                }

                petRepository.updatePet(
                    petId = s.petId,
                    petData = AddPetData(
                        name = s.name.trim(),
                        breed = s.breed,
                        birthDate = s.birthDate,
                        photos = photoUrls
                    )
                ).collect { result ->
                    if (result.isSuccess) {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            isSuccess = true
                        )
                        _navigationEvent.emit(EditPetNavigation.GoBack)
                    } else {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Erreur lors de la modification"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Erreur lors de l'upload de l'image"
                )
            }
        }
    }

    fun deletePet() {
        val s = _state.value
        _state.value = s.copy(isSaving = true, error = null)

        viewModelScope.launch {
            petRepository.deletePet(s.petId).collect { result ->
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        isDeleted = true
                    )
                    _navigationEvent.emit(EditPetNavigation.GoBack)
                } else {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = result.exceptionOrNull()?.message ?: "Erreur lors de la suppression"
                    )
                }
            }
        }
    }
}
