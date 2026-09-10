package com.paradox.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

@Composable
fun FiftyThirtyTwentyBar(
    needsPct: Double = 50.0,
    wantsPct: Double = 30.0,
    savingsPct: Double = 20.0,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "50/30/20 Pacing Tracker", color = OnSurfaceVariant, fontSize = 11.sp)
            Text(text = "Optimal Balance", color = ElectricEmerald, fontSize = 10.sp, style = Typography.labelSmall)
        }

        // Segmented Progress Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(9999.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(needsPct.toFloat().coerceAtLeast(1f))
                    .fillMaxHeight()
                    .background(ElectricEmerald)
            )
            Box(
                modifier = Modifier
                    .weight(wantsPct.toFloat().coerceAtLeast(1f))
                    .fillMaxHeight()
                    .background(NeonCyan)
            )
            Box(
                modifier = Modifier
                    .weight(savingsPct.toFloat().coerceAtLeast(1f))
                    .fillMaxHeight()
                    .background(VibrantIndigo)
            )
        }

        // Legend Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Needs: ${needsPct.toInt()}%", color = ElectricEmerald, fontSize = 9.sp)
            Text(text = "Wants: ${wantsPct.toInt()}%", color = NeonCyan, fontSize = 9.sp)
            Text(text = "Savings: ${savingsPct.toInt()}%", color = SoftIndigo, fontSize = 9.sp)
        }
    }
}
