package com.paradox.finance.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.core.Resource
import com.paradox.finance.data.preferences.AuthPreferences
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.ui.components.FinnyMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "",
    val currency: String = "INR",
    val isLoading: Boolean = false,
    val finnyMood: FinnyMood = FinnyMood.JOYFUL,
    val finnySpeech: String = "Welcome back! Checking your financial health...",
    
    // Safe-to-Spend Metrics
    val safeToSpendDaily: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val burnVelocity: String = "Pacing Normal",
    val healthScore: Int = 85,
    
    // 50/30/20 Breakdown
    val needsSpent: Double = 0.0,
    val needsTarget: Double = 0.0,
    val wantsSpent: Double = 0.0,
    val wantsTarget: Double = 0.0,
    val savingsSpent: Double = 0.0,
    val savingsTarget: Double = 0.0,
    
    val totalSpent: Double = 0.0,
    val totalBudget: Double = 0.0,
    
    // Graphs & Analytics
    val trendData: List<com.paradox.finance.ui.components.TrendPoint> = emptyList(),
    val categoryBreakdown: List<com.paradox.finance.ui.components.CategoryChartItem> = emptyList(),

    // Streaks
    val currentStreakDays: Int = 5,
    val totalExpensesCount: Int = 0
)

class DashboardViewModel(
    private val authPrefs: AuthPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            userName = authPrefs.getUserName() ?: "Investor",
            currency = authPrefs.getCurrency()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val api = ApiClient.getApi()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 0. Fetch Aggregated Dashboard Stats (Total Spent, Budget, Trend & Category Graphs)
            try {
                val dashRes = api.getDashboard("current_month")
                if (dashRes.isSuccessful && dashRes.body() != null) {
                    val rawBody = dashRes.body()!!
                    val data = (rawBody["data"] as? Map<*, *>) ?: rawBody
                    val spent = (data["total_expenses"] as? Number)?.toDouble() 
                        ?: (data["total_spent"] as? Number)?.toDouble() 
                        ?: data["total_spent"]?.toString()?.toDoubleOrNull() ?: 0.0
                    val budgetObj = data["budget"] as? Map<*, *>
                    val budget = (budgetObj?.get("amount") as? Number)?.toDouble() ?: 0.0

                    // Trend Graph Points
                    val trendList = (data["trend"] as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                    val parsedTrend = trendList.mapNotNull { item ->
                        val label = item["label"]?.toString() ?: ""
                        val total = (item["total"] as? Number)?.toDouble()
                            ?: item["total"]?.toString()?.toDoubleOrNull() ?: 0.0
                        if (label.isNotEmpty()) com.paradox.finance.ui.components.TrendPoint(label, total) else null
                    }

                    // Category Breakdown Bars
                    val catList = ((data["category_breakdown"] as? List<*>) ?: (data["expenses_by_category"] as? List<*>))?.filterIsInstance<Map<*, *>>() ?: emptyList()
                    val parsedCategories = catList.mapIndexedNotNull { idx, item ->
                        val name = item["category_name"]?.toString() ?: item["name"]?.toString() ?: "Other"
                        val total = (item["total"] as? Number)?.toDouble()
                            ?: item["total"]?.toString()?.toDoubleOrNull()
                            ?: (item["amount"] as? Number)?.toDouble() ?: 0.0
                        val pct = (item["percentage"] as? Number)?.toDouble() ?: 0.0
                        if (name.isNotEmpty()) {
                            com.paradox.finance.ui.components.CategoryChartItem(
                                categoryName = name,
                                total = total,
                                percentage = pct,
                                color = com.paradox.finance.ui.components.CATEGORY_COLORS[idx % com.paradox.finance.ui.components.CATEGORY_COLORS.size]
                            )
                        } else null
                    }

                    _uiState.value = _uiState.value.copy(
                        totalSpent = spent,
                        totalBudget = budget,
                        trendData = parsedTrend,
                        categoryBreakdown = parsedCategories
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }

            // 1. Fetch Safe-to-Spend
            try {
                val safeRes = api.getSafeToSpend()
                if (safeRes.isSuccessful && safeRes.body() != null) {
                    val rawBody = safeRes.body()!!
                    val data = (rawBody["data"] as? Map<*, *>) ?: rawBody
                    val daily = (data["daily_allowance"] as? Number)?.toDouble() ?: 0.0
                    val rem = (data["remaining_budget"] as? Number)?.toDouble() ?: 0.0
                    val pacing = data["pacing_status"]?.toString() ?: "Normal"

                    _uiState.value = _uiState.value.copy(
                        safeToSpendDaily = daily,
                        remainingBudget = rem,
                        burnVelocity = pacing
                    )
                }
            } catch (e: Exception) {
                // Heuristic fallback
            }

            // 2. Fetch Health Score
            try {
                val healthRes = api.getHealthScore()
                if (healthRes.isSuccessful && healthRes.body() != null) {
                    val rawBody = healthRes.body()!!
                    val data = (rawBody["data"] as? Map<*, *>) ?: rawBody
                    val score = (data["health_score"] as? Number)?.toInt() ?: 85
                    val commentary = data["commentary"]?.toString() ?: "Spending on track!"

                    val mood = when {
                        score >= 80 -> FinnyMood.JOYFUL
                        score >= 60 -> FinnyMood.CALM
                        score >= 40 -> FinnyMood.ALERT
                        else -> FinnyMood.STRESSED
                    }

                    _uiState.value = _uiState.value.copy(
                        healthScore = score,
                        finnyMood = mood,
                        finnySpeech = commentary
                    )
                }
            } catch (e: Exception) {
                // Fallback
            }

            // 3. Fetch 50/30/20
            try {
                val ftRes = api.getFiftyThirtyTwenty()
                if (ftRes.isSuccessful && ftRes.body() != null) {
                    val rawBody = ftRes.body()!!
                    val data = (rawBody["data"] as? Map<*, *>) ?: rawBody
                    val needs = (data["needs_spent"] as? Number)?.toDouble() ?: 0.0
                    val wants = (data["wants_spent"] as? Number)?.toDouble() ?: 0.0
                    val savings = (data["savings_spent"] as? Number)?.toDouble() ?: 0.0

                    _uiState.value = _uiState.value.copy(
                        needsSpent = needs,
                        wantsSpent = wants,
                        savingsSpent = savings
                    )
                }
            } catch (e: Exception) {
                // Fallback
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setCurrency(newCurrency: String) {
        authPrefs.setCurrency(newCurrency)
        _uiState.value = _uiState.value.copy(currency = newCurrency)
        viewModelScope.launch {
            try {
                api.updateProfile(com.paradox.finance.data.remote.dto.UpdateProfileRequest(currency = newCurrency))
            } catch (e: Exception) {
                // Ignored
            }
        }
    }
}
