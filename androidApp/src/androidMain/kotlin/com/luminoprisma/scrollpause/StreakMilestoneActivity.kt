package com.luminoprisma.scrollpause

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class StreakMilestoneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure this overlay-style activity reliably appears on top
        try {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        } catch (_: Exception) {}

        val milestone = intent.getStringExtra("milestone") ?: "Milestone Achieved!"

        setContent {
            StreakMilestoneContent(
                milestone = milestone,
                onClose = {
                    finish()
                }
            )
        }
    }
}

@Composable
fun StreakMilestoneContent(milestone: String, onClose: () -> Unit) {
    var showCelebration by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(500) // Small delay to build anticipation
        showCelebration = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Milestone title with celebration effect
            Text(
                if (showCelebration) "MILESTONE ACHIEVED!" else "MILESTONE ACHIEVED!",
                color = Color(0xFFFFD700), // Gold color
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Milestone badge with special styling
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (milestone.contains("Week")) Color(0xFF4CAF50) // Green for week
                        else if (milestone.contains("Month")) Color(0xFF2196F3) // Blue for month
                        else Color(0xFFFF9800) // Orange for 100 days
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🏆",
                        fontSize = 48.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        milestone,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Celebration message
            val celebrationMessage = when {
                milestone.contains("Week") -> "You've built a solid week of mindful habits! 🌟"
                milestone.contains("Month") -> "A whole month of conscious choices! You're amazing! 🚀"
                milestone.contains("100") -> "100 days of transformation! You're a mindfulness champion! 🏆"
                else -> "Incredible achievement! Keep up the great work! 💪"
            }
            
            Text(
                celebrationMessage,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(Modifier.height(48.dp))
            
            // Close button
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF004aad)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    "Continue Journey",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
