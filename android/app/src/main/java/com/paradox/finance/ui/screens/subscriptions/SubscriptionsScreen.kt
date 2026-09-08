package com.paradox.finance.ui.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch

data class SubscriptionItem(
    val description: String,
    val amount: Double,
    val frequency: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var subscriptions by remember { mutableStateOf<List<SubscriptionItem>>(emptyList()) }
    var totalMonthlyCommitment by remember { mutableDoubleStateOf(0.0) }
    
    val coroutineScope = rememberCoroutineScope()

    fun loadSubscriptions() {
        coroutineScope.launch {
            isLoading = true
            try {
                val api = ApiClient.apiService
                val res = api.getRecurringExpenses()
                if (res.isSuccessful) {
                    val data = res.body()?.get("data") as? Map<*, *>
                    if (data != null) {
                        totalMonthlyCommitment = (data["total_monthly_commitment"] as? Number)?.toDouble() ?: 0.0
                        val rawList = data["subscriptions"] as? List<Map<*, *>> ?: emptyList()
                        subscriptions = rawList.mapNotNull {
                            val desc = it["description"] as? String ?: "Recurring Bill"
                            val amt = (it["amount"] as? Number)?.toDouble() ?: 0.0
                            val freq = it["recurring_frequency"] as? String ?: "monthly"
                            val cat = it["category_name"] as? String ?: "Subscription"
                            SubscriptionItem(desc, amt, freq, cat)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSubscriptions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions & Bills", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadSubscriptions() }) {
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Indigo500)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Zinc900),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Monthly Fixed Commitment", color = Zinc400, fontSize = 13.sp)
                            Text(
                                "$currencySymbol${String.format("%,.2f", totalMonthlyCommitment)}",
                                color = Zinc50,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Total active subscriptions: ${subscriptions.size}",
                                color = Emerald400,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (subscriptions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Zinc900),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🔁", fontSize = 32.sp)
                                Text("No Active Subscriptions", color = Zinc200, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "When logging an expense, toggle 'Recurring' to track active memberships and bills here.",
                                    color = Zinc500,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(subscriptions) { sub ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Zinc900),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Indigo600.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🔁", fontSize = 18.sp)
                                    }

                                    Column {
                                        Text(sub.description, color = Zinc50, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                        Text("${sub.category} • ${sub.frequency.uppercase()}", color = Zinc400, fontSize = 12.sp)
                                    }
                                }

                                Text(
                                    "$currencySymbol${String.format("%,.2f", sub.amount)}",
                                    color = Zinc50,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
