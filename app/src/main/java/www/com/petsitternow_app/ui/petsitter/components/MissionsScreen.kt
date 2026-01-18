package www.com.petsitternow_app.ui.petsitter.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkRequest

/**
 * Main screen for petsitter missions.
 * Shows online toggle, active mission, or empty state.
 */
@Composable
fun MissionsScreen(
    isOnline: Boolean,
    isToggleLoading: Boolean,
    pendingMission: PetsitterMission?,
    activeMission: WalkRequest?,
    elapsedTime: String,
    distanceToOwner: Float?,
    isWithinCompletionRange: Boolean,
    onToggleOnline: (Boolean) -> Unit,
    onAcceptMission: () -> Unit,
    onDeclineMission: () -> Unit,
    onStartWalk: () -> Unit,
    onMarkReturning: () -> Unit,
    onCompleteMission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Online toggle (always visible)
            OnlineToggle(
                isOnline = isOnline,
                isLoading = isToggleLoading,
                onToggle = onToggleOnline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content based on state
            when {
                activeMission != null -> {
                    // Show active mission
                    ActiveMissionView(
                        mission = activeMission,
                        elapsedTime = elapsedTime,
                        distanceToOwner = distanceToOwner,
                        isWithinCompletionRange = isWithinCompletionRange,
                        onStartWalk = onStartWalk,
                        onMarkReturning = onMarkReturning,
                        onCompleteMission = onCompleteMission,
                        onOpenInMaps = {
                            openInGoogleMaps(context, activeMission.location)
                        }
                    )
                }
                isOnline -> {
                    // Waiting for missions
                    WaitingForMissionsContent()
                }
                else -> {
                    // Offline state
                    OfflineContent()
                }
            }
        }

        // Mission notification dialog (overlay)
        pendingMission?.let { mission ->
            MissionNotificationDialog(
                mission = mission,
                onAccept = onAcceptMission,
                onDecline = onDeclineMission
            )
        }
    }
}

@Composable
private fun WaitingForMissionsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(OnlineToggleColors.OnlineBackgroundLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = OnlineToggleColors.Online,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "En attente de missions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Vous recevrez une notification des qu'une mission sera disponible a proximite",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OfflineContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(OnlineToggleColors.OfflineBackgroundLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsWalk,
                contentDescription = null,
                tint = OnlineToggleColors.Offline,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Vous etes hors ligne",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Activez le mode en ligne pour recevoir des demandes de promenade",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

private fun openInGoogleMaps(context: android.content.Context, location: WalkLocation) {
    val uri = Uri.parse("google.navigation:q=${location.lat},${location.lng}")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        // Fallback to browser
        val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${location.lat},${location.lng}")
        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
    }
}
