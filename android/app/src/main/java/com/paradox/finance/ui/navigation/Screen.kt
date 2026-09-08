package com.paradox.finance.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object OtpVerification : Screen("otp_verify/{email}") {
        fun createRoute(email: String) = "otp_verify/$email"
    }
    object Main : Screen("main")
    object Dashboard : Screen("dashboard")
    object Expenses : Screen("expenses")
    object AiChat : Screen("ai_chat")
}
