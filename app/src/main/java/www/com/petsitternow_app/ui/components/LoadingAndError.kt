package www.com.petsitternow_app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Error types for better UX messaging.
 */
enum class ErrorType {
    NETWORK,
    SERVER,
    PERMISSION,
    NOT_FOUND,
    UNKNOWN
}

/**
 * Shimmer effect for loading states.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 500f, 0f),
        end = Offset(translateAnimation, 0f)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

/**
 * Shimmer loading placeholder for list items.
 */
@Composable
fun ShimmerListItem(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
            )
        }
    }
}

/**
 * Shimmer loading placeholder for cards.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(20.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )
    }
}

/**
 * Loading indicator with optional message.
 */
@Composable
fun LoadingIndicator(
    message: String = "Chargement...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Full-screen loading state.
 */
@Composable
fun FullScreenLoading(
    message: String = "Chargement...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(message = message)
    }
}

/**
 * Error view with retry button.
 */
@Composable
fun ErrorView(
    message: String,
    errorType: ErrorType = ErrorType.UNKNOWN,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (icon, title) = when (errorType) {
        ErrorType.NETWORK -> Icons.Default.WifiOff to "Pas de connexion"
        ErrorType.SERVER -> Icons.Default.CloudOff to "Erreur serveur"
        ErrorType.PERMISSION -> Icons.Default.Error to "Permission refusee"
        ErrorType.NOT_FOUND -> Icons.Default.Error to "Non trouve"
        ErrorType.UNKNOWN -> Icons.Default.Error to "Une erreur est survenue"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reessayer")
            }
        }
    }
}

/**
 * Full-screen error state.
 */
@Composable
fun FullScreenError(
    message: String,
    errorType: ErrorType = ErrorType.UNKNOWN,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ErrorView(
            message = message,
            errorType = errorType,
            onRetry = onRetry
        )
    }
}

/**
 * Empty state view.
 */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Inline error message with retry.
 */
@Composable
fun InlineError(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reessayer", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Offline banner.
 */
@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFF9800))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Mode hors ligne - Certaines fonctionnalites peuvent etre limitees",
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
