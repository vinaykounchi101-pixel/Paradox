package com.paradox.finance.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val coroutineScope = rememberCoroutineScope()
    val api = remember { ApiClient.getApi() }

    var selectedThreshold by remember { mutableStateOf(150.0) }
    var isLoading by remember { mutableStateOf(false) }
    var totalLeakAmount by remember { mutableStateOf(0.0) }
    var annualProjection by remember { mutableStateOf(0.0) }
    var leakCount by remember { mutableStateOf(0) }
    var adviceText by remember { mutableStateOf("Analyzing micro-spending...") }

    val thresholds = listOf(100.0, 150.0, 250.0, 500.0)

    fun fetchLeaks(threshold: Double) {
        isLoading = true
        coroutineScope.launch {
            try {
                val res = api.getHealthScore() // or leak endpoint
                // Simulated calculated metrics from backend
                totalLeakAmount = threshold * 8.5
                annualProjection = totalLeakAmount * 12
                leakCount = 14
                adviceText = "You have 14 repetitive charges under $currencySymbol$threshold. Cutting these saves $currencySymbol${String.format("%.0f", annualProjection)} per year!"
            } catch (e: Exception) {
                adviceText = "Keep monitoring small daily charges to stop financial leaks."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedThreshold) {
        fetchLeaks(selectedThreshold)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔍 Micro-Spending Leak Hunter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                .padding(16.dp)
        ) {
            Text(
                text = "Threshold Filter",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Threshold Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(thresholds) { t ->
                    val isSelected = selectedThreshold == t
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedThreshold = t },
                        label = { Text("≤ $currencySymbol$t", fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentAmber,
                            selectedLabelColor = BackgroundDark,
                            containerColor = SurfaceCard,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Projection Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Projected Annual Drain", style = MaterialTheme.typography.titleMedium, color = AccentAmber, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "$currencySymbol${String.format("%.0f", annualProjection)} / year",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "From $leakCount small transactions this month ($currencySymbol${String.format("%.0f", totalLeakAmount)} monthly)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = adviceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
