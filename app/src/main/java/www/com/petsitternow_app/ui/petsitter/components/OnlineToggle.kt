package www.com.petsitternow_app.ui.petsitter.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme

/**
 * Colors for online/offline states.
 * Supports both light and dark mode.
 */
object OnlineToggleColors {
    val Online = Color(0xFF4CAF50) // Green
    val Offline = Color(0xFF9E9E9E) // Grey

    // Light mode backgrounds
    val OnlineBackgroundLight = Color(0xFFE8F5E9) // Light green
    val OfflineBackgroundLight = Color(0xFFF5F5F5) // Light grey

    // Dark mode backgrounds
    val OnlineBackgroundDark = Color(0xFF1B5E20).copy(alpha = 0.3f) // Dark green
    val OfflineBackgroundDark = Color(0xFF424242).copy(alpha = 0.3f) // Dark grey
}

/**
 * Toggle component for petsitter online/offline status.
 */
@Composable
fun OnlineToggle(
    isOnline: Boolean,
    isLoading: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isOnline && isDarkTheme -> OnlineToggleColors.OnlineBackgroundDark
            isOnline -> OnlineToggleColors.OnlineBackgroundLight
            isDarkTheme -> OnlineToggleColors.OfflineBackgroundDark
            else -> OnlineToggleColors.OfflineBackgroundLight
        },
        label = "backgroundColor"
    )

    val statusColor by animateColorAsState(
        targetValue = if (isOnline) OnlineToggleColors.Online else OnlineToggleColors.Offline,
        label = "statusColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (isOnline) "En ligne" else "Hors ligne",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = statusColor,
                strokeWidth = 2.dp
            )
        } else {
            Switch(
                checked = isOnline,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = OnlineToggleColors.Online,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = OnlineToggleColors.Offline
                )
            )
        }
    }
}
