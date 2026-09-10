package com.paradox.finance.data.repository

import com.paradox.finance.data.api.ApiClient
import com.paradox.finance.data.local.AppDatabase
import com.paradox.finance.data.local.BudgetEntity
import com.paradox.finance.data.local.CategoryEntity
import com.paradox.finance.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepository(private val database: AppDatabase) {

    private val api get() = ApiClient.api
    private val categoryDao = database.categoryDao()
    private val budgetDao = database.budgetDao()

    val localCategories: Flow<List<CategoryDto>> = categoryDao.getAllCategories().map { list ->
        list.map { it.toDto() }
    }

    suspend fun refreshCategories(): Result<List<CategoryDto>> {
        return try {
            val response = api.getCategories()
            if (response.isSuccessful && response.body()?.data != null) {
                val items = response.body()!!.data
                categoryDao.insertCategories(items.map { CategoryEntity.fromDto(it) })
                Result.success(items)
            } else {
                Result.failure(Exception("Failed to fetch categories"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCategory(request: CreateCategoryRequest): Result<CategoryDto> {
        return try {
            val response = api.createCategory(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val created = response.body()!!.data
                categoryDao.insertCategory(CategoryEntity.fromDto(created))
                Result.success(created)
            } else {
                Result.failure(Exception("Failed to create category"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBudgetStatus(periodType: String, periodKey: String): Result<BudgetStatusResponse> {
        return try {
            val response = api.getBudgetStatus(periodType, periodKey)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch budget status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setBudget(amount: Double, periodType: String, periodKey: String): Result<BudgetDto> {
        return try {
            val response = api.setBudget(SetBudgetRequest(amount, periodType, periodKey))
            if (response.isSuccessful && response.body()?.data != null) {
                val created = response.body()!!.data
                budgetDao.insertBudget(BudgetEntity.fromDto(created))
                Result.success(created)
            } else {
                Result.failure(Exception("Failed to set budget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDashboardSummary(): Result<DashboardSummaryResponse> {
        return try {
            val response = api.getDashboardSummary()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch dashboard summary"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
