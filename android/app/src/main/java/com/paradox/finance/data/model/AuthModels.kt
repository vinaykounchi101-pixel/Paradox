package com.paradox.finance.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    val name: String? = null,
    val currency: String = "INR"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterInitiateRequest(
    val email: String,
    val password: String
)

data class VerifyOtpRequest(
    val email: String,
    val otp_code: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    val user: User
)

data class MessageResponse(
    val message: String
)
