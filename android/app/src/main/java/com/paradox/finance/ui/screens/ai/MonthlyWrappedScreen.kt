package com.paradox.finance.ui.screens.ai

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.ui.theme.*

@Composable
fun MonthlyWrappedScreen(
    currencySymbol: String,
    onClose: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }
    val totalSlides = 5

    val slideGradients = listOf(
        listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)),  // Indigo -> Cyan
        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),  // Pink -> Violet
        listOf(Color(0xFF10B981), Color(0xFF3B82F6)),  // Emerald -> Blue
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),  // Amber -> Rose
        listOf(Color(0xFF6366F1), Color(0xFF10B981))   // Indigo -> Emerald
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(slideGradients[currentSlide % slideGradients.size])
            )
            .clickable {
                if (currentSlide < totalSlides - 1) {
                    currentSlide++
                } else {
                    onClose()
                }
            }
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Story Progress Bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                for (i in 0 until totalSlides) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (i <= currentSlide) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Close Button
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Animated Slide Content
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WrappedSlideTransition"
            ) { slide ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (slide) {
                        0 -> {
                            Text("✨ PARADOX WRAPPED", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("This Month in Numbers", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("$currencySymbol 28,450", fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Total spent across 48 transactions", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                        1 -> {
                            Text("👑 TOP CATEGORY", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Food & Dining", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("$currencySymbol 11,200", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your favorite spot: Swiggy & Zomato 🍕", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                        2 -> {
                            Text("📊 50/30/20 DISCIPLINE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Needs vs Wants", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("88% Adherence", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Needs: 52% • Wants: 28% • Savings: 20%", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
                        }
                        3 -> {
                            Text("🏆 CONSISTENCY CHAMPION", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("18-Day Logging Streak", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("🔥 Top 5%", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You logged transactions every single day!", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
                        }
                        4 -> {
                            Text("🧠 FINANCIAL ARCHETYPE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 2.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("The Strategic Builder", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("🏛️", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Disciplined, forward-thinking, and intentional with investments and savings!", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (currentSlide < totalSlides - 1) "Tap to continue ➔" else "Tap to close ✕",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
            )
        }
    }
}
