package com.paradox.finance.ui.navigation

import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.paradox.finance.data.preferences.AuthPreferences
import com.paradox.finance.data.repository.AuthRepository
import com.paradox.finance.hardware.biometrics.BiometricHelper
import com.paradox.finance.ui.screens.auth.AuthViewModel
import com.paradox.finance.ui.screens.auth.LoginScreen
import com.paradox.finance.ui.screens.auth.RegisterOtpScreen

@Composable
fun AppNavHost(
    activity: FragmentActivity,
    authPreferences: AuthPreferences,
    navController: NavHostController = rememberNavController()
) {
    val authRepository = remember { AuthRepository(authPreferences) }
    val authViewModel = remember { AuthViewModel(authRepository, authPreferences) }

    val startDestination = if (authRepository.isLoggedIn()) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

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
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onError = { /* handled by prompt */ }
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
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            // Dashboard placeholder until Phase 5
            com.paradox.finance.SplashPreview()
        }
    }
}
