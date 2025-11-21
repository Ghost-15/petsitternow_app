package www.com.petsitternow_app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

sealed class DashboardNavigation {
    object GoToSignIn : DashboardNavigation()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<DashboardNavigation>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _navigationEvent.emit(DashboardNavigation.GoToSignIn)
        }
    }
}
