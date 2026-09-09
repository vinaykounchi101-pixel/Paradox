package com.paradox.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

enum class NavTab {
    DASHBOARD, QUICK_LOG, SIMULATOR, FINNY_AI
}

@Composable
fun ObsidianBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundPitchBlack)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9999.dp))
                .background(SurfaceObsidianSubtle.copy(alpha = 0.95f))
                .border(1.dp, BorderGlass, RoundedCornerShape(9999.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                isSelected = currentTab == NavTab.DASHBOARD,
                onClick = { onTabSelected(NavTab.DASHBOARD) }
            )

            NavBarItem(
                icon = Icons.Default.AddCircle,
                label = "Quick Log",
                isSelected = currentTab == NavTab.QUICK_LOG,
                isHighlight = true,
                onClick = { onTabSelected(NavTab.QUICK_LOG) }
            )

            NavBarItem(
                icon = Icons.Default.Sensors,
                label = "Simulator",
                isSelected = currentTab == NavTab.SIMULATOR,
                onClick = { onTabSelected(NavTab.SIMULATOR) }
            )

            NavBarItem(
                icon = Icons.Default.AutoAwesome,
                label = "Finny AI",
                isSelected = currentTab == NavTab.FINNY_AI,
                onClick = { onTabSelected(NavTab.FINNY_AI) }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val activeColor = if (isHighlight) NeonEmerald else NeonEmerald
    val inactiveColor = TextTertiary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9999.dp),
        color = if (isSelected && !isHighlight) SurfaceObsidianElevated else Color.Transparent,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected || isHighlight) activeColor else inactiveColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isHighlight) activeColor else inactiveColor
            )
        }
    }
}
