package com.paradox.finance.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.local.TokenManager
import com.paradox.finance.data.model.Expense
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.theme.*
import com.paradox.finance.ui.components.ParadoxCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiClient.getService(context) }

    val userCurrency by tokenManager.userCurrencyFlow.collectAsState(initial = "INR")
    val currencySymbol = if (userCurrency == "INR") "₹" else if (userCurrency == "EUR") "€" else "$"

    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadExpenses() {
        scope.launch {
            isLoading = true
            try {
                val res = apiService.getExpenses(limit = 100)
                if (res.isSuccessful) {
                    expenses = res.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // error handle
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadExpenses()
    }

    Scaffold(
        containerColor = ParadoxZinc950,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ParadoxZinc100
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ParadoxZinc100
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ParadoxZinc950)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ParadoxIndigo)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses) { expense ->
                    ParadoxCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = expense.description,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = ParadoxZinc100
                                    )
                                )
                                Text(
                                    text = "${expense.category?.name ?: "General"} • ${expense.date} • ${expense.paymentMethod?.name ?: "UPI"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = ParadoxZinc400,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "-$currencySymbol${String.format("%.2f", expense.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ParadoxRose
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                apiService.deleteExpense(expense.id)
                                                loadExpenses()
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = ParadoxZinc700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
