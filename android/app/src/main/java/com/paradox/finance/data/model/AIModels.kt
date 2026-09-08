package com.paradox.finance.data.model

import com.google.gson.annotations.SerializedName

data class CategorizeRequest(
    val description: String
)

data class CategorizeResponse(
    @SerializedName("category_name") val categoryName: String,
    val confidence: Double = 1.0,
    @SerializedName("is_new_category") val isNewCategory: Boolean = false,
    @SerializedName("provider_used") val providerUsed: String = "heuristic"
)

data class ParseExpenseRequest(
    val text: String
)

data class ParseExpenseResponse(
    val amount: Double? = null,
    val description: String? = null,
    val date: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("payment_method_name") val paymentMethodName: String? = null,
    @SerializedName("provider_used") val providerUsed: String = "heuristic"
)

data class SafeToSpendResponse(
    @SerializedName("safe_daily_allowance") val safeDailyAllowance: Double,
    @SerializedName("days_remaining") val daysRemaining: Int,
    @SerializedName("current_daily_burn_rate") val currentDailyBurnRate: Double,
    val pacing_status: String,
    val headline: String,
    val tip: String
)

data class AIChatMessage(
    val role: String,
    val content: String
)

data class AIChatRequest(
    val message: String,
    val history: List<AIChatMessage> = emptyList()
)

data class AIChatResponse(
    val reply: String,
    @SerializedName("suggested_actions") val suggestedActions: List<String> = emptyList(),
    @SerializedName("provider_used") val providerUsed: String = "gemini"
)

data class SimulatePurchaseRequest(
    val amount: Double,
    @SerializedName("category_name") val categoryName: String? = null,
    val description: String? = null
)

data class SimulatePurchaseResponse(
    val verdict: String,
    val headline: String,
    val advice: String,
    @SerializedName("current_remaining_budget") val currentRemainingBudget: Double
)

data class LeakCategory(
    val category: String,
    @SerializedName("transaction_count") val transactionCount: Int,
    @SerializedName("monthly_total") val monthlyTotal: Double,
    @SerializedName("projected_annual_drain") val projectedAnnualDrain: Double
)

data class LeakAnalysisResponse(
    @SerializedName("total_leaked_amount") val totalLeakedAmount: Double,
    @SerializedName("projected_annual_drain") val projectedAnnualDrain: Double,
    @SerializedName("leak_count") val leakCount: Int,
    val categories: List<LeakCategory> = emptyList(),
    val headline: String,
    val advice: String
)

data class SpendingForecastResponse(
    @SerializedName("projected_monthly_spend") val projectedMonthlySpend: Double,
    @SerializedName("budget_depletion_predicted") val budgetDepletionPredicted: Boolean = false,
    val headline: String,
    val advice: String
)

data class ScanReceiptRequest(
    @SerializedName("image_base64") val imageBase64: String
)

data class ScanReceiptResponse(
    val amount: Double? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    val date: String? = null,
    @SerializedName("category_suggestion") val categorySuggestion: String? = null,
    @SerializedName("payment_method_suggestion") val paymentMethodSuggestion: String? = null
)

data class ParseSmsRequest(
    val sms: String
)

data class ParseSmsResponse(
    val amount: Double? = null,
    val merchant: String? = null,
    val date: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("reference_number") val referenceNumber: String? = null
)

data class FiftyThirtyTwentyResponse(
    @SerializedName("needs_amount") val needsAmount: Double,
    @SerializedName("needs_percentage") val needsPercentage: Double,
    @SerializedName("wants_amount") val wantsAmount: Double,
    @SerializedName("wants_percentage") val wantsPercentage: Double,
    @SerializedName("savings_amount") val savingsAmount: Double,
    @SerializedName("savings_percentage") val savingsPercentage: Double,
    @SerializedName("adherence_score") val adherenceScore: Int = 85,
    val recommendation: String
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    @SerializedName("is_unlocked") val isUnlocked: Boolean = false,
    val progress: Double = 1.0
)

data class AchievementsResponse(
    val streaks: Int = 7,
    val achievements: List<AchievementItem> = emptyList(),
    @SerializedName("motivational_quote") val motivationalQuote: String = "Discipline is the bridge between goals and accomplishment."
)

data class MonthlyWrappedStorySlide(
    val title: String,
    val subtitle: String,
    val value: String,
    val emoji: String,
    val detail: String
)

data class MonthlyWrappedResponse(
    val month: String,
    val archetype: String,
    val slides: List<MonthlyWrappedStorySlide> = emptyList()
)
