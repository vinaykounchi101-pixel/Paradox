package com.paradox.finance.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paradox.finance.ui.screens.ai.FinnyChatScreen
import com.paradox.finance.ui.screens.analytics.AnalyticsScreen
import com.paradox.finance.ui.screens.auth.LoginScreen
import com.paradox.finance.ui.screens.auth.RegisterScreen
import com.paradox.finance.ui.screens.budget.BudgetScreen
import com.paradox.finance.ui.screens.dashboard.DashboardScreen
import com.paradox.finance.ui.screens.expenses.ExpensesLedgerScreen
import com.paradox.finance.ui.screens.savings.SavingsGoalsScreen
import com.paradox.finance.ui.screens.subscriptions.SubscriptionsScreen
import com.paradox.finance.ui.viewmodels.*

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    expenseViewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel,
    aiViewModel: AiViewModel,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (authViewModel.authState.value is AuthState.Authenticated) {
        NavRoutes.Dashboard.route
    } else {
        NavRoutes.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(NavRoutes.Register.route) },
                onLoginSuccess = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.navigate(NavRoutes.Login.route) },
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(NavRoutes.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Dashboard.route) {
            DashboardScreen(
                dashboardViewModel = dashboardViewModel,
                expenseViewModel = expenseViewModel,
                aiViewModel = aiViewModel,
                authViewModel = authViewModel,
                onNavigateToExpenses = { navController.navigate(NavRoutes.Expenses.route) },
                onNavigateToAnalytics = { navController.navigate(NavRoutes.Analytics.route) },
                onNavigateToBudget = { navController.navigate(NavRoutes.Budget.route) },
                onNavigateToFinnyChat = { navController.navigate(NavRoutes.FinnyChat.route) },
                onSignOut = {
                    authViewModel.logout()
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Expenses.route) {
            ExpensesLedgerScreen(
                expenseViewModel = expenseViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Analytics.route) {
            AnalyticsScreen(
                expenseViewModel = expenseViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Budget.route) {
            BudgetScreen(
                budgetViewModel = budgetViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Subscriptions.route) {
            SubscriptionsScreen(
                budgetViewModel = budgetViewModel,
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SavingsGoals.route) {
            SavingsGoalsScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.FinnyChat.route) {
            FinnyChatScreen(
                aiViewModel = aiViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = { navController.navigate(NavRoutes.Dashboard.route) }
            )
        }
    }
}
