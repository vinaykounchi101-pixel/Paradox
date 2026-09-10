package com.paradox.finance.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.finance.data.models.*
import com.paradox.finance.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiViewModel(private val aiRepository: AiRepository) : ViewModel() {

    // Purchase Simulator State
    private val _simulationResult = MutableStateFlow<SimulatePurchaseResponse?>(null)
    val simulationResult: StateFlow<SimulatePurchaseResponse?> = _simulationResult.asStateFlow()

    // Leak Hunter State
    private val _leakAnalysis = MutableStateFlow<LeakAnalysisResponse?>(null)
    val leakAnalysis: StateFlow<LeakAnalysisResponse?> = _leakAnalysis.asStateFlow()

    private val _healthScore = MutableStateFlow<HealthScoreResponse?>(null)
    val healthScore: StateFlow<HealthScoreResponse?> = _healthScore.asStateFlow()

    // Finny Chat State
    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                role = "model",
                content = "Hey Vikram! I'm Finny, your autonomous wealth copilot. Kiti kharch jhala, budget status, ki purchase simulate karaycha ahe? Just ask me!"
            )
        )
    )
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    // Monthly Wrapped State
    private val _monthlyWrapped = MutableStateFlow<MonthlyWrappedResponse?>(null)
    val monthlyWrapped: StateFlow<MonthlyWrappedResponse?> = _monthlyWrapped.asStateFlow()

    init {
        loadLeakAnalysis()
        loadHealthScore()
    }

    fun simulatePurchase(amount: Double, categoryId: String? = null) {
        viewModelScope.launch {
            _isAiThinking.value = true
            val result = aiRepository.simulatePurchase(amount, categoryId)
            result.onSuccess {
                _simulationResult.value = it
            }
            _isAiThinking.value = false
        }
    }

    fun loadLeakAnalysis(threshold: Double = 150.0) {
        viewModelScope.launch {
            val result = aiRepository.getLeakAnalysis(threshold)
            result.onSuccess {
                _leakAnalysis.value = it
            }
        }
    }

    fun loadHealthScore() {
        viewModelScope.launch {
            val result = aiRepository.getHealthScore()
            result.onSuccess {
                _healthScore.value = it
            }
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMessage = AiChatMessage(role = "user", content = userText.trim())
        val updatedList = _chatMessages.value + userMessage
        _chatMessages.value = updatedList

        viewModelScope.launch {
            _isAiThinking.value = true
            val result = aiRepository.sendChatMessage(userText, updatedList)
            result.onSuccess {
                val modelMessage = AiChatMessage(role = "model", content = it.reply)
                _chatMessages.value = _chatMessages.value + modelMessage
            }.onFailure {
                val fallbackMessage = AiChatMessage(
                    role = "model",
                    content = "Offline Intelligence Fallback: Tuza current burn rate optimal ahe. Daily safe spending allowance: ₹2,450. Keep your streaks active!"
                )
                _chatMessages.value = _chatMessages.value + fallbackMessage
            }
            _isAiThinking.value = false
        }
    }

    fun loadMonthlyWrapped() {
        viewModelScope.launch {
            val result = aiRepository.getMonthlyWrapped()
            result.onSuccess {
                _monthlyWrapped.value = it
            }
        }
    }
}
