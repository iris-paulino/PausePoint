package com.luminoprisma.scrollpause

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import PauseScreen
import createAppStorage
import setQrScanningActive
import resetTimerAndContinueTracking
import dismissAndContinueTracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PauseOverlayActivity : ComponentActivity() {
    private val hideReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private val qrScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // QR scanning is no longer active
        setQrScanningActive(false)
        
        if (result.resultCode == RESULT_OK) {
            val qrText = result.data?.getStringExtra("qr_text")
            if (qrText != null) {
                // Validate QR code and handle result
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val storage = createAppStorage()
                        val match = storage.validateQrCode(qrText)
                        if (match != null) {
                            // QR code is valid, reset timer and continue tracking
                            resetTimerAndContinueTracking()
                            finish()
                        } else {
                            // QR code is invalid, show error or keep overlay open
                            // For now, we'll keep the overlay open
                        }
                    } catch (e: Exception) {
                        // Error validating QR code, keep overlay open
                    }
                }
            }
        }
        // If result is not OK or QR text is null, keep overlay open
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent?.getStringExtra("message") ?: "Take a mindful pause"

        // Register receiver to close when HIDE is sent
        val filter = IntentFilter("com.luminoprisma.scrollpause.HIDE_BLOCKING_OVERLAY")
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(hideReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(hideReceiver, filter)
            }
        } catch (_: Exception) { }

        setContent {
            PauseOverlayContent(
                message = message, 
                onFinish = { 
                    // Dismiss and continue tracking when user dismisses
                    dismissAndContinueTracking()
                    finish() 
                },
                qrScanLauncher = qrScanLauncher
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("DEBUG: PauseOverlayActivity - onNewIntent called")
        // Bring the activity to the foreground when user switches to another tracked app
        // The activity is already running, so we just need to make sure it's visible
        // The existing content will remain the same
    }


    override fun onDestroy() {
        try { unregisterReceiver(hideReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
private fun PauseOverlayContent(message: String, onFinish: () -> Unit, qrScanLauncher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val ctx = LocalContext.current
    var durationText by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf(0) }

    // Extract time limit from message and set up proper display
    LaunchedEffect(message) {
        // Parse the message to extract time limit information
        // The message should contain time limit info, but if not, we'll use a default
        val timeLimitMatch = Regex("(\\d+)\\s*(minute|hour|h|m)").find(message)
        if (timeLimitMatch != null) {
            val value = timeLimitMatch.groupValues[1].toIntOrNull() ?: 0
            val unit = timeLimitMatch.groupValues[2]
            limit = if (unit.contains("hour") || unit == "h") value * 60 else value
            durationText = if (limit >= 60) "${limit / 60}h ${limit % 60}m" else "${limit}m"
        } else {
            // Fallback: try to get time limit from app storage
            try {
                val storage = createAppStorage()
                limit = storage.getTimeLimitMinutes()
                durationText = if (limit >= 60) "${limit / 60}h ${limit % 60}m" else "${limit}m"
            } catch (e: Exception) {
                // If all else fails, use a default
                limit = 30
                durationText = "30m"
            }
        }
    }

    PauseScreen(
        durationText = durationText,
        timeLimitMinutes = limit,
        onScanQr = {
            setQrScanningActive(true)
            qrScanLauncher.launch(Intent(ctx, QrScanActivity::class.java))
        },
        onClose = onFinish
    )
}


