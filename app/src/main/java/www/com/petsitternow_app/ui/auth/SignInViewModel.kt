package www.com.petsitternow_app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.NotificationRepository
import javax.inject.Inject


data class SignInState(
    val isLoading: Boolean = false,
    val isSignInSuccess: Boolean = false,
    val needsOnboarding: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _signInState = MutableStateFlow(SignInState())
    val signInState: StateFlow<SignInState> = _signInState.asStateFlow()

    fun login(email: String, password: String) {
        _signInState.value = SignInState(isLoading = true)
        repository.loginUser(email, password).onEach { result ->
            result.onSuccess {
                saveFcmTokenAfterLogin()
                checkOnboardingAndNavigate()
            }.onFailure {
                _signInState.value = SignInState(error = it.message ?: "An unknown error occurred")
            }
        }.launchIn(viewModelScope)
    }

    fun signInGoogle(idToken: String) {
        _signInState.value = SignInState(isLoading = true)
        repository.signInWithGoogle(idToken).onEach { result ->
            result.onSuccess {
                saveFcmTokenAfterLogin()
                checkOnboardingAndNavigate()
            }.onFailure {
                _signInState.value = SignInState(error = it.message ?: "An unknown error occurred")
            }
        }.launchIn(viewModelScope)
    }

    private fun saveFcmTokenAfterLogin() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                notificationRepository.saveFcmToken(token).collect { result ->
                    result.onSuccess {
                        Log.d(TAG, "FCM token saved after login")
                    }.onFailure { e ->
                        Log.e(TAG, "Failed to save FCM token after login", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get FCM token", e)
            }
        }
    }

    private fun checkOnboardingAndNavigate() {
        viewModelScope.launch {
            val onboardingCompleted = repository.isOnboardingCompleted()
            _signInState.value = SignInState(
                isSignInSuccess = true,
                needsOnboarding = !onboardingCompleted
            )
        }
    }

    companion object {
        private const val TAG = "SignInViewModel"
    }
}
