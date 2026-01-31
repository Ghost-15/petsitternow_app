package www.com.petsitternow_app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

sealed class DashboardNavigation {
    object GoToOnboarding : DashboardNavigation()
}

data class DashboardState(
    val isLoading: Boolean = true,
    val onboardingCompleted: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<DashboardNavigation>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    val navGraphIds = listOf(
        R.navigation.nav_dashboard,
        R.navigation.nav_history,
        R.navigation.nav_profile
    )

    val defaultSelectedItemId = R.id.nav_dashboard

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            repository.refreshToken()
            
            val onboardingCompleted = repository.isOnboardingCompleted()
            _state.value = _state.value.copy(
                isLoading = false,
                onboardingCompleted = onboardingCompleted
            )
            
            if (!onboardingCompleted) {
                _navigationEvent.emit(DashboardNavigation.GoToOnboarding)
            }
        }
    }
}
