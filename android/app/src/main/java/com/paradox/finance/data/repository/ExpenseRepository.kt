package com.paradox.finance.data.repository

import com.paradox.finance.core.Resource
import com.paradox.finance.data.local.AppDatabase
import com.paradox.finance.data.local.entity.CategoryEntity
import com.paradox.finance.data.local.entity.ExpenseEntity
import com.paradox.finance.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ExpenseRepository(private val database: AppDatabase) {

    private val api = ApiClient.getApi()
    private val expenseDao = database.expenseDao()
    private val categoryDao = database.categoryDao()

    fun getLocalExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()
    fun getLocalCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun syncExpenses(): Resource<List<ExpenseEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getExpenses(pageSize = 100)
            if (response.isSuccessful && response.body() != null) {
                val rawData = response.body()?.get("data") as? List<Map<*, *>> ?: emptyList()
                val list = rawData.map { item ->
                    val cat = item["category"] as? Map<*, *>
                    val pm = item["payment_method"] as? Map<*, *>
                    ExpenseEntity(
                        id = item["id"]?.toString() ?: UUID.randomUUID().toString(),
                        amount = (item["amount"] as? Number)?.toDouble() ?: item["amount"]?.toString()?.toDoubleOrNull() ?: 0.0,
                        date = item["date"]?.toString() ?: "",
                        description = item["description"]?.toString() ?: "",
                        categoryId = cat?.get("id")?.toString(),
                        categoryName = cat?.get("name")?.toString(),
                        categoryColor = cat?.get("color")?.toString(),
                        paymentMethodId = pm?.get("id")?.toString(),
                        paymentMethodName = pm?.get("name")?.toString(),
                        isRecurring = item["is_recurring"] as? Boolean ?: false,
                        recurringFrequency = item["recurring_frequency"]?.toString(),
                        isSynced = true
                    )
                }
                expenseDao.insertExpenses(list)
                Resource.Success(list)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to sync expenses", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Offline: Showing cached data")
        }
    }

    suspend fun createExpense(
        amount: Double,
        date: String,
        description: String,
        categoryId: String,
        paymentMethodId: String,
        isRecurring: Boolean = false,
        recurringFrequency: String? = null
    ): Resource<ExpenseEntity> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf(
                "amount" to amount,
                "date" to date,
                "description" to description,
                "category_id" to categoryId,
                "payment_method_id" to paymentMethodId,
                "is_recurring" to isRecurring,
                "recurring_frequency" to recurringFrequency
            )
            val response = api.createExpense(body)
            if (response.isSuccessful && response.body() != null) {
                val item = (response.body()?.get("data") as? Map<*, *>) ?: response.body()!!
                val entity = ExpenseEntity(
                    id = item["id"]?.toString() ?: UUID.randomUUID().toString(),
                    amount = amount,
                    date = date,
                    description = description,
                    categoryId = categoryId,
                    categoryName = null,
                    categoryColor = null,
                    paymentMethodId = paymentMethodId,
                    paymentMethodName = null,
                    isRecurring = isRecurring,
                    recurringFrequency = recurringFrequency,
                    isSynced = true
                )
                expenseDao.insertExpense(entity)
                syncExpenses()
                Resource.Success(entity)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to save expense", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun updateExpense(
        id: String,
        amount: Double,
        date: String,
        description: String,
        categoryId: String,
        paymentMethodId: String,
        isRecurring: Boolean = false,
        recurringFrequency: String? = null
    ): Resource<ExpenseEntity> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf(
                "amount" to amount,
                "date" to date,
                "description" to description,
                "category_id" to categoryId,
                "payment_method_id" to paymentMethodId,
                "is_recurring" to isRecurring,
                "recurring_frequency" to recurringFrequency
            )
            val response = api.updateExpense(id, body)
            if (response.isSuccessful && response.body() != null) {
                val item = (response.body()?.get("data") as? Map<*, *>) ?: response.body()!!
                val cat = item["category"] as? Map<*, *>
                val pm = item["payment_method"] as? Map<*, *>
                val entity = ExpenseEntity(
                    id = id,
                    amount = amount,
                    date = date,
                    description = description,
                    categoryId = categoryId,
                    categoryName = cat?.get("name")?.toString(),
                    categoryColor = cat?.get("color")?.toString(),
                    paymentMethodId = paymentMethodId,
                    paymentMethodName = pm?.get("name")?.toString(),
                    isRecurring = isRecurring,
                    recurringFrequency = recurringFrequency,
                    isSynced = true
                )
                expenseDao.insertExpense(entity)
                syncExpenses()
                Resource.Success(entity)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to update expense", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun deleteExpense(id: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteExpense(id)
            if (response.isSuccessful) {
                expenseDao.deleteExpenseById(id)
                Resource.Success(true)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to delete expense", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun syncCategories(): Resource<List<CategoryEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getCategories()
            if (response.isSuccessful && response.body() != null) {
                val rawData = response.body()?.get("data") as? List<Map<*, *>> ?: emptyList()
                val list = rawData.map { item ->
                    CategoryEntity(
                        id = item["id"]?.toString() ?: UUID.randomUUID().toString(),
                        name = item["name"]?.toString() ?: "",
                        color = item["color"]?.toString() ?: "#6366F1",
                        icon = item["icon"]?.toString(),
                        isDefault = item["is_default"] as? Boolean ?: false
                    )
                }
                categoryDao.insertCategories(list)
                Resource.Success(list)
            } else {
                Resource.Error("Failed to fetch categories")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Offline mode")
        }
    }

    suspend fun createCategory(name: String, color: String = "#6366F1", icon: String = "tag"): Resource<CategoryEntity> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf("name" to name.trim(), "color" to color, "icon" to icon)
            val response = api.createCategory(body)
            if (response.isSuccessful && response.body() != null) {
                val item = (response.body()?.get("data") as? Map<*, *>) ?: response.body()!!
                val entity = CategoryEntity(
                    id = item["id"]?.toString() ?: UUID.randomUUID().toString(),
                    name = name,
                    color = color,
                    icon = icon,
                    isDefault = false
                )
                categoryDao.insertCategories(listOf(entity))
                Resource.Success(entity)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to create category")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun getPaymentMethods(): Resource<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPaymentMethods()
            if (response.isSuccessful && response.body() != null) {
                val rawData = (response.body()?.get("data") as? List<Map<String, Any>>) ?: emptyList()
                Resource.Success(rawData)
            } else {
                Resource.Error("Failed to fetch payment methods")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }
}
