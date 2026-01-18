package www.com.petsitternow_app.ui.petsitter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import www.com.petsitternow_app.ui.petsitter.components.MissionsScreen
import www.com.petsitternow_app.util.TimeFormatter

/**
 * Fragment displaying missions for petsitters.
 */
@AndroidEntryPoint
class MissionsFragment : Fragment() {

    private val viewModel: PetsitterMissionsViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.toggleOnline(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                MaterialTheme {
                    MissionsScreen(
                        isOnline = uiState.isOnline,
                        isToggleLoading = uiState.isTogglingOnline,
                        pendingMission = uiState.pendingMission,
                        activeMission = uiState.activeMission,
                        elapsedTime = uiState.walkElapsedTime,
                        distanceToOwner = uiState.distanceToOwner?.toFloat(),
                        isWithinCompletionRange = uiState.isWithinCompletionRange,
                        onToggleOnline = { wantsOnline ->
                            if (wantsOnline) {
                                requestLocationPermissionAndGoOnline()
                            } else {
                                viewModel.toggleOnline(false)
                            }
                        },
                        onAcceptMission = { viewModel.acceptMission() },
                        onDeclineMission = { viewModel.declineMission() },
                        onStartWalk = { viewModel.startWalk() },
                        onMarkReturning = { viewModel.markReturning() },
                        onCompleteMission = { viewModel.completeMission() }
                    )
                }
            }
        }
    }

    private fun requestLocationPermissionAndGoOnline() {
        when {
            hasLocationPermission() -> viewModel.toggleOnline(true)
            else -> locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
