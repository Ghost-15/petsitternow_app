package www.com.petsitternow_app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.repository.AuthRepository
import javax.inject.Inject

sealed class DashboardNavigation {
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Graphs à charger dans la bottom navigation
    val navGraphIds = listOf(
        R.navigation.nav_dashboard,
        R.navigation.nav_message,
        R.navigation.nav_setting
    )

    // Item sélectionné par défaut
    val defaultSelectedItemId = R.id.nav_dashboard

}
