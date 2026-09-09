package com.paradox.finance.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.core.Resource
import com.paradox.finance.data.preferences.AuthPreferences
import com.paradox.finance.data.remote.dto.TokenResponse
import com.paradox.finance.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val otpCode: String = "",
    val otpSent: Boolean = false,
    val otpTimer: Int = 60,
    val canResendOtp: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isBiometricAvailable: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val authPrefs: AuthPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var pollingJob: Job? = null

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        if (repository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(isSuccess = true)
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onFullNameChanged(fullName: String) {
        _uiState.value = _uiState.value.copy(fullName = fullName, errorMessage = null)
    }

    fun onOtpChanged(otp: String) {
        if (otp.length <= 6) {
            _uiState.value = _uiState.value.copy(otpCode = otp, errorMessage = null)
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.login(state.email, state.password)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.loginWithGoogle(idToken)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun sendRegisterOtp() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.register(state.email, state.password, state.fullName)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpSent = true,
                        otpTimer = 60,
                        canResendOtp = false
                    )
                    startOtpCountdown()
                    startBackgroundPolling(state.email)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.otpCode.length < 6) {
            _uiState.value = state.copy(errorMessage = "Please enter the complete 6-digit code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.verifyOtp(state.email, state.otpCode, state.password, state.fullName)) {
                is Resource.Success -> {
                    pollingJob?.cancel()
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    private fun startOtpCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in 60 downTo 1) {
                _uiState.value = _uiState.value.copy(otpTimer = i)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(canResendOtp = true, otpTimer = 0)
        }
    }

    private fun startBackgroundPolling(email: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                val status = repository.checkRegisterStatus(email)
                if (status is Resource.Success && status.data) {
                    // Auto-verified via magic link on email!
                    login()
                    break
                }
            }
        }
    }

    fun onBiometricSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = true)
    }

    fun forgotPassword(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "Please enter your email address")
            return
        }
        viewModelScope.launch {
            when (val res = repository.forgotPassword(email)) {
                is Resource.Success -> onResult(true, res.data ?: "Reset link sent")
                is Resource.Error -> onResult(false, res.message ?: "Failed to send reset link")
                else -> Unit
            }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthUiState(
            email = "",
            password = "",
            isSuccess = false,
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        pollingJob?.cancel()
    }
}

