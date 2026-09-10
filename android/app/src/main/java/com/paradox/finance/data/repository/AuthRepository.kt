package com.paradox.finance.data.repository

import com.paradox.finance.data.api.ApiClient
import com.paradox.finance.data.api.TokenManager
import com.paradox.finance.data.models.*

class AuthRepository(private val tokenManager: TokenManager) {

    private val api get() = ApiClient.api

    private fun extractErrorMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            val json = org.json.JSONObject(errorBody)
            if (json.has("error")) {
                val errObj = json.optJSONObject("error")
                val msg = errObj?.optString("message")
                if (!msg.isNullOrBlank()) return msg
            }
            if (json.has("detail")) {
                val detail = json.opt("detail")
                if (detail is org.json.JSONArray && detail.length() > 0) {
                    val firstItem = detail.optJSONObject(0)
                    val msg = firstItem?.optString("msg") ?: firstItem?.optString("message")
                    if (!msg.isNullOrBlank()) return msg
                }
                return detail.toString()
            }
            if (json.has("message")) {
                val msg = json.optString("message")
                if (!msg.isNullOrBlank()) return msg
            }
            fallback
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun login(email: String, pass: String): Result<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email.trim(), pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveUserId(body.user.id)
                tokenManager.saveCurrency(body.user.currency)
                Result.success(body)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Invalid email or password")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, pass: String, name: String?): Result<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(email.trim(), pass, name?.ifBlank { "User" } ?: "User"))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveUserId(body.user.id)
                tokenManager.saveCurrency(body.user.currency)
                Result.success(body)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Registration failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun initiateRegistration(email: String): Result<GenericMessageResponse> {
        return try {
            val response = api.initiateRegistration(InitiateRegisterRequest(email.trim()))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "Initiate registration failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(email: String, otp: String, pass: String, name: String?): Result<AuthResponse> {
        return try {
            val response = api.verifyOtp(VerifyOtpRequest(email.trim(), otp.trim(), pass, name?.ifBlank { "User" } ?: "User"))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveToken(body.accessToken)
                tokenManager.saveUserId(body.user.id)
                tokenManager.saveCurrency(body.user.currency)
                Result.success(body)
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string(), "OTP verification failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkRegisterStatus(email: String): Result<RegisterStatusResponse> {
        return try {
            val response = api.checkRegisterStatus(email.trim())
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.isVerified && body.accessToken != null && body.user != null) {
                    tokenManager.saveToken(body.accessToken)
                    tokenManager.saveUserId(body.user.id)
                    tokenManager.saveCurrency(body.user.currency)
                }
                Result.success(body)
            } else {
                Result.failure(Exception("Status check failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forgotPassword(email: String): Result<GenericMessageResponse> {
        return try {
            val response = api.forgotPassword(ForgotPasswordRequest(email.trim()))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Password reset request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCurrency(currency: String): Result<UserDto> {
        return try {
            val response = api.updateProfile(UpdateProfileRequest(currency = currency))
            if (response.isSuccessful && response.body() != null) {
                tokenManager.saveCurrency(currency)
                Result.success(response.body()!!)
            } else {
                tokenManager.saveCurrency(currency)
                Result.failure(Exception("Currency sync failed"))
            }
        } catch (e: Exception) {
            tokenManager.saveCurrency(currency)
            Result.failure(e)
        }
    }

    fun isLoggedIn(): Boolean {
        return !tokenManager.getToken().isNullOrBlank()
    }

    fun logout() {
        tokenManager.clear()
    }
}
