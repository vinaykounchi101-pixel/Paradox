package com.paradox.finance.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.local.TokenManager
import com.paradox.finance.data.model.LoginRequest
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.theme.*
import com.paradox.finance.ui.components.ParadoxCard
import com.paradox.finance.ui.components.ParadoxGradientButton
import com.paradox.finance.ui.components.ParadoxTextField
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiClient.getService(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParadoxZinc950)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Title
        Text(
            text = "⚡ Paradox",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ParadoxIndigo
            )
        )
        Text(
            text = "AI-Powered Wealth & Expense Intelligence",
            style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400),
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // Login Card
        ParadoxCard {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ParadoxTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "you@example.com"
            )

            Spacer(modifier = Modifier.height(16.dp))

            ParadoxTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "••••••••"
            )

            Spacer(modifier = Modifier.height(24.dp))

            ParadoxGradientButton(
                text = "Sign In",
                isLoading = isLoading,
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                        return@ParadoxGradientButton
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            val response = apiService.login(LoginRequest(email.trim(), password))
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                tokenManager.saveAuthSession(
                                    token = body.accessToken,
                                    email = body.user.email,
                                    name = body.user.name,
                                    currency = body.user.currency
                                )
                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Login failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400)
            )
            Text(
                text = "Register",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ParadoxIndigo,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}
