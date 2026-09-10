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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

@Composable
fun CategoryBarChartView(
    categoriesWithSpend: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        ChartEmerald, ChartCyan, ChartIndigo, ChartRose,
        ChartAmber, ChartPurple, ChartBlue, ChartPink
    )

    val items = if (categoriesWithSpend.isEmpty()) listOf(
        "Dining" to 18400.0,
        "Groceries" to 12200.0,
        "Shopping" to 9500.0,
        "Travel" to 6800.0,
        "Tech" to 4200.0,
        "OTT" to 3110.0
    ) else categoriesWithSpend

    val maxAmount = items.maxOfOrNull { it.second }?.coerceAtLeast(100.0) ?: 1000.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface1)
            .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Category Distribution",
                color = OnSurface,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                items.take(6).forEachIndexed { index, pair ->
                    val color = palette[index % palette.size]
                    val heightRatio = (pair.second / maxAmount).toFloat().coerceIn(0.1f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .width(22.dp)
                                .height((heightRatio * 85).dp)
                        ) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(color, color.copy(alpha = 0.4f))
                                ),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            // Top Highlight Cap
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.6f),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, 3.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }

                        Text(
                            text = (pair.first ?: "Cat").take(5),
                            color = OnSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
