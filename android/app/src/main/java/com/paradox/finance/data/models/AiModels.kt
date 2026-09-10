package com.paradox.finance.data.models

import com.google.gson.annotations.SerializedName

data class AiCategorizeRequest(
    @SerializedName("description") val description: String,
    @SerializedName("amount") val amount: Double? = null
)

data class AiCategorizeResponse(
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("category_name") val categoryName: String? = "General",
    @SerializedName("confidence") val confidence: Double = 0.9,
    @SerializedName("reasoning") val reasoning: String? = null,
    @SerializedName("is_new_category") val isNewCategory: Boolean = false
) {
    val displayCategoryName: String get() = categoryName ?: "General"
}

data class AiParseExpenseRequest(
    @SerializedName("text") val text: String
)

data class AiParseExpenseResponse(
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("payment_method_name") val paymentMethodName: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("confidence") val confidence: Double = 0.9
)

data class AiParseSmsRequest(
    @SerializedName("text") val text: String = "",
    @SerializedName("sms_text") val smsText: String = ""
)

data class AiScanReceiptRequest(
    @SerializedName("text") val text: String = "",
    @SerializedName("image_base64") val imageBase64: String = "",
    @SerializedName("mime_type") val mimeType: String = "image/jpeg"
)

data class AiScanReceiptResponse(
    @SerializedName("total_amount") val totalAmount: Double? = null,
    @SerializedName("amount") val rawAmount: Double? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    @SerializedName("merchant") val rawMerchant: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("payment_method_name") val paymentMethodName: String? = null,
    @SerializedName("confidence") val confidence: Double = 0.9
) {
    val amount: Double? get() = totalAmount ?: rawAmount
    val merchant: String? get() = merchantName ?: rawMerchant
}

data class SimulatePurchaseRequest(
    @SerializedName("amount") val amount: Double,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("description") val description: String? = null
)

data class SimulatePurchaseResponse(
    @SerializedName("verdict") val verdict: String? = "safe",
    @SerializedName("headline") val headline: String? = "Affordable",
    @SerializedName("advice") val advice: String? = "Optimal purchase",
    @SerializedName("current_remaining_budget") val currentRemainingBudget: Double? = null,
    @SerializedName("projected_remaining_budget") val projectedRemainingBudget: Double? = null,
    @SerializedName("safe_to_spend_daily_before") val safeDailyBefore: Double? = null,
    @SerializedName("safe_to_spend_daily_after") val safeDailyAfter: Double? = null,
    @SerializedName("can_proceed") val canProceed: Boolean? = true,
    @SerializedName("is_affordable") val isAffordableRaw: Boolean? = null,
    @SerializedName("current_buffer") val currentBufferRaw: Double? = null,
    @SerializedName("projected_buffer") val projectedBufferRaw: Double? = null
) {
    val displayVerdict: String get() = headline ?: verdict ?: "Affordable"
    val displayAdvice: String get() = advice ?: "Safe purchase according to your velocity."
    val isAffordable: Boolean get() = canProceed ?: isAffordableRaw ?: true
    val currentBuffer: Double get() = currentRemainingBudget ?: currentBufferRaw ?: 0.0
    val projectedBuffer: Double get() = projectedRemainingBudget ?: projectedBufferRaw ?: 0.0
    val currentDailyAllowance: Double get() = safeDailyBefore ?: 0.0
    val projectedDailyAllowance: Double get() = safeDailyAfter ?: 0.0
}

data class SafeToSpendResponse(
    @SerializedName("safe_daily_allowance") val safeDailyAllowance: Double? = null,
    @SerializedName("safe_daily_spend") val rawSafeDailySpend: Double? = null,
    @SerializedName("status") val status: String? = "optimal",
    @SerializedName("pacing_status") val rawPacingStatus: String? = null,
    @SerializedName("days_remaining") val daysRemaining: Int? = null,
    @SerializedName("days_left") val rawDaysLeft: Int? = null,
    @SerializedName("remaining_budget") val remainingBudget: Double? = null,
    @SerializedName("remaining_limit") val rawRemainingLimit: Double? = null,
    @SerializedName("burn_status_message") val burnStatusMessage: String? = null,
    @SerializedName("message") val rawMessage: String? = null
) {
    val safeDailySpend: Double get() = safeDailyAllowance ?: rawSafeDailySpend ?: 0.0
    val pacingStatus: String get() = status ?: rawPacingStatus ?: "Optimal"
    val daysLeft: Int get() = daysRemaining ?: rawDaysLeft ?: 0
    val displayPacingStatus: String get() = pacingStatus
    val displayMessage: String get() = burnStatusMessage ?: rawMessage ?: "Safe pacing"
}

data class HealthScorePillarDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("score") val score: Int = 0,
    @SerializedName("max_score") val maxScore: Int = 40,
    @SerializedName("feedback") val feedback: String = ""
)

data class HealthScoreResponse(
    @SerializedName("score") val score: Int = 85,
    @SerializedName("status") val status: String? = "good",
    @SerializedName("headline") val headline: String? = "Optimal financial health.",
    @SerializedName("grade") val grade: String? = "A",
    @SerializedName("pillars") val pillars: List<HealthScorePillarDto> = emptyList(),
    @SerializedName("recommendations") val recommendations: List<String> = emptyList()
) {
    val displayGrade: String get() = grade ?: if (score >= 80) "A" else if (score >= 60) "B" else "C"
    val displaySummary: String get() = headline ?: "Optimal financial health."
}

data class LeakItemDto(
    @SerializedName("merchant_or_pattern") val merchantOrPattern: String? = null,
    @SerializedName("name") val rawName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("category") val rawCategory: String? = null,
    @SerializedName("monthly_drain") val monthlyDrain: Double? = null,
    @SerializedName("monthly_cost") val rawMonthlyCost: Double? = null,
    @SerializedName("annualized_drain") val annualizedDrain: Double? = null,
    @SerializedName("annual_drain") val rawAnnualDrain: Double? = null,
    @SerializedName("frequency_per_month") val frequencyPerMonth: Int? = null,
    @SerializedName("frequency_count") val rawFrequencyCount: Int? = null,
    @SerializedName("savings_tip") val savingsTip: String? = null,
    @SerializedName("plug_tip") val rawPlugTip: String? = null
) {
    val displayName: String get() = merchantOrPattern ?: rawName ?: "Micro-Spend Leak"
    val displayCategory: String get() = categoryName ?: rawCategory ?: "General"
    val monthlyCost: Double get() = monthlyDrain ?: rawMonthlyCost ?: 0.0
    val annualDrain: Double get() = annualizedDrain ?: rawAnnualDrain ?: (monthlyCost * 12)
    val frequencyCount: Int get() = frequencyPerMonth ?: rawFrequencyCount ?: 0
    val displayPlugTip: String get() = savingsTip ?: rawPlugTip ?: "Actionable savings tip available."
}

data class LeakAnalysisResponse(
    @SerializedName("total_monthly_leak") val totalMonthlyLeak: Double = 0.0,
    @SerializedName("total_annual_leak") val totalAnnualLeak: Double? = null,
    @SerializedName("projected_annual_drain") val rawProjectedAnnualDrain: Double? = null,
    @SerializedName("leak_streak_days") val leakStreakDays: Int = 14,
    @SerializedName("leaks") val leaks: List<LeakItemDto> = emptyList(),
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("actionable_savings_tip") val rawActionableSavingsTip: String? = null
) {
    val projectedAnnualDrain: Double get() = totalAnnualLeak ?: rawProjectedAnnualDrain ?: (totalMonthlyLeak * 12)
    val displayActionableTip: String get() = summary ?: rawActionableSavingsTip ?: "Audit micro transactions to boost savings velocity."
}

data class FiftyThirtyTwentyItemDto(
    @SerializedName("category_type") val categoryType: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("target_percentage") val targetPercentage: Double = 50.0,
    @SerializedName("actual_amount") val actualAmount: Double = 0.0,
    @SerializedName("actual_percentage") val actualPercentage: Double = 50.0,
    @SerializedName("variance_amount") val varianceAmount: Double = 0.0,
    @SerializedName("status") val status: String = "on_track"
)

data class FiftyThirtyTwentyResponse(
    @SerializedName("needs") val needsItem: FiftyThirtyTwentyItemDto? = null,
    @SerializedName("wants") val wantsItem: FiftyThirtyTwentyItemDto? = null,
    @SerializedName("savings") val savingsItem: FiftyThirtyTwentyItemDto? = null,
    @SerializedName("needs_percentage") val rawNeedsPercentage: Double? = null,
    @SerializedName("wants_percentage") val rawWantsPercentage: Double? = null,
    @SerializedName("savings_percentage") val rawSavingsPercentage: Double? = null,
    @SerializedName("adherence_score") val adherenceScore: Int = 85,
    @SerializedName("rebalance_advice") val rebalanceAdviceList: List<String>? = null,
    @SerializedName("rebalancing_advice") val rawRebalancingAdvice: String? = null
) {
    val needsPercentage: Double get() = needsItem?.actualPercentage ?: rawNeedsPercentage ?: 50.0
    val wantsPercentage: Double get() = wantsItem?.actualPercentage ?: rawWantsPercentage ?: 30.0
    val savingsPercentage: Double get() = savingsItem?.actualPercentage ?: rawSavingsPercentage ?: 20.0
    val displayAdvice: String get() = rebalanceAdviceList?.firstOrNull() ?: rawRebalancingAdvice ?: "Maintain balanced allocations."
}

data class AiChatMessage(
    @SerializedName("role") val role: String = "assistant",
    @SerializedName("content") val content: String? = "",
    @SerializedName("timestamp") val timestamp: String? = null
) {
    val displayContent: String get() = content ?: ""
}

data class AiChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("history") val history: List<AiChatMessage> = emptyList()
)

data class AiChatResponse(
    @SerializedName("reply") val reply: String? = "",
    @SerializedName("suggested_followups") val suggestedFollowups: List<String> = emptyList(),
    @SerializedName("suggested_actions") val rawSuggestedActions: List<String> = emptyList(),
    @SerializedName("provider_used") val providerUsed: String? = "heuristic"
) {
    val displayReply: String get() = reply ?: "I am here to help you optimize your spending."
    val suggestedActions: List<String> get() = if (suggestedFollowups.isNotEmpty()) suggestedFollowups else rawSuggestedActions
}

data class MonthlyWrappedSlideDto(
    @SerializedName("slide_number") val slideNumber: Int = 1,
    @SerializedName("title") val title: String? = "",
    @SerializedName("subtitle") val subtitle: String? = "",
    @SerializedName("highlight_metric") val highlightMetric: String? = "",
    @SerializedName("archetype") val archetype: String? = null,
    @SerializedName("description") val description: String? = ""
) {
    val displayTitle: String get() = title ?: ""
    val displaySubtitle: String get() = subtitle ?: ""
    val displayMetric: String get() = highlightMetric ?: ""
    val displayDescription: String get() = description ?: ""
}

data class MonthlyWrappedResponse(
    @SerializedName("month") val month: String? = null,
    @SerializedName("month_name") val rawMonthName: String? = null,
    @SerializedName("total_spent") val totalSpent: Double? = null,
    @SerializedName("total_spend") val rawTotalSpend: Double? = null,
    @SerializedName("archetype_title") val archetypeTitle: String? = null,
    @SerializedName("financial_archetype") val rawFinancialArchetype: String? = null,
    @SerializedName("slides") val slides: List<MonthlyWrappedSlideDto> = emptyList()
) {
    val displayMonthName: String get() = month ?: rawMonthName ?: "Current Month"
    val monthName: String? get() = displayMonthName
    val totalSpend: Double get() = totalSpent ?: rawTotalSpend ?: 0.0
    val topCategory: String get() = "General"
    val savingsStreak: Int get() = 0
    val displayArchetype: String get() = archetypeTitle ?: rawFinancialArchetype ?: "Strategic Saver"
    val financialArchetype: String get() = displayArchetype
}

data class FinnyVibeCheckResponse(
    @SerializedName("vibe_emoji") val vibeEmoji: String? = null,
    @SerializedName("emoji") val rawEmoji: String? = null,
    @SerializedName("vibe_title") val vibeTitle: String? = null,
    @SerializedName("roast_commentary") val roastCommentary: String? = null,
    @SerializedName("roast_or_praise") val rawRoastOrPraise: String? = null,
    @SerializedName("burn_rate_status") val burnRateStatus: String? = null,
    @SerializedName("mood") val rawMood: String? = null
) {
    val displayMood: String get() = burnRateStatus?.uppercase() ?: rawMood ?: "JOYFUL"
    val displayVibeTitle: String get() = vibeTitle ?: "On Track"
    val displayRoastOrPraise: String get() = roastCommentary ?: rawRoastOrPraise ?: "Great pacing!"
    val displayEmoji: String get() = vibeEmoji ?: rawEmoji ?: "⚡"
    val mood: String get() = displayMood
    val roastOrPraise: String get() = displayRoastOrPraise
}
