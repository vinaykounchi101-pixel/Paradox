package com.paradox.finance.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.core.Constants
import com.paradox.finance.data.preferences.AuthPreferences
import com.paradox.finance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authPreferences: AuthPreferences,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    var biometricEnabled by remember { mutableStateOf(authPreferences.isBiometricEnabled()) }
    var currentCurrency by remember { mutableStateOf(authPreferences.getCurrency()) }
    val userEmail = authPreferences.getUserEmail() ?: "user@paradox.com"
    val userName = authPreferences.getUserName() ?: "Paradox User"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Profile", fontWeight = FontWeight.Bold) },
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
            // Profile Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(PrimaryIndigo, AccentEmerald))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "P",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(userEmail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preferences Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Security & Display", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccentEmerald)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Biometric App Lock", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                authPreferences.setBiometricEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = AccentEmerald
                            )
                        )
                    }

                    HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))

                    // Currency Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Default Currency", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${Constants.CURRENCY_SYMBOLS[currentCurrency]} $currentCurrency", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRose.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRose.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(9999.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = AccentRose)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out", color = AccentRose, fontWeight = FontWeight.Bold)
            }
        }
    }
}
