package com.paradox.finance.ui.screens.expenses

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.paradox.finance.core.Constants
import com.paradox.finance.hardware.voice.VoiceInputHelper
import com.paradox.finance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddExpenseDialog(
    viewModel: ExpenseViewModel,
    isEditMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showSmsDrawer by remember { mutableStateOf(false) }
    var smsInputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val currencySymbol = Constants.CURRENCY_SYMBOLS[state.currency] ?: "₹"

    // Voice Helper
    val voiceHelper = remember {
        VoiceInputHelper(
            context = context,
            onResult = { text ->
                viewModel.onQuickAddInputChanged(text)
                viewModel.parseNaturalLanguageExpense()
            },
            onError = { err ->
                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
            },
            onListeningStateChanged = { listening ->
                isListening = listening
            }
        )
    }

    // Audio Permission Launcher for Voice Input
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceHelper.startListening()
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required for voice input", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Image Picker for Receipt OCR
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleReceiptScan(context, it) }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sub-bar Header: Velocity & Telemetry Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEditMode) "Edit Expense" else "Log Expense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentEmerald))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTO-TRIGGER",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                    }
                }

                if (!isEditMode) {
                    // Mode Selector Pills: [ Keypad ] [ AI Voice ] [ OCR Scan ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(9999.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Mode 1: Keypad
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Dialpad, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Keypad", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BackgroundDark)
                            }
                        }

                        // Mode 2: AI Voice
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = if (isListening) AccentRose.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (isListening) androidx.compose.foundation.BorderStroke(1.dp, AccentRose) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isListening) {
                                        voiceHelper.stopListening()
                                    } else {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            voiceHelper.startListening()
                                        } else {
                                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = if (isListening) AccentRose else TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Voice", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isListening) AccentRose else TextSecondary)
                            }
                        }

                        // Mode 3: OCR Scan
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { imagePickerLauncher.launch("image/*") }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("OCR Scan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                            }
                        }
                    }
                }

                // Large Numeric Entry Display Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currencySymbol,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.amountInput.isNotBlank()) state.amountInput else "0.00",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val selectedCatName = state.categories.find { it.id == state.selectedCategoryId }?.name ?: "Select Classification"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentCyan))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCatName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentCyan
                            )
                        }
                    }
                }

                // AI Natural Language Quick Input Field
                OutlinedTextField(
                    value = state.quickAddInput,
                    onValueChange = { viewModel.onQuickAddInputChanged(it) },
                    placeholder = { Text("e.g. Swiggy dinner ₹450 via UPI", fontSize = 12.sp, color = TextMuted) },
                    trailingIcon = {
                        IconButton(onClick = { viewModel.parseNaturalLanguageExpense() }) {
                            if (state.isLoading) {
                                CircularProgressIndicator(color = AccentEmerald, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Parse", tint = AccentEmerald)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description Field
                OutlinedTextField(
                    value = state.descriptionInput,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = { Text("Merchant / Description") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Squircle Grid (3 Columns)
                Column {
                    Text(
                        text = "CLASSIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.categories) { cat ->
                            val isSelected = state.selectedCategoryId == cat.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) AccentEmerald.copy(alpha = 0.15f) else SurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentEmerald else BorderDark
                                ),
                                modifier = Modifier.clickable { viewModel.onCategorySelected(cat.id) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when {
                                            cat.name.contains("Food", true) || cat.name.contains("Dining", true) -> Icons.Default.Restaurant
                                            cat.name.contains("Groceries", true) -> Icons.Default.ShoppingCart
                                            cat.name.contains("Transport", true) || cat.name.contains("Transit", true) -> Icons.Default.LocalTaxi
                                            cat.name.contains("Shopping", true) -> Icons.Default.ShoppingBag
                                            cat.name.contains("Bills", true) || cat.name.contains("Utilities", true) -> Icons.Default.ReceiptLong
                                            else -> Icons.Default.Category
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) AccentEmerald else TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextPrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Amount Numerical Field
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentEmerald,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = AccentRose,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Action Buttons: Discard & Confirm Log
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(9999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isEditMode) {
                                viewModel.updateExpense()
                            } else {
                                viewModel.saveExpense()
                            }
                            onDismiss()
                        },
                        enabled = !state.isLoading && state.amountInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(9999.dp),
                        modifier = Modifier.weight(1.5f).height(50.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = BackgroundDark, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = if (isEditMode) "Save Changes" else "Confirm Log",
                                color = BackgroundDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
