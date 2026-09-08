package com.paradox.finance.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    @SerializedName("display_name") val displayName: String? = null,
    val avatar_url: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = true,
    val currency: String = "INR"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class DirectRegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("display_name") val displayName: String = "User"
)

data class InitiateRegistrationRequest(
    val email: String
)

data class VerifyOtpRegisterRequest(
    val email: String,
    val otp: String,
    val password: String,
    @SerializedName("display_name") val displayName: String = "User"
)

data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class UpdateProfileRequest(
    val currency: String? = null,
    @SerializedName("display_name") val displayName: String? = null
)

data class SwitchAccountRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class RegistrationStatusResponse(
    val email: String,
    val status: String,
    val message: String
) {
    val isVerified: Boolean
        get() = status == "verified" || status == "completed"
}

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    val user: User,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

data class MessageResponse(
    val message: String,
    val success: Boolean = true
)
