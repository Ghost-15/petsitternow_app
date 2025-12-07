package www.com.petsitternow_app.ui.auth


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

// 1. Définir l'état de l'interface utilisateur
data class SignInState(
    val isLoading: Boolean = false,
    val isSignInSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // 2. Exposer l'état à la vue
    private val _signInState = MutableStateFlow(SignInState())
    val signInState: StateFlow<SignInState> = _signInState

    // 3. Fonction pour gérer la logique de connexion
    fun login(email: String, password: String) {
        repository.loginUser(email, password).onEach { result ->
            _signInState.value = when {
                result.isSuccess -> {
                    SignInState(isSignInSuccess = true)
                }
                result.isFailure -> {
                    SignInState(error = result.exceptionOrNull()?.message ?: "An unknown error occurred")
                }
                else -> {
                     SignInState(isLoading = true) // Considérer un état de chargement initial
                }
            }
        }.launchIn(viewModelScope)
    }

    fun signInGoogle(idToken: String) {
        viewModelScope.launch {
            repository.firebaseSignInWithGoogle(idToken)
                .onStart {
                    _signInState.value = SignInState(isLoading = true)
                }
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            _signInState.value = SignInState(isSignInSuccess = true)
                        },
                        onFailure = {
                            _signInState.value = SignInState(error = it.message)
                            Log.d("Auth", "Exception2 ${it.message}")
                        }
                    )
                }
        }
    }


}
