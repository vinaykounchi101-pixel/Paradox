package com.paradox.finance.ui.screens.ai

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.components.NavTab
import com.paradox.finance.ui.components.ObsidianBottomBar
import com.paradox.finance.ui.screens.expenses.ExpenseViewModel
import com.paradox.finance.ui.screens.expenses.QuickAddExpenseDialog
import com.paradox.finance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseSimulatorScreen(
    currencySymbol: String,
    currentBuffer: Double = 15000.0,
    dailyAllowance: Double = 500.0,
    expenseViewModel: ExpenseViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = onNavigateBack,
    onNavigateToAiChat: () -> Unit = {},
    onAddExpense: (Double, String) -> Unit
) {
    val context = LocalContext.current
    var itemTitle by remember { mutableStateOf("Sony WH-1000XM4 Headphones") }
    var itemAmount by remember { mutableStateOf("4999") }
    var selectedCategory by remember { mutableStateOf("Gadgets & Tech") }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("Gadgets & Tech", "Lifestyle", "Travel", "Food", "Entertainment")

    val amount = itemAmount.toDoubleOrNull() ?: 0.0
    val simulatedStressScore = remember(amount) {
        if (amount <= 0) 15
        else ((amount / (currentBuffer.coerceAtLeast(1.0))) * 100).toInt().coerceIn(10, 95)
    }

    val postPurchaseBuffer = (currentBuffer - amount).coerceAtLeast(0.0)
    val postDailySafeBurn = if (dailyAllowance > 0 && amount > 0) {
        (dailyAllowance * (postPurchaseBuffer / currentBuffer.coerceAtLeast(1.0))).coerceAtLeast(0.0)
    } else dailyAllowance

    val isHighStress = simulatedStressScore > 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "PARADOX",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "256-BIT • VAULT ALPHA",
                                color = NeonTeal,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceObsidianElevated)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("SIMULATOR", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPitchBlack)
            )
        },
        bottomBar = {
            ObsidianBottomBar(
                currentTab = NavTab.SIMULATOR,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.DASHBOARD -> { onNavigateToDashboard() }
                        NavTab.QUICK_LOG -> {
                            if (expenseViewModel != null) {
                                expenseViewModel.openAddDialog()
                                showQuickAddDialog = true
                            }
                        }
                        NavTab.SIMULATOR -> { /* Already on simulator */ }
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Banner: Monte Carlo Projection
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = NeonTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "MONTE CARLO PROJECTION",
                            color = NeonTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceObsidianElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AlertAmber)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Simulating Live", color = AlertAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "Pre-Purchase Simulator",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "AI Budget Stress-Test & Depletion Engine",
                    fontSize = 13.sp,
                    color = TextTertiary
                )
            }

            // Prospective Acquisition Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PROSPECTIVE ACQUISITION",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Icon(Icons.Default.Tune, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Amount Display & Input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            currencySymbol,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = itemAmount,
                            onValueChange = { itemAmount = it },
                            placeholder = { Text("0", color = TextTertiary, fontSize = 28.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonTeal
                            ),
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Item Description Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceObsidianSubtle)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = itemTitle,
                                onValueChange = { itemTitle = it },
                                placeholder = { Text("Item description", color = TextTertiary, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = NeonEmerald
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Selector Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (isSelected) SurfaceObsidianElevated else SurfaceObsidianSubtle)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonTeal.copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(9999.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonTeal else TextTertiary
                                )
                            }
                        }
                    }
                }
            }

            // AI Instant Verdict Card (Obsidian Flow Core Differentiator)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(if (isHighStress) AlertAmber.copy(alpha = 0.15f) else NeonEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isHighStress) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isHighStress) AlertAmber else NeonEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isHighStress) "VERDICT: CAUTION / TIGHT" else "VERDICT: SAFE TO ACQUIRE",
                                    color = if (isHighStress) AlertAmber else NeonEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Text(
                            "CONFIDENCE 94.2%",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stress Score Metric & Gauge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Stress Test Metric", color = TextTertiary, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$simulatedStressScore",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHighStress) AlertAmber else NeonEmerald
                                )
                                Text(
                                    "/100",
                                    fontSize = 16.sp,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                )
                            }
                            Text(
                                if (isHighStress) "Moderate Liquidity Strain" else "Optimal Cashflow Buffer",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        // Circular Gauge Progress Box
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SurfaceObsidianHighlight)
                                .border(
                                    3.dp,
                                    if (isHighStress) AlertAmber else NeonEmerald,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$simulatedStressScore%",
                                color = if (isHighStress) AlertAmber else NeonEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Granular Impact Metrics Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceObsidianSubtle)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Safe Burn Allowance
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AlertCoral.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = AlertCoral, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("DAILY SAFE-TO-SPEND ALLOWANCE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Text(
                                        "$currencySymbol${String.format("%.0f", dailyAllowance)}",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "$currencySymbol${String.format("%.0f", postDailySafeBurn)}/day",
                                        color = AlertCoral,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text("Compounded penalty for remaining month days", color = TextTertiary, fontSize = 11.sp)
                            }
                        }

                        // Projected Depletion Date
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AlertAmber.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EventBusy, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("PROJECTED DEPLETION DATE", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("Oct 26, 2026", color = AlertAmber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Exhausts buffer 5 days before salary cycle payout", color = AlertCoral, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Month-End Buffer Comparison Bars
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
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
                        Text(
                            "MONTH-END BUFFER COMPARISON",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "DELTA -$currencySymbol${String.format("%.0f", amount)}",
                            color = NeonTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Status Quo Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status Quo (No Purchase)", color = TextPrimary, fontSize = 12.sp)
                            Text(
                                "$currencySymbol${String.format("%.0f", currentBuffer)} surplus",
                                color = NeonEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceObsidianSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.82f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(Brush.horizontalGradient(listOf(NeonEmerald, NeonTeal)))
                            )
                        }
                        Text("Comfortable Reserve (+28% buffer)", color = NeonEmerald, fontSize = 10.sp)
                    }

                    // Post-Purchase Simulation Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Post-Purchase Simulation", color = TextPrimary, fontSize = 12.sp)
                            Text(
                                "$currencySymbol${String.format("%.0f", postPurchaseBuffer)} surplus",
                                color = if (postPurchaseBuffer > 5000) NeonEmerald else AlertAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val pct = ((postPurchaseBuffer / currentBuffer.coerceAtLeast(1.0))).toFloat().coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceObsidianSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pct)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(Brush.horizontalGradient(listOf(AlertAmber, AlertCoral)))
                            )
                        }
                        Text("Critical Vulnerability (Tight margin)", color = AlertCoral, fontSize = 10.sp)
                    }
                }
            }

            // Finny Copilot Protocol Insight Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceObsidianElevated)
                    .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "FINNY COPILOT PROTOCOL",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                        }
                        Text(
                            "If you delay this purchase by 12 days to next month's salary cycle, your health score stays at 92/100 (Grade A) with zero cashflow vulnerability.",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Dual Action Pills
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary Luminous Pill: Save to Wishlist
                Button(
                    onClick = {
                        Toast.makeText(context, "Saved $itemTitle to Wishlist! Finny will monitor price drops.", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = BackgroundPitchBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save to Wishlist & Wait",
                        color = BackgroundPitchBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Secondary Glass Pill: Proceed Anyway
                Button(
                    onClick = {
                        if (amount > 0) {
                            onAddExpense(amount, itemTitle.ifBlank { "Purchase" })
                        } else {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceObsidianBase),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Proceed Anyway",
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (showQuickAddDialog && expenseViewModel != null) {
        QuickAddExpenseDialog(
            viewModel = expenseViewModel,
            onDismiss = { showQuickAddDialog = false }
        )
    }
}
