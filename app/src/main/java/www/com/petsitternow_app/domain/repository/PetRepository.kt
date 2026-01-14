package www.com.petsitternow_app.domain.repository

import kotlinx.coroutines.flow.Flow

data class AddPetData(
    val name: String,
    val breed: String,
    val birthDate: String,
    val photos: List<String> = emptyList()
)

interface PetRepository {
    fun addPet(ownerId: String, petData: AddPetData): Flow<Result<String>>
}
