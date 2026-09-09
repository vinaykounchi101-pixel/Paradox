package com.paradox.finance.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.core.Constants
import com.paradox.finance.ui.components.NavTab
import com.paradox.finance.ui.components.ObsidianBottomBar
import com.paradox.finance.ui.components.TrendGraphView
import com.paradox.finance.ui.components.TrendPoint
import com.paradox.finance.ui.screens.expenses.ExpenseViewModel
import com.paradox.finance.ui.screens.expenses.QuickAddExpenseDialog
import com.paradox.finance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    expenseViewModel: ExpenseViewModel? = null,
    onNavigateToExpenses: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToLeakHunter: () -> Unit,
    onNavigateToSimulator: () -> Unit,
    onNavigateToWrapped: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val expState by expenseViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) }
    val currencySymbol = Constants.CURRENCY_SYMBOLS[state.currency] ?: "₹"
    
    var isBalanceHidden by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var selectedVelocityFilter by remember { mutableStateOf("All") }

    // Auto-refresh when Dashboard is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshDashboard()
        expenseViewModel?.loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Emblem + Vault Alpha
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.refreshDashboard() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(NeonEmerald, NeonTeal))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("P", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BackgroundPitchBlack)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "PARADOX",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.5.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(NeonEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "256-BIT • VAULT ALPHA",
                                        color = NeonTeal,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Right Enclave Tier & Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .clickable { onNavigateToSettings() }
                                .padding(start = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ENCLAVE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(state.userName.ifBlank { "Tier Alpha" }, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    state.userName.firstOrNull()?.uppercase() ?: "P",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = BackgroundPitchBlack
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPitchBlack)
            )
        },
        bottomBar = {
            ObsidianBottomBar(
                currentTab = NavTab.DASHBOARD,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.DASHBOARD -> { viewModel.refreshDashboard() }
                        NavTab.QUICK_LOG -> {
                            if (expenseViewModel != null) {
                                expenseViewModel.openAddDialog()
                                showQuickAddDialog = true
                            } else {
                                onNavigateToExpenses()
                            }
                        }
                        NavTab.SIMULATOR -> { onNavigateToSimulator() }
                        NavTab.FINNY_AI -> { onNavigateToAiChat() }
                    }
                }
            )
        },
        containerColor = BackgroundPitchBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Net Worth Hero Card (Stitch Screen 1)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isBalanceHidden = !isBalanceHidden }
                                .padding(2.dp)
                        ) {
                            Text(
                                "TOTAL NET WORTH",
                                color = TextTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .clickable { onNavigateToBudget() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "+$currencySymbol 12,450 (+4.8%)",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Balance Numeric Readout
                    val displayNetWorth = if (state.totalBudget > 0) state.totalBudget - state.totalSpent else 248500.0
                    Text(
                        text = if (isBalanceHidden) "••••••••" else "$currencySymbol${String.format("%,.2f", displayNetWorth)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    // 50/30/20 Pacing Tracker Sub-bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBudget() },
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("50/30/20 Pacing", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Needs 48% • Wants 28% • Savings 24%", color = TextTertiary, fontSize = 10.sp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceObsidianSubtle),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(modifier = Modifier.weight(0.48f).fillMaxHeight().background(NeedsColor))
                            Box(modifier = Modifier.weight(0.28f).fillMaxHeight().background(WantsColor))
                            Box(modifier = Modifier.weight(0.24f).fillMaxHeight().background(SavingsColor))
                        }
                    }
                }
            }

            // 2. Monthly Spend Cap Micro-Tracker Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToBudget() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "MONTHLY SPEND CAP",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        val spent = if (state.totalSpent > 0) state.totalSpent else 13800.0
                        val limit = if (state.totalBudget > 0) state.totalBudget else 50000.0
                        Text(
                            "$currencySymbol${String.format("%,.0f", spent)} spent of $currencySymbol${String.format("%,.0f", limit)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val spent = if (state.totalSpent > 0) state.totalSpent else 13800.0
                    val limit = if (state.totalBudget > 0) state.totalBudget else 50000.0
                    val progress = (spent / limit).toFloat().coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceObsidianSubtle)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Brush.horizontalGradient(listOf(NeonEmerald, NeonTeal)))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val remaining = if (state.remainingBudget > 0) state.remainingBudget else (limit - spent).coerceAtLeast(0.0)
                        Text(
                            "$currencySymbol${String.format("%,.0f", remaining)} remaining",
                            color = NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val safeBurn = if (state.safeToSpendDaily > 0) state.safeToSpendDaily else 1087.0
                        Text(
                            "$currencySymbol${String.format("%,.0f", safeBurn)}/day Safe Burn",
                            color = NeonTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 3. 4-Column Quick Action Monoliths (Stitch Screen 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Monolith 1: + Log Expense
                Surface(
                    onClick = {
                        if (expenseViewModel != null) {
                            expenseViewModel.openAddDialog()
                            showQuickAddDialog = true
                        } else {
                            onNavigateToExpenses()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceObsidianBase,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("+ Log", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Expense", color = TextTertiary, fontSize = 9.sp)
                    }
                }

                // Monolith 2: 🛍️ Simulator
                Surface(
                    onClick = { onNavigateToSimulator() },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceObsidianBase,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Afford?", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Simulator", color = TextTertiary, fontSize = 9.sp)
                    }
                }

                // Monolith 3: ⚡ Leak Hunter
                Surface(
                    onClick = { onNavigateToLeakHunter() },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceObsidianBase,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AlertAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Leaks", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Audit", color = TextTertiary, fontSize = 9.sp)
                    }
                }

                // Monolith 4: 📊 Insights
                Surface(
                    onClick = { onNavigateToAnalytics() },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceObsidianBase,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Insights", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Velocity", color = TextTertiary, fontSize = 9.sp)
                    }
                }
            }

            // 4. Spending Velocity Curve Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SPENDING VELOCITY", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("Safe Pacing (-14% vs avg)", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Filter Pills
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("All", "UPI", "Cards", "Cash").forEach { filter ->
                                val isSelected = selectedVelocityFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(if (isSelected) SurfaceObsidianElevated else SurfaceObsidianSubtle)
                                        .border(1.dp, if (isSelected) NeonEmerald.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(9999.dp))
                                        .clickable { selectedVelocityFilter = filter }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        filter,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonEmerald else TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    val points = remember(state.trendData) {
                        if (state.trendData.isNotEmpty()) {
                            state.trendData
                        } else {
                            listOf(
                                TrendPoint("W1", 3400.0),
                                TrendPoint("W2", 2800.0),
                                TrendPoint("W3", 4200.0),
                                TrendPoint("W4", 3400.0)
                            )
                        }
                    }

                    TrendGraphView(
                        data = points,
                        currencySymbol = currencySymbol,
                        lineColor = NeonEmerald
                    )
                }
            }

            // 5. Recent Transactions Ledger Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Outflows", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "View All",
                            color = NeonEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToExpenses() }
                        )
                    }

                    val liveExpenses = expState?.expenses ?: emptyList()
                    val filteredExpenses = remember(liveExpenses, selectedVelocityFilter) {
                        if (selectedVelocityFilter == "All") liveExpenses
                        else liveExpenses.filter { it.paymentMethodName?.contains(selectedVelocityFilter, ignoreCase = true) == true }
                    }

                    if (filteredExpenses.isEmpty()) {
                        // Sample Stitch ledger rows
                        listOf(
                            Triple("Swiggy Gourmet", 640.0, "Food • UPI"),
                            Triple("Uber Premier", 320.0, "Transit • Card"),
                            Triple("Blue Tokai Coffee", 180.0, "Lifestyle • UPI")
                        ).forEach { (title, amt, sub) ->
                            TransactionRow(
                                title = title,
                                amount = amt,
                                subtitle = sub,
                                currencySymbol = currencySymbol,
                                onClick = { onNavigateToExpenses() }
                            )
                        }
                    } else {
                        filteredExpenses.take(5).forEach { exp ->
                            val sub = "${exp.categoryName ?: "Expense"} • ${exp.paymentMethodName ?: "UPI"}"
                            TransactionRow(
                                title = exp.description.ifBlank { "Expense" },
                                amount = exp.amount,
                                subtitle = sub,
                                currencySymbol = currencySymbol,
                                onClick = {
                                    expenseViewModel?.openEditDialog(exp)
                                    showQuickAddDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Add Expense Dialog (Stitch Frictionless Expense Capture)
    if (showQuickAddDialog && expenseViewModel != null) {
        QuickAddExpenseDialog(
            viewModel = expenseViewModel,
            onDismiss = {
                showQuickAddDialog = false
                viewModel.refreshDashboard()
            }
        )
    }
}

@Composable
private fun TransactionRow(
    title: String,
    amount: Double,
    subtitle: String,
    currencySymbol: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceObsidianSubtle)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceObsidianHighlight),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (title.contains("Swiggy", ignoreCase = true) || subtitle.contains("Food", ignoreCase = true)) "🍔"
                    else if (title.contains("Uber", ignoreCase = true) || subtitle.contains("Transit", ignoreCase = true)) "🚗"
                    else if (title.contains("Coffee", ignoreCase = true) || title.contains("Chai", ignoreCase = true)) "☕"
                    else "💳"
                    Text(icon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(subtitle, color = TextTertiary, fontSize = 11.sp)
                }
            }

            Text(
                "-$currencySymbol${String.format("%,.0f", amount)}",
                color = AlertCoral,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
