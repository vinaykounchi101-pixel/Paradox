package com.paradox.finance.data.remote

import com.paradox.finance.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ParadoxApiService {

    // Health
    @GET("api/v1/health")
    suspend fun checkHealth(): Response<Map<String, String>>

    // Auth & Multi-Tenancy
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun directRegister(@Body request: DirectRegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/register/initiate")
    suspend fun initiateRegistration(@Body request: InitiateRegistrationRequest): Response<MessageResponse>

    @POST("api/v1/auth/register/verify-otp")
    suspend fun verifyOtpRegister(@Body request: VerifyOtpRegisterRequest): Response<AuthResponse>

    @GET("api/v1/auth/register/status")
    suspend fun checkRegistrationStatus(@Query("email") email: String): Response<RegistrationStatusResponse>

    @POST("api/v1/auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @GET("api/v1/auth/me")
    suspend fun getProfile(): Response<User>

    @PATCH("api/v1/auth/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @POST("api/v1/auth/switch-account")
    suspend fun switchAccount(@Body request: SwitchAccountRequest): Response<AuthResponse>

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

    // AI & Financial Intelligence
    @POST("api/v1/ai/categorize")
    suspend fun categorizeExpense(@Body request: CategorizeRequest): Response<CategorizeResponse>

    @POST("api/v1/ai/parse-expense")
    suspend fun parseExpense(@Body request: ParseExpenseRequest): Response<ParseExpenseResponse>

    @POST("api/v1/ai/scan-receipt")
    suspend fun scanReceipt(@Body request: ScanReceiptRequest): Response<ScanReceiptResponse>

    @POST("api/v1/ai/parse-sms")
    suspend fun parseSms(@Body request: ParseSmsRequest): Response<ParseSmsResponse>

    @GET("api/v1/ai/safe-to-spend")
    suspend fun getSafeToSpend(): Response<SafeToSpendResponse>

    @POST("api/v1/ai/simulate-purchase")
    suspend fun simulatePurchase(@Body request: SimulatePurchaseRequest): Response<SimulatePurchaseResponse>

    @GET("api/v1/ai/leak-analysis")
    suspend fun getLeakAnalysis(@Query("threshold") threshold: Double = 150.0): Response<LeakAnalysisResponse>

    @GET("api/v1/ai/forecast")
    suspend fun getSpendingForecast(): Response<SpendingForecastResponse>

    @GET("api/v1/ai/fifty-thirty-twenty")
    suspend fun getFiftyThirtyTwenty(): Response<FiftyThirtyTwentyResponse>

    @GET("api/v1/ai/achievements")
    suspend fun getAchievements(): Response<AchievementsResponse>

    @GET("api/v1/ai/monthly-wrapped")
    suspend fun getMonthlyWrapped(): Response<MonthlyWrappedResponse>

    @POST("api/v1/ai/chat")
    suspend fun sendChatMessage(@Body request: AIChatRequest): Response<AIChatResponse>
}
