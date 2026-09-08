package com.paradox.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.model.AchievementsResponse
import com.paradox.finance.theme.*

@Composable
fun AchievementsCard(
    data: AchievementsResponse?,
    modifier: Modifier = Modifier
) {
    ParadoxCard(
        borderColor = ParadoxPurple.copy(alpha = 0.35f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏆 STREAKS & DISCIPLINE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ParadoxPurple,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "🔥 ${data?.streaks ?: 7} Day Streak",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ParadoxAmber,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val achievements = data?.achievements ?: emptyList()
        if (achievements.isEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ParadoxZinc800,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "🛡️ Budget Champion", style = MaterialTheme.typography.labelSmall.copy(color = ParadoxZinc100, fontWeight = FontWeight.Bold))
                        Text(text = "Kept spend under cap", style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400, fontSize = 11.sp))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ParadoxZinc800,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "🎯 Consistency Ace", style = MaterialTheme.typography.labelSmall.copy(color = ParadoxZinc100, fontWeight = FontWeight.Bold))
                        Text(text = "Logged daily expenses", style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400, fontSize = 11.sp))
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(achievements) { ach ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (ach.isUnlocked) ParadoxIndigo.copy(alpha = 0.2f) else ParadoxZinc800,
                        modifier = Modifier.width(150.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "${ach.icon} ${ach.title}", style = MaterialTheme.typography.labelSmall.copy(color = ParadoxZinc100, fontWeight = FontWeight.Bold))
                            Text(text = ach.description, style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400, fontSize = 11.sp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "💡 \"${data?.motivationalQuote ?: "Discipline is the bridge between goals and accomplishment."}\"",
            style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400, fontSize = 12.sp)
        )
    }
}
