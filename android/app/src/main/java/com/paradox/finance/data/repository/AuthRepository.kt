package com.paradox.finance.data.repository

import com.paradox.finance.core.Resource
import com.paradox.finance.data.preferences.AuthPreferences
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.data.remote.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val authPrefs: AuthPreferences) {

    private val api = ApiClient.getApi()

    suspend fun login(email: String, password: String): Resource<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequest(email = email.trim(), password = password))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                authPrefs.saveTokens(token.accessToken, token.refreshToken)
                fetchAndSaveProfile()
                Resource.Success(token)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Invalid credentials", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun loginWithGoogle(idToken: String): Resource<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.googleLogin(GoogleLoginRequest(idToken = idToken))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                authPrefs.saveTokens(token.accessToken, token.refreshToken)
                fetchAndSaveProfile()
                Resource.Success(token)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Google login failed", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun register(email: String, password: String, fullName: String? = null): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(RegisterRequest(email = email.trim(), password = password, fullName = fullName?.trim()))
            if (response.isSuccessful) {
                Resource.Success("OTP verification code sent to your email!")
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Registration failed", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun verifyOtp(email: String, otpCode: String, password: String, fullName: String? = null): Resource<TokenResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.verifyOtp(VerifyOtpRequest(email = email.trim(), otpCode = otpCode.trim(), password = password, fullName = fullName?.trim()))
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                authPrefs.saveTokens(token.accessToken, token.refreshToken)
                fetchAndSaveProfile()
                Resource.Success(token)
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Invalid OTP code", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun checkRegisterStatus(email: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = api.checkRegisterStatus(email.trim())
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.isVerified)
            } else {
                Resource.Error("Registration pending")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    suspend fun forgotPassword(email: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.forgotPassword(mapOf("email" to email.trim()))
            if (response.isSuccessful) {
                Resource.Success("Password reset instructions have been sent to your email.")
            } else {
                Resource.Error(response.errorBody()?.string() ?: "Failed to send reset link", response.code())
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network connection error")
        }
    }

    suspend fun fetchAndSaveProfile(): Resource<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                authPrefs.saveUser(user.email, user.fullName, user.currency)
                Resource.Success(user)
            } else {
                Resource.Error("Failed to load profile")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    fun isLoggedIn(): Boolean = !authPrefs.getAccessToken().isNullOrBlank()

    fun logout() {
        authPrefs.clear()
    }
}
