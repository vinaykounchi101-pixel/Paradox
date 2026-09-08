package com.paradox.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.model.FiftyThirtyTwentyResponse
import com.paradox.finance.theme.*
import java.util.Locale

@Composable
fun FiftyThirtyTwentyCard(
    data: FiftyThirtyTwentyResponse?,
    currencySymbol: String = "₹",
    modifier: Modifier = Modifier
) {
    ParadoxCard(
        borderColor = ParadoxIndigo.copy(alpha = 0.35f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "50 / 30 / 20 BUDGET RULE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ParadoxIndigoLight,
                    letterSpacing = 1.sp
                )
            )

            data?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ParadoxEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${it.adherenceScore}/100 Score",
                        color = ParadoxEmerald,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tri-Color Segmented Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ParadoxZinc800)
        ) {
            val needsWeight = ((data?.needsPercentage ?: 50.0) / 100.0).toFloat().coerceIn(0.1f, 0.8f)
            val wantsWeight = ((data?.wantsPercentage ?: 30.0) / 100.0).toFloat().coerceIn(0.1f, 0.8f)
            val savingsWeight = ((data?.savingsPercentage ?: 20.0) / 100.0).toFloat().coerceIn(0.1f, 0.8f)

            Box(modifier = Modifier.weight(needsWeight).fillMaxHeight().background(ParadoxIndigo))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.weight(wantsWeight).fillMaxHeight().background(ParadoxAmber))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.weight(savingsWeight).fillMaxHeight().background(ParadoxEmerald))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 Pillars Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Needs (50%)", style = MaterialTheme.typography.labelSmall.copy(color = ParadoxIndigoLight))
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.0f", data?.needsAmount ?: 0.0)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ParadoxZinc100)
                )
            }
            Column {
                Text(text = "Wants (30%)", style = MaterialTheme.typography.labelSmall.copy(color = ParadoxAmber))
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.0f", data?.wantsAmount ?: 0.0)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ParadoxZinc100)
                )
            }
            Column {
                Text(text = "Savings (20%)", style = MaterialTheme.typography.labelSmall.copy(color = ParadoxEmerald))
                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%.0f", data?.savingsAmount ?: 0.0)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = ParadoxZinc100)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = data?.recommendation ?: "Keep essential fixed commitments below 50% to build resilient monthly savings.",
            style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400, fontSize = 12.sp)
        )
    }
}
