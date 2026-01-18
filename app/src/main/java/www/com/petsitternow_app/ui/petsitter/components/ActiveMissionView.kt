package www.com.petsitternow_app.ui.petsitter.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus

/**
 * Colors for mission status.
 * These colors work in both light and dark mode.
 */
object MissionStatusColors {
    val Assigned = Color(0xFF9C27B0) // Purple
    val GoingToOwner = Color(0xFF2196F3) // Blue
    val Walking = Color(0xFF4CAF50) // Green
    val Returning = Color(0xFFFF9800) // Orange
}

/**
 * View displaying the active mission details for a petsitter.
 */
@Composable
fun ActiveMissionView(
    mission: WalkRequest,
    elapsedTime: String,
    distanceToOwner: Float?,
    isWithinCompletionRange: Boolean,
    onStartWalk: () -> Unit,
    onMarkReturning: () -> Unit,
    onCompleteMission: () -> Unit,
    onOpenInMaps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Status header
            MissionStatusHeader(mission.status)

            Spacer(modifier = Modifier.height(20.dp))

            // Owner info
            mission.owner?.let { owner ->
                OwnerInfoCard(
                    ownerName = "${owner.firstName} ${owner.lastName}",
                    petNames = mission.petIds.joinToString(", "), // Would need pet names from somewhere
                    address = mission.location.address
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer (visible during walking phases)
            if (mission.status.isWalkingPhase()) {
                TimerCard(
                    time = elapsedTime,
                    statusColor = getStatusColor(mission.status)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Distance info (for returning phase)
            if (mission.status == WalkStatus.RETURNING && distanceToOwner != null) {
                DistanceCard(
                    distance = distanceToOwner,
                    isWithinRange = isWithinCompletionRange
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action button
            ActionButton(
                status = mission.status,
                isWithinCompletionRange = isWithinCompletionRange,
                onStartWalk = onStartWalk,
                onMarkReturning = onMarkReturning,
                onCompleteMission = onCompleteMission
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Open in Maps button
            if (mission.status == WalkStatus.ASSIGNED || mission.status == WalkStatus.GOING_TO_OWNER) {
                OutlinedButton(
                    onClick = onOpenInMaps,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ouvrir dans Maps")
                }
            }
        }
    }
}

@Composable
private fun MissionStatusHeader(status: WalkStatus) {
    val (icon, title, subtitle, color) = when (status) {
        WalkStatus.ASSIGNED, WalkStatus.GOING_TO_OWNER -> StatusInfo(
            Icons.Default.LocationOn,
            "En route vers le client",
            "Dirigez-vous vers l'adresse indiquee",
            MissionStatusColors.GoingToOwner
        )
        WalkStatus.IN_PROGRESS, WalkStatus.WALKING -> StatusInfo(
            Icons.AutoMirrored.Filled.DirectionsWalk,
            "Promenade en cours",
            "Profitez de la balade !",
            MissionStatusColors.Walking
        )
        WalkStatus.RETURNING -> StatusInfo(
            Icons.Default.Home,
            "Retour en cours",
            "Ramenez le chien chez son proprietaire",
            MissionStatusColors.Returning
        )
        else -> StatusInfo(
            Icons.Default.Check,
            "Mission",
            "",
            Color.Gray
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private data class StatusInfo(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val color: Color
)

@Composable
private fun OwnerInfoCard(
    ownerName: String,
    petNames: String,
    address: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2196F3)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ownerName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ownerName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = petNames,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            address?.let {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerCard(time: String, statusColor: Color) {
    // Pulse animation for the timer
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(statusColor.copy(alpha = alpha))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time.ifEmpty { "0s" },
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
private fun DistanceCard(distance: Float, isWithinRange: Boolean) {
    val distanceText = if (distance < 1000) {
        "${distance.toInt()}m"
    } else {
        String.format("%.1f km", distance / 1000)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isWithinRange) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Distance du point de depart",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = distanceText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWithinRange) MissionStatusColors.Walking else MissionStatusColors.Returning
            )
        }

        if (isWithinRange) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MissionStatusColors.Walking)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "OK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        } else {
            Text(
                text = "< 100m requis",
                fontSize = 12.sp,
                color = MissionStatusColors.Returning
            )
        }
    }
}

@Composable
private fun ActionButton(
    status: WalkStatus,
    isWithinCompletionRange: Boolean,
    onStartWalk: () -> Unit,
    onMarkReturning: () -> Unit,
    onCompleteMission: () -> Unit
) {
    when (status) {
        WalkStatus.ASSIGNED, WalkStatus.GOING_TO_OWNER -> {
            Button(
                onClick = onStartWalk,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MissionStatusColors.Walking
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "J'ai recupere le chien",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        WalkStatus.IN_PROGRESS, WalkStatus.WALKING -> {
            Button(
                onClick = onMarkReturning,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MissionStatusColors.Returning
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Je ramene le chien",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        WalkStatus.RETURNING -> {
            Button(
                onClick = onCompleteMission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = isWithinCompletionRange,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MissionStatusColors.Walking,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mission terminee",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        else -> { /* No action button for other states */ }
    }
}

private fun getStatusColor(status: WalkStatus): Color {
    return when (status) {
        WalkStatus.ASSIGNED, WalkStatus.GOING_TO_OWNER -> MissionStatusColors.GoingToOwner
        WalkStatus.IN_PROGRESS, WalkStatus.WALKING -> MissionStatusColors.Walking
        WalkStatus.RETURNING -> MissionStatusColors.Returning
        else -> Color.Gray
    }
}
