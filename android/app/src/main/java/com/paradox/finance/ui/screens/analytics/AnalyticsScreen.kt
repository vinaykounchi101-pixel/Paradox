package com.paradox.finance.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.ui.components.CategoryBarChartView
import com.paradox.finance.ui.components.CategoryChartItem
import com.paradox.finance.ui.components.CATEGORY_COLORS
import com.paradox.finance.ui.components.TrendGraphView
import com.paradox.finance.ui.components.TrendPoint
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("current_month") }
    var totalSpent by remember { mutableDoubleStateOf(0.0) }
    var trendData by remember { mutableStateOf<List<TrendPoint>>(emptyList()) }
    var categoryChartItems by remember { mutableStateOf<List<CategoryChartItem>>(emptyList()) }
    var categoryBreakdown by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    var avgDailySpend by remember { mutableDoubleStateOf(0.0) }
    
    val coroutineScope = rememberCoroutineScope()

    fun loadAnalytics() {
        coroutineScope.launch {
            isLoading = true
            try {
                val api = ApiClient.apiService
                val res = api.getDashboard(selectedPeriod)
                if (res.isSuccessful) {
                    val rawBody = res.body()
                    val data = (rawBody?.get("data") as? Map<*, *>) ?: rawBody
                    if (data != null) {
                        totalSpent = (data["total_expenses"] as? Number)?.toDouble()
                            ?: (data["total_spent"] as? Number)?.toDouble()
                            ?: data["total_spent"]?.toString()?.toDoubleOrNull() ?: 0.0
                        
                        // Parse Trend
                        val trendList = (data["trend"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                        trendData = trendList.mapNotNull { item ->
                            val label = item["label"]?.toString() ?: ""
                            val total = (item["total"] as? Number)?.toDouble()
                                ?: item["total"]?.toString()?.toDoubleOrNull() ?: 0.0
                            if (label.isNotEmpty()) TrendPoint(label, total) else null
                        }

                        // Parse Category Chart
                        val catList = ((data["category_breakdown"] as? List<*>) ?: (data["expenses_by_category"] as? List<*>))?.filterIsInstance<Map<*, *>>() ?: emptyList()
                        categoryChartItems = catList.mapIndexedNotNull { idx, item ->
                            val name = item["category_name"]?.toString() ?: item["name"]?.toString() ?: "Other"
                            val amount = (item["total"] as? Number)?.toDouble()
                                ?: item["total"]?.toString()?.toDoubleOrNull()
                                ?: (item["amount"] as? Number)?.toDouble() ?: 0.0
                            val pct = (item["percentage"] as? Number)?.toDouble() ?: 0.0
                            if (name.isNotEmpty()) {
                                CategoryChartItem(
                                    categoryName = name,
                                    total = amount,
                                    percentage = pct,
                                    color = CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
                                )
                            } else null
                        }

                        categoryBreakdown = categoryChartItems.map { it.categoryName to it.total }

                        // Payment Methods
                        val pmList = (data["expenses_by_payment_method"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                        paymentMethods = pmList.mapNotNull {
                            val name = it["name"]?.toString() ?: "Cash"
                            val amount = (it["total"] as? Number ?: it["amount"] as? Number)?.toDouble() ?: 0.0
                            name to amount
                        }.sortedByDescending { it.second }

                        // Daily Avg
                        avgDailySpend = if (selectedPeriod == "current_week") totalSpent / 7.0 else totalSpent / 30.0
                    }
                }
            } catch (e: Exception) {
                // Fallback / offline values
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedPeriod) {
        loadAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadAnalytics() }) {
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
            // Period Filter Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Zinc900)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "current_month" to "This Month",
                    "last_30_days" to "Last 30 Days",
                    "current_week" to "This Week"
                ).forEach { (key, label) ->
                    val isSelected = selectedPeriod == key
                    Button(
                        onClick = { selectedPeriod = key },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Indigo600 else Color.Transparent,
                            contentColor = if (isSelected) Color.White else Zinc400
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
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
                // Key Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Zinc900),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Outflow", color = Zinc400, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$currencySymbol${String.format("%,.2f", totalSpent)}",
                                color = Zinc50,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Zinc900),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Daily Average", color = Zinc400, fontSize = 12.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$currencySymbol${String.format("%,.2f", avgDailySpend)}",
                                color = Emerald400,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Spending Trends Curve Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Zinc900),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Indigo400, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Spending Trends", color = Zinc50, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("Velocity Curve", color = Zinc400, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TrendGraphView(
                            data = trendData,
                            currencySymbol = currencySymbol,
                            lineColor = Indigo400
                        )
                    }
                }

                // Category Bar Chart Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Zinc900),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PieChart, contentDescription = null, tint = Emerald400, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Category Breakdown", color = Zinc50, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text("By Category", color = Zinc400, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryBarChartView(
                            items = categoryChartItems,
                            currencySymbol = currencySymbol
                        )
                    }
                }

                // Category Detailed List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Zinc900),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Category Details", color = Zinc50, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        if (categoryBreakdown.isEmpty()) {
                            Text("No category data available for this period.", color = Zinc500, fontSize = 13.sp)
                        } else {
                            categoryBreakdown.forEach { (cat, amount) ->
                                val pct = if (totalSpent > 0) (amount / totalSpent) else 0.0
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, color = Zinc200, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            "$currencySymbol${String.format("%,.2f", amount)} (${String.format("%.1f", pct * 100)}%)",
                                            color = Zinc400,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { pct.toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = Indigo500,
                                        trackColor = Zinc800
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Methods Breakdown
                if (paymentMethods.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Zinc900),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Payment Channels", color = Zinc50, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            paymentMethods.forEach { (pm, amount) ->
                                val pct = if (totalSpent > 0) (amount / totalSpent) else 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(pm, color = Zinc300, fontSize = 14.sp)
                                    Text(
                                        "$currencySymbol${String.format("%,.2f", amount)} (${String.format("%.1f", pct * 100)}%)",
                                        color = Emerald400,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
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
