package www.com.petsitternow_app.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
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

    override fun isUserAuthenticated(): Boolean = auth.currentUser != null

    override fun logout() = auth.signOut()
}
