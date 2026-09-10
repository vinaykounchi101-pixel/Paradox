package com.paradox.finance.data.models

import com.google.gson.annotations.SerializedName

data class BudgetDto(
    @SerializedName("id") val id: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("period_type") val periodType: String,
    @SerializedName("period_key") val periodKey: String,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class BudgetStatusResponse(
    @SerializedName("period_type") val periodType: String,
    @SerializedName("period_key") val periodKey: String,
    @SerializedName("budget_amount") val budgetAmount: Double = 0.0,
    @SerializedName("spent_amount") val spentAmount: Double = 0.0,
    @SerializedName("remaining_amount") val remainingAmount: Double = 0.0,
    @SerializedName("utilization_percentage") val utilizationPercentage: Double = 0.0,
    @SerializedName("is_over_budget") val isOverBudget: Boolean = false,
    @SerializedName("status") val status: String = "safe",
    @SerializedName("daily_safe_allowance") val dailySafeAllowance: Double = 0.0
)

data class SetBudgetRequest(
    @SerializedName("amount") val amount: Double,
    @SerializedName("period_type") val periodType: String,
    @SerializedName("period_key") val periodKey: String
)

data class DashboardSummaryResponse(
    @SerializedName("total_balance") val totalBalance: Double = 0.0,
    @SerializedName("month_spend") val monthSpend: Double = 0.0,
    @SerializedName("budget_target") val budgetTarget: Double = 0.0,
    @SerializedName("budget_remaining") val budgetRemaining: Double = 0.0,
    @SerializedName("daily_burn_velocity") val dailyBurnVelocity: Double = 0.0,
    @SerializedName("days_left_in_cycle") val daysLeftInCycle: Int = 0,
    @SerializedName("recent_expenses") val recentExpenses: List<ExpenseDto> = emptyList(),
    @SerializedName("category_distribution") val categoryDistribution: Map<String, Double> = emptyMap()
)
