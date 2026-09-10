package com.paradox.finance.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.data.models.UserDto
import com.paradox.finance.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserDto) : AuthState()
    data class OtpSent(val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(
        if (authRepository.isLoggedIn()) AuthState.Authenticated(UserDto(id = "local", email = "user@paradox.com"))
        else AuthState.Idle
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentCurrency = MutableStateFlow("INR")
    val currentCurrency: StateFlow<String> = _currentCurrency.asStateFlow()

    private var pollingJob: Job? = null

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, pass)
            result.onSuccess {
                _authState.value = AuthState.Authenticated(it.user)
                _currentCurrency.value = it.user.currency
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Authentication failed")
            }
        }
    }

    fun register(email: String, pass: String, name: String?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(email, pass, name)
            result.onSuccess {
                _authState.value = AuthState.Authenticated(it.user)
                _currentCurrency.value = it.user.currency
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Registration failed")
            }
        }
    }

    fun verifyOtp(email: String, otp: String, pass: String, name: String?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            stopPollingStatus()
            val result = authRepository.verifyOtp(email, otp, pass, name)
            result.onSuccess {
                _authState.value = AuthState.Authenticated(it.user)
                _currentCurrency.value = it.user.currency
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "OTP Verification failed")
            }
        }
    }

    private fun startPollingStatus(email: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repeat(60) {
                delay(3000)
                val statusResult = authRepository.checkRegisterStatus(email)
                statusResult.onSuccess { status ->
                    if (status.isVerified && status.user != null) {
                        _authState.value = AuthState.Authenticated(status.user)
                        _currentCurrency.value = status.user.currency
                        pollingJob?.cancel()
                    }
                }
            }
        }
    }

    private fun stopPollingStatus() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setCurrency(currency: String) {
        _currentCurrency.value = currency
        viewModelScope.launch {
            authRepository.updateCurrency(currency)
        }
    }

    fun logout() {
        stopPollingStatus()
        authRepository.logout()
        _authState.value = AuthState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopPollingStatus()
    }
}
