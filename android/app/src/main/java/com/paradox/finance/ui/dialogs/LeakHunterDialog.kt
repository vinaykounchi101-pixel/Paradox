package com.paradox.finance.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
fun LeakHunterDialog(
    aiViewModel: AiViewModel,
    onDismiss: () -> Unit
) {
    val leakAnalysis by aiViewModel.leakAnalysis.collectAsState()
    var selectedThreshold by remember { mutableStateOf(150.0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ObsidianCanvas)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Leak Hunter & Radar", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineSmall)
                        Text(text = "🔥 14-Day Leak-Free Streak", color = ElectricEmerald, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedOutline)
                    }
                }

                // Threshold Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50.0, 100.0, 150.0, 300.0, 500.0).forEach { th ->
                        val isSelected = selectedThreshold == th
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (isSelected) GlassSurface2 else GlassSurface1)
                                .border(1.dp, if (isSelected) WarningAmber else GlassBorderStroke, RoundedCornerShape(9999.dp))
                                .clickable {
                                    selectedThreshold = th
                                    aiViewModel.loadLeakAnalysis(th)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "<₹${th.toInt()}",
                                color = if (isSelected) WarningAmber else OnSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Annual Drain Banner
                val leak = leakAnalysis
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassSurface1)
                        .border(1.dp, AlertCoral.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(text = "🚨 Projected Annual Micro-Drain", color = AlertCoral, fontSize = 12.sp, style = Typography.labelLarge)
                        Text(
                            text = CurrencyFormatter.format(leak?.projectedAnnualDrain ?: 38400.0),
                            color = OnSurfaceHigh,
                            fontSize = 24.sp,
                            style = Typography.displayMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Based on ${CurrencyFormatter.format(leak?.totalMonthlyLeak ?: 3200.0)}/month in sub-threshold spends",
                            color = MutedOutline,
                            fontSize = 11.sp
                        )
                    }
                }

                // Leak Clusters List
                Text(text = "Detected Repetitive Clusters", color = OnSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))

                val items = leak?.leaks ?: emptyList()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface1)
                                .border(1.dp, GlassBorderStroke, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.displayName, color = OnSurfaceHigh, fontSize = 13.sp, style = Typography.labelLarge)
                                    Text(text = "${item.frequencyCount} txns • ${item.displayPlugTip}", color = OnSurfaceVariant, fontSize = 11.sp)
                                }
                                Text(
                                    text = "${CurrencyFormatter.format(item.monthlyCost)}/mo",
                                    color = AlertCoral,
                                    fontSize = 13.sp,
                                    style = Typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
