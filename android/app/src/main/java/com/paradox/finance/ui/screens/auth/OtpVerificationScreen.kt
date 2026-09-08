package com.paradox.finance.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.paradox.finance.data.model.VerifyOtpRequest
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.theme.*
import com.paradox.finance.ui.components.ParadoxCard
import com.paradox.finance.ui.components.ParadoxGradientButton
import com.paradox.finance.ui.components.ParadoxTextField
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
    var isLoading by remember { mutableStateOf(false) }

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
            text = "Enter the 6-digit code sent to\n$email",
            style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc400),
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        ParadoxCard {
            ParadoxTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                label = "6-Digit OTP Code",
                placeholder = "123456"
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
                    scope.launch {
                        isLoading = true
                        try {
                            val response = apiService.verifyOtp(VerifyOtpRequest(email, otpCode))
                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                tokenManager.saveAuthSession(
                                    token = body.accessToken,
                                    email = body.user.email,
                                    name = body.user.name,
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
