package com.paradox.finance.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.data.models.BudgetStatusResponse
import com.paradox.finance.data.models.RecurringSummaryResponse
import com.paradox.finance.data.repository.BudgetRepository
import com.paradox.finance.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetViewModel(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _budgetStatus = MutableStateFlow<BudgetStatusResponse?>(null)
    val budgetStatus: StateFlow<BudgetStatusResponse?> = _budgetStatus.asStateFlow()

    private val _recurringSummary = MutableStateFlow<RecurringSummaryResponse?>(null)
    val recurringSummary: StateFlow<RecurringSummaryResponse?> = _recurringSummary.asStateFlow()

    private val _currentPeriodType = MutableStateFlow("monthly")
    val currentPeriodType: StateFlow<String> = _currentPeriodType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadBudget("monthly")
        loadRecurring()
    }

    fun setPeriodType(type: String) {
        _currentPeriodType.value = type
        loadBudget(type)
    }

    fun loadBudget(periodType: String = _currentPeriodType.value) {
        viewModelScope.launch {
            _isLoading.value = true
            val periodKey = getCurrentPeriodKey(periodType)
            val result = budgetRepository.getBudgetStatus(periodType, periodKey)
            result.onSuccess {
                _budgetStatus.value = it
            }
            _isLoading.value = false
        }
    }

    fun setBudgetTarget(amount: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            val periodType = _currentPeriodType.value
            val periodKey = getCurrentPeriodKey(periodType)
            budgetRepository.setBudget(amount, periodType, periodKey)
            loadBudget(periodType)
        }
    }

    fun loadRecurring() {
        viewModelScope.launch {
            val result = expenseRepository.getRecurringSummary()
            result.onSuccess {
                _recurringSummary.value = it
            }
        }
    }

    private fun getCurrentPeriodKey(periodType: String): String {
        val now = Date()
        return when (periodType.lowercase()) {
            "daily" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
            "weekly" -> SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(now)
            else -> SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(now)
        }
    }
}
