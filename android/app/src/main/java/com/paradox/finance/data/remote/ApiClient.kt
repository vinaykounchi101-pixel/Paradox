package com.paradox.finance.data.remote

import android.content.Context
import com.paradox.finance.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Production Render Backend URL (matches web)
    private const val BASE_URL = "https://paradox-2t3x.onrender.com/"

    @Volatile
    private var apiService: ParadoxApiService? = null

    fun getService(context: Context): ParadoxApiService {
        return apiService ?: synchronized(this) {
            val tokenManager = TokenManager(context.applicationContext)
            val authInterceptor = AuthInterceptor(tokenManager)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(ParadoxApiService::class.java).also {
                apiService = it
            }
        }
    }
}
