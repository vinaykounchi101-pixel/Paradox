package com.paradox.finance.ui.screens.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AuthViewModel
import com.paradox.finance.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val currentCurrency by authViewModel.currentCurrency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goal Optimizer", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineMedium) },
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
            // Active Goal Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderActive, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Target Goal: Emergency Vault", color = OnSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = CurrencyFormatter.format(150000.0, currentCurrency),
                        color = ElectricEmerald,
                        fontSize = 32.sp,
                        style = Typography.displayLarge
                    )
                    LinearProgressIndicator(
                        progress = { 0.68f },
                        color = ElectricEmerald,
                        trackColor = SurfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(9999.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Accumulated: ${CurrencyFormatter.format(102000.0, currentCurrency)} (68%)", color = OnSurfaceVariant, fontSize = 11.sp)
                        Text(text = "3 Months Left", color = LuminousCyan, fontSize = 11.sp)
                    }
                }
            }

            // AI Discretionary Recommendation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassSurface2)
                    .border(1.dp, LuminousCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(text = "✨ Finny Optimization Plan", color = LuminousCyan, fontSize = 13.sp, style = Typography.labelLarge)
                    Text(
                        text = "Trimming discretionary Dining & Weekend Shopping by ₹3,500/mo will achieve this milestone 18 days ahead of schedule.",
                        color = OnSurface,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
