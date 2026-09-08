package com.paradox.finance.data.model

import com.google.gson.annotations.SerializedName

data class BudgetStatusResponse(
    @SerializedName("period_type") val periodType: String,
    @SerializedName("period_key") val periodKey: String,
    @SerializedName("budget_amount") val budgetAmount: Double,
    @SerializedName("spent_amount") val spentAmount: Double,
    @SerializedName("remaining_amount") val remainingAmount: Double,
    @SerializedName("percentage_used") val percentageUsed: Double,
    val status: String // "under_budget", "warning", "exceeded"
)

data class DashboardSummaryResponse(
    @SerializedName("total_monthly_spent") val totalMonthlySpent: Double,
    @SerializedName("recent_expenses") val recentExpenses: List<Expense> = emptyList(),
    @SerializedName("budget_status") val budgetStatus: BudgetStatusResponse? = null
)
