package com.paradox.finance.ui.dialogs

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paradox.finance.data.models.CategoryDto
import com.paradox.finance.data.models.ExpenseDto
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.ExpenseViewModel
import com.paradox.finance.utils.CameraOcrHelper
import com.paradox.finance.utils.CurrencyFormatter
import com.paradox.finance.utils.SmsParserHelper
import com.paradox.finance.utils.VoiceInputHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddExpenseDialog(
    expenseViewModel: ExpenseViewModel,
    existingExpense: ExpenseDto? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val categories by expenseViewModel.categories.collectAsState()

    var amountText by remember { mutableStateOf(existingExpense?.amount?.toString() ?: "") }
    var descriptionText by remember { mutableStateOf(existingExpense?.description ?: "") }
    var aiInputText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(existingExpense?.categoryId) }
    var selectedDate by remember { mutableStateOf(existingExpense?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var isRecurring by remember { mutableStateOf(existingExpense?.isRecurring ?: false) }
    var recurringFrequency by remember { mutableStateOf(existingExpense?.recurringFrequency ?: "monthly") }

    var isVoiceListening by remember { mutableStateOf(false) }
    var showSmsDrawer by remember { mutableStateOf(false) }
    var rawSmsInput by remember { mutableStateOf("") }

    val voiceHelper = remember {
        VoiceInputHelper(
            context = context,
            onResult = { text ->
                aiInputText = text
                val parsed = SmsParserHelper.parseIndianSms(text)
                if (parsed.amount != null) amountText = parsed.amount.toString()
                if (parsed.merchant != null) descriptionText = parsed.merchant
            },
            onError = {},
            onListeningStateChanged = { isVoiceListening = it }
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val base64 = CameraOcrHelper.scaleAndEncodeToBase64(bitmap)
            // Local OCR / simulated parse
            descriptionText = "Receipt OCR Scan"
            if (amountText.isBlank()) amountText = "450.00"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(ObsidianCanvas)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingExpense != null) "Edit Expense" else "New Expense",
                        color = OnSurfaceHigh,
                        fontSize = 18.sp,
                        style = Typography.headlineSmall
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedOutline)
                    }
                }

                // Currency Amount Stage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹",
                        color = ElectricEmerald,
                        fontSize = 32.sp,
                        style = Typography.displayMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        placeholder = { Text("0.00", color = MutedOutline, fontSize = 28.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = OnSurfaceHigh,
                            unfocusedTextColor = OnSurfaceHigh,
                            cursorColor = ElectricEmerald
                        ),
                        textStyle = Typography.displayMedium.copy(fontSize = 32.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Amount Increments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(100, 500, 1000, 2000).forEach { inc ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceContainerHigh)
                                .clickable {
                                    val cur = amountText.toDoubleOrNull() ?: 0.0
                                    amountText = (cur + inc).toString()
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "+₹$inc", color = OnSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                }

                // AI Quick Add Bar
                OutlinedTextField(
                    value = aiInputText,
                    onValueChange = {
                        aiInputText = it
                        val parsed = SmsParserHelper.parseIndianSms(it)
                        if (parsed.amount != null) amountText = parsed.amount.toString()
                        if (parsed.merchant != null) descriptionText = parsed.merchant
                    },
                    placeholder = { Text("e.g. '350 for Pizza via UPI'", color = MutedOutline, fontSize = 12.sp) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { voiceHelper.startListening() }) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice",
                                    tint = if (isVoiceListening) AlertCoral else ElectricEmerald
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9999.dp))
                                    .background(ElectricEmerald)
                                    .clickable {
                                        val parsed = SmsParserHelper.parseIndianSms(aiInputText)
                                        if (parsed.amount != null) amountText = parsed.amount.toString()
                                        if (parsed.merchant != null) descriptionText = parsed.merchant
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Auto-Fill", color = PitchBlack, fontSize = 10.sp, style = Typography.labelSmall)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                )

                // Multimodal Actions Grid (2-Columns)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // CameraX Scan Bill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface1)
                            .border(1.dp, GlassBorderStroke, RoundedCornerShape(12.dp))
                            .clickable { cameraLauncher.launch(null) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                            Text(text = "Scan Bill", color = OnSurface, fontSize = 11.sp)
                        }
                    }

                    // Paste SMS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface1)
                            .border(1.dp, GlassBorderStroke, RoundedCornerShape(12.dp))
                            .clickable { showSmsDrawer = !showSmsDrawer }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Default.Sms, contentDescription = "SMS", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Text(text = "Paste SMS", color = OnSurface, fontSize = 11.sp)
                        }
                    }
                }

                // Description Field
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description / Note", color = OnSurfaceVariant, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = GlassBorderStroke,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection Grid
                Text(
                    text = "Select Category",
                    color = OnSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) GlassSurface2 else GlassSurface1)
                                .border(1.dp, if (isSelected) ElectricEmerald else GlassBorderStroke, RoundedCornerShape(12.dp))
                                .clickable { selectedCategoryId = cat.id }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.displayName,
                                color = if (isSelected) ElectricEmerald else OnSurface,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Recurring Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔁 Recurring Subscription", color = OnSurface, fontSize = 12.sp)
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PitchBlack, checkedTrackColor = ElectricEmerald)
                    )
                }

                // Action Button
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            if (existingExpense != null) {
                                expenseViewModel.updateExpense(
                                    id = existingExpense.id,
                                    amount = amt,
                                    description = descriptionText,
                                    date = selectedDate,
                                    categoryId = selectedCategoryId,
                                    onSuccess = onDismiss
                                )
                            } else {
                                expenseViewModel.createExpense(
                                    amount = amt,
                                    description = descriptionText.ifBlank { "Expense" },
                                    date = selectedDate,
                                    categoryId = selectedCategoryId,
                                    isRecurring = isRecurring,
                                    recurringFrequency = if (isRecurring) recurringFrequency else null,
                                    onSuccess = onDismiss
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(50.dp)
                ) {
                    Text(
                        text = if (existingExpense != null) "Update Expense" else "Confirm & Record Expense",
                        color = PitchBlack,
                        fontSize = 14.sp,
                        style = Typography.labelLarge
                    )
                }
            }
        }
    }
}
