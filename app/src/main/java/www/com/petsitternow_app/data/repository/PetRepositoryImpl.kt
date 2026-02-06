package www.com.petsitternow_app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.AddPetData
import www.com.petsitternow_app.domain.repository.Pet
import www.com.petsitternow_app.domain.repository.PetRepository
import javax.inject.Inject

class PetRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PetRepository {

    override fun addPet(ownerId: String, petData: AddPetData): Flow<Result<String>> = flow {
        try {
            val petDocument = mapOf(
                "name" to petData.name.trim(),
                "breed" to petData.breed,
                "birthDate" to petData.birthDate,
                "photos" to petData.photos,
                "ownerId" to ownerId,
                "isActive" to true,
                "createdAt" to System.currentTimeMillis()
            )

            val docRef = firestore.collection("pets")
                .add(petDocument)
                .await()

            emit(Result.success(docRef.id))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getPets(ownerId: String): Flow<Result<List<Pet>>> = flow {
        try {
            val snapshot = firestore.collection("pets")
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val pets = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Pet(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    breed = data["breed"] as? String ?: "",
                    birthDate = data["birthDate"] as? String ?: "",
                    photos = (data["photos"] as? List<String>) ?: emptyList(),
                    ownerId = data["ownerId"] as? String ?: ownerId,
                    isActive = data["isActive"] as? Boolean ?: true
                )
            }
            emit(Result.success(pets))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getPetById(petId: String): Flow<Result<Pet>> = flow {
        try {
            val doc = firestore.collection("pets")
                .document(petId)
                .get()
                .await()

            val data = doc.data
            if (data == null) {
                emit(Result.failure(Exception("Animal non trouvé")))
                return@flow
            }

            val pet = Pet(
                id = doc.id,
                name = data["name"] as? String ?: "",
                breed = data["breed"] as? String ?: "",
                birthDate = data["birthDate"] as? String ?: "",
                photos = (data["photos"] as? List<String>) ?: emptyList(),
                ownerId = data["ownerId"] as? String ?: "",
                isActive = data["isActive"] as? Boolean ?: true
            )
            emit(Result.success(pet))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun updatePet(petId: String, petData: AddPetData): Flow<Result<Unit>> = flow {
        try {
            val updateData = mapOf(
                "name" to petData.name.trim(),
                "breed" to petData.breed,
                "birthDate" to petData.birthDate,
                "photos" to petData.photos,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("pets")
                .document(petId)
                .update(updateData)
                .await()

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun deletePet(petId: String): Flow<Result<Unit>> = flow {
        try {
            firestore.collection("pets")
                .document(petId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
