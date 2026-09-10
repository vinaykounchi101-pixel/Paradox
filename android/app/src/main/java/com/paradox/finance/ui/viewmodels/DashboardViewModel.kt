package com.paradox.finance.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.data.models.*
import com.paradox.finance.data.repository.AiRepository
import com.paradox.finance.data.repository.BudgetRepository
import com.paradox.finance.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val summary: DashboardSummaryResponse? = null,
    val safeToSpend: SafeToSpendResponse? = null,
    val fiftyThirtyTwenty: FiftyThirtyTwentyResponse? = null,
    val finnyVibe: FinnyVibeCheckResponse? = null,
    val isBalanceVisible: Boolean = true,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun toggleBalanceVisibility() {
        _uiState.value = _uiState.value.copy(
            isBalanceVisible = !_uiState.value.isBalanceVisible
        )
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            // Parallel background fetches
            val summaryJob = launch {
                budgetRepository.getDashboardSummary().onSuccess {
                    _uiState.value = _uiState.value.copy(summary = it)
                }
            }

            val safeSpendJob = launch {
                aiRepository.getSafeToSpend().onSuccess {
                    _uiState.value = _uiState.value.copy(safeToSpend = it)
                }
            }

            val splitJob = launch {
                aiRepository.getFiftyThirtyTwenty().onSuccess {
                    _uiState.value = _uiState.value.copy(fiftyThirtyTwenty = it)
                }
            }

            val vibeJob = launch {
                aiRepository.getFinnyVibe().onSuccess {
                    _uiState.value = _uiState.value.copy(finnyVibe = it)
                }
            }

            expenseRepository.refreshExpenses()
            budgetRepository.refreshCategories()

            summaryJob.join()
            safeSpendJob.join()
            splitJob.join()
            vibeJob.join()

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
