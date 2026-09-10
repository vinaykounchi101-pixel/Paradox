package com.paradox.finance.data.repository

import com.paradox.finance.data.api.ApiClient
import com.paradox.finance.data.local.AppDatabase
import com.paradox.finance.data.local.ExpenseEntity
import com.paradox.finance.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ExpenseRepository(private val database: AppDatabase) {

    private val api get() = ApiClient.api
    private val expenseDao = database.expenseDao()

    val localExpenses: Flow<List<ExpenseDto>> = expenseDao.getAllExpenses().map { list ->
        list.map { it.toDto() }
    }

    suspend fun refreshExpenses(): Result<List<ExpenseDto>> {
        return try {
            val response = api.getExpenses(pageSize = 100)
            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.data
                val entities = items.map { ExpenseEntity.fromDto(it) }
                expenseDao.insertExpenses(entities)
                Result.success(items)
            } else {
                Result.failure(Exception("Failed to fetch expenses"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createExpense(request: CreateExpenseRequest): Result<ExpenseDto> {
        return try {
            val response = api.createExpense(request)
            if (response.isSuccessful && response.body()?.data != null) {
                val created = response.body()!!.data
                expenseDao.insertExpense(ExpenseEntity.fromDto(created))
                Result.success(created)
            } else {
                Result.failure(Exception("Failed to create expense: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpense(id: String, request: UpdateExpenseRequest): Result<ExpenseDto> {
        return try {
            val response = api.updateExpense(id, request)
            if (response.isSuccessful && response.body()?.data != null) {
                val updated = response.body()!!.data
                expenseDao.insertExpense(ExpenseEntity.fromDto(updated))
                Result.success(updated)
            } else {
                Result.failure(Exception("Failed to update expense: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(id: String): Result<Unit> {
        return try {
            expenseDao.deleteExpenseById(id)
            val response = api.deleteExpense(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete expense on server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecurringSummary(): Result<RecurringSummaryResponse> {
        return try {
            val response = api.getRecurringSummary()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch recurring summary"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importCsv(file: File): Result<ImportCsvResponse> {
        return try {
            val reqFile = file.asRequestBody("text/csv".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, reqFile)
            val response = api.importExpensesCsv(body)
            if (response.isSuccessful && response.body() != null) {
                refreshExpenses()
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Import failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
