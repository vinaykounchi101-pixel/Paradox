package com.paradox.finance.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paradox.finance.core.Constants
import com.paradox.finance.data.local.AppDatabase
import com.paradox.finance.data.preferences.AuthPreferences
import com.paradox.finance.data.repository.AuthRepository
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.hardware.biometrics.BiometricHelper
import com.paradox.finance.ui.screens.ai.FinnyChatScreen
import com.paradox.finance.ui.screens.ai.LeakHunterScreen
import com.paradox.finance.ui.screens.ai.MonthlyWrappedScreen
import com.paradox.finance.ui.screens.ai.PurchaseSimulatorScreen
import com.paradox.finance.ui.screens.analytics.AnalyticsScreen
import com.paradox.finance.ui.screens.auth.AuthViewModel
import com.paradox.finance.ui.screens.auth.LoginScreen
import com.paradox.finance.ui.screens.auth.RegisterOtpScreen
import com.paradox.finance.ui.screens.budget.BudgetScreen
import com.paradox.finance.ui.screens.dashboard.DashboardScreen
import com.paradox.finance.ui.screens.dashboard.DashboardViewModel
import com.paradox.finance.ui.screens.expenses.ExpenseListScreen
import com.paradox.finance.ui.screens.expenses.ExpenseViewModel
import com.paradox.finance.ui.screens.goals.SavingsGoalsScreen
import com.paradox.finance.ui.screens.settings.SettingsScreen
import com.paradox.finance.ui.screens.subscriptions.SubscriptionsScreen

@Composable
fun AppNavHost(
    activity: FragmentActivity,
    authPreferences: AuthPreferences,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    val authRepository = remember { AuthRepository(authPreferences) }
    val expenseRepository = remember { ExpenseRepository(database) }

    val authViewModel = remember { AuthViewModel(authRepository, authPreferences) }
    val dashboardViewModel = remember { DashboardViewModel(authPreferences, expenseRepository) }
    val expenseViewModel = remember { ExpenseViewModel(expenseRepository, authPreferences) }

    val startDestination = if (authRepository.isLoggedIn()) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    val currencySymbol = Constants.CURRENCY_SYMBOLS[authPreferences.getCurrency()] ?: "₹"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.RegisterOtp.route)
                },
                onLoginSuccess = {
                    dashboardViewModel.refreshDashboard()
                    expenseViewModel.loadData()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBiometricClick = {
                    if (BiometricHelper.isBiometricAvailable(activity)) {
                        BiometricHelper.authenticate(
                            activity = activity,
                            onSuccess = {
                                authViewModel.onBiometricSuccess()
                                dashboardViewModel.refreshDashboard()
                                expenseViewModel.loadData()
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onError = { /* Handled */ }
                        )
                    }
                }
            )
        }

        composable(Screen.RegisterOtp.route) {
            RegisterOtpScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    dashboardViewModel.refreshDashboard()
                    expenseViewModel.loadData()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            LaunchedEffect(Unit) {
                dashboardViewModel.refreshDashboard()
                expenseViewModel.loadData()
            }
            DashboardScreen(
                viewModel = dashboardViewModel,
                expenseViewModel = expenseViewModel,
                onNavigateToExpenses = {
                    navController.navigate(Screen.Expenses.route)
                },
                onNavigateToAiChat = {
                    navController.navigate(Screen.AiCopilot.route)
                },
                onNavigateToLeakHunter = {
                    navController.navigate(Screen.LeakHunter.route)
                },
                onNavigateToSimulator = {
                    navController.navigate(Screen.PurchaseSimulator.route)
                },
                onNavigateToWrapped = {
                    navController.navigate(Screen.MonthlyWrapped.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToBudget = {
                    navController.navigate(Screen.Budget.route)
                },
                onNavigateToSubscriptions = {
                    navController.navigate(Screen.Subscriptions.route)
                },
                onNavigateToGoals = {
                    navController.navigate(Screen.SavingsGoals.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                currencySymbol = currencySymbol,
                expenseRepository = expenseRepository,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToExpenses = {
                    navController.navigate(Screen.Expenses.route)
                }
            )
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                currencySymbol = currencySymbol,
                expenseRepository = expenseRepository,
                onNavigateBack = {
                    dashboardViewModel.refreshDashboard()
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SavingsGoals.route) {
            SavingsGoalsScreen(
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Expenses.route) {
            ExpenseListScreen(
                viewModel = expenseViewModel,
                onNavigateBack = {
                    dashboardViewModel.refreshDashboard()
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AiCopilot.route) {
            FinnyChatScreen(
                expenseViewModel = expenseViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToSimulator = {
                    navController.navigate(Screen.PurchaseSimulator.route)
                }
            )
        }

        composable(Screen.LeakHunter.route) {
            LeakHunterScreen(
                currencySymbol = currencySymbol,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PurchaseSimulator.route) {
            val dashState by dashboardViewModel.uiState.collectAsState()
            PurchaseSimulatorScreen(
                currencySymbol = currencySymbol,
                currentBuffer = dashState.remainingBudget,
                dailyAllowance = dashState.safeToSpendDaily,
                expenseViewModel = expenseViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToAiChat = {
                    navController.navigate(Screen.AiCopilot.route)
                },
                onAddExpense = { amount, desc ->
                    expenseViewModel.openAddDialog()
                    expenseViewModel.onAmountChanged(amount.toString())
                    expenseViewModel.onDescriptionChanged(desc)
                    navController.navigate(Screen.Expenses.route)
                }
            )
        }

        composable(Screen.MonthlyWrapped.route) {
            MonthlyWrappedScreen(
                currencySymbol = currencySymbol,
                onClose = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                authPreferences = authPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
