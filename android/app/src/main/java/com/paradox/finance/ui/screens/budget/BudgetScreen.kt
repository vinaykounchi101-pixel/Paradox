package com.paradox.finance.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    var selectedGranularity by remember { mutableStateOf("month") } // month, week, day
    var budgetAmount by remember { mutableStateOf<Double?>(null) }
    var totalSpent by remember { mutableDoubleStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var inputAmount by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val currentPeriodKey = remember(selectedGranularity) {
        val now = Date()
        when (selectedGranularity) {
            "month" -> SimpleDateFormat("yyyy-MM", Locale.US).format(now)
            "week" -> SimpleDateFormat("yyyy-'W'ww", Locale.US).format(now)
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        }
    }

    fun loadBudget() {
        coroutineScope.launch {
            isLoading = true
            try {
                val api = ApiClient.apiService
                val budgetRes = api.getBudget(selectedGranularity, currentPeriodKey)
                if (budgetRes.isSuccessful) {
                    val data = budgetRes.body()?.get("data") as? Map<*, *>
                    budgetAmount = (data?.get("amount") as? Number)?.toDouble()
                }

                val periodParam = if (selectedGranularity == "week") "current_week" else "current_month"
                val dashRes = api.getDashboard(periodParam)
                if (dashRes.isSuccessful) {
                    val data = dashRes.body()?.get("data") as? Map<*, *>
                    totalSpent = (data?.get("total_expenses") as? Number)?.toDouble() ?: 0.0
                }
            } catch (e: Exception) {
                // Ignore network failure
            } finally {
                isLoading = false
            }
        }
    }

    fun saveBudget() {
        val target = inputAmount.toDoubleOrNull() ?: return
        coroutineScope.launch {
            isSaving = true
            try {
                val api = ApiClient.apiService
                val payload = mapOf(
                    "amount" to target,
                    "period_type" to selectedGranularity,
                    "period_key" to currentPeriodKey
                )
                val res = api.upsertBudget(payload)
                if (res.isSuccessful) {
                    budgetAmount = target
                    showEditDialog = false
                }
            } catch (e: Exception) {
                // Handled
            } finally {
                isSaving = false
            }
        }
    }

    LaunchedEffect(selectedGranularity) {
        loadBudget()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Planner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadBudget() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Zinc950,
                    titleContentColor = Zinc50,
                    navigationIconContentColor = Zinc50,
                    actionIconContentColor = Indigo400
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
            // Granularity Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Zinc900)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "month" to "Monthly",
                    "week" to "Weekly",
                    "day" to "Daily"
                ).forEach { (key, label) ->
                    val isSelected = selectedGranularity == key
                    Button(
                        onClick = { selectedGranularity = key },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Indigo600 else Color.Transparent,
                            contentColor = if (isSelected) Color.White else Zinc400
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Indigo500)
                }
            } else {
                val budget = budgetAmount ?: 0.0
                val progress = if (budget > 0) (totalSpent / budget).toFloat().coerceIn(0f, 1f) else 0f
                val isOverBudget = budget > 0 && totalSpent > budget
                val remaining = if (budget > 0) (budget - totalSpent).coerceAtLeast(0.0) else 0.0

                // Main Budget Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Zinc900),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    when (selectedGranularity) {
                                        "month" -> "Monthly Target"
                                        "week" -> "Weekly Target"
                                        else -> "Daily Target"
                                    },
                                    color = Zinc400,
                                    fontSize = 13.sp
                                )
                                Text(
                                    if (budgetAmount != null) "$currencySymbol${String.format("%,.2f", budget)}" else "No Budget Set",
                                    color = Zinc50,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    inputAmount = budgetAmount?.toString() ?: ""
                                    showEditDialog = true
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Indigo600.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Budget", tint = Indigo400)
                            }
                        }

                        // Progress Meter
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = if (isOverBudget) Rose500 else if (progress > 0.85f) Amber500 else Emerald500,
                            trackColor = Zinc800
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Spent: $currencySymbol${String.format("%,.2f", totalSpent)}",
                                color = Zinc300,
                                fontSize = 13.sp
                            )
                            Text(
                                if (isOverBudget) "Over by $currencySymbol${String.format("%,.2f", totalSpent - budget)}"
                                else "Left: $currencySymbol${String.format("%,.2f", remaining)}",
                                color = if (isOverBudget) Rose400 else Emerald400,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // AI Budget Insight Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Zinc900),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡 Budget Pacing Tip", color = Indigo400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (budgetAmount == null) {
                                "Set a budget target to enable real-time financial pacing, burn rate warnings, and safe daily spend limits."
                            } else if (isOverBudget) {
                                "You have exceeded your $selectedGranularity target by $currencySymbol${String.format("%,.2f", totalSpent - budget)}. Consider cutting discretionary spending."
                            } else {
                                "You've utilized ${String.format("%.1f", progress * 100)}% of your $selectedGranularity allowance. Keep this pace to finish within target."
                            },
                            color = Zinc300,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Edit Budget Modal Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = Zinc900,
            title = {
                Text(
                    "Set ${selectedGranularity.replaceFirstChar { it.uppercase() }} Budget",
                    color = Zinc50,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your target budget amount:", color = Zinc400, fontSize = 13.sp)
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text(currencySymbol, color = Indigo400) },
                        placeholder = { Text("e.g. 25000", color = Zinc600) },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveBudget() },
                    enabled = !isSaving && inputAmount.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Save Target")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Zinc400)
                }
            }
        )
    }
}
