package com.paradox.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val date: String,
    val description: String,
    val categoryId: String?,
    val categoryName: String?,
    val categoryColor: String?,
    val paymentMethodId: String?,
    val paymentMethodName: String?,
    val isRecurring: Boolean = false,
    val recurringFrequency: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val icon: String? = null,
    val isDefault: Boolean = false
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val periodType: String,
    val periodKey: String,
    val targetAmount: Double,
    val totalSpent: Double,
    val remainingAmount: Double,
    val percentageUsed: Double
)
