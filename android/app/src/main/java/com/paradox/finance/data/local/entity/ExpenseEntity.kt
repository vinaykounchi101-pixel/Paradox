package com.paradox.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paradox.finance.data.model.Category
import com.paradox.finance.data.model.Expense
import com.paradox.finance.data.model.PaymentMethod

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val description: String,
    val date: String,
    val categoryId: String,
    val categoryName: String?,
    val paymentMethodId: String,
    val paymentMethodName: String?,
    val isRecurring: Boolean = false,
    val isSynced: Boolean = true
) {
    fun toExpense(): Expense {
        return Expense(
            id = id,
            amount = amount,
            description = description,
            date = date,
            categoryId = categoryId,
            paymentMethodId = paymentMethodId,
            category = categoryName?.let { Category(id = categoryId, name = it) },
            paymentMethod = paymentMethodName?.let { PaymentMethod(id = paymentMethodId, name = it) },
            isRecurring = isRecurring
        )
    }

    companion object {
        fun fromExpense(expense: Expense, isSynced: Boolean = true): ExpenseEntity {
            return ExpenseEntity(
                id = expense.id,
                amount = expense.amount,
                description = expense.description,
                date = expense.date,
                categoryId = expense.categoryId,
                categoryName = expense.category?.name,
                paymentMethodId = expense.paymentMethodId,
                paymentMethodName = expense.paymentMethod?.name,
                isRecurring = expense.isRecurring,
                isSynced = isSynced
            )
        }
    }
}
