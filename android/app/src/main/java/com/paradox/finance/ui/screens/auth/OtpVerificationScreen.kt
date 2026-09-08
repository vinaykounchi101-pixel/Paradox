package com.paradox.finance.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.local.TokenManager
import com.paradox.finance.data.model.VerifyOtpRegisterRequest
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.theme.*
import com.paradox.finance.ui.components.ParadoxCard
import com.paradox.finance.ui.components.ParadoxGradientButton
import com.paradox.finance.ui.components.ParadoxTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun OtpVerificationScreen(
    email: String,
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiClient.getService(context) }

    var otpCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Real-Time Cross-Device Background Status Polling (Magic Link Sync)
    LaunchedEffect(email) {
        while (isActive) {
            delay(3000) // Poll every 3 seconds
            try {
                val statusRes = apiService.checkRegistrationStatus(email)
                if (statusRes.isSuccessful && statusRes.body()?.isVerified == true) {
                    Toast.makeText(context, "⚡ Magic link verified! Logging in...", Toast.LENGTH_LONG).show()
                    onVerificationSuccess()
                    break
                }
            } catch (e: Exception) {
                // Ignore transient network errors during background polling
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParadoxZinc950)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📬 Verify Email",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ParadoxZinc100
            )
        )
        Text(
            text = "Enter the 6-digit code or click the magic link sent to\n$email",
            style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400),
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Live Cross-device sync indicator
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ParadoxIndigo.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ParadoxIndigo.copy(alpha = 0.4f)),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = ParadoxIndigo,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live Sync: Magic link approval listener active",
                    style = MaterialTheme.typography.labelSmall.copy(color = ParadoxIndigoLight)
                )
            }
        }

        ParadoxCard {
            ParadoxTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                label = "6-Digit OTP Code",
                placeholder = "123456"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ParadoxTextField(
                value = password,
                onValueChange = { password = it },
                label = "Set Password",
                placeholder = "At least 8 characters",
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            ParadoxGradientButton(
                text = "Verify & Complete",
                isLoading = isLoading,
                onClick = {
                    if (otpCode.length != 6) {
                        Toast.makeText(context, "Please enter full 6-digit code", Toast.LENGTH_SHORT).show()
                        return@ParadoxGradientButton
                    }
                    if (password.length < 8) {
                        Toast.makeText(context, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                        return@ParadoxGradientButton
                    }
                    scope.launch {
                        isLoading = true
                        try {
                            val response = apiService.verifyOtpRegister(
                                VerifyOtpRegisterRequest(
                                    email = email.trim(),
                                    otp = otpCode.trim(),
                                    password = password
                                )
                            )
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                tokenManager.saveAuthSession(
                                    token = body.accessToken,
                                    email = body.user.email,
                                    name = body.user.displayName ?: body.user.email,
                                    currency = body.user.currency
                                )
                                Toast.makeText(context, "Account verified successfully!", Toast.LENGTH_SHORT).show()
                                onVerificationSuccess()
                            } else {
                                Toast.makeText(context, "Invalid OTP code", Toast.LENGTH_SHORT).show()
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
    }
}
