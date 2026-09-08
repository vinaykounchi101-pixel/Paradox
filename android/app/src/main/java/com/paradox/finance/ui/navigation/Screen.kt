package com.paradox.finance.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object RegisterOtp : Screen("register_otp")
    object Dashboard : Screen("dashboard")
    object Expenses : Screen("expenses")
    object AiCopilot : Screen("ai_copilot")
    object LeakHunter : Screen("leak_hunter")
    object PurchaseSimulator : Screen("purchase_simulator")
    object MonthlyWrapped : Screen("monthly_wrapped")
    object Analytics : Screen("analytics")
    object Budget : Screen("budget")
    object Subscriptions : Screen("subscriptions")
    object SavingsGoals : Screen("savings_goals")
    object Settings : Screen("settings")
}
