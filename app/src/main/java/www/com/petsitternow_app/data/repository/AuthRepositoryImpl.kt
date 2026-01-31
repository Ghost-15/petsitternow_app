package www.com.petsitternow_app.data.repository

import android.util.Log
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override fun loginUser(email: String, password: String): Flow<Result<AuthResult>> = flow {
        try {
            emit(Result.success(auth.signInWithEmailAndPassword(email, password).await()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun createUser(email: String, password: String): Flow<Result<AuthResult>> = flow {
        try {
            emit(Result.success(auth.createUserWithEmailAndPassword(email, password).await()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun signInWithGoogle(idToken: String): Flow<Result<AuthResult>> = flow {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun isUserAuthenticated(): Boolean = auth.currentUser != null

    override fun logout() = auth.signOut()

    override suspend fun isOnboardingCompleted(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            val tokenResult = user.getIdToken(false).await()
            val claims = tokenResult.claims
            claims["onboardingCompleted"] as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun refreshToken() {
        auth.currentUser?.getIdToken(true)?.await()
    }

    override suspend fun getUserType(): String? {
        val user = auth.currentUser ?: return null
        return try {
            // Force refresh to get latest custom claims (e.g. after onboarding)
            val tokenResult = user.getIdToken(true).await()
            val claims = tokenResult.claims
            Log.d("AuthRepository", "Custom claims keys: ${claims?.keys?.joinToString() ?: "null"}")
            val userType = claims["role"] as? String
            Log.d("AuthRepository", "userType from claims (role): $userType")
            userType
        } catch (e: Exception) {
            Log.e("AuthRepository", "getUserType failed", e)
            null
        }
    }
}
