package www.com.petsitternow_app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : NotificationRepository {

    companion object {
        private const val TAG = "NotificationRepository"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_PETSITTERS_PROFILES = "petsitters_profiles"
        private const val FIELD_FCM_TOKEN = "fcmToken"
    }

    override fun saveFcmToken(token: String): Flow<Result<Unit>> = flow {
        val uid = kotlin.runCatching {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        }.getOrNull() ?: run {
            emit(Result.failure(Exception("Non authentifié")))
            return@flow
        }

        try {
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .set(mapOf(FIELD_FCM_TOKEN to token), SetOptions.merge())
                .await()
            Log.d(TAG, "FCM token saved to users/$uid")

            val userType = authRepository.getUserType()
            if (userType == "petsitter") {
                firestore.collection(COLLECTION_PETSITTERS_PROFILES)
                    .document(uid)
                    .set(mapOf(FIELD_FCM_TOKEN to token), SetOptions.merge())
                    .await()
                Log.d(TAG, "FCM token saved to petsitters_profiles/$uid")
            }

            emit(Result.success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
            emit(Result.failure(e))
        }
    }

    override fun clearFcmToken(): Flow<Result<Unit>> = flow {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: run {
            emit(Result.success(Unit))
            return@flow
        }

        try {
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .update(FIELD_FCM_TOKEN, com.google.firebase.firestore.FieldValue.delete())
                .await()

            try {
                firestore.collection(COLLECTION_PETSITTERS_PROFILES)
                    .document(uid)
                    .update(FIELD_FCM_TOKEN, com.google.firebase.firestore.FieldValue.delete())
                    .await()
            } catch (_: Exception) {
                // Document may not exist for owners
            }

            emit(Result.success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear FCM token", e)
            emit(Result.failure(e))
        }
    }
}
