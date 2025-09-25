package com.myapplication

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import PauseScreen

class PauseOverlayActivity : ComponentActivity() {
    private val hideReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent?.getStringExtra("message") ?: "Take a mindful pause"

        // Register receiver to close when HIDE is sent
        val filter = IntentFilter("com.myapplication.HIDE_BLOCKING_OVERLAY")
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(hideReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(hideReceiver, filter)
            }
        } catch (_: Exception) { }

        setContent {
            PauseOverlayContent(message = message, onFinish = { finish() })
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002) {
            // Close overlay after scanner returns
            finish()
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(hideReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
private fun PauseOverlayContent(message: String, onFinish: () -> Unit) {
    val ctx = LocalContext.current
    var durationText by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf(0) }

    // Use provided message; duration/limit are not strictly needed for visuals
    LaunchedEffect(Unit) {
        durationText = ""
        limit = 0
    }

    PauseScreen(
        durationText = durationText,
        timeLimitMinutes = limit,
        onScanQr = {
            @Suppress("DEPRECATION")
            (ctx as? PauseOverlayActivity)?.startActivityForResult(Intent(ctx, QrScanActivity::class.java), 1002)
        },
        onClose = onFinish
    )
}


