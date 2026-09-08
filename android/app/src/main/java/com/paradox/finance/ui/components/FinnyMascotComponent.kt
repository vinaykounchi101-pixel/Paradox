package com.paradox.finance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.theme.*

enum class FinnyMood {
    JOYFUL, CALM, ALERT, STRESSED
}

@Composable
fun FinnyMascotComponent(
    mood: FinnyMood = FinnyMood.JOYFUL,
    headline: String = "Your Financial Copilot is Active",
    tip: String = "Pacing is optimal. You're well within your daily safe allowance.",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "finny_pulse")
    val floatScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "finny_scale"
    )

    val (mascotEmoji, statusColor, statusLabel) = when (mood) {
        FinnyMood.JOYFUL -> Triple("🤖✨", ParadoxEmerald, "Pacing: Excellent")
        FinnyMood.CALM -> Triple("🤖", ParadoxIndigoLight, "Pacing: Balanced")
        FinnyMood.ALERT -> Triple("🤖⚠️", ParadoxAmber, "Pacing: Caution")
        FinnyMood.STRESSED -> Triple("🤖🔥", ParadoxRose, "Pacing: Over Safe Threshold")
    }

    ParadoxCard(
        borderColor = statusColor.copy(alpha = 0.4f),
        backgroundColor = ParadoxZinc900,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mascot Avatar Circle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .scale(floatScale)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.5.dp, statusColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = mascotEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Finny AI Copilot",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ParadoxZinc100
                        )
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ParadoxZinc200,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Text(
                    text = tip,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ParadoxZinc400,
                        lineHeight = 15.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
