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
            android.util.Log.d("PetRepo", "Fetching pets for ownerId: $ownerId")
            val snapshot = firestore.collection("pets")
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()

            android.util.Log.d("PetRepo", "Found ${snapshot.documents.size} documents")
            val pets = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                android.util.Log.d("PetRepo", "Doc: ${doc.id}, data: $data")
                Pet(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    breed = data["breed"] as? String ?: "",
                    birthDate = data["birthDate"] as? String ?: "",
                    photos = (data["photos"] as? List<String>) ?: emptyList(),
                    ownerId = data["ownerId"] as? String ?: ownerId
                )
            }
            emit(Result.success(pets))
        } catch (e: Exception) {
            android.util.Log.e("PetRepo", "Error fetching pets", e)
            emit(Result.failure(e))
        }
    }
}
