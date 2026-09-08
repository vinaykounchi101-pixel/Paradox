package com.paradox.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paradox.finance.data.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val isDefault: Boolean = false
) {
    fun toCategory(): Category {
        return Category(
            id = id,
            name = name,
            color = color,
            icon = icon,
            isDefault = isDefault
        )
    }

    companion object {
        fun fromCategory(category: Category): CategoryEntity {
            return CategoryEntity(
                id = category.id,
                name = category.name,
                color = category.color,
                icon = category.icon,
                isDefault = category.isDefault
            )
        }
    }
}
