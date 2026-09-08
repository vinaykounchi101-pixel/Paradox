package com.paradox.finance.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paradox.finance.data.local.TokenManager
import com.paradox.finance.ui.screens.auth.LoginScreen
import com.paradox.finance.ui.screens.auth.OtpVerificationScreen
import com.paradox.finance.ui.screens.auth.RegisterScreen
import com.paradox.finance.ui.screens.dashboard.DashboardScreen
import com.paradox.finance.ui.screens.expenses.AddExpenseBottomSheet
import com.paradox.finance.ui.screens.expenses.ExpenseListScreen
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun ParadoxNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val tokenManager = remember { TokenManager(context) }

    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = tokenManager.accessTokenFlow.firstOrNull()
        startDestination = if (!token.isNullOrBlank()) Screen.Dashboard.route else Screen.Login.route
    }

    if (startDestination == null) return

    NavHost(
        navController = navController,
        startDestination = startDestination!!
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToOtp = { email ->
                    navController.navigate(Screen.OtpVerification.createRoute(email))
                }
            )
        }

        composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            OtpVerificationScreen(
                email = email,
                onVerificationSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToExpenses = { navController.navigate(Screen.Expenses.route) },
                onOpenAddExpense = { showAddExpenseSheet = true },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )

            if (showAddExpenseSheet) {
                AddExpenseBottomSheet(
                    onDismiss = { showAddExpenseSheet = false },
                    onExpenseAdded = {
                        showAddExpenseSheet = false
                    }
                )
            }
        }

        composable(Screen.Expenses.route) {
            ExpenseListScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
