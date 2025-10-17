package com.luminoprisma.scrollpause

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luminoprisma.scrollpause.R
import createAppStorage
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import openLastTrackedApp

class CongratulationsOverlayActivity : ComponentActivity() {
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

        setContent {
            CongratulationsOverlayContent(
                onClose = {
                    // Dismiss and return to the last tracked app
                    finish()
                    val trackedApps = runBlocking {
                        val storage = createAppStorage()
                        storage.getSelectedAppPackages()
                    }
                    openLastTrackedApp(trackedApps)
                }
            )
        }
    }
}

@Composable
fun BadgeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    number: Int = 2
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Badge yellow image
        Image(
            painter = painterResource(R.drawable.yellowbadge),
            contentDescription = "Achievement Badge",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        
        // Number overlay positioned higher on the badge
        Text(
            text = number.toString(),
            color = Color(0xFF004aad), // Dark blue theme color
            fontSize = (size * 0.4f).value.sp, // Smaller font size to fit nicely on badge
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-size * 0.1f)) // Move text up by 10% of badge size
        )
    }
}

@Composable
fun getRandomCongratulationMessage(): String {
    val messages = listOf(
        "Look at you go! Your legs just got a better workout than your thumbs! 🦵",
        "Walking to your QR code? You're basically a marathon runner now! 🏃",
        "Your scrolling thumb is probably wondering where you went! 👍",
        "Someone's been skipping leg day... but NOT today! 💪",
        "Breaking News: Local human stands up AND walks. Scientists amazed! 📰",
        "That QR code didn't scan itself! Well done, movement champion! 🎯",
        "Your couch is proud of you for leaving it! 🛋️",
        "Achievement Unlocked: Actually Moving Your Body! 🏆",
        "Who knew standing could feel this good? (Your body did.) 🌟",
        "You've earned the right to sit back down... but maybe walk around first? 😄"
    )
    return messages[Random.nextInt(messages.size)]
}

@Composable
private fun CongratulationsOverlayContent(onClose: () -> Unit) {
    var dayStreakCounter by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val storage = createAppStorage()
        dayStreakCounter = storage.getDayStreakCounter()
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
            // Congratulations title
            Text(
                "Congratulations!",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Badge icon
            BadgeIcon(
                size = 150.dp,
                number = dayStreakCounter
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Days without doomscrolling text
            Text(
                if (dayStreakCounter == 1) "day without doomscrolling" else "days without doomscrolling",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Message subheading with random variation
            val congratulationMessage = getRandomCongratulationMessage()
            Text(
                congratulationMessage,
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(Modifier.height(48.dp))
            
            // Back to Dashboard button
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2C4877)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Close",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

