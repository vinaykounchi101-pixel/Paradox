package com.paradox.finance.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

data class CategoryChartItem(
    val categoryName: String,
    val total: Double,
    val percentage: Double = 0.0,
    val color: Color = PrimaryIndigo
)

val CATEGORY_COLORS = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFF43F5E), // Rose
    Color(0xFF10B981), // Emerald
    Color(0xFFA855F7), // Purple
    Color(0xFF3B82F6), // Blue
    Color(0xFFF59E0B), // Amber
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEC4899)  // Pink
)

@Composable
fun CategoryBarChartView(
    items: List<CategoryChartItem>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    if (items.isEmpty() || items.all { it.total == 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(SurfaceDark, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏷️", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No category expense data available",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        return
    }

    // Assign fallback colors if needed
    val coloredItems = remember(items) {
        val maxT = items.sumOf { it.total }.coerceAtLeast(1.0)
        items.mapIndexed { idx, itm ->
            val pct = if (itm.percentage > 0) itm.percentage else (itm.total / maxT) * 100.0
            itm.copy(
                percentage = pct,
                color = if (itm.color != PrimaryIndigo || idx == 0) itm.color else CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
            )
        }
    }

    val maxVal = (coloredItems.maxOfOrNull { it.total } ?: 10.0).coerceAtLeast(10.0) * 1.25
    val barCount = coloredItems.size

    Column(modifier = modifier.fillMaxWidth()) {
        // Active Selection Header / Tooltip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedIndex != null && selectedIndex in coloredItems.indices) {
                val item = coloredItems[selectedIndex!!]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(item.color, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${String.format("%.1f", item.percentage)}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Text(
                    text = "$currencySymbol${String.format("%,.2f", item.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = item.color
                )
            } else {
                Text(
                    text = "Categories (${coloredItems.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "Tap a bar to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        // Canvas Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(coloredItems) {
                        detectTapGestures(
                            onPress = { offset ->
                                val leftPad = 16.dp.toPx()
                                val rightPad = 16.dp.toPx()
                                val usableW = size.width - leftPad - rightPad
                                if (barCount > 0) {
                                    val slotW = usableW / barCount
                                    val relX = (offset.x - leftPad).coerceIn(0f, usableW)
                                    val idx = (relX / slotW).toInt().coerceIn(0, barCount - 1)
                                    selectedIndex = idx
                                }
                            }
                        )
                    }
            ) {
                val leftPad = 16.dp.toPx()
                val rightPad = 16.dp.toPx()
                val topPad = 16.dp.toPx()
                val bottomPad = 28.dp.toPx()

                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad

                // 1. Gridlines
                val gridCount = 3
                for (i in 0..gridCount) {
                    val ratio = i.toFloat() / gridCount
                    val y = topPad + chartH - (ratio * chartH)

                    drawLine(
                        color = BorderDark.copy(alpha = 0.35f),
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // 2. Bars
                val slotW = chartW / barCount
                val barW = (slotW * 0.55f).coerceAtMost(36.dp.toPx())
                val barGap = (slotW - barW) / 2f

                coloredItems.forEachIndexed { idx, item ->
                    val isHovered = selectedIndex == idx
                    val x = leftPad + idx * slotW + barGap
                    val ratio = (item.total / maxVal).toFloat()
                    val barH = (ratio * chartH * animationProgress.value).coerceAtLeast(4.dp.toPx())
                    val y = topPad + chartH - barH

                    val alpha = if (selectedIndex == null || isHovered) 1f else 0.45f

                    // 3D-effect shadow on base
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                item.color.copy(alpha = alpha),
                                item.color.copy(alpha = alpha * 0.7f)
                            ),
                            startY = y,
                            endY = topPad + chartH
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Top highlight cap
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (isHovered) 0.5f else 0.25f),
                        topLeft = Offset(x, y),
                        size = Size(barW, 4.dp.toPx()),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        // X-Axis Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            coloredItems.forEachIndexed { idx, itm ->
                val isSelected = selectedIndex == idx
                val label = if (itm.categoryName.length > 7) "${itm.categoryName.take(6)}.." else itm.categoryName
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) itm.color else TextSecondary
                )
            }
        }
    }
}
