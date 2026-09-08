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

    // Image Picker for Receipt OCR
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleReceiptScan(context, it) }
    }

    // Voice Helper
    val voiceHelper = remember {
        VoiceInputHelper(
            context = context,
            onResult = { text ->
                viewModel.onQuickAddInputChanged(text)
                viewModel.parseNaturalLanguageExpense()
            },
            onError = { /* Error handled */ },
            onListeningStateChanged = { listening ->
                isListening = listening
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEditMode) "Edit Expense Record" else "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isEditMode) {
                    // AI Quick-Add Input Row
                    OutlinedTextField(
                        value = state.quickAddInput,
                        onValueChange = { viewModel.onQuickAddInputChanged(it) },
                        placeholder = { Text("e.g. Spent 450 for Zomato pizza", style = MaterialTheme.typography.bodyMedium) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Voice Mic Button
                                IconButton(onClick = {
                                    if (isListening) voiceHelper.stopListening() else voiceHelper.startListening()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = if (isListening) AccentRose else PrimaryIndigo
                                    )
                                }
                                // Auto-Fill Button
                                IconButton(onClick = { viewModel.parseNaturalLanguageExpense() }) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Parse", tint = AccentAmber)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Voice Listening status banner
                    AnimatedVisibility(visible = isListening) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .background(AccentRose.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(color = AccentRose, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Listening to your voice...", color = AccentRose, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Tools Grid: [ 📷 Scan Bill ] & [ 💬 Paste SMS ]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (state.isOcrScanning) "Scanning..." else "Scan Bill", fontSize = 12.sp, color = TextPrimary)
                        }

                        Button(
                            onClick = { showSmsDrawer = !showSmsDrawer },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste SMS", fontSize = 12.sp, color = TextPrimary)
                        }
                    }

                    // SMS Paste Drawer
                    AnimatedVisibility(visible = showSmsDrawer) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = smsInputText,
                                onValueChange = {
                                    smsInputText = it
                                    viewModel.handleSmsPaste(it)
                                },
                                placeholder = { Text("Paste bank / UPI SMS here...", fontSize = 12.sp) },
                                maxLines = 3,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Category Suggestion Banner with 1-Click Add & Select
                    AnimatedVisibility(visible = state.suggestedCategory != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .fillMaxWidth()
                                .background(AccentAmber.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "✨ Suggests: ${state.suggestedCategory}",
                                color = AccentAmber,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            TextButton(onClick = { viewModel.applySuggestedCategory() }) {
                                Text("+ Add & Select", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Amount Field
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    label = { Text("Amount ($currencySymbol)") },
                    leadingIcon = { Text(currencySymbol, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo, modifier = Modifier.padding(start = 12.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Field
                OutlinedTextField(
                    value = state.descriptionInput,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = { Text("Description / Merchant") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Picker Chips
                Text("Category", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories) { cat ->
                        val isSelected = state.selectedCategoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PrimaryIndigo else SurfaceCard)
                                .border(1.dp, if (isSelected) PrimaryIndigo else BorderDark, RoundedCornerShape(20.dp))
                                .clickable { viewModel.onCategorySelected(cat.id) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.errorMessage!!,
                        color = AccentRose,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save / Update Expense Button
                Button(
                    onClick = {
                        if (isEditMode) {
                            viewModel.updateExpense()
                        } else {
                            viewModel.saveExpense()
                        }
                    },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            text = if (isEditMode) "Save Changes" else "Record Expense",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
