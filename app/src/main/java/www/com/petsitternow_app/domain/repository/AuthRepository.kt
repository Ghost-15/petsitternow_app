package www.com.petsitternow_app.domain.repository

import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun loginUser(email: String, password: String): Flow<Result<AuthResult>>
    fun createUser(email: String, password: String): Flow<Result<AuthResult>>
    fun firebaseSignInWithGoogle(idToken: String): Flow<Result<AuthResult>>
    fun isUserAuthenticated(): Boolean
    fun logout()
}
