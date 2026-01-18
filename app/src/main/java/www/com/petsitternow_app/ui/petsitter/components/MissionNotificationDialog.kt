package www.com.petsitternow_app.ui.petsitter.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import www.com.petsitternow_app.domain.model.PetsitterMission

/**
 * Colors for mission notification.
 * Supports both light and dark mode.
 */
object MissionNotificationColors {
    val Accept = Color(0xFF4CAF50) // Green
    val Decline = Color(0xFFF44336) // Red
    val Timer = Color(0xFFFF9800) // Orange

    // Light mode
    val BackgroundLight = Color(0xFFF8F9FA)
    val CardBackgroundLight = Color.White
    val DetailRowLight = Color(0xFFF5F5F5)
    val IconBackgroundLight = Color(0xFFE3F2FD)

    // Dark mode
    val BackgroundDark = Color(0xFF1A1A1A)
    val CardBackgroundDark = Color(0xFF2D2D2D)
    val DetailRowDark = Color(0xFF3D3D3D)
    val IconBackgroundDark = Color(0xFF1A3A5C)
}

/**
 * Full-screen dialog for incoming mission notification.
 * Displays mission details with a countdown timer.
 */
@Composable
fun MissionNotificationDialog(
    mission: PetsitterMission,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    timeoutSeconds: Int = 30
) {
    var remainingSeconds by remember { mutableIntStateOf(timeoutSeconds) }

    // Countdown timer
    LaunchedEffect(mission.requestId) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
        // Auto-decline when timer reaches 0
        onDecline()
    }

    val progress by animateFloatAsState(
        targetValue = remainingSeconds.toFloat() / timeoutSeconds,
        animationSpec = tween(900),
        label = "timerProgress"
    )

    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) MissionNotificationColors.BackgroundDark else MissionNotificationColors.BackgroundLight
    val cardBackgroundColor = if (isDarkTheme) MissionNotificationColors.CardBackgroundDark else MissionNotificationColors.CardBackgroundLight

    Dialog(
        onDismissRequest = { /* Cannot dismiss manually */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackgroundColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with timer
                    Text(
                        text = "Nouvelle mission !",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Countdown timer
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = when {
                                remainingSeconds <= 10 -> MissionNotificationColors.Decline
                                remainingSeconds <= 20 -> MissionNotificationColors.Timer
                                else -> MissionNotificationColors.Accept
                            },
                            strokeWidth = 6.dp,
                            trackColor = Color(0xFFE0E0E0)
                        )
                        Text(
                            text = "${remainingSeconds}s",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                remainingSeconds <= 10 -> MissionNotificationColors.Decline
                                else -> Color.Black
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mission details
                    MissionDetailRow(
                        icon = Icons.Default.Person,
                        label = "Client",
                        value = mission.ownerName
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MissionDetailRow(
                        icon = Icons.Default.Pets,
                        label = "Animaux",
                        value = mission.petNames.joinToString(", ")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MissionDetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "Duree",
                        value = "${mission.duration} min"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    MissionDetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Distance",
                        value = "${String.format("%.1f", mission.distance / 1000)} km"
                    )

                    mission.location.address?.let { address ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = address,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MissionNotificationColors.Decline
                            )
                        ) {
                            Text(
                                text = "Refuser",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MissionNotificationColors.Accept
                            )
                        ) {
                            Text(
                                text = "Accepter",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionDetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
