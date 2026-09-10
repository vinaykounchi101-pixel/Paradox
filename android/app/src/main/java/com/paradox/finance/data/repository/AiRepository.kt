package com.paradox.finance.data.repository

import com.paradox.finance.data.api.ApiClient
import com.paradox.finance.data.models.*

class AiRepository {

    private val api get() = ApiClient.api

    suspend fun categorize(description: String, amount: Double?): Result<AiCategorizeResponse> {
        return try {
            val response = api.aiCategorize(AiCategorizeRequest(description, amount))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("AI categorization failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseExpense(text: String): Result<AiParseExpenseResponse> {
        return try {
            val response = api.aiParseExpense(AiParseExpenseRequest(text))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Natural language parsing failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseSms(smsText: String): Result<AiParseExpenseResponse> {
        return try {
            val response = api.aiParseSms(AiParseSmsRequest(text = smsText, smsText = smsText))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("SMS parsing failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanReceipt(base64: String): Result<AiScanReceiptResponse> {
        return try {
            val response = api.aiScanReceipt(AiScanReceiptRequest(text = "", imageBase64 = base64))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Receipt OCR failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun simulatePurchase(amount: Double, categoryId: String?): Result<SimulatePurchaseResponse> {
        return try {
            val response = api.aiSimulatePurchase(SimulatePurchaseRequest(amount = amount, categoryId = categoryId))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Purchase simulation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSafeToSpend(): Result<SafeToSpendResponse> {
        return try {
            val response = api.aiSafeToSpend()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch safe to spend"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHealthScore(): Result<HealthScoreResponse> {
        return try {
            val response = api.aiHealthScore()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch health score"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeakAnalysis(threshold: Double = 150.0): Result<LeakAnalysisResponse> {
        return try {
            val response = api.aiLeakAnalysis(threshold)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch leak analysis"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFiftyThirtyTwenty(): Result<FiftyThirtyTwentyResponse> {
        return try {
            val response = api.aiFiftyThirtyTwenty()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch 50/30/20 breakdown"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(message: String, history: List<AiChatMessage>): Result<AiChatResponse> {
        return try {
            val response = api.aiChat(AiChatRequest(message, history))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("AI Chat failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMonthlyWrapped(): Result<MonthlyWrappedResponse> {
        return try {
            val response = api.aiMonthlyWrapped()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch monthly wrapped"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFinnyVibe(): Result<FinnyVibeCheckResponse> {
        return try {
            val response = api.aiVibeCheck()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data)
            } else {
                Result.failure(Exception("Failed to fetch Finny vibe"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
