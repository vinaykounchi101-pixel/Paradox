package com.paradox.finance.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    var goalName by remember { mutableStateOf("Emergency Fund") }
    var targetAmount by remember { mutableStateOf("50000") }
    var monthlySavings by remember { mutableStateOf("5000") }
    
    var isCalculating by remember { mutableStateOf(false) }
    var monthsRequired by remember { mutableDoubleStateOf(10.0) }
    var aiAdvice by remember { mutableStateOf("Saving $currencySymbol$monthlySavings/month will reach this milestone in 10 months.") }
    var discretionaryCuts by remember { mutableStateOf<List<String>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    fun calculatePlan() {
        val target = targetAmount.toDoubleOrNull() ?: return
        val monthly = monthlySavings.toDoubleOrNull() ?: return
        if (monthly <= 0) return

        coroutineScope.launch {
            isCalculating = true
            try {
                val api = ApiClient.apiService
                val payload = mapOf(
                    "goal_name" to goalName,
                    "target_amount" to target,
                    "target_months" to (target / monthly).toInt().coerceAtLeast(1)
                )
                val res = api.calculateSavingsPlan(payload)
                if (res.isSuccessful) {
                    val data = res.body()?.get("data") as? Map<*, *>
                    if (data != null) {
                        aiAdvice = data["recommendation"] as? String ?: "Plan calculated successfully."
                        val cuts = data["recommended_cuts"] as? List<Map<*, *>> ?: emptyList()
                        discretionaryCuts = cuts.mapNotNull {
                            val cat = it["category"] as? String ?: "Discretionary"
                            val cutAmt = (it["suggested_cut"] as? Number)?.toDouble() ?: 0.0
                            "Trim $cat by $currencySymbol${String.format("%,.0f", cutAmt)}/mo"
                        }
                    }
                }
                monthsRequired = target / monthly
            } catch (e: Exception) {
                monthsRequired = target / monthly
                aiAdvice = "At $currencySymbol$monthly/month, you will complete '$goalName' in ${String.format("%.1f", monthsRequired)} months."
            } finally {
                isCalculating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        calculatePlan()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goal Optimizer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Zinc950,
                    titleContentColor = Zinc50,
                    navigationIconContentColor = Zinc50
                )
            )
        },
        containerColor = Zinc950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Goal Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Zinc900),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("🎯 Define Savings Goal", color = Zinc50, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Goal Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Zinc50,
                            unfocusedTextColor = Zinc200,
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Zinc700,
                            focusedContainerColor = Zinc800,
                            unfocusedContainerColor = Zinc800
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it },
                        label = { Text("Target Amount") },
                        prefix = { Text(currencySymbol, color = Indigo400) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Zinc50,
                            unfocusedTextColor = Zinc200,
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Zinc700,
                            focusedContainerColor = Zinc800,
                            unfocusedContainerColor = Zinc800
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = monthlySavings,
                        onValueChange = { monthlySavings = it },
                        label = { Text("Monthly Contribution") },
                        prefix = { Text(currencySymbol, color = Emerald400) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Zinc50,
                            unfocusedTextColor = Zinc200,
                            focusedBorderColor = Indigo500,
                            unfocusedBorderColor = Zinc700,
                            focusedContainerColor = Zinc800,
                            unfocusedContainerColor = Zinc800
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { calculatePlan() },
                        enabled = !isCalculating,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCalculating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Text("⚡ Optimize Feasibility", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Milestone Estimation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Zinc900),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⏱️ Timeline Feasibility", color = Emerald400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "${String.format("%.1f", monthsRequired)} Months",
                        color = Zinc50,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(aiAdvice, color = Zinc300, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            // AI Discretionary Cut Suggestions
            if (discretionaryCuts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Zinc900),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✂️ Suggested Category Cuts", color = Amber400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        discretionaryCuts.forEach { cut ->
                            Text("• $cut", color = Zinc300, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
