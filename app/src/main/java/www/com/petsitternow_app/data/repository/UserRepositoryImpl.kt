package www.com.petsitternow_app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.UserRepository
import java.time.Instant
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun saveOnboardingData(
        userId: String,
        firstName: String,
        lastName: String,
        phone: String,
        gender: String,
        dateOfBirth: String,
        userType: String,
        address: String,
        city: String,
        codePostal: String
    ): Flow<Result<Unit>> = flow {
        try {
            val data = mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "phone" to phone,
                "gender" to gender,
                "dateOfBirth" to dateOfBirth,
                "userType" to userType,
                "address" to address,
                "city" to city,
                "codePostal" to codePostal,
                "onboardingCompleted" to true,
                "updatedAt" to Instant.now().toString()
            )

            firestore.collection("users")
                .document(userId)
                .update(data)
                .await()

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}

