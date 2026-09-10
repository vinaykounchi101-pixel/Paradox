package com.paradox.finance.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.components.CategoryBarChartView
import com.paradox.finance.ui.components.TrendGraphView
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AuthViewModel
import com.paradox.finance.ui.viewmodels.ExpenseViewModel
import com.paradox.finance.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    expenseViewModel: ExpenseViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val expenses by expenseViewModel.expenses.collectAsState()
    val currentCurrency by authViewModel.currentCurrency.collectAsState()
    var selectedTimeframe by remember { mutableStateOf("This Month") }

    val categoryTotals = expenses.groupBy { it.category?.name ?: "Other" }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalOutflow = expenses.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights & Velocity", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeframe Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("This Month", "Last 30 Days", "This Week").forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) ElectricEmerald.copy(alpha = 0.2f) else GlassSurface1)
                            .border(1.dp, if (isSelected) ElectricEmerald else GlassBorderStroke, RoundedCornerShape(9999.dp))
                            .clickable { selectedTimeframe = tf }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf,
                            color = if (isSelected) ElectricEmerald else OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Total Outflow Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(text = "Total Spending Velocity", color = OnSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = CurrencyFormatter.format(totalOutflow, currentCurrency),
                        color = OnSurfaceHigh,
                        fontSize = 32.sp,
                        style = Typography.displayLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(text = "Average: ${CurrencyFormatter.format(totalOutflow / 30, currentCurrency)} / day", color = LuminousCyan, fontSize = 11.sp)
                }
            }

            // 3D Category Distribution Bar Chart
            CategoryBarChartView(
                categoriesWithSpend = categoryTotals
            )

            // Spending Velocity Bezier Curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "Daily Trend Velocity", color = OnSurface, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    TrendGraphView(
                        dataPoints = expenses.take(10).map { it.amount }.reversed(),
                        currency = currentCurrency
                    )
                }
            }

            // Payment Channel Breakdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Payment Channel Distribution", color = OnSurface, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "⚡ UPI: 64%", color = ElectricEmerald, fontSize = 12.sp)
                        Text(text = "💳 Credit Cards: 28%", color = NeonCyan, fontSize = 12.sp)
                        Text(text = "💵 Cash: 8%", color = SoftIndigo, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
