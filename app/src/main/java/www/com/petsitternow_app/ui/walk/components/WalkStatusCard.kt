package www.com.petsitternow_app.ui.walk.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import www.com.petsitternow_app.domain.model.ActiveWalk
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.model.WalkStatus

/**
 * Color palette for walk statuses.
 * These colors are designed to work in both light and dark mode.
 */
object WalkStatusColors {
    val Matching = Color(0xFF2196F3) // Blue
    val Assigned = Color(0xFF9C27B0) // Purple
    val Walking = Color(0xFF4CAF50) // Green
    val Returning = Color(0xFFFF9800) // Orange
    val Completed = Color(0xFF4CAF50) // Green
    val Cancelled = Color(0xFF9E9E9E) // Grey
    val Failed = Color(0xFFF44336) // Red
}

/**
 * Card displaying the current walk status for owners.
 * Adapts its appearance based on the walk status.
 */
@Composable
fun WalkStatusCard(
    walkRequest: WalkRequest,
    activeWalk: ActiveWalk?,
    elapsedTime: String,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(300)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AnimatedContent(
            targetState = walkRequest.status,
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f))
                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f))
            },
            label = "statusTransition"
        ) { status ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (status) {
                    WalkStatus.PENDING, WalkStatus.MATCHING -> MatchingContent(onCancel)
                    WalkStatus.ASSIGNED, WalkStatus.GOING_TO_OWNER -> AssignedContent(walkRequest, onCancel)
                    WalkStatus.IN_PROGRESS, WalkStatus.WALKING -> WalkingContent(walkRequest, elapsedTime)
                    WalkStatus.RETURNING -> ReturningContent(walkRequest, elapsedTime)
                    WalkStatus.COMPLETED -> CompletedContent(elapsedTime, onDismiss)
                    WalkStatus.CANCELLED -> CancelledContent(onDismiss)
                    WalkStatus.FAILED -> FailedContent(onDismiss)
                    WalkStatus.DISMISSED -> { /* Should not display */ }
                }
            }
        }
    }
}

@Composable
private fun MatchingContent(onCancel: () -> Unit) {
    StatusHeader(
        icon = null,
        title = "Recherche en cours...",
        subtitle = "Nous cherchons un petsitter disponible",
        statusColor = WalkStatusColors.Matching,
        showProgress = true
    )

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Annuler la demande")
    }
}

@Composable
private fun AssignedContent(walkRequest: WalkRequest, onCancel: () -> Unit) {
    StatusHeader(
        icon = Icons.Default.Person,
        title = "Petsitter en route",
        subtitle = walkRequest.petsitter?.let {
            "${it.firstName} ${it.lastName} arrive..."
        } ?: "Un petsitter arrive...",
        statusColor = WalkStatusColors.Assigned
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Petsitter info card
    walkRequest.petsitter?.let { petsitter ->
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
                    .background(WalkStatusColors.Assigned),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = petsitter.firstName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "${petsitter.firstName} ${petsitter.lastName}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Petsitter certifie",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Annuler")
    }
}

@Composable
private fun WalkingContent(walkRequest: WalkRequest, elapsedTime: String) {
    StatusHeader(
        icon = Icons.Default.Person,
        title = "Promenade en cours",
        subtitle = walkRequest.petsitter?.let {
            "${it.firstName} promene votre chien"
        } ?: "Promenade en cours",
        statusColor = WalkStatusColors.Walking
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Timer display
    TimerDisplay(
        time = elapsedTime,
        color = WalkStatusColors.Walking
    )
}

@Composable
private fun ReturningContent(walkRequest: WalkRequest, elapsedTime: String) {
    StatusHeader(
        icon = Icons.Default.Home,
        title = "Retour en cours",
        subtitle = walkRequest.petsitter?.let {
            "${it.firstName} ramene votre chien"
        } ?: "Le petsitter revient",
        statusColor = WalkStatusColors.Returning
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Timer display
    TimerDisplay(
        time = elapsedTime,
        color = WalkStatusColors.Returning
    )
}

@Composable
private fun CompletedContent(elapsedTime: String, onDismiss: () -> Unit) {
    StatusHeader(
        icon = Icons.Default.Check,
        title = "Promenade terminee !",
        subtitle = "Duree totale: $elapsedTime",
        statusColor = WalkStatusColors.Completed
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WalkStatusColors.Completed
        )
    ) {
        Text("Fermer")
    }
}

@Composable
private fun CancelledContent(onDismiss: () -> Unit) {
    StatusHeader(
        icon = Icons.Default.Close,
        title = "Promenade annulee",
        subtitle = "La demande a ete annulee",
        statusColor = WalkStatusColors.Cancelled
    )

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("Fermer")
    }
}

@Composable
private fun FailedContent(onDismiss: () -> Unit) {
    StatusHeader(
        icon = Icons.Default.Warning,
        title = "Aucun petsitter disponible",
        subtitle = "Reessayez plus tard",
        statusColor = WalkStatusColors.Failed
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WalkStatusColors.Failed
        )
    ) {
        Text("Fermer")
    }
}

@Composable
private fun StatusHeader(
    icon: ImageVector?,
    title: String,
    subtitle: String,
    statusColor: Color,
    showProgress: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = statusColor,
                    strokeWidth = 3.dp
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimerDisplay(
    time: String,
    color: Color
) {
    // Pulse animation for the timer
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
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
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = alpha))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = time.ifEmpty { "0s" },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
