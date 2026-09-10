package com.paradox.finance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paradox.finance.data.models.ExpenseDto

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val description: String,
    val date: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val paymentMethodId: String?,
    val paymentMethodName: String?,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val createdAt: String? = null
) {
    fun toDto() = ExpenseDto(
        id = id,
        rawAmount = amount,
        description = description,
        date = date,
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        isRecurring = isRecurring,
        recurringFrequency = recurringFrequency,
        createdAt = createdAt
    )

    companion object {
        fun fromDto(dto: ExpenseDto) = ExpenseEntity(
            id = dto.id,
            amount = dto.amount,
            description = dto.displayDescription,
            date = dto.displayDate,
            categoryId = dto.categoryId,
            categoryName = dto.category?.name,
            categoryIcon = dto.category?.icon,
            paymentMethodId = dto.paymentMethodId,
            paymentMethodName = dto.paymentMethod?.name,
            isRecurring = dto.isRecurring,
            recurringFrequency = dto.recurringFrequency,
            createdAt = dto.createdAt
        )
    }
}
