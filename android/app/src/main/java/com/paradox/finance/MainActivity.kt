package com.paradox.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.paradox.finance.data.local.TokenManager
import com.paradox.finance.data.repository.ExpenseRepository
import com.paradox.finance.ui.navigation.ParadoxNavGraph
import com.paradox.finance.ui.navigation.Screen
import com.paradox.finance.ui.theme.BackgroundDark
import com.paradox.finance.ui.theme.ParadoxTheme
import com.paradox.finance.ui.theme.PrimaryCyan
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var repository: ExpenseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tokenManager = TokenManager(applicationContext)
        repository = ExpenseRepository(applicationContext)

        setContent {
            ParadoxTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val token = tokenManager.accessTokenFlow.first()
                    startDestination = if (!token.isNullOrEmpty()) {
                        Screen.Main.route
                    } else {
                        Screen.Login.route
                    }
                }

                if (startDestination == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDark),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryCyan)
                    }
                } else {
                    val navController = rememberNavController()
                    ParadoxNavGraph(
                        navController = navController,
                        startDestination = startDestination!!,
                        tokenManager = tokenManager,
                        repository = repository
                    )
                }
            }
        }
    }
}
