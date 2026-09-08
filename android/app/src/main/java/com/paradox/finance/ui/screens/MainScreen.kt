package com.paradox.finance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.local.TokenManager
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.ui.screens.analytics.AnalyticsScreen
import com.paradox.finance.ui.screens.chat.AiChatScreen
import com.paradox.finance.ui.screens.dashboard.DashboardScreen
import com.paradox.finance.ui.screens.expenses.AddExpenseBottomSheet
import com.paradox.finance.ui.screens.expenses.ExpenseListScreen
import com.paradox.finance.ui.screens.tools.CurrencySelectorBottomSheet
import com.paradox.finance.ui.screens.tools.LeakHunterBottomSheet
import com.paradox.finance.ui.screens.tools.MonthlyWrappedDialog
import com.paradox.finance.ui.screens.tools.PurchaseSimulatorBottomSheet
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch

sealed class MainTab(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    object Dashboard : MainTab("dashboard", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Expenses : MainTab("expenses", "Expenses", Icons.Outlined.ReceiptLong, Icons.Filled.ReceiptLong)
    object Chat : MainTab("copilot", "AI Finny", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble)
    object Analytics : MainTab("analytics", "Analytics", Icons.Outlined.BarChart, Icons.Filled.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: ExpenseRepository,
    tokenManager: TokenManager,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf<MainTab>(MainTab.Dashboard) }

    val currencySymbol by tokenManager.currencyFlow.collectAsState(initial = "₹")

    // BottomSheet states
    var showAddExpense by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showWrapped by remember { mutableStateOf(false) }
    var showLeakHunter by remember { mutableStateOf(false) }
    var showPurchaseSimulator by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PrimaryPurple, PrimaryCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(
                            text = "Paradox",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Currency Switcher Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCurrencyPicker = true },
                        color = SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Text(
                            text = currencySymbol,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Logout Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                tokenManager.clearTokens()
                                onLogout()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    MainTab.Dashboard,
                    MainTab.Expenses,
                    MainTab.Chat,
                    MainTab.Analytics
                )

                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryCyan,
                            selectedTextColor = PrimaryCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = PrimaryPurple.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpense = true },
                containerColor = PrimaryCyan,
                contentColor = BackgroundDark,
                shape = CircleShape,
                modifier = Modifier.offset(y = (-8).dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(28.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                MainTab.Dashboard -> DashboardScreen(
                    repository = repository,
                    currencySymbol = currencySymbol,
                    onNavigateToExpenses = { selectedTab = MainTab.Expenses },
                    onNavigateToChat = { selectedTab = MainTab.Chat },
                    onNavigateToAnalytics = { selectedTab = MainTab.Analytics },
                    onOpenAddExpense = { showAddExpense = true },
                    onOpenWrapped = { showWrapped = true },
                    onOpenLeakHunter = { showLeakHunter = true },
                    onOpenPurchaseSimulator = { showPurchaseSimulator = true }
                )
                MainTab.Expenses -> ExpenseListScreen(
                    repository = repository,
                    currencySymbol = currencySymbol,
                    onAddExpenseClick = { showAddExpense = true }
                )
                MainTab.Chat -> AiChatScreen(
                    repository = repository,
                    currencySymbol = currencySymbol
                )
                MainTab.Analytics -> AnalyticsScreen(
                    repository = repository,
                    currencySymbol = currencySymbol
                )
            }
        }
    }

    // Modal Overlays
    if (showAddExpense) {
        AddExpenseBottomSheet(
            repository = repository,
            currencySymbol = currencySymbol,
            onDismiss = { showAddExpense = false },
            onExpenseAdded = {
                // automatic flow refresh
            }
        )
    }

    if (showCurrencyPicker) {
        CurrencySelectorBottomSheet(
            tokenManager = tokenManager,
            currentCurrency = currencySymbol,
            onDismiss = { showCurrencyPicker = false },
            onSelectCurrency = { newSymbol ->
                coroutineScope.launch {
                    tokenManager.saveCurrency(newSymbol)
                    showCurrencyPicker = false
                }
            }
        )
    }

    if (showWrapped) {
        MonthlyWrappedDialog(
            onDismiss = { showWrapped = false }
        )
    }

    if (showLeakHunter) {
        LeakHunterBottomSheet(
            onDismiss = { showLeakHunter = false }
        )
    }

    if (showPurchaseSimulator) {
        PurchaseSimulatorBottomSheet(
            onDismiss = { showPurchaseSimulator = false }
        )
    }
}
