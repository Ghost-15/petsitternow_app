package www.com.petsitternow_app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

sealed class MainNavigation {
    object GoToDashboard : MainNavigation()
    object GoToSignIn : MainNavigation()
    object GoToOnboarding : MainNavigation()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<MainNavigation>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            delay(3000L) // Conserve le délai du splash screen
            
            if (repository.isUserAuthenticated()) {
                val onboardingCompleted = repository.isOnboardingCompleted()
                
                if (onboardingCompleted) {
                    _navigationEvent.emit(MainNavigation.GoToDashboard)
                } else {
                    _navigationEvent.emit(MainNavigation.GoToOnboarding)
                }
            } else {
                _navigationEvent.emit(MainNavigation.GoToSignIn)
            }
        }
    }
}
