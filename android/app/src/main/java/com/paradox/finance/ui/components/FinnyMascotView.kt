package com.paradox.finance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

enum class FinnyMood {
    JOYFUL, CALM, ALERT, STRESSED
}

@Composable
fun FinnyMascotView(
    mood: FinnyMood = FinnyMood.JOYFUL,
    speechText: String = "Your finances look healthy! Keep it up.",
    onClick: () -> Unit = {}
) {
    var showSpeechBubble by remember { mutableStateOf(true) }

    val moodColor by animateColorAsState(
        targetValue = when (mood) {
            FinnyMood.JOYFUL -> AccentEmerald
            FinnyMood.CALM -> AccentCyan
            FinnyMood.ALERT -> AccentAmber
            FinnyMood.STRESSED -> AccentRose
        },
        label = "FinnyMoodColor"
    )

    val moodEmoji = when (mood) {
        FinnyMood.JOYFUL -> "🐬"
        FinnyMood.CALM -> "🌊"
        FinnyMood.ALERT -> "⚡"
        FinnyMood.STRESSED -> "🔥"
    }

    // Gentle floating breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "FinnyFloat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FinnyScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showSpeechBubble = !showSpeechBubble
                onClick()
            }
            .padding(vertical = 12.dp)
    ) {
        // Speech Bubble
        if (showSpeechBubble) {
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .border(1.dp, moodColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = speechText,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Finny Mascot Avatar
        Box(
            modifier = Modifier
                .scale(scale)
                .size(76.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(moodColor.copy(alpha = 0.35f), SurfaceDark)
                    )
                )
                .border(2.dp, moodColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = moodEmoji,
                fontSize = 38.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Finny • ${mood.name.lowercase().replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.labelSmall,
            color = moodColor,
            fontWeight = FontWeight.Bold
        )
    }
}
