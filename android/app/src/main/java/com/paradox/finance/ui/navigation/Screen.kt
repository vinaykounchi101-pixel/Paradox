package com.paradox.finance.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object RegisterOtp : Screen("register_otp")
    object Dashboard : Screen("dashboard")
    object Expenses : Screen("expenses")
    object AiCopilot : Screen("ai_copilot")
    object Settings : Screen("settings")
}
