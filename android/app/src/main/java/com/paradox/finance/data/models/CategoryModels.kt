package com.paradox.finance.data.models

import com.google.gson.annotations.SerializedName

data class CategoryDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String? = "General",
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("budget_limit") val budgetLimit: Double? = null,
    @SerializedName("created_at") val createdAt: String? = null
) {
    val displayName: String get() = name ?: "General"
}

data class CreateCategoryRequest(
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String? = "category",
    @SerializedName("color") val color: String? = "#4edea3",
    @SerializedName("budget_limit") val budgetLimit: Double? = null
)

data class PaymentMethodDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String? = "Cash",
    @SerializedName("type") val type: String? = "upi",
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("user_id") val userId: String? = null
) {
    val displayName: String get() = name ?: "Cash"
}

data class CreatePaymentMethodRequest(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String? = "upi",
    @SerializedName("icon") val icon: String? = null
)
