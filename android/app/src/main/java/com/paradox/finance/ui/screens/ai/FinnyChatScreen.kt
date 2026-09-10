package com.paradox.finance.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.components.BottomBarDestination
import com.paradox.finance.ui.components.ObsidianBottomBar
import com.paradox.finance.ui.theme.*
import com.paradox.finance.ui.viewmodels.AiViewModel
import com.paradox.finance.utils.VoiceInputHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinnyChatScreen(
    aiViewModel: AiViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val chatMessages by aiViewModel.chatMessages.collectAsState()
    val isThinking by aiViewModel.isAiThinking.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val voiceHelper = remember {
        VoiceInputHelper(
            context = context,
            onResult = { text ->
                inputText = text
                aiViewModel.sendChatMessage(text)
                inputText = ""
            },
            onError = {},
            onListeningStateChanged = { isListening = it }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GlassSurface2)
                                .border(1.dp, ElectricEmerald, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Finny", tint = ElectricEmerald, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text(text = "Finny Copilot", color = OnSurfaceHigh, fontSize = 15.sp, style = Typography.labelLarge)
                            Text(text = "Gemini 3.6 Flash • RAG Synced", color = ElectricEmerald, fontSize = 10.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PitchBlack)
            )
        },
        bottomBar = {
            ObsidianBottomBar(
                currentDestination = BottomBarDestination.FINNY_AI,
                onNavigate = { dest ->
                    if (dest == BottomBarDestination.DASHBOARD) onNavigateToDashboard()
                }
            )
        },
        containerColor = PitchBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Suggestion Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Kiti shillak ahe?", "Analyze leaks", "Simulate purchase").forEach { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(GlassSurface1)
                            .border(1.dp, GlassBorderStroke, RoundedCornerShape(9999.dp))
                            .clickable { aiViewModel.sendChatMessage(chip) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = chip, color = LuminousCyan, fontSize = 11.sp)
                    }
                }
            }

            // Message Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                                .background(if (isUser) ElectricEmerald.copy(alpha = 0.2f) else GlassSurface1)
                                .border(
                                    1.dp,
                                    if (isUser) ElectricEmerald.copy(alpha = 0.4f) else GlassBorderStroke,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.displayContent,
                                color = if (isUser) OnSurfaceHigh else OnSurface,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Text(
                            text = "Finny is analyzing telemetry...",
                            color = ElectricEmerald,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Finny in English/Minglish...", color = MutedOutline, fontSize = 12.sp) },
                    trailingIcon = {
                        IconButton(onClick = { voiceHelper.startListening() }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = if (isListening) AlertCoral else ElectricEmerald
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
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            aiViewModel.sendChatMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ElectricEmerald)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = PitchBlack, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
