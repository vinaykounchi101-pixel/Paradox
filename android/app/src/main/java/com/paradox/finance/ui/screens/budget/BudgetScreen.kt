package com.paradox.finance.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AuthViewModel
import com.paradox.finance.ui.viewmodels.BudgetViewModel
import com.paradox.finance.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgetViewModel: BudgetViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val budgetStatus by budgetViewModel.budgetStatus.collectAsState()
    val periodType by budgetViewModel.currentPeriodType.collectAsState()
    val currentCurrency by authViewModel.currentCurrency.collectAsState()

    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var targetInput by remember { mutableStateOf("60000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget & Pacing Planner", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period Type Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("monthly", "weekly", "daily").forEach { type ->
                    val isSelected = periodType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) ElectricEmerald.copy(alpha = 0.2f) else GlassSurface1)
                            .border(1.dp, if (isSelected) ElectricEmerald else GlassBorderStroke, RoundedCornerShape(9999.dp))
                            .clickable { budgetViewModel.setPeriodType(type) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.replaceFirstChar { it.uppercase() },
                            color = if (isSelected) ElectricEmerald else OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Master Target Hero Card
            val status = budgetStatus
            val target = status?.budgetAmount ?: 60000.0
            val spent = status?.spentAmount ?: 42150.0
            val remaining = (target - spent).coerceAtLeast(0.0)
            val utilPct = if (target > 0) ((spent / target) * 100).toInt() else 0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Target Budget Cap", color = OnSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = CurrencyFormatter.format(target, currentCurrency),
                        color = OnSurfaceHigh,
                        fontSize = 32.sp,
                        style = Typography.displayLarge
                    )

                    // Linear Progress Indicator
                    LinearProgressIndicator(
                        progress = { (spent / target).toFloat().coerceIn(0f, 1f) },
                        color = if (utilPct > 100) AlertCoral else ElectricEmerald,
                        trackColor = SurfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(9999.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Spent: ${CurrencyFormatter.format(spent, currentCurrency)} ($utilPct%)", color = OnSurfaceVariant, fontSize = 11.sp)
                        Text(text = "Remaining: ${CurrencyFormatter.format(remaining, currentCurrency)}", color = ElectricEmerald, fontSize = 11.sp)
                    }
                }
            }

            // Pacing Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface2)
                    .border(1.dp, ElectricEmerald.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(text = "🟢 Safe Pacing Status", color = ElectricEmerald, fontSize = 13.sp, style = Typography.labelLarge)
                    Text(
                        text = "Projected budget surplus of ${CurrencyFormatter.format(remaining * 0.25, currentCurrency)} at end of cycle.",
                        color = OnSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Set Target Button
            Button(
                onClick = { showSetBudgetDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                shape = RoundedCornerShape(9999.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "+ Set New Target Cap", color = PitchBlack, fontSize = 13.sp, style = Typography.labelLarge)
            }
        }
    }

    if (showSetBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showSetBudgetDialog = false },
            title = { Text("Set Target Budget Cap", color = OnSurfaceHigh) },
            text = {
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Budget Amount (₹)", color = OnSurfaceVariant) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = NeonCyan
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = targetInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            budgetViewModel.setBudgetTarget(amt)
                            showSetBudgetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald)
                ) {
                    Text("Save", color = PitchBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetBudgetDialog = false }) {
                    Text("Cancel", color = MutedOutline)
                }
            },
            containerColor = ObsidianCanvas
        )
    }
}
