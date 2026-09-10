package com.paradox.finance.data.api

import com.paradox.finance.data.models.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ParadoxApi {

    // Auth
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/register/initiate")
    suspend fun initiateRegistration(@Body request: InitiateRegisterRequest): Response<GenericMessageResponse>

    @POST("api/v1/auth/register/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    @GET("api/v1/auth/register/status")
    suspend fun checkRegisterStatus(@Query("email") email: String): Response<RegisterStatusResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericMessageResponse>

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericMessageResponse>

    @GET("api/v1/auth/me")
    suspend fun getProfile(): Response<UserDto>

    @PATCH("api/v1/auth/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDto>

    @POST("api/v1/auth/switch-account")
    suspend fun switchAccount(@Body request: SwitchAccountRequest): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<GenericMessageResponse>

    // Expenses
    @GET("api/v1/expenses")
    suspend fun getExpenses(
        @Query("category_id") categoryId: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100
    ): Response<PaginatedEnvelope<ExpenseDto>>

    @POST("api/v1/expenses")
    suspend fun createExpense(@Body request: CreateExpenseRequest): Response<DataEnvelope<ExpenseDto>>

    @GET("api/v1/expenses/{id}")
    suspend fun getExpense(@Path("id") id: String): Response<DataEnvelope<ExpenseDto>>

    @PATCH("api/v1/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: String,
        @Body request: UpdateExpenseRequest
    ): Response<DataEnvelope<ExpenseDto>>

    @DELETE("api/v1/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>

    @GET("api/v1/expenses/recurring")
    suspend fun getRecurringSummary(): Response<DataEnvelope<RecurringSummaryResponse>>

    @GET("api/v1/expenses/export")
    @Streaming
    suspend fun exportExpenses(): Response<ResponseBody>

    @Multipart
    @POST("api/v1/expenses/import")
    suspend fun importExpensesCsv(@Part file: MultipartBody.Part): Response<ImportCsvResponse>

    // Categories
    @GET("api/v1/categories")
    suspend fun getCategories(): Response<DataEnvelope<List<CategoryDto>>>

    @POST("api/v1/categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): Response<DataEnvelope<CategoryDto>>

    @DELETE("api/v1/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    // Payment Methods
    @GET("api/v1/payment-methods")
    suspend fun getPaymentMethods(): Response<DataEnvelope<List<PaymentMethodDto>>>

    @POST("api/v1/payment-methods")
    suspend fun createPaymentMethod(@Body request: CreatePaymentMethodRequest): Response<DataEnvelope<PaymentMethodDto>>

    // Budget
    @GET("api/v1/budget")
    suspend fun getBudgetStatus(
        @Query("period_type") periodType: String,
        @Query("period_key") periodKey: String
    ): Response<DataEnvelope<BudgetStatusResponse>>

    @PUT("api/v1/budget")
    suspend fun setBudget(@Body request: SetBudgetRequest): Response<DataEnvelope<BudgetDto>>

    // Dashboard
    @GET("api/v1/dashboard")
    suspend fun getDashboardSummary(): Response<DataEnvelope<DashboardSummaryResponse>>

    // AI Intelligence Suite
    @POST("api/v1/ai/categorize")
    suspend fun aiCategorize(@Body request: AiCategorizeRequest): Response<DataEnvelope<AiCategorizeResponse>>

    @POST("api/v1/ai/parse-expense")
    suspend fun aiParseExpense(@Body request: AiParseExpenseRequest): Response<DataEnvelope<AiParseExpenseResponse>>

    @POST("api/v1/ai/parse-sms")
    suspend fun aiParseSms(@Body request: AiParseSmsRequest): Response<DataEnvelope<AiParseExpenseResponse>>

    @POST("api/v1/ai/scan-receipt")
    suspend fun aiScanReceipt(@Body request: AiScanReceiptRequest): Response<DataEnvelope<AiScanReceiptResponse>>

    @POST("api/v1/ai/simulate-purchase")
    suspend fun aiSimulatePurchase(@Body request: SimulatePurchaseRequest): Response<DataEnvelope<SimulatePurchaseResponse>>

    @GET("api/v1/ai/safe-to-spend")
    suspend fun aiSafeToSpend(): Response<DataEnvelope<SafeToSpendResponse>>

    @GET("api/v1/ai/health-score")
    suspend fun aiHealthScore(): Response<DataEnvelope<HealthScoreResponse>>

    @GET("api/v1/ai/leak-analysis")
    suspend fun aiLeakAnalysis(@Query("threshold") threshold: Double = 150.0): Response<DataEnvelope<LeakAnalysisResponse>>

    @GET("api/v1/ai/fifty-thirty-twenty")
    suspend fun aiFiftyThirtyTwenty(): Response<DataEnvelope<FiftyThirtyTwentyResponse>>

    @POST("api/v1/ai/chat")
    suspend fun aiChat(@Body request: AiChatRequest): Response<DataEnvelope<AiChatResponse>>

    @GET("api/v1/ai/monthly-wrapped")
    suspend fun aiMonthlyWrapped(): Response<DataEnvelope<MonthlyWrappedResponse>>

    @GET("api/v1/ai/vibe-check")
    suspend fun aiVibeCheck(): Response<DataEnvelope<FinnyVibeCheckResponse>>
}
