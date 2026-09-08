package com.paradox.finance.data.remote

import android.content.Context
import com.google.gson.GsonBuilder
import com.paradox.finance.core.Constants
import com.paradox.finance.data.preferences.AuthPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var retrofit: Retrofit? = null
    private var authPreferences: AuthPreferences? = null

    fun initialize(context: Context) {
        authPreferences = AuthPreferences(context)
    }

    private class AuthHeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val token = authPreferences?.getAccessToken() 
                ?: runCatching { AuthPreferences(com.paradox.finance.ParadoxApplication.instance).getAccessToken() }.getOrNull()

            val requestBuilder = original.newBuilder()
            if (!token.isNullOrBlank() && !original.url.encodedPath.contains("/auth/login") && !original.url.encodedPath.contains("/auth/register")) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            return chain.proceed(requestBuilder.build())
        }
    }

    private fun getOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor())
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getApi(): ParadoxApi {
        if (retrofit == null) {
            val gson = GsonBuilder()
                .setLenient()
                .create()

            retrofit = Retrofit.Builder()
                .baseUrl(Constants.DEFAULT_BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit!!.create(ParadoxApi::class.java)
    }

    val apiService: ParadoxApi
        get() = getApi()
}

