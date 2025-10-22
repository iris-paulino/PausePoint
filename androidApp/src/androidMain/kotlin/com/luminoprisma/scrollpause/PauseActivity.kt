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
import createAdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.WindowManager

class PauseActivity : ComponentActivity() {
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
                            // QR code is invalid, show error or keep pause screen open
                            // For now, we'll keep the pause screen open
                        }
                    } catch (e: Exception) {
                        // Error validating QR code, keep pause screen open
                    }
                }
            }
        }
        // If result is not OK or QR text is null, keep pause screen open
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure this activity reliably appears on top
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

        val message = intent?.getStringExtra("message") ?: "Take a mindful pause"

        // Register receiver to close when HIDE is sent
        val filter = IntentFilter("com.luminoprisma.scrollpause.HIDE_PAUSE_SCREEN")
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(hideReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(hideReceiver, filter)
            }
        } catch (_: Exception) { }

        setContent {
            PauseActivityContent(
                message = message, 
                onFinish = { 
                    // Show ad before dismissing pause screen
                    println("DEBUG: PauseActivity - onFinish called, attempting to show ad")
                    try {
                        val adManager = createAdManager()
                        println("DEBUG: PauseActivity - AdManager created successfully")
                        if (adManager.isAdLoaded()) {
                            println("DEBUG: Pause - Ad is loaded, showing interstitial ad")
                            adManager.showInterstitialAd(
                                onAdClosed = {
                                    println("DEBUG: Pause ad completed, dismissing pause screen")
                                    performSelfContainedDismiss()
                                    finish()
                                },
                                onAdFailedToLoad = {
                                    println("DEBUG: Pause ad failed, dismissing anyway")
                                    performSelfContainedDismiss()
                                    finish()
                                }
                            )
                        } else {
                            println("DEBUG: Pause - No ad loaded, attempting to load and show...")
                            // Try to load an ad first
                            adManager.loadAd()
                            
                            // Wait a moment for the ad to load, then try to show it
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(2000) // Wait 2 seconds for ad to load
                                val isLoadedAfterWait = adManager.isAdLoaded()
                                println("DEBUG: Pause - Ad loaded status after wait: $isLoadedAfterWait")
                                
                                if (isLoadedAfterWait) {
                                    println("DEBUG: Pause - Ad loaded after wait, showing interstitial ad")
                                    adManager.showInterstitialAd(
                                        onAdClosed = {
                                            println("DEBUG: Pause ad completed after wait, dismissing pause screen")
                                            performSelfContainedDismiss()
                                            finish()
                                        },
                                        onAdFailedToLoad = {
                                            println("DEBUG: Pause ad failed after wait, dismissing anyway")
                                            performSelfContainedDismiss()
                                            finish()
                                        }
                                    )
                                } else {
                                    println("DEBUG: Pause - Ad still not loaded after wait, dismissing without ad")
                                    performSelfContainedDismiss()
                                    finish()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Error showing pause ad: ${e.message}, dismissing anyway")
                        performSelfContainedDismiss()
                        finish()
                    }
                },
                qrScanLauncher = qrScanLauncher
            )
        }
    }

    private fun performSelfContainedDismiss() {
        println("DEBUG: PauseActivity - performSelfContainedDismiss called")
        // Use the main app's dismiss callback instead of duplicating logic
        try {
            dismissAndContinueTracking()
            println("DEBUG: PauseActivity - called dismissAndContinueTracking()")
        } catch (e: Exception) {
            println("DEBUG: PauseActivity - error calling dismissAndContinueTracking: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("DEBUG: PauseActivity - onNewIntent called")
    }

    override fun onDestroy() {
        try { unregisterReceiver(hideReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}

// Helper to start this activity robustly from any Context (e.g., service)
internal fun startPauseActivity(ctx: Context, message: String?) {
    try {
        val i = Intent(ctx, PauseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (!message.isNullOrEmpty()) putExtra("message", message)
        }
        ctx.startActivity(i)
    } catch (e: Exception) {
        println("DEBUG: startPauseActivity - error: ${e.message}")
    }
}

@Composable
private fun PauseActivityContent(message: String, onFinish: () -> Unit, qrScanLauncher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val ctx = LocalContext.current
    var durationText by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf(0) }
    var restartTrackingOnUnlock by remember { mutableStateOf(false) }
    var dayStreakCounter by remember { mutableStateOf(0) }

    // Initialize preference from storage so pause screen matches main app
    LaunchedEffect(Unit) {
        try {
            val storage = createAppStorage()
            restartTrackingOnUnlock = storage.getAutoRestartOnDismiss()
            dayStreakCounter = storage.getDayStreakCounter()
        } catch (_: Exception) {}
    }

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
        dayStreakCounter = dayStreakCounter,
        onScanQr = {
            setQrScanningActive(true)
            qrScanLauncher.launch(Intent(ctx, QrScanActivity::class.java))
        },
        onClose = onFinish
    )
}



