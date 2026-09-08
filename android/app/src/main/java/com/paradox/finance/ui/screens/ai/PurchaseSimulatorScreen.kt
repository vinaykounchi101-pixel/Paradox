package com.paradox.finance.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSimulatorScreen(
    currencySymbol: String,
    currentBuffer: Double = 15000.0,
    dailyAllowance: Double = 500.0,
    onNavigateBack: () -> Unit,
    onAddExpense: (Double, String) -> Unit
) {
    var itemTitle by remember { mutableStateOf("") }
    var itemAmount by remember { mutableStateOf("") }
    var simulatedResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun simulate() {
        val amount = itemAmount.toDoubleOrNull() ?: return
        val newBuffer = currentBuffer - amount
        val daysOfBuffer = amount / (dailyAllowance.coerceAtLeast(1.0))

        if (amount > currentBuffer) {
            simulatedResult = Pair(
                false,
                "⚠️ This purchase exceeds your remaining monthly buffer of $currencySymbol${String.format("%.0f", currentBuffer)} by $currencySymbol${String.format("%.0f", amount - currentBuffer)}!"
            )
        } else if (daysOfBuffer > 10) {
            simulatedResult = Pair(
                false,
                "⚡ Caution: This purchase consumes ${String.format("%.1f", daysOfBuffer)} days worth of daily safe allowance! Remaining buffer drops to $currencySymbol${String.format("%.0f", newBuffer)}."
            )
        } else {
            simulatedResult = Pair(
                true,
                "✅ Safe Purchase! You have ample buffer remaining ($currencySymbol${String.format("%.0f", newBuffer)}). Pacing remains healthy."
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛍️ 'Can I Afford This?'", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Pre-Purchase Decision Simulator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Simulate the impact of a planned purchase before spending.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Item Name
            OutlinedTextField(
                value = itemTitle,
                onValueChange = { itemTitle = it },
                label = { Text("What do you want to buy?") },
                placeholder = { Text("e.g. Sony WH-1000XM5") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Item Price
            OutlinedTextField(
                value = itemAmount,
                onValueChange = { itemAmount = it },
                label = { Text("Price ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { simulate() },
                enabled = itemAmount.toDoubleOrNull() != null && itemAmount.toDouble() > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Impact", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulation Result Banner
            AnimatedVisibility(visible = simulatedResult != null) {
                val (isSafe, message) = simulatedResult ?: return@AnimatedVisibility
                val color = if (isSafe) AccentEmerald else AccentRose

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (isSafe) "🟢 AFFORDABLE" else "🔴 FINANCIAL ALERT",
                            color = color,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val amt = itemAmount.toDoubleOrNull() ?: 0.0
                                onAddExpense(amt, itemTitle.ifBlank { "Purchase" })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("➕ Add as Expense Anyway", color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}
