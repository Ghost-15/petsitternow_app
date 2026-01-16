package www.com.petsitternow_app.domain.repository

import kotlinx.coroutines.flow.Flow

data class AddPetData(
    val name: String,
    val breed: String,
    val birthDate: String,
    val photos: List<String> = emptyList()
)

data class Pet(
    val id: String,
    val name: String,
    val breed: String,
    val birthDate: String,
    val photos: List<String>,
    val ownerId: String
)

interface PetRepository {
    fun addPet(ownerId: String, petData: AddPetData): Flow<Result<String>>
    fun getPets(ownerId: String): Flow<Result<List<Pet>>>
    fun getPetById(petId: String): Flow<Result<Pet>>
    fun updatePet(petId: String, petData: AddPetData): Flow<Result<Unit>>
    fun deletePet(petId: String): Flow<Result<Unit>>
}
