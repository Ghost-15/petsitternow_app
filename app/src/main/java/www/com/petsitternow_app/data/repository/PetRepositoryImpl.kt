package www.com.petsitternow_app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.AddPetData
import www.com.petsitternow_app.domain.repository.PetRepository
import java.util.UUID
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
}
