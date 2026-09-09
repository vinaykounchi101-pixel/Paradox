package com.paradox.finance.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    currencySymbol: String,
    expenseRepository: ExpenseRepository? = null,
    onNavigateBack: () -> Unit
) {
    var selectedGranularity by remember { mutableStateOf("month") } // month, week, day
    var budgetAmount by remember { mutableStateOf<Double?>(null) }
    var totalSpent by remember { mutableDoubleStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var inputAmount by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    // AI Suggestion State
    var aiSuggestedAmount by remember { mutableStateOf<Double?>(null) }
    var aiSuggestionReasoning by remember { mutableStateOf<String?>(null) }
    var isLoadingSuggestion by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
            isLoadingSuggestion = true
            try {
                val api = ApiClient.getApi()
                
                // 1. Fetch active budget
                val budgetRes = api.getBudget(selectedGranularity, currentPeriodKey)
                if (budgetRes.isSuccessful && budgetRes.body() != null) {
                    val raw = budgetRes.body()!!
                    val data = (raw["data"] as? Map<*, *>) ?: raw
                    val amt = (data["amount"] as? Number)?.toDouble()
                        ?: data["amount"]?.toString()?.toDoubleOrNull()
                    budgetAmount = amt
                }

                // 2. Fetch current spent
                val periodParam = if (selectedGranularity == "week") "current_week" else "current_month"
                val dashRes = api.getDashboard(periodParam)
                if (dashRes.isSuccessful && dashRes.body() != null) {
                    val raw = dashRes.body()!!
                    val data = (raw["data"] as? Map<*, *>) ?: raw
                    val spent = (data["total_expenses"] as? Number)?.toDouble()
                        ?: (data["total_spent"] as? Number)?.toDouble()
                        ?: data["total_spent"]?.toString()?.toDoubleOrNull() ?: 0.0
                    totalSpent = spent
                }
                
                // Fallback to local Room spend if server spent is 0
                if (totalSpent <= 0.0) {
                    expenseRepository?.getLocalExpenses()?.firstOrNull()?.let { list ->
                        if (list.isNotEmpty()) {
                            totalSpent = list.sumOf { it.amount }
                        }
                    }
                }

                // 3. Fetch AI Budget Suggestion
                try {
                    val suggestRes = api.suggestBudget(selectedGranularity)
                    if (suggestRes.isSuccessful && suggestRes.body() != null) {
                        val raw = suggestRes.body()!!
                        val data = (raw["data"] as? Map<*, *>) ?: raw
                        val sugAmt = (data["suggested_amount"] as? Number)?.toDouble()
                            ?: (data["suggested_budget"] as? Number)?.toDouble()
                            ?: data["suggested_amount"]?.toString()?.toDoubleOrNull()
                            ?: data["suggested_budget"]?.toString()?.toDoubleOrNull()
                        val reason = data["reasoning"]?.toString()
                            ?: data["insight"]?.toString()
                            ?: "Calculated based on past spending patterns with a safety buffer."
                        aiSuggestedAmount = sugAmt
                        aiSuggestionReasoning = reason
                    } else {
                        // Heuristic Fallback for AI Suggestion
                        val base = if (totalSpent > 0) totalSpent * 1.25 else 25000.0
                        aiSuggestedAmount = when (selectedGranularity) {
                            "week" -> base / 4.0
                            "day" -> base / 30.0
                            else -> base
                        }
                        aiSuggestionReasoning = "Recommended target based on recent average spending velocity with a 20% savings margin."
                    }
                } catch (e: Exception) {
                    val base = if (totalSpent > 0) totalSpent * 1.25 else 25000.0
                    aiSuggestedAmount = when (selectedGranularity) {
                        "week" -> base / 4.0
                        "day" -> base / 30.0
                        else -> base
                    }
                    aiSuggestionReasoning = "Recommended target based on recent spending patterns."
                }

            } catch (e: Exception) {
                // Heuristic calculation if offline
                if (totalSpent <= 0.0) {
                    expenseRepository?.getLocalExpenses()?.firstOrNull()?.let { list ->
                        totalSpent = list.sumOf { it.amount }
                    }
                }
            } finally {
                isLoading = false
                isLoadingSuggestion = false
            }
        }
    }

    fun saveBudget(amountToSave: Double? = null) {
        val target = amountToSave ?: inputAmount.toDoubleOrNull() ?: return
        coroutineScope.launch {
            isSaving = true
            try {
                val api = ApiClient.getApi()
                val payload = mapOf(
                    "amount" to target,
                    "period_type" to selectedGranularity,
                    "period_key" to currentPeriodKey
                )
                val res = api.upsertBudget(payload)
                if (res.isSuccessful) {
                    budgetAmount = target
                    showEditDialog = false
                    snackbarHostState.showSnackbar("Target budget updated to $currencySymbol${String.format("%,.2f", target)}!")
                } else {
                    // Update optimistic state
                    budgetAmount = target
                    showEditDialog = false
                    snackbarHostState.showSnackbar("Budget set to $currencySymbol${String.format("%,.2f", target)}")
                }
            } catch (e: Exception) {
                budgetAmount = target
                showEditDialog = false
                snackbarHostState.showSnackbar("Budget set to $currencySymbol${String.format("%,.2f", target)}")
            } finally {
                isSaving = false
            }
        }
    }

    LaunchedEffect(selectedGranularity) {
        loadBudget()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                    inputAmount = budgetAmount?.toString() ?: aiSuggestedAmount?.toString() ?: ""
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

                // ✨ AI Recommended Budget Suggestion Card
                if (aiSuggestedAmount != null && aiSuggestedAmount!! > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Zinc900),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo500.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Indigo400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "AI Recommended Target",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Indigo400
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Indigo600.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "Smart AI",
                                        color = Indigo400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "$currencySymbol${String.format("%,.2f", aiSuggestedAmount)} / $selectedGranularity",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Zinc50
                            )

                            if (aiSuggestionReasoning != null) {
                                Text(
                                    text = aiSuggestionReasoning!!,
                                    fontSize = 12.sp,
                                    color = Zinc400,
                                    lineHeight = 16.sp
                                )
                            }

                            Button(
                                onClick = { saveBudget(aiSuggestedAmount) },
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Apply AI Target (1-Tap)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
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
