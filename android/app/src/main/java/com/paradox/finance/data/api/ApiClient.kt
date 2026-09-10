package com.paradox.finance.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Production Render API with local fallback
    private const val BASE_URL = "https://paradox-2t3x.onrender.com/"

    private var retrofit: Retrofit? = null
    private var apiService: ParadoxApi? = null

    fun initialize(tokenManager: TokenManager) {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            android.util.Log.d("PARADOX_API", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(tokenManager)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit?.create(ParadoxApi::class.java)
    }

    val api: ParadoxApi
        get() = apiService ?: throw IllegalStateException("ApiClient must be initialized before use.")
}
