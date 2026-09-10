package com.paradox.finance.data.models

import com.google.gson.annotations.SerializedName

data class ExpenseDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("amount") val rawAmount: Any? = null,
    @SerializedName("description") val description: String? = "Expense",
    @SerializedName("date") val date: String? = "",
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("payment_method_id") val paymentMethodId: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("is_recurring") val isRecurring: Boolean = false,
    @SerializedName("recurring_frequency") val recurringFrequency: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("category") val category: CategoryDto? = null,
    @SerializedName("payment_method") val paymentMethod: PaymentMethodDto? = null
) {
    val amount: Double
        get() = when (rawAmount) {
            is Number -> rawAmount.toDouble()
            is String -> rawAmount.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    val displayDescription: String get() = description ?: "Expense"
    val displayDate: String get() = date ?: ""
}

data class CreateExpenseRequest(
    @SerializedName("amount") val amount: Double,
    @SerializedName("description") val description: String,
    @SerializedName("date") val date: String,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("payment_method_id") val paymentMethodId: String? = null,
    @SerializedName("is_recurring") val isRecurring: Boolean = false,
    @SerializedName("recurring_frequency") val recurringFrequency: String? = null
)

data class UpdateExpenseRequest(
    @SerializedName("amount") val amount: Double? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("payment_method_id") val paymentMethodId: String? = null,
    @SerializedName("is_recurring") val isRecurring: Boolean? = null,
    @SerializedName("recurring_frequency") val recurringFrequency: String? = null
)

data class ExpenseListResponse(
    @SerializedName("items") val items: List<ExpenseDto> = emptyList(),
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 50
)

data class RecurringCommitmentDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String? = "Subscription",
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("frequency") val frequency: String = "monthly",
    @SerializedName("normalized_monthly_amount") val normalizedMonthlyAmount: Double = 0.0,
    @SerializedName("category_name") val categoryName: String? = null,
    @SerializedName("category_icon") val categoryIcon: String? = null
) {
    val displayName: String get() = name ?: "Subscription"
}

data class RecurringSummaryResponse(
    @SerializedName("commitments") val commitments: List<RecurringCommitmentDto> = emptyList(),
    @SerializedName("total_monthly_commitment") val totalMonthlyCommitment: Double = 0.0,
    @SerializedName("active_count") val activeCount: Int = 0
)

data class ImportCsvResponse(
    @SerializedName("message") val message: String = "",
    @SerializedName("imported_count") val importedCount: Int = 0,
    @SerializedName("skipped_count") val skippedCount: Int = 0
)
