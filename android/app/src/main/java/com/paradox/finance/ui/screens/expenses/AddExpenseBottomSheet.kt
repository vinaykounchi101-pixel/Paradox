package com.paradox.finance.ui.screens.expenses

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.model.*
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.theme.*
import com.paradox.finance.ui.components.ParadoxGradientButton
import com.paradox.finance.ui.components.ParadoxTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    onDismiss: () -> Unit,
    onExpenseAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiClient.getService(context) }

    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quickAddText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethodId by remember { mutableStateOf<String?>(null) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isAiParsing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val catRes = apiService.getCategories()
                if (catRes.isSuccessful) {
                    categories = catRes.body() ?: emptyList()
                    selectedCategoryId = categories.firstOrNull()?.id
                }

                val payRes = apiService.getPaymentMethods()
                if (payRes.isSuccessful) {
                    paymentMethods = payRes.body() ?: emptyList()
                    selectedPaymentMethodId = paymentMethods.firstOrNull()?.id
                }
            } catch (e: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ParadoxZinc900,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ParadoxZinc700) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚡ Add Transaction",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = ParadoxZinc100
                )
            )

            // AI Quick Add Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParadoxTextField(
                    value = quickAddText,
                    onValueChange = { quickAddText = it },
                    placeholder = "AI: '₹450 for Swiggy lunch via UPI'",
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (quickAddText.isBlank()) return@Button
                        scope.launch {
                            isAiParsing = true
                            try {
                                val res = apiService.parseExpense(ParseExpenseRequest(quickAddText))
                                if (res.isSuccessful && res.body() != null) {
                                    val parsed = res.body()!!
                                    if (parsed.amount != null) amountText = parsed.amount.toString()
                                    if (!parsed.description.isNullOrBlank()) description = parsed.description
                                    if (!parsed.categoryName.isNullOrBlank()) {
                                        val match = categories.find { it.name.equals(parsed.categoryName, ignoreCase = true) }
                                        if (match != null) selectedCategoryId = match.id
                                    }
                                    Toast.makeText(context, "✨ AI Auto-Filled!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {}
                            finally {
                                isAiParsing = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ParadoxIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    if (isAiParsing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ParadoxZinc100)
                    } else {
                        Text("Fill", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Amount Input
            ParadoxTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = "Amount",
                placeholder = "0.00"
            )

            // Description Input
            ParadoxTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description / Merchant",
                placeholder = "e.g., Grocery store, Coffee"
            )

            // Category Chips
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat.id == selectedCategoryId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ParadoxIndigo else ParadoxZinc800)
                                .border(1.dp, if (isSelected) ParadoxIndigoLight else ParadoxZinc700, RoundedCornerShape(8.dp))
                                .clickable { selectedCategoryId = cat.id }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) ParadoxZinc100 else ParadoxZinc400,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // Submit Button
            ParadoxGradientButton(
                text = "Record Expense",
                isLoading = isSubmitting,
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        Toast.makeText(context, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                        return@ParadoxGradientButton
                    }
                    if (description.isBlank()) {
                        Toast.makeText(context, "Enter description", Toast.LENGTH_SHORT).show()
                        return@ParadoxGradientButton
                    }
                    if (selectedCategoryId == null || selectedPaymentMethodId == null) {
                        Toast.makeText(context, "Select category and payment method", Toast.LENGTH_SHORT).show()
                        return@ParadoxGradientButton
                    }

                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    scope.launch {
                        isSubmitting = true
                        try {
                            val res = apiService.createExpense(
                                CreateExpenseRequest(
                                    amount = amount,
                                    description = description.trim(),
                                    date = currentDate,
                                    categoryId = selectedCategoryId!!,
                                    paymentMethodId = selectedPaymentMethodId!!
                                )
                            )
                            if (res.isSuccessful) {
                                Toast.makeText(context, "Expense added!", Toast.LENGTH_SHORT).show()
                                onExpenseAdded()
                            } else {
                                Toast.makeText(context, "Failed to save: ${res.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
