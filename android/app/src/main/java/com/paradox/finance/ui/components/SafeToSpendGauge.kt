package com.paradox.finance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*
import com.paradox.finance.utils.CurrencyFormatter

@Composable
fun SafeToSpendGauge(
    dailyAllowance: Double,
    pacingStatus: String,
    daysLeft: Int,
    currency: String = "INR",
    modifier: Modifier = Modifier
) {
    val statusColor = when (pacingStatus.lowercase()) {
        "optimal", "safe" -> ElectricEmerald
        "moderate", "warning" -> WarningAmber
        else -> AlertCoral
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface1)
            .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Safe Daily Velocity",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "${CurrencyFormatter.format(dailyAllowance, currency)} / day",
                    color = OnSurfaceHigh,
                    fontSize = 20.sp,
                    style = Typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Text(
                    text = "$daysLeft days remaining in cycle",
                    color = MutedOutline,
                    fontSize = 11.sp
                )
            }

            // Radial Speedometer Arc
            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    // Track Arc
                    drawArc(
                        color = SurfaceContainerHighest,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        size = Size(size.width - stroke, size.height - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2)
                    )
                    // Active Progress Arc
                    drawArc(
                        brush = Brush.sweepGradient(listOf(NeonEmerald, NeonCyan)),
                        startAngle = 140f,
                        sweepAngle = 190f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        size = Size(size.width - stroke, size.height - stroke),
                        topLeft = Offset(stroke / 2, stroke / 2)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = pacingStatus.take(4).uppercase(),
                        color = statusColor,
                        fontSize = 8.sp,
                        style = Typography.labelSmall
                    )
                }
            }
        }
    }
}
