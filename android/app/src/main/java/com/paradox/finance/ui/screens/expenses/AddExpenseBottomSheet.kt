package com.paradox.finance.ui.screens.expenses

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.ui.components.ParadoxGradientButton
import com.paradox.finance.ui.components.ParadoxTextField
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    repository: ExpenseRepository,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ocrStatus by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Food & Dining", "Shopping", "Transport", 
        "Entertainment", "Utilities & Bills", "Healthcare", 
        "Investment", "Other"
    )

    // Speech to text launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                description = spokenText
                val numberRegex = "(\\d+([.,]\\d+)?)".toRegex()
                val match = numberRegex.find(spokenText)
                if (match != null) {
                    amount = match.value.replace(",", ".")
                }
            }
        }
    }

    // Camera Receipt launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            ocrStatus = "Scanning receipt with on-device OCR..."
            coroutineScope.launch {
                try {
                    val recognized = repository.scanReceipt(bitmap)
                    if (recognized != null) {
                        amount = recognized.amount.toString()
                        description = recognized.merchant ?: "Receipt Scan"
                        ocrStatus = "Receipt scanned successfully!"
                    } else {
                        ocrStatus = "Receipt processed. Please verify details."
                    }
                } catch (e: Exception) {
                    ocrStatus = "Could not parse automatically. Please enter details."
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Expense",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Voice input quick action
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say expense like 'Spent 500 on dinner'")
                            }
                            speechLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = PrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Camera receipt quick action
                    IconButton(
                        onClick = {
                            cameraLauncher.launch(null)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryCyan.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Receipt",
                            tint = PrimaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (ocrStatus != null) {
                Text(
                    text = ocrStatus!!,
                    color = PrimaryCyan,
                    fontSize = 12.sp
                )
            }

            // Amount Input
            ParadoxTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Amount ($currencySymbol)",
                placeholder = "0.00",
                leadingIcon = Icons.Default.AttachMoney,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Description Input
            ParadoxTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description / Merchant",
                placeholder = "e.g. Swiggy, Groceries, Amazon",
                leadingIcon = Icons.Default.Edit
            )

            // Category Chips
            Text(
                text = "Category",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = category },
                        color = if (isSelected) PrimaryPurple else SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryCyan else SurfaceBorder
                        )
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = DangerRed,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Save Button
            ParadoxGradientButton(
                text = "Add Transaction",
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount == null || parsedAmount <= 0) {
                        errorMessage = "Please enter a valid amount"
                        return@ParadoxGradientButton
                    }
                    if (description.isBlank()) {
                        errorMessage = "Please enter a description"
                        return@ParadoxGradientButton
                    }

                    coroutineScope.launch {
                        isSubmitting = true
                        errorMessage = null
                        try {
                            val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
                            repository.addExpense(
                                amount = parsedAmount,
                                description = description.trim(),
                                date = isoDate,
                                categoryName = selectedCategory
                            )
                            onExpenseAdded()
                            onDismiss()
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to save transaction"
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                isLoading = isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
