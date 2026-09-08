package com.paradox.finance.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.paradox.finance.ui.components.CategoryBarChartView
import com.paradox.finance.ui.components.FinnyMascotView
import com.paradox.finance.ui.components.TrendGraphView
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
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
    val currencySymbol = Constants.CURRENCY_SYMBOLS[state.currency] ?: "₹"
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDark,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PrimaryIndigo, AccentEmerald))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("P", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("PARADOX", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextPrimary)
                            Text(state.userName.ifBlank { "Investor" }, fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 6.dp))

                    // Navigation Items
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = PrimaryIndigo) },
                        label = { Text("Dashboard", fontWeight = FontWeight.SemiBold) },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = PrimaryIndigo.copy(alpha = 0.15f),
                            selectedTextColor = PrimaryIndigo
                        )
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondary) },
                        label = { Text("Expenses") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToExpenses()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null, tint = TextSecondary) },
                        label = { Text("Spending Analytics") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToAnalytics()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.TrackChanges, contentDescription = null, tint = TextSecondary) },
                        label = { Text("Budget Planner") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToBudget()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Repeat, contentDescription = null, tint = TextSecondary) },
                        label = { Text("Subscriptions & Bills") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSubscriptions()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Savings, contentDescription = null, tint = TextSecondary) },
                        label = { Text("Savings Goals") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToGoals()
                        }
                    )

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 6.dp))
                    Text("AI SUPERPOWERS", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentAmber) },
                        label = { Text("Leak Hunter") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToLeakHunter()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = AccentEmerald) },
                        label = { Text("Purchase Simulator") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSimulator()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = AccentViolet) },
                        label = { Text("Finny AI Chat") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToAiChat()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFEC4899)) },
                        label = { Text("Monthly Wrapped") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToWrapped()
                        }
                    )

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 6.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary) },
                        label = { Text("Settings & Profile") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToSettings()
                        }
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AccentRose) },
                        label = { Text("Sign Out", color = AccentRose, fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PARADOX", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    },
                    actions = {
                        // Currency Switcher Pill
                        Box {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                                modifier = Modifier
                                    .clickable { showCurrencyDropdown = true }
                                    .padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$currencySymbol ${state.currency}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = PrimaryIndigo
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showCurrencyDropdown,
                                onDismissRequest = { showCurrencyDropdown = false }
                            ) {
                                Constants.SUPPORTED_CURRENCIES.forEach { curr ->
                                    DropdownMenuItem(
                                        text = { Text("${Constants.CURRENCY_SYMBOLS[curr]} $curr") },
                                        onClick = {
                                            viewModel.setCurrency(curr)
                                            showCurrencyDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Finny Mascot
                FinnyMascotView(
                    mood = state.finnyMood,
                    speechText = state.finnySpeech,
                    onClick = onNavigateToAiChat
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Wrapped Visual Story Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWrapped() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFFEC4899), PrimaryIndigo))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Paradox Monthly Wrapped", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Your monthly financial story is ready", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryIndigo)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Monthly Outflow & Total Spent Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("This Month's Spending", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol${String.format("%,.2f", state.totalSpent)}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentRose
                            )
                        }

                        if (state.totalBudget > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Budget", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = "$currencySymbol${String.format("%,.0f", state.totalBudget)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Safe-to-Spend Speedometer Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Daily Safe Allowance", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            Surface(
                                color = AccentEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Score: ${state.healthScore}/100",
                                    color = AccentEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$currencySymbol${String.format("%.0f", state.safeToSpendDaily)} / day",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentEmerald))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remaining Buffer: $currencySymbol${String.format("%.2f", state.remainingBudget)} (${state.burnVelocity})",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spending Trends Curve Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Spending Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("Weekly Aggregate", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TrendGraphView(
                            data = state.trendData,
                            currencySymbol = currencySymbol
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spending Categories Breakdown Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PieChart, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Spending Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("By Category", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryBarChartView(
                            items = state.categoryBreakdown,
                            currencySymbol = currencySymbol
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 50/30/20 Budget Rule Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("50 / 30 / 20 Budget Rule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented Progress Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            Box(modifier = Modifier.weight(0.5f).fillMaxHeight().background(NeedsColor))
                            Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(WantsColor))
                            Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(SavingsColor))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BudgetItem("Needs (50%)", "$currencySymbol${String.format("%.0f", state.needsSpent)}", NeedsColor)
                            BudgetItem("Wants (30%)", "$currencySymbol${String.format("%.0f", state.wantsSpent)}", WantsColor)
                            BudgetItem("Savings (20%)", "$currencySymbol${String.format("%.0f", state.savingsSpent)}", SavingsColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // AI Superpowers Grid: Leak Hunter & Purchase Simulator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNavigateToLeakHunter,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("🔍 Leak Hunter", fontSize = 12.sp, color = AccentAmber, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateToSimulator,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("🛍️ Simulator", fontSize = 12.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Financial Suite Tools Grid: Analytics, Budget, Subscriptions, Goals
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNavigateToAnalytics,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("📊 Analytics", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onNavigateToBudget,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("🎯 Budget", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNavigateToSubscriptions,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("🔁 Recurring", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onNavigateToGoals,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("✨ Goals", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Navigation: Expenses & Finny Chat
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onNavigateToExpenses,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Expenses")
                    }

                    Button(
                        onClick = onNavigateToAiChat,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finny AI", color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun BudgetItem(label: String, value: String, dotColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
