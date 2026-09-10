package com.paradox.finance.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AiViewModel
import com.paradox.finance.utils.CurrencyFormatter

@Composable
fun MonthlyWrappedModal(
    aiViewModel: AiViewModel,
    onDismiss: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }
    val wrapped by aiViewModel.monthlyWrapped.collectAsState()

    LaunchedEffect(Unit) {
        aiViewModel.loadMonthlyWrapped()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.90f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.80f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianCanvas)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header & Story Bars
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (index <= currentSlide) ElectricEmerald else SurfaceContainerHigh)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Paradox Wrapped • ${wrapped?.monthName ?: "October"}", color = ElectricEmerald, fontSize = 12.sp, style = Typography.labelLarge)
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedOutline)
                        }
                    }
                }

                // Slide Content
                val w = wrapped
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    when (currentSlide) {
                        0 -> {
                            Text(text = "Total Monthly Outflow", color = OnSurfaceVariant, fontSize = 14.sp)
                            Text(
                                text = CurrencyFormatter.format(w?.totalSpend ?: 54210.0),
                                color = OnSurfaceHigh,
                                fontSize = 32.sp,
                                style = Typography.displayLarge,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Text(text = "You managed your burn velocity within safe thresholds!", color = ElectricEmerald, fontSize = 12.sp)
                        }
                        1 -> {
                            Text(text = "Top Spending Domain", color = OnSurfaceVariant, fontSize = 14.sp)
                            Text(
                                text = w?.topCategory ?: "Dining & Gourmet",
                                color = NeonCyan,
                                fontSize = 28.sp,
                                style = Typography.headlineLarge,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Text(text = "Accounted for 34% of your total discretionary expenses.", color = OnSurface, fontSize = 12.sp)
                        }
                        2 -> {
                            Text(text = "Discipline Streak", color = OnSurfaceVariant, fontSize = 14.sp)
                            Text(
                                text = "🔥 ${w?.savingsStreak ?: 14} Days",
                                color = WarningAmber,
                                fontSize = 32.sp,
                                style = Typography.displayLarge,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Text(text = "Maintained streak without any threshold leak violations!", color = OnSurface, fontSize = 12.sp)
                        }
                        3 -> {
                            Text(text = "Your Financial Archetype", color = OnSurfaceVariant, fontSize = 14.sp)
                            Text(
                                text = w?.financialArchetype ?: "The Strategic Sovereign",
                                color = ElectricEmerald,
                                fontSize = 24.sp,
                                style = Typography.headlineMedium,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Text(text = "Balanced growth velocity with disciplined budget buffers.", color = OnSurface, fontSize = 12.sp)
                        }
                        else -> {
                            Text(text = "Finny Copilot Verdict", color = OnSurfaceVariant, fontSize = 14.sp)
                            Text(
                                text = "Grade A (88 Optimal)",
                                color = NeonEmerald,
                                fontSize = 28.sp,
                                style = Typography.headlineLarge,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                            Text(text = "Ready to conquer the next billing cycle with Paradox!", color = OnSurface, fontSize = 12.sp)
                        }
                    }
                }

                // Next Button
                Button(
                    onClick = {
                        if (currentSlide < 4) currentSlide++ else onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (currentSlide < 4) "Next Slide ➔" else "Done & Return",
                        color = PitchBlack,
                        fontSize = 13.sp,
                        style = Typography.labelLarge
                    )
                }
            }
        }
    }
}
