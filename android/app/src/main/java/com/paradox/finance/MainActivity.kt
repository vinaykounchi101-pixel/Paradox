package com.paradox.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paradox.finance.data.repository.AiRepository
import com.paradox.finance.data.repository.AuthRepository
import com.paradox.finance.data.repository.BudgetRepository
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.ui.navigation.AppNavigation
import com.paradox.finance.ui.theme.ParadoxTheme
import com.paradox.finance.ui.viewmodels.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ParadoxApplication
        val authRepository = AuthRepository(app.tokenManager)
        val expenseRepository = ExpenseRepository(app.database)
        val budgetRepository = BudgetRepository(app.database)
        val aiRepository = AiRepository()

        setContent {
            ParadoxTheme {
                val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
                val dashboardViewModel: DashboardViewModel = viewModel { DashboardViewModel(budgetRepository, expenseRepository, aiRepository) }
                val expenseViewModel: ExpenseViewModel = viewModel { ExpenseViewModel(expenseRepository, budgetRepository) }
                val budgetViewModel: BudgetViewModel = viewModel { BudgetViewModel(budgetRepository, expenseRepository) }
                val aiViewModel: AiViewModel = viewModel { AiViewModel(aiRepository) }

                AppNavigation(
                    authViewModel = authViewModel,
                    dashboardViewModel = dashboardViewModel,
                    expenseViewModel = expenseViewModel,
                    budgetViewModel = budgetViewModel,
                    aiViewModel = aiViewModel
                )
            }
        }
    }
}
