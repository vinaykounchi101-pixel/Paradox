package com.paradox.finance.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paradox.finance.data.models.CategoryDto

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String? = "category",
    val color: String? = "#4edea3",
    val isDefault: Boolean = false,
    val budgetLimit: Double? = null
) {
    fun toDto() = CategoryDto(
        id = id,
        name = name,
        icon = icon,
        color = color,
        isDefault = isDefault,
        budgetLimit = budgetLimit
    )

    companion object {
        fun fromDto(dto: CategoryDto) = CategoryEntity(
            id = dto.id,
            name = dto.displayName,
            icon = dto.icon,
            color = dto.color,
            isDefault = dto.isDefault,
            budgetLimit = dto.budgetLimit
        )
    }
}
