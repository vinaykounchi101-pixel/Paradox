package com.paradox.finance.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.paradox.finance.data.model.ExpenseResponse
import com.paradox.finance.data.model.FinnyEmotion
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.ui.components.AchievementsCard
import com.paradox.finance.ui.components.FiftyThirtyTwentyCard
import com.paradox.finance.ui.components.FinnyMascotComponent
import com.paradox.finance.ui.components.ParadoxCard
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: ExpenseRepository,
    currencySymbol: String,
    onNavigateToExpenses: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenWrapped: () -> Unit,
    onOpenLeakHunter: () -> Unit,
    onOpenPurchaseSimulator: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var totalSpent by remember { mutableDoubleStateOf(0.0) }
    var monthlyBudget by remember { mutableDoubleStateOf(50000.0) }
    var recentExpenses by remember { mutableStateOf<List<ExpenseResponse>>(emptyList()) }
    var finnyMessage by remember { mutableStateOf("Looking good! You're on track with your budget this month.") }
    var finnyEmotion by remember { mutableStateOf(FinnyEmotion.JOYFUL) }

    fun refreshData() {
        coroutineScope.launch {
            isRefreshing = true
            try {
                repository.syncExpenses()
                val expenses = repository.getExpenses()
                recentExpenses = expenses.take(5)
                val total = expenses.sumOf { it.amount }
                totalSpent = total
                
                if (total > monthlyBudget * 0.85) {
                    finnyEmotion = FinnyEmotion.STRESSED
                    finnyMessage = "Alert: You have used over 85% of your monthly budget! Let's slow down non-essential spending."
                } else if (total > monthlyBudget * 0.5) {
                    finnyEmotion = FinnyEmotion.CALM
                    finnyMessage = "Steady pace. You've spent ${(total / monthlyBudget * 100).toInt()}% of your budget."
                } else {
                    finnyEmotion = FinnyEmotion.JOYFUL
                    finnyMessage = "Superb discipline! Your savings rate is healthy."
                }
            } catch (e: Exception) {
                // local fallback if offline
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val currencyFormatter = remember(currencySymbol) {
        val format = NumberFormat.getNumberInstance(Locale.getDefault())
        format.minimumFractionDigits = 0
        format.maximumFractionDigits = 2
        format
    }

    Scaffold(
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            // Finny Mascot Hero
            item {
                FinnyMascotComponent(
                    emotion = finnyEmotion,
                    speechText = finnyMessage,
                    onTapMascot = {
                        onNavigateToChat()
                    }
                )
            }

            // Spend Summary Balance Card
            item {
                ParadoxCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        PrimaryPurple.copy(alpha = 0.25f),
                                        PrimaryCyan.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Spending",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Active",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$currencySymbol ${currencyFormatter.format(totalSpent)}",
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar
                        val progress = if (monthlyBudget > 0) (totalSpent / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (progress > 0.85f) DangerRed else PrimaryCyan,
                            trackColor = SurfaceBorder
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(progress * 100).toInt()}% of budget",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Cap: $currencySymbol ${currencyFormatter.format(monthlyBudget)}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Quick Actions Hub
            item {
                Text(
                    text = "Quick Superpowers",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CameraAlt,
                        label = "Scan Receipt",
                        gradient = listOf(PrimaryPurple, PrimaryCyan),
                        onClick = onOpenAddExpense
                    )
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Search,
                        label = "Leak Hunter",
                        gradient = listOf(Color(0xFFFF5252), Color(0xFFFF7A00)),
                        onClick = onOpenLeakHunter
                    )
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ShoppingBag,
                        label = "Simulator",
                        gradient = listOf(Color(0xFF7C3AED), Color(0xFFC084FC)),
                        onClick = onOpenPurchaseSimulator
                    )
                    QuickActionBtn(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoAwesome,
                        label = "Wrapped",
                        gradient = listOf(Color(0xFF00C853), Color(0xFF00E5FF)),
                        onClick = onOpenWrapped
                    )
                }
            }

            // 50/30/20 Rule Breakdown Card
            item {
                FiftyThirtyTwentyCard(
                    needsSpent = totalSpent * 0.50,
                    wantsSpent = totalSpent * 0.30,
                    savingsSpent = totalSpent * 0.20,
                    currencySymbol = currencySymbol
                )
            }

            // Streaks & Badges Gamification Card
            item {
                AchievementsCard(
                    streakDays = 5,
                    totalBadges = 12,
                    unlockedBadges = 4
                )
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "See All",
                        color = PrimaryCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateToExpenses() }
                    )
                }
            }

            // Recent Transactions List
            if (recentExpenses.isEmpty()) {
                item {
                    ParadoxCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions recorded yet.\nTap + to add your first expense!",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentExpenses) { expense ->
                    ParadoxCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToExpenses() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = expense.description.ifEmpty { "Expense" },
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = expense.category ?: "General",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text(
                                text = "-$currencySymbol ${currencyFormatter.format(expense.amount)}",
                                color = DangerRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionBtn(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
