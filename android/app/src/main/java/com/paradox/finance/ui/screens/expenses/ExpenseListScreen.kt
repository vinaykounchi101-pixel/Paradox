package com.paradox.finance.ui.screens.expenses

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.paradox.finance.core.Constants
import com.paradox.finance.data.local.entity.ExpenseEntity
import com.paradox.finance.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val currencySymbol = Constants.CURRENCY_SYMBOLS[state.currency] ?: "₹"
    val context = LocalContext.current

    // Toast notifications for export/import
    LaunchedEffect(state.exportSuccessMessage) {
        state.exportSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(state.importSuccessMessage) {
        state.importSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Import CSV Button
                    IconButton(
                        onClick = { viewModel.openImportDialog() },
                        enabled = !state.isImporting && !state.isExporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import CSV",
                            tint = AccentEmerald
                        )
                    }

                    // Export CSV Button
                    IconButton(
                        onClick = { viewModel.exportExpensesCsv(context) },
                        enabled = !state.isImporting && !state.isExporting
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PrimaryIndigo,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = PrimaryIndigo
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = PrimaryIndigo,
                contentColor = TextPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Success / Error status banners
                if (state.importSuccessMessage != null) {
                    Surface(
                        color = AccentEmerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text("✨", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.importSuccessMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentEmerald,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearMessages() },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                if (state.expenses.isEmpty() && !state.isSyncing) {
                    // Empty state
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No expenses recorded yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + to add or use ⬆️ to import bank CSV",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.expenses, key = { it.id }) { expense ->
                            ExpenseCardItem(
                                expense = expense,
                                currencySymbol = currencySymbol,
                                onEdit = { viewModel.openEditDialog(expense) },
                                onDelete = { viewModel.deleteExpense(expense.id) }
                            )
                        }
                    }
                }
            }

            if (state.showAddDialog) {
                QuickAddExpenseDialog(
                    viewModel = viewModel,
                    isEditMode = false,
                    onDismiss = { viewModel.closeAddDialog() }
                )
            }

            if (state.showEditDialog) {
                QuickAddExpenseDialog(
                    viewModel = viewModel,
                    isEditMode = true,
                    onDismiss = { viewModel.closeEditDialog() }
                )
            }

            if (state.showImportDialog) {
                ImportCsvDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeImportDialog() }
                )
            }
        }
    }
}

@Composable
fun ImportCsvDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = uri.lastPathSegment ?: "statement.csv"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!state.isImporting) onDismiss() },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Bank Statement (CSV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Upload your bank statement or spreadsheet (.csv). Paradox AI will parse transactions, categorize each purchase, and update your finances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        color = if (selectedUri != null) AccentEmerald else PrimaryIndigo.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isImporting) {
                            filePickerLauncher.launch("*/*")
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedUri != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = if (selectedUri != null) AccentEmerald else PrimaryIndigo,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedFileName ?: "Tap to choose CSV file",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedUri != null) AccentEmerald else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supports standard bank exports with Date, Amount & Narration",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = AccentRose,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedUri?.let { uri ->
                        viewModel.importExpensesCsv(context, uri)
                    }
                },
                enabled = selectedUri != null && !state.isImporting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Categorizing...", fontSize = 12.sp)
                } else {
                    Text("Start AI Import", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isImporting
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ExpenseCardItem(
    expense: ExpenseEntity,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Category Icon Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (expense.categoryName?.firstOrNull() ?: 'E').uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.description.ifBlank { "Expense" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = expense.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        if (expense.categoryName != null) {
                            Text(" • ", color = TextMuted)
                            Text(
                                text = expense.categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentAmber
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "-$currencySymbol${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentRose
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
