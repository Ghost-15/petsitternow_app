package www.com.petsitternow_app.view.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

sealed class SettingNavigation {
    object GoToSignIn : SettingNavigation()
}

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SettingNavigation>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _navigationEvent.emit(SettingNavigation.GoToSignIn)
        }
    }
}