package com.paradox.finance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*
import com.paradox.finance.utils.CurrencyFormatter

@Composable
fun TrendGraphView(
    dataPoints: List<Double>,
    currency: String = "INR",
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val points = if (dataPoints.isEmpty()) listOf(1200.0, 1800.0, 1400.0, 2600.0, 2100.0, 3100.0, 2450.0) else dataPoints
    val maxVal = (points.maxOrNull() ?: 3000.0) * 1.25
    val minVal = 0.0

    Column(modifier = modifier) {
        if (selectedIndex != null && selectedIndex!! in points.indices) {
            val amount = points[selectedIndex!!]
            Text(
                text = "Day ${selectedIndex!! + 1}: ${CurrencyFormatter.format(amount, currency)}",
                color = ElectricEmerald,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                        val index = (offset.x / stepX).toInt().coerceIn(0, points.size - 1)
                        selectedIndex = index
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val stepX = width / (points.size - 1).coerceAtLeast(1)

            // Benchmark Threshold Dashed Line (Average)
            val avg = points.average()
            val avgY = height - ((avg - minVal) / (maxVal - minVal) * height).toFloat()
            drawLine(
                color = OutlineVariant,
                start = Offset(0f, avgY),
                end = Offset(width, avgY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Calculate Curve Path
            val path = Path()
            val fillPath = Path()

            val coordinates = points.mapIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - minVal) / (maxVal - minVal) * height).toFloat()
                Offset(x, y)
            }

            if (coordinates.isNotEmpty()) {
                path.moveTo(coordinates[0].x, coordinates[0].y)
                fillPath.moveTo(coordinates[0].x, height)
                fillPath.lineTo(coordinates[0].x, coordinates[0].y)

                for (i in 0 until coordinates.size - 1) {
                    val p0 = coordinates[i]
                    val p1 = coordinates[i + 1]
                    val cx = (p0.x + p1.x) / 2
                    path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(coordinates.last().x, height)
                fillPath.close()

                // Draw Area Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonEmerald.copy(alpha = 0.35f),
                            NeonCyan.copy(alpha = 0.10f),
                            PitchBlack.copy(alpha = 0.0f)
                        )
                    )
                )

                // Draw Cubic Bezier Stroke
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(NeonEmerald, NeonCyan)),
                    style = Stroke(width = 3.5.dp.toPx())
                )

                // Draw Point Anchors
                coordinates.forEachIndexed { index, offset ->
                    if (selectedIndex == index) {
                        drawCircle(color = NeonEmerald, radius = 6.dp.toPx(), center = offset)
                        drawCircle(color = PitchBlack, radius = 3.dp.toPx(), center = offset)
                    }
                }
            }
        }
    }
}
