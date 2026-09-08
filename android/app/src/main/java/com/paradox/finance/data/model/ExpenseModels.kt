package com.paradox.finance.data.model

import com.google.gson.annotations.SerializedName

data class Category(
    val id: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false
)

data class PaymentMethod(
    val id: String,
    val name: String,
    val icon: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false
)

data class Expense(
    val id: String,
    val amount: Double,
    val description: String,
    val date: String,
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("payment_method_id") val paymentMethodId: String,
    val category: Category? = null,
    @SerializedName("payment_method") val paymentMethod: PaymentMethod? = null,
    @SerializedName("is_recurring") val isRecurring: Boolean = false,
    @SerializedName("recurring_frequency") val recurringFrequency: String? = null
)

data class CreateExpenseRequest(
    val amount: Double,
    val description: String,
    val date: String,
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("payment_method_id") val paymentMethodId: String,
    @SerializedName("is_recurring") val isRecurring: Boolean = false,
    @SerializedName("recurring_frequency") val recurringFrequency: String? = null
)

data class CreateCategoryRequest(
    val name: String,
    val color: String? = "#6366f1",
    val icon: String? = "sparkles"
)
