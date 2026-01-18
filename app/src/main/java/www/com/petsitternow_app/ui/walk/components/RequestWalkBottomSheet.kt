package www.com.petsitternow_app.ui.walk.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import www.com.petsitternow_app.domain.model.WalkDuration
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.repository.Pet

/**
 * Bottom sheet for requesting a new walk.
 * Allows selecting pets, duration, and pickup location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestWalkBottomSheet(
    pets: List<Pet>,
    currentLocation: WalkLocation?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRequestWalk: (selectedPetIds: List<String>, duration: WalkDuration, location: WalkLocation) -> Unit,
    onRequestLocation: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val selectedPetIds = remember { mutableStateListOf<String>() }
    var selectedDuration by remember { mutableStateOf(WalkDuration.DURATION_30) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "Nouvelle promenade",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Pet selection
            Text(
                text = "Selectionnez vos chiens",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (pets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun chien enregistre",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height((pets.size.coerceAtMost(3) * 64).dp)
                ) {
                    items(pets) { pet ->
                        PetSelectionItem(
                            pet = pet,
                            isSelected = pet.id in selectedPetIds,
                            onToggle = {
                                if (pet.id in selectedPetIds) {
                                    selectedPetIds.remove(pet.id)
                                } else {
                                    selectedPetIds.add(pet.id)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Duration selection
            Text(
                text = "Duree de la promenade",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WalkDuration.entries.forEach { duration ->
                    DurationOption(
                        duration = duration,
                        isSelected = selectedDuration == duration,
                        onClick = { selectedDuration = duration },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Location
            Text(
                text = "Lieu de prise en charge",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onRequestLocation() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = WalkStatusColors.Walking,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = currentLocation?.address?.takeIf { it.isNotEmpty() }
                        ?: "Position actuelle",
                    fontSize = 14.sp,
                    color = if (currentLocation != null) Color.Black else Color.Gray,
                    modifier = Modifier.weight(1f)
                )

                if (currentLocation != null) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = WalkStatusColors.Completed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit button
            Button(
                onClick = {
                    currentLocation?.let { location ->
                        onRequestWalk(selectedPetIds.toList(), selectedDuration, location)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedPetIds.isNotEmpty() && currentLocation != null && !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalkStatusColors.Walking
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Demander une promenade",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PetSelectionItem(
    pet: Pet,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = WalkStatusColors.Walking
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Pet avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WalkStatusColors.Walking.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pet.name.take(1).uppercase(),
                color = WalkStatusColors.Walking,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = pet.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = pet.breed ?: "Chien",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DurationOption(
    duration: WalkDuration,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationText = when (duration) {
        WalkDuration.DURATION_30 -> "30 min"
        WalkDuration.DURATION_45 -> "45 min"
        WalkDuration.DURATION_60 -> "1h"
    }

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) WalkStatusColors.Walking.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (isSelected) WalkStatusColors.Walking else Color.Gray
        )
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            modifier = Modifier.size(16.dp),
            colors = RadioButtonDefaults.colors(
                selectedColor = WalkStatusColors.Walking
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = durationText,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
