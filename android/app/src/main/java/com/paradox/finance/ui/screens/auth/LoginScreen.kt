package com.paradox.finance.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.dialogs.ForgotPasswordDialog
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AuthState
import com.paradox.finance.ui.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("vinay@paradox.com") }
    var password by remember { mutableStateOf("Password123!") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo & Title
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(ElectricEmerald, CyanContainer))
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PitchBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "P", color = ElectricEmerald, fontSize = 28.sp, style = Typography.displayLarge)
                }
            }

            Text(
                text = "PARADOX",
                color = OnSurfaceHigh,
                fontSize = 24.sp,
                style = Typography.headlineLarge,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = "Autonomous Financial Intelligence",
                color = MutedOutline,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Auth Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GlassSurface1)
                    .border(1.dp, GlassBorderStroke, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = OnSurfaceVariant, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorderStroke,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = OnSurfaceVariant, fontSize = 11.sp) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle",
                                    tint = MutedOutline
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlassBorderStroke,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = LuminousCyan,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { showForgotPassword = true }
                        )
                    }

                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = AlertCoral,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { authViewModel.login(email, password) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricEmerald),
                        shape = RoundedCornerShape(9999.dp),
                        enabled = authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(color = PitchBlack, modifier = Modifier.size(20.dp))
                        } else {
                            Text(text = "Authenticate & Unlock", color = PitchBlack, fontSize = 13.sp, style = Typography.labelLarge)
                        }
                    }
                }
            }

            // Biometric Option
            IconButton(
                onClick = { authViewModel.login(email, password) },
                modifier = Modifier
                    .padding(top = 20.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(GlassSurface2)
                    .border(1.dp, VibrantIndigo.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Biometrics", tint = SoftIndigo, modifier = Modifier.size(28.dp))
            }

            // Navigation to Register
            Row(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = "Don't have an account?", color = OnSurfaceVariant, fontSize = 12.sp)
                Text(
                    text = "Sign Up / Register",
                    color = ElectricEmerald,
                    fontSize = 12.sp,
                    style = Typography.labelLarge,
                    modifier = Modifier.clickable(onClick = onNavigateToRegister)
                )
            }
        }
    }

    if (showForgotPassword) {
        ForgotPasswordDialog(
            onSubmit = { /* Dispatched in repo */ },
            onDismiss = { showForgotPassword = false }
        )
    }
}
