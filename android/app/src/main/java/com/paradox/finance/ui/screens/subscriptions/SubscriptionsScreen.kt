package com.paradox.finance.ui.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AuthViewModel
import com.paradox.finance.ui.viewmodels.BudgetViewModel
import com.paradox.finance.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    budgetViewModel: BudgetViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val summary by budgetViewModel.recurringSummary.collectAsState()
    val currentCurrency by authViewModel.currentCurrency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions & Commitments", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineMedium) },
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(text = "Total Fixed Monthly Burden", color = OnSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = CurrencyFormatter.format(summary?.totalMonthlyCommitment ?: 3420.0, currentCurrency),
                        color = OnSurfaceHigh,
                        fontSize = 32.sp,
                        style = Typography.displayLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Text(text = "Across ${summary?.activeCount ?: 5} active periodic services", color = LuminousCyan, fontSize = 11.sp)
                }
            }

            // Commitments List
            val items = summary?.commitments ?: emptyList()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface1)
                            .border(1.dp, GlassBorderStroke, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Repeat, contentDescription = "Sub", tint = NeonCyan, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(text = item.displayName, color = OnSurfaceHigh, fontSize = 13.sp, style = Typography.labelLarge)
                                    Text(text = "${item.categoryName ?: "Subscription"} • ${item.frequency.uppercase()}", color = OnSurfaceVariant, fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "${CurrencyFormatter.format(item.amount, currentCurrency)} / mo",
                                color = OnSurfaceHigh,
                                fontSize = 13.sp,
                                style = Typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
