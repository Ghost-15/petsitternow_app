package www.com.petsitternow_app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

data class SignUpState(
    val isLoading: Boolean = false,
    val isSignUpSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(email: String, password: String) {
        repository.createUser(email, password).onEach { result ->
            _signUpState.value = when {
                result.isSuccess -> {
                    SignUpState(isSignUpSuccess = true)
                }
                result.isFailure -> {
                    SignUpState(error = result.exceptionOrNull()?.message ?: "An unknown error occurred")
                }
                else -> {
                    SignUpState(isLoading = true)
                }
            }
        }.launchIn(viewModelScope)
    }
}
