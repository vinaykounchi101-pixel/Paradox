package com.paradox.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

enum class BottomBarDestination {
    DASHBOARD,
    LOG_EXPENSE,
    SIMULATOR,
    FINNY_AI
}

@Composable
fun ObsidianBottomBar(
    currentDestination: BottomBarDestination,
    onNavigate: (BottomBarDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(9999.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(9999.dp))
                .background(GlassSurfaceFloating)
                .border(1.dp, GlassBorderStroke, RoundedCornerShape(9999.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard
            BottomNavItem(
                icon = Icons.Default.GridView,
                label = "Home",
                isSelected = currentDestination == BottomBarDestination.DASHBOARD,
                onClick = { onNavigate(BottomBarDestination.DASHBOARD) }
            )

            // Center + Log Action
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(ElectricEmerald, CyanContainer))
                    )
                    .clickable { onNavigate(BottomBarDestination.LOG_EXPENSE) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Log",
                    tint = PitchBlack,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Pre-Purchase Simulator
            BottomNavItem(
                icon = Icons.Default.Calculate,
                label = "Simulate",
                isSelected = currentDestination == BottomBarDestination.SIMULATOR,
                onClick = { onNavigate(BottomBarDestination.SIMULATOR) }
            )

            // Finny AI Copilot
            BottomNavItem(
                icon = Icons.Default.AutoAwesome,
                label = "Finny",
                isSelected = currentDestination == BottomBarDestination.FINNY_AI,
                onClick = { onNavigate(BottomBarDestination.FINNY_AI) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) NeonEmerald else MutedOutline,
            modifier = Modifier.size(22.dp)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(NeonEmerald)
            )
        } else {
            Text(
                text = label,
                color = MutedOutline,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
