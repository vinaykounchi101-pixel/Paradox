package com.paradox.finance.ui.screens.expenses

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.core.PredictiveDictionary
import com.paradox.finance.core.Resource
import com.paradox.finance.data.local.entity.CategoryEntity
import com.paradox.finance.data.local.entity.ExpenseEntity
import com.paradox.finance.data.preferences.AuthPreferences
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.hardware.camera.ReceiptScannerHelper
import com.paradox.finance.hardware.sms.SmsTransactionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExpenseUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val paymentMethods: List<Map<String, Any>> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val importSuccessMessage: String? = null,
    val currency: String = "INR",
    val errorMessage: String? = null,
    
    // Quick Add & Edit Form
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingExpenseId: String? = null,
    val showImportDialog: Boolean = false,
    val quickAddInput: String = "",
    val amountInput: String = "",
    val descriptionInput: String = "",
    val selectedCategoryId: String? = null,
    val selectedPaymentMethodId: String? = null,
    val dateInput: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = "monthly",
    val suggestedCategory: String? = null,
    val isListeningVoice: Boolean = false,
    val isOcrScanning: Boolean = false
)

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val authPrefs: AuthPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState(currency = authPrefs.getCurrency()))
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private val api = ApiClient.getApi()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            repository.getLocalExpenses().collect { list ->
                _uiState.value = _uiState.value.copy(expenses = list)
            }
        }

        viewModelScope.launch {
            repository.getLocalCategories().collect { list ->
                _uiState.value = _uiState.value.copy(categories = list)
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            repository.syncCategories()
            repository.syncExpenses()
            val pmRes = repository.getPaymentMethods()
            if (pmRes is Resource.Success) {
                _uiState.value = _uiState.value.copy(paymentMethods = pmRes.data)
            }
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            quickAddInput = "",
            amountInput = "",
            descriptionInput = "",
            suggestedCategory = null,
            dateInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun onDescriptionChanged(text: String) {
        _uiState.value = _uiState.value.copy(descriptionInput = text)
        checkPredictiveCategory(text)
    }

    fun onAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amountInput = amount)
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onQuickAddInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(quickAddInput = text)
        checkPredictiveCategory(text)
    }

    private fun checkPredictiveCategory(text: String) {
        val suggestion = PredictiveDictionary.predict(text)
        if (suggestion != null) {
            _uiState.value = _uiState.value.copy(suggestedCategory = suggestion.categoryName)
            val matchedCat = _uiState.value.categories.find { it.name.equals(suggestion.categoryName, ignoreCase = true) }
            if (matchedCat != null) {
                _uiState.value = _uiState.value.copy(selectedCategoryId = matchedCat.id)
            }
        }
    }

    fun applySuggestedCategory() {
        val catName = _uiState.value.suggestedCategory ?: return
        val existing = _uiState.value.categories.find { it.name.equals(catName, ignoreCase = true) }
        if (existing != null) {
            _uiState.value = _uiState.value.copy(selectedCategoryId = existing.id)
        } else {
            // 1-Click Create & Select Category in DB!
            viewModelScope.launch {
                when (val res = repository.createCategory(catName)) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(selectedCategoryId = res.data.id)
                    }
                    else -> Unit
                }
            }
        }
    }

    fun parseNaturalLanguageExpense() {
        val text = _uiState.value.quickAddInput
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val res = api.parseExpense(mapOf("text" to text))
                if (res.isSuccessful && res.body() != null) {
                    val rawBody = res.body()!!
                    val data = (rawBody["data"] as? Map<*, *>) ?: rawBody
                    val amount = (data["amount"] as? Number)?.toDouble() 
                        ?: data["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                    val merchant = data["description"]?.toString() ?: text
                    val catName = data["category"]?.toString()
                    val pmName = data["payment_method"]?.toString()
                    val date = data["date"]?.toString()

                    _uiState.value = _uiState.value.copy(
                        amountInput = if (amount > 0) String.format(Locale.US, "%.2f", amount) else _uiState.value.amountInput,
                        descriptionInput = merchant,
                        dateInput = date ?: _uiState.value.dateInput,
                        suggestedCategory = catName
                    )

                    if (catName != null) {
                        applySuggestedCategory()
                    }
                }
            } catch (e: Exception) {
                // Fallback to local parsing
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun handleSmsPaste(smsText: String) {
        val parsed = SmsTransactionParser.parse(smsText) ?: return
        _uiState.value = _uiState.value.copy(
            amountInput = parsed.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "",
            descriptionInput = parsed.merchant ?: "",
            dateInput = parsed.date
        )
        parsed.merchant?.let { checkPredictiveCategory(it) }
    }

    fun handleReceiptScan(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOcrScanning = true)
            val result = ReceiptScannerHelper.scanReceipt(context, uri)
            if (result is Resource.Success) {
                val rawBody = result.data
                val data = (rawBody["data"] as? Map<*, *>) ?: rawBody
                val amount = (data["amount"] as? Number)?.toDouble()
                    ?: data["amount"]?.toString()?.toDoubleOrNull()
                val merchant = data["description"]?.toString()
                    ?: data["merchant"]?.toString() 
                    ?: data["store"]?.toString()
                val date = data["date"]?.toString()
                val cat = data["category"]?.toString()

                _uiState.value = _uiState.value.copy(
                    amountInput = if (amount != null && amount > 0) String.format(Locale.US, "%.2f", amount) else "",
                    descriptionInput = merchant ?: "Receipt Expense",
                    dateInput = date ?: _uiState.value.dateInput,
                    suggestedCategory = cat
                )
                if (cat != null) applySuggestedCategory()
            }
            _uiState.value = _uiState.value.copy(isOcrScanning = false)
        }
    }

    fun saveExpense() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid amount")
            return
        }

        val categoryId = state.selectedCategoryId ?: state.categories.firstOrNull()?.id ?: "1"
        val paymentMethodId = state.selectedPaymentMethodId ?: state.paymentMethods.firstOrNull()?.get("id")?.toString() ?: "1"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = repository.createExpense(
                amount = amount,
                date = state.dateInput,
                description = state.descriptionInput.ifBlank { "Expense" },
                categoryId = categoryId,
                paymentMethodId = paymentMethodId,
                isRecurring = state.isRecurring,
                recurringFrequency = state.recurringFrequency
            )
            if (res is Resource.Success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddDialog = false,
                    amountInput = "",
                    descriptionInput = "",
                    quickAddInput = "",
                    selectedCategoryId = null
                )
            } else if (res is Resource.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
            }
        }
    }

    fun openEditDialog(expense: ExpenseEntity) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            editingExpenseId = expense.id,
            amountInput = if (expense.amount > 0) String.format(Locale.US, "%.2f", expense.amount) else "",
            descriptionInput = expense.description,
            selectedCategoryId = expense.categoryId,
            selectedPaymentMethodId = expense.paymentMethodId,
            dateInput = expense.date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
            isRecurring = expense.isRecurring,
            recurringFrequency = expense.recurringFrequency ?: "monthly",
            errorMessage = null
        )
    }

    fun closeEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingExpenseId = null)
    }

    fun updateExpense() {
        val state = _uiState.value
        val expenseId = state.editingExpenseId ?: return
        val amount = state.amountInput.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid amount")
            return
        }

        val categoryId = state.selectedCategoryId ?: state.categories.firstOrNull()?.id ?: return
        val paymentMethodId = state.selectedPaymentMethodId ?: state.paymentMethods.firstOrNull()?.get("id")?.toString() ?: "1"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val res = repository.updateExpense(
                id = expenseId,
                amount = amount,
                date = state.dateInput,
                description = state.descriptionInput.ifBlank { "Expense" },
                categoryId = categoryId,
                paymentMethodId = paymentMethodId,
                isRecurring = state.isRecurring,
                recurringFrequency = state.recurringFrequency
            )
            if (res is Resource.Success) {
                _uiState.value = _uiState.value.copy(isLoading = false, showEditDialog = false, editingExpenseId = null)
            } else if (res is Resource.Error) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = res.message)
            }
        }
    }

    fun openImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = true, errorMessage = null, importSuccessMessage = null)
    }

    fun closeImportDialog() {
        _uiState.value = _uiState.value.copy(showImportDialog = false)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, exportSuccessMessage = null, importSuccessMessage = null)
    }

    fun exportExpensesCsv(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null, exportSuccessMessage = null)
            try {
                var csvBytes: ByteArray? = null
                
                // 1. Try server export first
                try {
                    val res = api.exportExpenses()
                    if (res.isSuccessful && res.body() != null) {
                        csvBytes = res.body()!!.bytes()
                    }
                } catch (_: Exception) {
                    // Fall back to local database export
                }

                // 2. If server export is null or empty, generate from local Room expenses
                if (csvBytes == null || csvBytes.isEmpty()) {
                    val localExpenses = _uiState.value.expenses.ifEmpty {
                        repository.getLocalExpenses().firstOrNull() ?: emptyList()
                    }
                    val sb = StringBuilder()
                    sb.append("Date,Description,Category,Payment Method,Amount,Recurring\n")
                    localExpenses.forEach { exp ->
                        val date = exp.date
                        val desc = exp.description.replace("\"", "\"\"")
                        val cat = (exp.categoryName ?: "Other").replace("\"", "\"\"")
                        val pm = (exp.paymentMethodName ?: "Cash").replace("\"", "\"\"")
                        val amt = exp.amount
                        val rec = exp.isRecurring
                        sb.append("\"$date\",\"$desc\",\"$cat\",\"$pm\",$amt,$rec\n")
                    }
                    csvBytes = sb.toString().toByteArray(Charsets.UTF_8)
                }

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "paradox_expenses_$timestamp.csv"
                
                // Save to cache dir for sharing via FileProvider
                val exportDir = File(context.cacheDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                val file = File(exportDir, fileName)
                FileOutputStream(file).use { it.write(csvBytes) }

                // Also save to app documents directory
                try {
                    val docDir = context.getExternalFilesDir(null)
                    if (docDir != null && (docDir.exists() || docDir.mkdirs())) {
                        val docFile = File(docDir, fileName)
                        FileOutputStream(docFile).use { it.write(csvBytes) }
                    }
                } catch (_: Exception) {
                    // Ignored
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportSuccessMessage = "Exported to $fileName"
                )

                // Trigger Android system Share / Save Sheet
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Paradox Expenses Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(sendIntent, "Export Paradox Expenses CSV").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "Export error: ${e.localizedMessage ?: "Unknown"}"
                )
            }
        }
    }

    fun importExpensesCsv(context: Context, fileUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes == null || bytes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "Could not read selected CSV file"
                    )
                    return@launch
                }

                val reqBody = bytes.toRequestBody("text/csv".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "statement.csv", reqBody)

                val res = api.importExpensesCsv(part)
                if (res.isSuccessful && res.body() != null) {
                    val rawBody = res.body()!!
                    val data = (rawBody["data"] as? Map<*, *>) ?: rawBody
                    val msg = data["message"]?.toString()
                        ?: "Successfully imported ${data["imported"] ?: "transactions"} with AI categorization!"

                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        showImportDialog = false,
                        importSuccessMessage = msg
                    )
                    // Refresh data
                    loadData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        errorMessage = "Import failed: HTTP ${res.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "Import error: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            repository.deleteExpense(id)
        }
    }
}
