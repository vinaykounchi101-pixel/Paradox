package com.paradox.finance.ui.screens.ai

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeakHunterScreen(
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val api = remember { ApiClient.getApi() }

    var selectedThreshold by remember { mutableStateOf(150.0) }
    var isChaiCapActive by remember { mutableStateOf(false) }

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
                        Text("LEAK HUNTER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPitchBlack)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gamified Discipline Streak Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceObsidianHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔥", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("14-Day Discipline Streak", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(NeonEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LVL 3", color = NeonEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Obsidian Saver Tier Active • +120 Vault XP", color = NeonTeal, fontSize = 11.sp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceObsidianElevated)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("VAULT SECURE", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Financial Health Score Hero Widget (Telemetry Engine)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceObsidianBase),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QueryStats, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TELEMETRY ENGINE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceObsidianElevated)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("LIVE AUDIT", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular Gauge Progress Box
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(SurfaceObsidianHighlight)
                                .border(5.dp, NeonEmerald, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GRADE A", color = NeonTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("88", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                Text("OPTIMAL", color = TextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Financial Health Score", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Top 4% of peer wealth cohort", color = TextTertiary, fontSize = 11.sp)

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Metric 1: Adherence
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceObsidianElevated)
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("Adherence", color = TextTertiary, fontSize = 9.sp)
                                        Text("94%", color = NeonEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Metric 2: Burn
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceObsidianElevated)
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("Burn Velocity", color = TextTertiary, fontSize = 9.sp)
                                        Text("Safe", color = NeonTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Metric 3: Discipline
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceObsidianElevated)
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text("Discipline", color = TextTertiary, fontSize = 9.sp)
                                        Text("82%", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Habitual Micro-Leak Hunter Section
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AlertCoral.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Radar, contentDescription = null, tint = AlertCoral, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Leak Hunter", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Transactions < $currencySymbol$selectedThreshold audited", color = TextTertiary, fontSize = 11.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AlertCoral.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("DRAIN DETECTED", color = AlertCoral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Interactive Threshold Filter Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50.0, 100.0, 150.0, 300.0, 500.0).forEach { threshold ->
                            val isSelected = selectedThreshold == threshold
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(if (isSelected) NeonEmerald.copy(alpha = 0.2f) else SurfaceObsidianHighlight)
                                    .border(1.dp, if (isSelected) NeonEmerald else Color.Transparent, RoundedCornerShape(9999.dp))
                                    .clickable {
                                        selectedThreshold = threshold
                                        Toast.makeText(context, "Scanning expenses under $currencySymbol${threshold.toInt()}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "<$currencySymbol${threshold.toInt()}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonEmerald else TextTertiary
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceObsidianElevated)
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceObsidianHighlight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("☕", fontSize = 20.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Cutting Chai & Snacks", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Repeats 2.8x daily • Avg $currencySymbol" + "40/day", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(AlertAmber.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("High Frequency", color = AlertAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Annualized Drain Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AlertCoral.copy(alpha = 0.1f))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = AlertCoral, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("ANNUALIZED DRAIN IMPACT", color = AlertCoral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$currencySymbol" + "14,600 / year", color = AlertCoral, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Equals 1 round-trip domestic flight ✈️ or nearly 2 full months of groceries.", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            // Action Chips Row
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        isChaiCapActive = !isChaiCapActive
                                        Toast.makeText(context, if (isChaiCapActive) "₹50 Daily Chai Cap Active!" else "Chai Cap Removed", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isChaiCapActive) NeonEmerald.copy(alpha = 0.2f) else SurfaceObsidianHighlight
                                    ),
                                    shape = RoundedCornerShape(9999.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        if (isChaiCapActive) Icons.Default.CheckCircle else Icons.Default.LockClock,
                                        contentDescription = null,
                                        tint = NeonEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isChaiCapActive) "₹50 Chai Cap Enforced" else "Set ₹50 daily chai cap",
                                        color = NeonEmerald,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Alerts snoozed for 7 days", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceObsidianHighlight),
                                    shape = RoundedCornerShape(9999.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Snooze alerts", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Subscription Guardian
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Subscription Guardian", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Automated recurring billing scan", color = TextTertiary, fontSize = 11.sp)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Monthly Load", color = TextTertiary, fontSize = 9.sp)
                            Text("$currencySymbol" + "2,449/mo", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Overlap Warning Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceObsidianElevated)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("3 Overlapping Streaming Services Detected", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Simultaneous billing found on 3 distinct OTT accounts with identical weekend screen time.", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            // Service Badges Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceObsidianBase)
                                        .padding(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Netflix 4K", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("₹649/mo", color = TextTertiary, fontSize = 9.sp)
                                        Text("Used 2h", color = AlertCoral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceObsidianBase)
                                        .padding(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Prime Video", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("₹299/mo", color = TextTertiary, fontSize = 9.sp)
                                        Text("High use", color = NeonEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceObsidianBase)
                                        .padding(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Hotstar Super", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("₹299/mo", color = TextTertiary, fontSize = 9.sp)
                                        Text("Dormant 21d", color = AlertAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
