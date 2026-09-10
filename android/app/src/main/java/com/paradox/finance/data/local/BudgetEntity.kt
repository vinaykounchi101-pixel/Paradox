package com.paradox.finance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paradox.finance.data.models.BudgetDto

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val periodType: String,
    val periodKey: String,
    val createdAt: String? = null
) {
    fun toDto() = BudgetDto(
        id = id,
        amount = amount,
        periodType = periodType,
        periodKey = periodKey,
        createdAt = createdAt
    )

    companion object {
        fun fromDto(dto: BudgetDto) = BudgetEntity(
            id = dto.id,
            amount = dto.amount,
            periodType = dto.periodType,
            periodKey = dto.periodKey,
            createdAt = dto.createdAt
        )
    }
}
