package com.paradox.finance.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AiViewModel
import com.paradox.finance.ui.viewmodels.ExpenseViewModel
import com.paradox.finance.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSimulatorDialog(
    aiViewModel: AiViewModel,
    expenseViewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("18500") }
    var itemNote by remember { mutableStateOf("Sony WH-1000XM5") }
    val simulationResult by aiViewModel.simulationResult.collectAsState()
    val isThinking by aiViewModel.isAiThinking.collectAsState()

    LaunchedEffect(Unit) {
        aiViewModel.simulatePurchase(18500.0)
    }

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
                    Text(
                        text = "Purchase Stress Simulator",
                        color = OnSurfaceHigh,
                        fontSize = 16.sp,
                        style = Typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedOutline)
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        val amt = it.toDoubleOrNull()
                        if (amt != null && amt > 0) aiViewModel.simulatePurchase(amt)
                    },
                    label = { Text("Simulate Amount (₹)", color = OnSurfaceVariant, fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                // Decision Card
                val res = simulationResult
                if (res != null) {
                    val verdictColor = if (res.isAffordable) ElectricEmerald else WarningAmber
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassSurface1)
                            .border(1.dp, verdictColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = res.displayVerdict,
                                color = verdictColor,
                                fontSize = 15.sp,
                                style = Typography.labelLarge
                            )
                            Text(
                                text = "Current Buffer: ${CurrencyFormatter.format(res.currentBuffer)} ➔ Projected: ${CurrencyFormatter.format(res.projectedBuffer)}",
                                color = OnSurface,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Daily allowance drops from ${CurrencyFormatter.format(res.currentDailyAllowance)} to ${CurrencyFormatter.format(res.projectedDailyAllowance)}/day",
                                color = OnSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "💡 Finny Advice: ${res.displayAdvice}",
                                color = LuminousCyan,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else if (isThinking) {
                    CircularProgressIndicator(
                        color = ElectricEmerald,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                }

                // Action: Proceed Anyway & Log
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            expenseViewModel.createExpense(
                                amount = amt,
                                description = itemNote.ifBlank { "Simulated Purchase" },
                                date = today,
                                categoryId = null,
                                onSuccess = onDismiss
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(text = "Proceed & Log Expense", color = PitchBlack, fontSize = 13.sp, style = Typography.labelLarge)
                }
            }
        }
    }
}
