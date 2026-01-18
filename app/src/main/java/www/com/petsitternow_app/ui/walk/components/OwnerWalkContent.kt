package www.com.petsitternow_app.ui.walk.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.WalkDuration
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.repository.Pet

/**
 * Main walk content for owner dashboard.
 * Shows either a FAB to request a walk or the current walk status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerWalkContent(
    activeWalk: WalkRequest?,
    activeWalkDetails: ActiveWalk?,
    pets: List<Pet>,
    currentLocation: WalkLocation?,
    elapsedTime: String,
    isLoading: Boolean,
    onRequestWalk: (petIds: List<String>, duration: WalkDuration, location: WalkLocation) -> Unit,
    onCancelWalk: () -> Unit,
    onDismissWalk: () -> Unit,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        if (activeWalk != null) {
            // Show walk status card
            WalkStatusCard(
                walkRequest = activeWalk,
                activeWalk = activeWalkDetails,
                elapsedTime = elapsedTime,
                onCancel = onCancelWalk,
                onDismiss = onDismissWalk,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        } else {
            // Show FAB to request walk
            FloatingActionButton(
                onClick = {
                    onRequestLocation()
                    showBottomSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(64.dp),
                shape = CircleShape,
                containerColor = WalkStatusColors.Walking,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = "Demander une promenade",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Bottom sheet for requesting walk
        if (showBottomSheet) {
            RequestWalkBottomSheet(
                pets = pets,
                currentLocation = currentLocation,
                isLoading = isLoading,
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                },
                onRequestWalk = { petIds, duration, location ->
                    onRequestWalk(petIds, duration, location)
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                },
                onRequestLocation = onRequestLocation,
                sheetState = sheetState
            )
        }
    }
}
