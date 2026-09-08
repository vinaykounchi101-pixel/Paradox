package com.paradox.finance.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.paradox.finance.data.model.DirectRegisterRequest
import com.paradox.finance.data.model.InitiateRegistrationRequest
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.theme.*
import com.paradox.finance.ui.components.ParadoxCard
import com.paradox.finance.ui.components.ParadoxGradientButton
import com.paradox.finance.ui.components.ParadoxTextField
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToOtp: (String) -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiClient.getService(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParadoxZinc950)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚡ Paradox",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = ParadoxIndigo
            )
        )
        Text(
            text = "AI-Powered Wealth Intelligence",
            style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400),
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        ParadoxCard {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ParadoxTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "you@example.com",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            ParadoxTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "At least 8 characters",
                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                ),
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "👁️" else "🙈",
                        modifier = Modifier
                            .clickable { passwordVisible = !passwordVisible }
                            .padding(8.dp),
                        fontSize = 16.sp
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Direct Instant Registration Button
            ParadoxGradientButton(
                text = "Sign Up",
                isLoading = isLoading,
                onClick = {
                    if (email.isBlank() || password.length < 8) {
                        Toast.makeText(context, "Please enter valid email and 8+ char password", Toast.LENGTH_SHORT).show()
                        return@ParadoxGradientButton
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            val response = apiService.directRegister(
                                DirectRegisterRequest(email = email.trim(), password = password)
                            )
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                tokenManager.saveAuthSession(
                                    token = body.accessToken,
                                    email = body.user.email,
                                    name = body.user.displayName ?: body.user.email,
                                    currency = body.user.currency
                                )
                                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                onRegistrationSuccess()
                            } else {
                                Toast.makeText(context, "Registration failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Email OTP Option
            Button(
                onClick = {
                    if (email.isBlank()) {
                        Toast.makeText(context, "Enter your email first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        isSendingOtp = true
                        try {
                            val res = apiService.initiateRegistration(InitiateRegistrationRequest(email.trim()))
                            if (res.isSuccessful) {
                                Toast.makeText(context, "OTP code sent to $email", Toast.LENGTH_LONG).show()
                                onNavigateToOtp(email.trim())
                            } else {
                                Toast.makeText(context, "Could not send OTP", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSendingOtp = false
                        }
                    }
                },
                enabled = !isSendingOtp && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ParadoxZinc800)
            ) {
                if (isSendingOtp) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ParadoxIndigoLight)
                } else {
                    Text(text = "✉️ Send 6-Digit Email OTP", style = MaterialTheme.typography.labelMedium.copy(color = ParadoxZinc200))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google OAuth Registration Button
            Button(
                onClick = {
                    Toast.makeText(context, "⚡ Connecting to Google Secure Sign-Up...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ParadoxZinc800)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌐 ", fontSize = 16.sp)
                    Text(
                        text = "Sign Up with Google",
                        style = MaterialTheme.typography.labelLarge.copy(color = ParadoxZinc100)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400)
            )
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ParadoxIndigo,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}
