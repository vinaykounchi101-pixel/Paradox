package com.paradox.finance.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.models.ExpenseDto
import com.paradox.finance.ui.dialogs.ImportCsvDialog
import com.paradox.finance.ui.dialogs.QuickAddExpenseDialog
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AuthViewModel
import com.paradox.finance.ui.viewmodels.ExpenseViewModel
import com.paradox.finance.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesLedgerScreen(
    expenseViewModel: ExpenseViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val expenses by expenseViewModel.expenses.collectAsState()
    val currentCurrency by authViewModel.currentCurrency.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedExpenseToEdit by remember { mutableStateOf<ExpenseDto?>(null) }

    val filteredExpenses = expenses.filter {
        (searchQuery.isBlank() || it.displayDescription.contains(searchQuery, ignoreCase = true)) &&
        (selectedFilterCategory == null || it.category?.displayName == selectedFilterCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Ledger", color = OnSurfaceHigh, fontSize = 16.sp, style = Typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Import", tint = ElectricEmerald)
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = ElectricEmerald)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search merchants, notes...", color = MutedOutline, fontSize = 12.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MutedOutline) },
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

            // Category Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Dining", "Groceries", "Shopping", "Tech", "Travel").forEach { cat ->
                    val isSelected = (cat == "All" && selectedFilterCategory == null) || selectedFilterCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(if (isSelected) ElectricEmerald.copy(alpha = 0.2f) else GlassSurface1)
                            .border(1.dp, if (isSelected) ElectricEmerald else GlassBorderStroke, RoundedCornerShape(9999.dp))
                            .clickable { selectedFilterCategory = if (cat == "All") null else cat }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) ElectricEmerald else OnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Ledger Items List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredExpenses) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassSurface1)
                            .border(1.dp, GlassBorderStroke, RoundedCornerShape(14.dp))
                            .clickable { selectedExpenseToEdit = item }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Receipt, contentDescription = "Item", tint = ElectricEmerald, modifier = Modifier.size(20.dp))
                                }

                                Column {
                                    Text(text = item.displayDescription, color = OnSurfaceHigh, fontSize = 14.sp, style = Typography.labelLarge)
                                    Text(text = "${item.category?.displayName ?: "Expense"} • ${item.displayDate}", color = OnSurfaceVariant, fontSize = 11.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "-${CurrencyFormatter.format(item.amount, currentCurrency)}",
                                    color = OnSurfaceHigh,
                                    fontSize = 14.sp,
                                    style = Typography.labelLarge
                                )
                                IconButton(onClick = { expenseViewModel.deleteExpense(item.id) }, modifier = Modifier.size(20.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MutedOutline, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportCsvDialog(
            expenseViewModel = expenseViewModel,
            onDismiss = { showImportDialog = false }
        )
    }

    if (showAddDialog || selectedExpenseToEdit != null) {
        QuickAddExpenseDialog(
            expenseViewModel = expenseViewModel,
            existingExpense = selectedExpenseToEdit,
            onDismiss = {
                showAddDialog = false
                selectedExpenseToEdit = null
            }
        )
    }
}
