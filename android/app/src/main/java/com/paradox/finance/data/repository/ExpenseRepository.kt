package com.paradox.finance.data.repository

import android.content.Context
import com.paradox.finance.data.local.ParadoxDatabase
import com.paradox.finance.data.local.entity.CategoryEntity
import com.paradox.finance.data.local.entity.ExpenseEntity
import com.paradox.finance.data.model.CreateExpenseRequest
import com.paradox.finance.data.model.Expense
import com.paradox.finance.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*

class ExpenseRepository(context: Context) {
    private val database = ParadoxDatabase.getDatabase(context)
    private val expenseDao = database.expenseDao()
    private val categoryDao = database.categoryDao()
    private val apiService = ApiClient.getService(context)

    val expensesFlow: Flow<List<Expense>> = expenseDao.getAllExpensesFlow().map { entities ->
        entities.map { it.toExpense() }
    }

    suspend fun refreshExpenses(): Result<List<Expense>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getExpenses(limit = 100)
            if (response.isSuccessful && response.body() != null) {
                val expenses = response.body()!!
                val entities = expenses.map { ExpenseEntity.fromExpense(it, isSynced = true) }
                expenseDao.insertExpenses(entities)
                syncPendingExpenses()
                Result.success(expenses)
            } else {
                Result.failure(Exception("Failed to fetch remote expenses: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExpense(
        amount: Double,
        description: String,
        date: String,
        categoryId: String,
        categoryName: String?,
        paymentMethodId: String,
        paymentMethodName: String?
    ): Result<Expense> = withContext(Dispatchers.IO) {
        val tempId = UUID.randomUUID().toString()
        val localEntity = ExpenseEntity(
            id = tempId,
            amount = amount,
            description = description,
            date = date,
            categoryId = categoryId,
            categoryName = categoryName,
            paymentMethodId = paymentMethodId,
            paymentMethodName = paymentMethodName,
            isSynced = false
        )

        // Save immediately to Room DB for 0ms UI responsiveness
        expenseDao.insertExpense(localEntity)

        // Attempt remote sync
        try {
            val res = apiService.createExpense(
                CreateExpenseRequest(
                    amount = amount,
                    description = description,
                    date = date,
                    categoryId = categoryId,
                    paymentMethodId = paymentMethodId
                )
            )
            if (res.isSuccessful && res.body() != null) {
                val syncedExpense = res.body()!!
                expenseDao.deleteExpenseById(tempId)
                expenseDao.insertExpense(ExpenseEntity.fromExpense(syncedExpense, isSynced = true))
                Result.success(syncedExpense)
            } else {
                Result.success(localEntity.toExpense())
            }
        } catch (e: Exception) {
            // Offline - preserved in Room DB with isSynced = false
            Result.success(localEntity.toExpense())
        }
    }

    suspend fun syncPendingExpenses() = withContext(Dispatchers.IO) {
        try {
            val pending = expenseDao.getUnsyncedExpenses()
            for (expense in pending) {
                val res = apiService.createExpense(
                    CreateExpenseRequest(
                        amount = expense.amount,
                        description = expense.description,
                        date = expense.date,
                        categoryId = expense.categoryId,
                        paymentMethodId = expense.paymentMethodId
                    )
                )
                if (res.isSuccessful && res.body() != null) {
                    val synced = res.body()!!
                    expenseDao.deleteExpenseById(expense.id)
                    expenseDao.insertExpense(ExpenseEntity.fromExpense(synced, isSynced = true))
                }
            }
        } catch (e: Exception) {
            // Keep pending
        }
    }
}
