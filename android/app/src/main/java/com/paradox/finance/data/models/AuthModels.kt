package com.paradox.finance.data.models

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val fullName: String? = "User",
    @SerializedName("currency") val currency: String = "INR",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("is_verified") val isVerified: Boolean = true,
    @SerializedName("created_at") val createdAt: String? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("display_name") val displayName: String? = "User"
)

data class InitiateRegisterRequest(
    @SerializedName("email") val email: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("password") val password: String,
    @SerializedName("display_name") val displayName: String? = "User"
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    @SerializedName("user") val user: UserDto,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

data class RegisterStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("email") val email: String,
    @SerializedName("message") val message: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("user") val user: UserDto? = null
)

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

data class ResetPasswordRequest(
    @SerializedName("token") val token: String,
    @SerializedName("new_password") val newPassword: String
)

data class UpdateProfileRequest(
    @SerializedName("display_name") val fullName: String? = null,
    @SerializedName("currency") val currency: String? = null
)

data class GenericMessageResponse(
    @SerializedName("message") val message: String,
    @SerializedName("success") val success: Boolean = true
)

data class SwitchAccountRequest(
    @SerializedName("refresh_token") val refreshToken: String
)
