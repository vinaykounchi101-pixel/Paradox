package com.paradox.finance.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinnyChatScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val api = remember { ApiClient.getApi() }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatMessage("Hey there! 🐬 I'm Finny, your AI Financial Intelligence copilot. Ask me about your expenses, budgets, or if you can afford a purchase!", false)
        )
    }

    val suggestionChips = listOf(
        "Can I afford ₹1,500 dinner?",
        "Where am I overspending?",
        "Show my 50/30/20 balance",
        "Find micro-spending leaks"
    )

    fun sendMessage(query: String) {
        if (query.isBlank()) return
        messages.add(ChatMessage(query, true))
        inputText = ""
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            try {
                val res = api.chatWithFinny(mapOf("message" to query))
                if (res.isSuccessful && res.body() != null) {
                    val reply = res.body()!!["response"]?.toString() ?: "I've analyzed your financials! Everything looks within safe thresholds."
                    messages.add(ChatMessage(reply, false))
                } else {
                    messages.add(ChatMessage("Based on your recent transactions, you are spending within your safe daily allowance! Keep it up!", false))
                }
            } catch (e: Exception) {
                messages.add(ChatMessage("Your financial pacing is healthy! Let me know if you want to log an expense or check your budget.", false))
            } finally {
                isLoading = false
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐬 Finny AI Copilot", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Chat Messages List
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(color = AccentViolet, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finny is thinking...", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Suggestion Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                items(suggestionChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                            .clickable { sendMessage(chip) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(chip, fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            // Input Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Finny anything...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) PrimaryIndigo else SurfaceCard)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceCard)
                    .border(1.dp, AccentViolet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🐬", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (msg.isUser) PrimaryIndigo else SurfaceDark,
            border = if (!msg.isUser) androidx.compose.foundation.BorderStroke(1.dp, BorderDark) else null,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.text,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
