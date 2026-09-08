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
    val pacing_status: String, // "comfortable", "on_track", "burning_fast"
    val headline: String,
    val tip: String
)

data class AIChatMessage(
    val role: String, // "user" or "assistant"
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
