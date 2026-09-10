package com.paradox.finance.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object Register : NavRoutes("register")
    object Dashboard : NavRoutes("dashboard")
    object Expenses : NavRoutes("expenses")
    object Analytics : NavRoutes("analytics")
    object Budget : NavRoutes("budget")
    object Subscriptions : NavRoutes("subscriptions")
    object SavingsGoals : NavRoutes("savings_goals")
    object FinnyChat : NavRoutes("finny_chat")
}
