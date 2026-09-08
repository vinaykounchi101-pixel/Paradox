package com.paradox.finance.data.remote

import com.paradox.finance.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ParadoxApiService {

    // Health
    @GET("api/v1/health")
    suspend fun checkHealth(): Response<Map<String, String>>

    // Auth
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterInitiateRequest): Response<MessageResponse>

    @POST("api/v1/auth/register/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    @GET("api/v1/auth/me")
    suspend fun getProfile(): Response<User>

    // Expenses
    @GET("api/v1/expenses")
    suspend fun getExpenses(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("category_id") categoryId: String? = null
    ): Response<List<Expense>>

    @POST("api/v1/expenses")
    suspend fun createExpense(@Body request: CreateExpenseRequest): Response<Expense>

    @DELETE("api/v1/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>

    // Categories & Payment Methods
    @GET("api/v1/categories")
    suspend fun getCategories(): Response<List<Category>>

    @POST("api/v1/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): Response<Category>

    @GET("api/v1/payment-methods")
    suspend fun getPaymentMethods(): Response<List<PaymentMethod>>

    // Dashboard & Budget
    @GET("api/v1/budget")
    suspend fun getBudgetStatus(
        @Query("period_type") periodType: String = "month",
        @Query("period_key") periodKey: String? = null
    ): Response<BudgetStatusResponse>

    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>

    // AI Features
    @POST("api/v1/ai/categorize")
    suspend fun categorizeExpense(@Body request: CategorizeRequest): Response<CategorizeResponse>

    @POST("api/v1/ai/parse-expense")
    suspend fun parseExpense(@Body request: ParseExpenseRequest): Response<ParseExpenseResponse>

    @GET("api/v1/ai/safe-to-spend")
    suspend fun getSafeToSpend(): Response<SafeToSpendResponse>

    @POST("api/v1/ai/chat")
    suspend fun sendChatMessage(@Body request: AIChatRequest): Response<AIChatResponse>
}
