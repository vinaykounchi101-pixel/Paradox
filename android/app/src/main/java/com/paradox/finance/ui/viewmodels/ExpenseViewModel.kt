package com.paradox.finance.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.data.models.*
import com.paradox.finance.data.repository.BudgetRepository
import com.paradox.finance.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    val expenses: StateFlow<List<ExpenseDto>> = expenseRepository.localExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryDto>> = budgetRepository.localCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            expenseRepository.refreshExpenses()
            budgetRepository.refreshCategories()
        }
    }

    fun createExpense(
        amount: Double,
        description: String,
        date: String,
        categoryId: String?,
        paymentMethodId: String? = null,
        isRecurring: Boolean = false,
        recurringFrequency: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val request = CreateExpenseRequest(
                amount = amount,
                description = description,
                date = date,
                categoryId = categoryId,
                paymentMethodId = paymentMethodId,
                isRecurring = isRecurring,
                recurringFrequency = recurringFrequency
            )
            val result = expenseRepository.createExpense(request)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Expense logged successfully!"
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.message ?: "Failed to log expense"
            }
        }
    }

    fun updateExpense(
        id: String,
        amount: Double?,
        description: String?,
        date: String?,
        categoryId: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val request = UpdateExpenseRequest(
                amount = amount,
                description = description,
                date = date,
                categoryId = categoryId
            )
            val result = expenseRepository.updateExpense(id, request)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Expense updated!"
                onSuccess()
            }.onFailure {
                _feedbackMessage.value = it.message ?: "Update failed"
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(id)
        }
    }

    fun createCategory(name: String, icon: String = "category", color: String = "#4edea3", onCreated: (CategoryDto) -> Unit) {
        viewModelScope.launch {
            val result = budgetRepository.createCategory(CreateCategoryRequest(name, icon, color))
            result.onSuccess {
                onCreated(it)
            }
        }
    }

    fun importCsv(file: File) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = expenseRepository.importCsv(file)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Imported ${it.importedCount} transactions!"
            }.onFailure {
                _feedbackMessage.value = it.message ?: "Import failed"
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
