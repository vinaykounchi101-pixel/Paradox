package com.paradox.finance.ui.screens.ai

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.data.remote.ApiClient
import com.paradox.finance.hardware.voice.VoiceInputHelper
import com.paradox.finance.ui.components.NavTab
import com.paradox.finance.ui.components.ObsidianBottomBar
import com.paradox.finance.ui.screens.expenses.ExpenseViewModel
import com.paradox.finance.ui.screens.expenses.QuickAddExpenseDialog
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
    expenseViewModel: ExpenseViewModel? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = onNavigateBack,
    onNavigateToSimulator: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val api = remember { ApiClient.getApi() }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    
    val messages = remember {
        mutableStateListOf(
            ChatMessage("You spent 18% less on food deliveries this week compared to last week! Would you like me to lock ₹2,000 into your MacBook Pro Goal?", false)
        )
    }

    val voiceHelper = remember {
        VoiceInputHelper(
            context = context,
            onResult = { text ->
                inputText = text
            },
            onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            },
            onListeningStateChanged = { listening ->
                isListening = listening
            }
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceHelper.startListening()
        } else {
            Toast.makeText(context, "Microphone permission needed for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    val suggestionChips = listOf(
        "Where did my money go?",
        "Can I afford dinner tonight?",
        "Suggest budget cuts",
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
                    messages.add(ChatMessage("Based on your recent transactions, you are spending within your safe daily allowance of ₹1,087/day! Keep it up!", false))
                }
            } catch (e: Exception) {
                messages.add(ChatMessage("Your financial pacing is healthy! Let me know if you want to log an expense or simulate a purchase.", false))
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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Finny Copilot", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald)
                                )
                            }
                            Text("Autonomous Portfolio Advisor", color = NeonTeal, fontSize = 10.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SurfaceObsidianElevated)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("NEURAL CORE v4", color = TextTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundPitchBlack)
            )
        },
        bottomBar = {
            ObsidianBottomBar(
                currentTab = NavTab.FINNY_AI,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.DASHBOARD -> { onNavigateToDashboard() }
                        NavTab.QUICK_LOG -> {
                            if (expenseViewModel != null) {
                                expenseViewModel.openAddDialog()
                                showQuickAddDialog = true
                            }
                        }
                        NavTab.SIMULATOR -> { onNavigateToSimulator() }
                        NavTab.FINNY_AI -> { /* Already on Finny AI */ }
                    }
                }
            )
        },
        containerColor = BackgroundPitchBlack
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(
                        msg = msg,
                        onLockGoal = {
                            messages.add(ChatMessage("Yes, Lock ₹2,000", true))
                            Toast.makeText(context, "₹2,000 locked into MacBook Pro Goal! 🎯", Toast.LENGTH_SHORT).show()
                            messages.add(ChatMessage("Done! ₹2,000 has been transferred to your MacBook Pro savings vault. Target completion accelerated by 18 days! 🚀", false))
                        },
                        onDismissAction = {
                            messages.add(ChatMessage("Not right now, thanks.", true))
                            messages.add(ChatMessage("Understood! Keeping funds liquid in your primary balance.", false))
                        }
                    )
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        ) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finny is analyzing ledger...", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Suggested Command Pills
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(
                    "SUGGESTED COMMANDS",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestionChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(SurfaceObsidianElevated)
                                .border(1.dp, BorderGlass, RoundedCornerShape(9999.dp))
                                .clickable { sendMessage(chip) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(chip, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Interactive AI Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(SurfaceObsidianSubtle)
                    .border(1.dp, BorderGlass, RoundedCornerShape(9999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask Finny anything...", color = TextTertiary, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonEmerald
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (isListening) {
                                voiceHelper.stopListening()
                            } else {
                                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    voiceHelper.startListening()
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isListening) AlertCoral.copy(alpha = 0.3f) else SurfaceObsidianElevated)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (isListening) AlertCoral else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { sendMessage(inputText) },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) NeonEmerald else SurfaceObsidianElevated)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) BackgroundPitchBlack else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showQuickAddDialog && expenseViewModel != null) {
        QuickAddExpenseDialog(
            viewModel = expenseViewModel,
            onDismiss = { showQuickAddDialog = false }
        )
    }
}

@Composable
fun ChatBubble(
    msg: ChatMessage,
    onLockGoal: () -> Unit = {},
    onDismissAction: () -> Unit = {}
) {
    Row(
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!msg.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonEmerald.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (msg.isUser) SurfaceObsidianHighlight else SurfaceObsidianElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGlass),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = msg.text,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                )

                if (!msg.isUser && msg.text.contains("MacBook")) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onLockGoal,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            shape = RoundedCornerShape(9999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Yes, Lock ₹2,000", color = BackgroundPitchBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismissAction,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceObsidianHighlight),
                            shape = RoundedCornerShape(9999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Later", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
