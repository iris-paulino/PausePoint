package com.luminoprisma.scrollpause

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import createAppStorage
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
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            backgroundColor = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Congratulations!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    "You've maintained your focus for",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "$dayStreakCounter ${if (dayStreakCounter == 1) "day" else "days"}!",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

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
}

