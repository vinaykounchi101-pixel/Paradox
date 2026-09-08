package com.paradox.finance.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.local.TokenManager
import com.paradox.finance.data.model.*
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.theme.*
import com.paradox.finance.ui.components.ParadoxCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToExpenses: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiClient.getService(context) }

    val userCurrency by tokenManager.userCurrencyFlow.collectAsState(initial = "INR")
    val currencySymbol = if (userCurrency == "INR") "₹" else if (userCurrency == "EUR") "€" else "$"

    var isRefreshing by remember { mutableStateOf(false) }
    var dashboardSummary by remember { mutableStateOf<DashboardSummaryResponse?>(null) }
    var safeToSpend by remember { mutableStateOf<SafeToSpendResponse?>(null) }

    fun loadData() {
        scope.launch {
            isRefreshing = true
            try {
                val summaryRes = apiService.getDashboardSummary()
                if (summaryRes.isSuccessful) dashboardSummary = summaryRes.body()

                val safeRes = apiService.getSafeToSpend()
                if (safeRes.isSuccessful) safeToSpend = safeRes.body()
            } catch (e: Exception) {
                // Keep local state on error
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        containerColor = ParadoxZinc950,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚡ Paradox",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ParadoxZinc100
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ParadoxZinc400
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            tokenManager.clearSession()
                            onLogout()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = ParadoxRose
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ParadoxZinc950)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddExpense,
                containerColor = ParadoxIndigo,
                contentColor = ParadoxZinc100,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Finny Mascot & Mood Card
            item {
                ParadoxCard(
                    borderColor = ParadoxIndigo.copy(alpha = 0.4f),
                    backgroundColor = ParadoxZinc900
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🤖",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Finny AI Copilot",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ParadoxIndigoLight
                                )
                            )
                            Text(
                                text = safeToSpend?.headline ?: "Monitoring your financial health in real-time.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc100)
                            )
                        }
                    }
                }
            }

            // Safe to Spend Gauge
            item {
                ParadoxCard(
                    borderColor = ParadoxEmerald.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "SAFE-TO-SPEND TODAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ParadoxEmerald,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${String.format("%.2f", safeToSpend?.safeDailyAllowance ?: 0.0)}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = ParadoxZinc100
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = safeToSpend?.tip ?: "Calculated from your monthly budget pacing & fixed subscriptions.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400)
                    )
                }
            }

            // Monthly Total Card
            item {
                ParadoxCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "THIS MONTH'S SPENT",
                                style = MaterialTheme.typography.labelSmall.copy(color = ParadoxZinc400)
                            )
                            Text(
                                text = "$currencySymbol${String.format("%.2f", dashboardSummary?.totalMonthlySpent ?: 0.0)}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ParadoxZinc100
                                )
                            )
                        }
                        dashboardSummary?.budgetStatus?.let { budget ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (budget.status == "exceeded") ParadoxRose.copy(alpha = 0.2f) else ParadoxIndigo.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${budget.percentageUsed.toInt()}% of budget",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (budget.status == "exceeded") ParadoxRose else ParadoxIndigoLight,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Recent Expenses Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = ParadoxZinc100
                        )
                    )
                    Text(
                        text = "View All →",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ParadoxIndigo,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.clickable { onNavigateToExpenses() }
                    )
                }
            }

            // Transactions list
            val expenses = dashboardSummary?.recentExpenses ?: emptyList()
            if (expenses.isEmpty()) {
                item {
                    ParadoxCard {
                        Text(
                            text = "No expenses recorded yet. Tap + to log your first transaction!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400)
                        )
                    }
                }
            } else {
                items(expenses) { expense ->
                    ParadoxCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = ParadoxZinc900
                    ) {
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
                                    text = "${expense.category?.name ?: "General"} • ${expense.date}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = ParadoxZinc400,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Text(
                                text = "-$currencySymbol${String.format("%.2f", expense.amount)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ParadoxRose
                                )
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
