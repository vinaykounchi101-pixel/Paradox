package com.paradox.finance.data.remote

import com.paradox.finance.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ParadoxApi {

    // ==========================================
    // 🔐 AUTHENTICATION ENDPOINTS
    // ==========================================

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<TokenResponse>

    @POST("api/v1/auth/google")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<TokenResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<Map<String, Any>>

    @POST("api/v1/auth/register/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<TokenResponse>

    @GET("api/v1/auth/register/status")
    suspend fun checkRegisterStatus(
        @Query("email") email: String
    ): Response<RegisterStatusResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest? = null
    ): Response<TokenResponse>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("api/v1/auth/me")
    suspend fun getProfile(): Response<UserResponse>

    @PATCH("api/v1/auth/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<UserResponse>

    // ==========================================
    // 💰 EXPENSES ENDPOINTS
    // ==========================================

    @GET("api/v1/expenses")
    suspend fun getExpenses(
        @Query("page_size") pageSize: Int = 100,
        @Query("page") page: Int = 1,
        @Query("category_id") categoryId: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/expenses/export")
    suspend fun exportExpenses(
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ResponseBody>

    @Multipart
    @POST("api/v1/expenses/import")
    suspend fun importExpensesCsv(
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>

    @POST("api/v1/expenses")
    suspend fun createExpense(
        @Body expense: Map<String, Any?>
    ): Response<Map<String, Any>>

    @PATCH("api/v1/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") expenseId: String,
        @Body expense: Map<String, Any?>
    ): Response<Map<String, Any>>

    @DELETE("api/v1/expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") expenseId: String
    ): Response<Map<String, String>>

    // ==========================================
    // 📂 CATEGORIES & PAYMENT METHODS
    // ==========================================

    @GET("api/v1/categories")
    suspend fun getCategories(): Response<Map<String, Any>>

    @POST("api/v1/categories")
    suspend fun createCategory(
        @Body category: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("api/v1/payment-methods")
    suspend fun getPaymentMethods(): Response<Map<String, Any>>

    // ==========================================
    // 📊 BUDGETS
    // ==========================================

    @GET("api/v1/budget")
    suspend fun getBudget(
        @Query("period_type") periodType: String,
        @Query("period_key") periodKey: String
    ): Response<Map<String, Any>>

    @PUT("api/v1/budget")
    suspend fun upsertBudget(
        @Body budget: Map<String, Any>
    ): Response<Map<String, Any>>

    // ==========================================
    // 🧠 AI FINANCIAL INTELLIGENCE & SUPERPOWERS
    // ==========================================

    @POST("api/v1/ai/categorize")
    suspend fun categorize(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("api/v1/ai/parse-expense")
    suspend fun parseExpense(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("api/v1/ai/parse-sms")
    suspend fun parseSms(
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @Multipart
    @POST("api/v1/ai/scan-receipt")
    suspend fun scanReceipt(
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>

    @GET("api/v1/ai/safe-to-spend")
    suspend fun getSafeToSpend(
        @Query("period_key") periodKey: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/ai/suggest-budget")
    suspend fun suggestBudget(
        @Query("period_type") periodType: String = "month"
    ): Response<Map<String, Any>>

    @GET("api/v1/ai/health-score")
    suspend fun getHealthScore(): Response<Map<String, Any>>

    @POST("api/v1/ai/chat")
    suspend fun chatWithFinny(
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("api/v1/ai/fifty-thirty-twenty")
    suspend fun getFiftyThirtyTwenty(
        @Query("month_key") monthKey: String? = null
    ): Response<Map<String, Any>>

    @GET("api/v1/ai/achievements")
    suspend fun getAchievements(): Response<Map<String, Any>>

    @GET("api/v1/dashboard")
    suspend fun getDashboard(
        @Query("period") period: String = "current_month"
    ): Response<Map<String, Any>>

    @GET("api/v1/expenses/recurring")
    suspend fun getRecurringExpenses(): Response<Map<String, Any>>

    @GET("api/v1/ai/monthly-wrapped")
    suspend fun getMonthlyWrapped(
        @Query("month_key") monthKey: String? = null
    ): Response<Map<String, Any>>

    @POST("api/v1/ai/savings-plan")
    suspend fun calculateSavingsPlan(
        @Body request: Map<String, Any>
    ): Response<Map<String, Any>>

    @GET("api/v1/ai/subscription-audit")
    suspend fun getSubscriptionAudit(): Response<Map<String, Any>>

    @GET("api/v1/ai/forecast")
    suspend fun getForecast(): Response<Map<String, Any>>

    @GET("api/v1/ai/anomalies")
    suspend fun getAnomalies(): Response<Map<String, Any>>
}

