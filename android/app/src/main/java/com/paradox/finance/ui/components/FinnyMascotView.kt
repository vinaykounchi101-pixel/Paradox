package com.paradox.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

@Composable
fun FinnyMascotView(
    mood: String = "JOYFUL",
    speechText: String = "Great pacing, Vikram! Your recurring subscriptions are down 14% this week.",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val auraColor = when (mood.uppercase()) {
        "JOYFUL", "CALM" -> ElectricEmerald
        "ALERT" -> WarningAmber
        else -> AlertCoral
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface1)
            .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mascot Glowing Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(auraColor.copy(alpha = 0.5f), GlassSurface2)
                        )
                    )
                    .border(1.5.dp, auraColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Finny",
                    tint = auraColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Speech Bubble
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Finny Copilot",
                        color = OnSurfaceHigh,
                        fontSize = 13.sp,
                        style = Typography.labelLarge
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(auraColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mood.lowercase().replaceFirstChar { it.uppercase() },
                            color = auraColor,
                            fontSize = 9.sp,
                            style = Typography.labelSmall
                        )
                    }
                }

                Text(
                    text = speechText,
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
