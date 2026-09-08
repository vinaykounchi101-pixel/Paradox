package com.paradox.finance.data.model

import com.google.gson.annotations.SerializedName

data class CategorySpend(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    val color: String? = null,
    val icon: String? = null,
    @SerializedName("total_spent") val totalSpent: Double,
    val percentage: Double = 0.0
)

data class BudgetStatusResponse(
    @SerializedName("total_budget") val totalBudget: Double,
    @SerializedName("total_spent") val totalSpent: Double,
    @SerializedName("remaining_budget") val remainingBudget: Double,
    @SerializedName("percent_used") val percentUsed: Double,
    @SerializedName("days_remaining") val daysRemaining: Int = 15,
    @SerializedName("category_breakdown") val categoryBreakdown: List<CategorySpend> = emptyList()
)

data class DashboardSummaryResponse(
    @SerializedName("total_monthly_spent") val totalMonthlySpent: Double,
    @SerializedName("safe_to_spend_today") val safeToSpendToday: Double,
    @SerializedName("recent_expenses") val recentExpenses: List<Expense> = emptyList(),
    @SerializedName("category_spend") val categorySpend: List<CategorySpend> = emptyList()
)
