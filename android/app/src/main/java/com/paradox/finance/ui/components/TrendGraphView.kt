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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

data class TrendPoint(
    val label: String,
    val value: Double
)

@Composable
fun TrendGraphView(
    data: List<TrendPoint>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    lineColor: Color = PrimaryIndigo,
    gridColor: Color = BorderDark
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    if (data.isEmpty() || data.all { it.value == 0.0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(SurfaceDark, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📈", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No spending trend data recorded for this period",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        return
    }

    val maxVal = (data.maxOfOrNull { it.value } ?: 10.0).coerceAtLeast(10.0) * 1.15
    val pointCount = data.size

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected point preview / tooltip header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedIndex != null && selectedIndex in data.indices) {
                val pt = data[selectedIndex!!]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(PrimaryIndigo, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "$currencySymbol${String.format("%,.2f", pt.value)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentRose
                )
            } else {
                Text(
                    text = "Peak: $currencySymbol${String.format("%,.0f", data.maxOfOrNull { it.value } ?: 0.0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "Tap points to inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures(
                            onPress = { offset ->
                                val leftPad = 40.dp.toPx()
                                val rightPad = 16.dp.toPx()
                                val usableW = size.width - leftPad - rightPad
                                if (pointCount > 1) {
                                    val step = usableW / (pointCount - 1)
                                    val relX = (offset.x - leftPad).coerceIn(0f, usableW)
                                    val idx = (relX / step).toInt().coerceIn(0, pointCount - 1)
                                    selectedIndex = idx
                                }
                            }
                        )
                    }
            ) {
                val leftPad = 40.dp.toPx()
                val rightPad = 16.dp.toPx()
                val topPad = 16.dp.toPx()
                val bottomPad = 28.dp.toPx()

                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad

                // 1. Draw horizontal gridlines (3 lines)
                val gridCount = 3
                for (i in 0..gridCount) {
                    val ratio = i.toFloat() / gridCount
                    val y = topPad + chartH - (ratio * chartH)
                    val lineVal = maxVal * ratio

                    drawLine(
                        color = gridColor.copy(alpha = 0.4f),
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // 2. Compute coordinates of points
                val coords = mutableListOf<Offset>()
                for (i in 0 until pointCount) {
                    val x = if (pointCount == 1) leftPad + chartW / 2 else leftPad + (i.toFloat() / (pointCount - 1)) * chartW
                    val v = data[i].value * animationProgress.value
                    val y = topPad + chartH - (v.toFloat() / maxVal.toFloat()) * chartH
                    coords.add(Offset(x, y))
                }

                // 3. Build smooth cubic bezier curve
                if (coords.isNotEmpty()) {
                    val linePath = Path()
                    val areaPath = Path()

                    linePath.moveTo(coords[0].x, coords[0].y)
                    areaPath.moveTo(coords[0].x, coords[0].y)

                    for (i in 1 until coords.size) {
                        val prev = coords[i - 1]
                        val curr = coords[i]
                        val cp1x = prev.x + (curr.x - prev.x) / 2
                        val cp1y = prev.y
                        val cp2x = prev.x + (curr.x - prev.x) / 2
                        val cp2y = curr.y

                        linePath.cubicTo(cp1x, cp1y, cp2x, cp2y, curr.x, curr.y)
                        areaPath.cubicTo(cp1x, cp1y, cp2x, cp2y, curr.x, curr.y)
                    }

                    // Complete area path to bottom
                    val bottomY = topPad + chartH
                    areaPath.lineTo(coords.last().x, bottomY)
                    areaPath.lineTo(coords.first().x, bottomY)
                    areaPath.close()

                    // Draw Gradient Area
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.35f * animationProgress.value),
                                lineColor.copy(alpha = 0.0f)
                            ),
                            startY = topPad,
                            endY = bottomY
                        )
                    )

                    // Draw Line Path
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // 4. Draw Anchor Points & glow
                    coords.forEachIndexed { idx, pt ->
                        val isSelected = selectedIndex == idx

                        if (isSelected) {
                            // Pulsing glow circle
                            drawCircle(
                                color = lineColor.copy(alpha = 0.25f),
                                radius = 12.dp.toPx(),
                                center = pt
                            )
                        }

                        // Outer ring
                        drawCircle(
                            color = BackgroundDark,
                            radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                            center = pt
                        )

                        // Inner dot
                        drawCircle(
                            color = if (isSelected) AccentRose else lineColor,
                            radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        // X-Axis Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, end = 12.dp, top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEachIndexed { idx, pt ->
                val isSelected = selectedIndex == idx
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TextPrimary else TextMuted
                )
            }
        }
    }
}
