package com.paradox.finance.data.remote.dto

import com.google.gson.annotations.SerializedName

// Login & Register Requests
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String? = null
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp_code") val otpCode: String,
    @SerializedName("password") val password: String,
    @SerializedName("full_name") val fullName: String? = null
)

data class GoogleLoginRequest(
    @SerializedName("id_token") val idToken: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

// Auth Responses
data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("token_type") val tokenType: String = "bearer"
)

data class UserResponse(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("is_verified") val isVerified: Boolean = true
)

data class RegisterStatusResponse(
    @SerializedName("is_verified") val isVerified: Boolean,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String? = null
)

data class UpdateProfileRequest(
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("full_name") val fullName: String? = null
)
