package com.paradox.finance.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.models.ExpenseDto
import com.paradox.finance.ui.components.*
import com.paradox.finance.ui.dialogs.*
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AiViewModel
import com.paradox.finance.ui.viewmodels.AuthViewModel
import com.paradox.finance.ui.viewmodels.DashboardViewModel
import com.paradox.finance.ui.viewmodels.ExpenseViewModel
import com.paradox.finance.utils.CurrencyFormatter

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    expenseViewModel: ExpenseViewModel,
    aiViewModel: AiViewModel,
    authViewModel: AuthViewModel,
    onNavigateToExpenses: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToFinnyChat: () -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by dashboardViewModel.uiState.collectAsState()
    val expenses by expenseViewModel.expenses.collectAsState()
    val currentCurrency by authViewModel.currentCurrency.collectAsState()

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showSimulatorDialog by remember { mutableStateOf(false) }
    var showLeakHunterDialog by remember { mutableStateOf(false) }
    var showWrappedModal by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var selectedExpenseToEdit by remember { mutableStateOf<ExpenseDto?>(null) }

    Scaffold(
        bottomBar = {
            ObsidianBottomBar(
                currentDestination = BottomBarDestination.DASHBOARD,
                onNavigate = { dest ->
                    when (dest) {
                        BottomBarDestination.DASHBOARD -> {}
                        BottomBarDestination.LOG_EXPENSE -> showQuickAddDialog = true
                        BottomBarDestination.SIMULATOR -> showSimulatorDialog = true
                        BottomBarDestination.FINNY_AI -> onNavigateToFinnyChat()
                    }
                }
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Wordmark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(ElectricEmerald, CyanContainer))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "P", color = PitchBlack, fontSize = 18.sp, style = Typography.labelLarge)
                    }
                    Text(text = "PARADOX", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineMedium)
                }

                // Controls: Currency Pill & Profile
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Currency Pill
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(GlassSurface2)
                                .border(1.dp, GlassBorderStroke, RoundedCornerShape(9999.dp))
                                .clickable { showCurrencyMenu = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${CurrencyFormatter.getSymbol(currentCurrency)} $currentCurrency",
                                color = ElectricEmerald,
                                fontSize = 11.sp,
                                style = Typography.labelSmall
                            )
                        }

                        DropdownMenu(
                            expanded = showCurrencyMenu,
                            onDismissRequest = { showCurrencyMenu = false },
                            modifier = Modifier.background(SurfaceContainerHigh)
                        ) {
                            listOf("INR", "USD", "EUR", "GBP").forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text("${CurrencyFormatter.getSymbol(curr)} $curr", color = OnSurface) },
                                    onClick = {
                                        authViewModel.setCurrency(curr)
                                        showCurrencyMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Notification Bell (Wrapped trigger)
                    IconButton(onClick = { showWrappedModal = true }) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Alerts", tint = OnSurfaceVariant)
                    }

                    // Profile / Sign Out
                    IconButton(onClick = { showProfileDialog = true }) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GlassSurface2)
                                .border(1.dp, ElectricEmerald.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Profile", tint = ElectricEmerald, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Total Net Worth Hero Card
            val totalBal = uiState.summary?.totalBalance ?: 842650.0
            val monthSpend = uiState.summary?.monthSpend ?: 54210.0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(GlassSurface2, GlassSurface1)
                        )
                    )
                    .border(1.dp, GlassBorderActive, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total Net Worth", color = OnSurfaceVariant, fontSize = 12.sp)
                        IconButton(onClick = { dashboardViewModel.toggleBalanceVisibility() }) {
                            Icon(
                                imageVector = if (uiState.isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle",
                                tint = MutedOutline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = if (uiState.isBalanceVisible) CurrencyFormatter.format(totalBal, currentCurrency) else "••••••••",
                        color = OnSurfaceHigh,
                        fontSize = 32.sp,
                        style = Typography.displayLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "This Month Outflow: ${CurrencyFormatter.format(monthSpend, currentCurrency)}",
                            color = AlertCoral,
                            fontSize = 11.sp
                        )
                        Text(text = "Velocity: -8.4% vs last cycle", color = ElectricEmerald, fontSize = 11.sp)
                    }

                    // 50/30/20 Segmented Progress Bar
                    FiftyThirtyTwentyBar(
                        needsPct = uiState.fiftyThirtyTwenty?.needsPercentage ?: 50.0,
                        wantsPct = uiState.fiftyThirtyTwenty?.wantsPercentage ?: 30.0,
                        savingsPct = uiState.fiftyThirtyTwenty?.savingsPercentage ?: 20.0,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Safe-to-Spend Speedometer & Gauge
            SafeToSpendGauge(
                dailyAllowance = uiState.safeToSpend?.safeDailySpend ?: 2450.0,
                pacingStatus = uiState.safeToSpend?.pacingStatus ?: "Optimal",
                daysLeft = uiState.safeToSpend?.daysLeft ?: 12,
                currency = currentCurrency
            )

            // Finny Mascot Companion Card
            FinnyMascotView(
                mood = uiState.finnyVibe?.mood ?: "JOYFUL",
                speechText = uiState.finnyVibe?.roastOrPraise ?: "Great pacing, Vikram! Your recurring subscriptions are down 14% this week. Tap to simulate weekend plans.",
                onClick = onNavigateToFinnyChat
            )

            // 4 Quick Action Monoliths (2x2 Grid)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // + Log Expense Monolith
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(listOf(ElectricEmerald.copy(alpha = 0.25f), GlassSurface1))
                            )
                            .border(1.dp, ElectricEmerald.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { showQuickAddDialog = true }
                            .padding(14.dp)
                    ) {
                        Column {
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Log", tint = ElectricEmerald, modifier = Modifier.size(24.dp))
                            Text(text = "+ Log Expense", color = OnSurfaceHigh, fontSize = 13.sp, style = Typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            Text(text = "Instant 0ms Capture", color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                    }

                    // Afford? Simulator Monolith
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.20f), GlassSurface1))
                            )
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { showSimulatorDialog = true }
                            .padding(14.dp)
                    ) {
                        Column {
                            Icon(imageVector = Icons.Default.Calculate, contentDescription = "Simulate", tint = NeonCyan, modifier = Modifier.size(24.dp))
                            Text(text = "Afford? Check", color = OnSurfaceHigh, fontSize = 13.sp, style = Typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            Text(text = "Stress Test Purchases", color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Leaks Audit Monolith
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(listOf(WarningAmber.copy(alpha = 0.20f), GlassSurface1))
                            )
                            .border(1.dp, WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { showLeakHunterDialog = true }
                            .padding(14.dp)
                    ) {
                        Column {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Leaks", tint = WarningAmber, modifier = Modifier.size(24.dp))
                            Text(text = "Leaks Audit", color = OnSurfaceHigh, fontSize = 13.sp, style = Typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            Text(text = "Projected Drain Radar", color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                    }

                    // Insights Velocity Monolith
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(listOf(VibrantIndigo.copy(alpha = 0.20f), GlassSurface1))
                            )
                            .border(1.dp, VibrantIndigo.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable(onClick = onNavigateToAnalytics)
                            .padding(14.dp)
                    ) {
                        Column {
                            Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Insights", tint = SoftIndigo, modifier = Modifier.size(24.dp))
                            Text(text = "Insights Velocity", color = OnSurfaceHigh, fontSize = 13.sp, style = Typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            Text(text = "Category Breakdown", color = OnSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Spending Velocity Bezier Curve Graph Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Spending Velocity", color = OnSurfaceHigh, fontSize = 14.sp, style = Typography.headlineSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("All", "UPI", "Cards", "Cash").forEachIndexed { idx, filter ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(if (idx == 0) ElectricEmerald.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.dp, if (idx == 0) ElectricEmerald else GlassBorderStroke, RoundedCornerShape(9999.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = filter, color = if (idx == 0) ElectricEmerald else MutedOutline, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    TrendGraphView(
                        dataPoints = expenses.take(10).map { it.amount }.reversed(),
                        currency = currentCurrency,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            // Recent Activity Ledger
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Recent Activity", color = OnSurfaceHigh, fontSize = 14.sp, style = Typography.headlineSmall)
                    Text(
                        text = "See All",
                        color = ElectricEmerald,
                        fontSize = 12.sp,
                        style = Typography.labelSmall,
                        modifier = Modifier.clickable(onClick = onNavigateToExpenses)
                    )
                }

                if (expenses.isEmpty()) {
                    Text(text = "No expenses recorded yet. Tap '+ Log Expense' to add!", color = MutedOutline, fontSize = 12.sp)
                } else {
                    expenses.take(4).forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassSurface1)
                                .border(1.dp, GlassBorderStroke, RoundedCornerShape(12.dp))
                                .clickable { selectedExpenseToEdit = item }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceContainerHigh),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Receipt, contentDescription = "Receipt", tint = ElectricEmerald, modifier = Modifier.size(18.dp))
                                    }
                                    Column {
                                        Text(text = item.displayDescription, color = OnSurfaceHigh, fontSize = 13.sp, style = Typography.labelLarge)
                                        Text(text = "${item.category?.displayName ?: "Expense"} • ${item.displayDate}", color = OnSurfaceVariant, fontSize = 11.sp)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "-${CurrencyFormatter.format(item.amount, currentCurrency)}",
                                        color = OnSurfaceHigh,
                                        fontSize = 13.sp,
                                        style = Typography.labelLarge
                                    )
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MutedOutline, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Dialogs
    if (showQuickAddDialog || selectedExpenseToEdit != null) {
        QuickAddExpenseDialog(
            expenseViewModel = expenseViewModel,
            existingExpense = selectedExpenseToEdit,
            onDismiss = {
                showQuickAddDialog = false
                selectedExpenseToEdit = null
                dashboardViewModel.loadDashboardData()
            }
        )
    }

    if (showSimulatorDialog) {
        PurchaseSimulatorDialog(
            aiViewModel = aiViewModel,
            expenseViewModel = expenseViewModel,
            onDismiss = { showSimulatorDialog = false }
        )
    }

    if (showLeakHunterDialog) {
        LeakHunterDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showLeakHunterDialog = false }
        )
    }

    if (showWrappedModal) {
        MonthlyWrappedModal(
            aiViewModel = aiViewModel,
            onDismiss = { showWrappedModal = false }
        )
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = SurfaceContainerHigh,
            title = {
                Text("User Profile & Security", color = OnSurfaceHigh, style = Typography.headlineMedium, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Logged in as:", color = OnSurfaceVariant, fontSize = 12.sp)
                    Text("vinay@paradox.com", color = ElectricEmerald, fontSize = 14.sp, style = Typography.labelLarge)
                    Text("Active Currency: $currentCurrency", color = OnSurface, fontSize = 12.sp)
                    Text("AI Engine: Gemini 3.6 Flash • RAG Synced", color = LuminousCyan, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProfileDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCoral)
                ) {
                    Text("Sign Out", color = PitchBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = OnSurfaceVariant)
                }
            }
        )
    }
}
