package com.paradox.finance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paradox.finance.data.local.dao.BudgetDao
import com.paradox.finance.data.local.dao.CategoryDao
import com.paradox.finance.data.local.dao.ExpenseDao
import com.paradox.finance.data.local.entity.BudgetEntity
import com.paradox.finance.data.local.entity.CategoryEntity
import com.paradox.finance.data.local.entity.ExpenseEntity

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paradox_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
