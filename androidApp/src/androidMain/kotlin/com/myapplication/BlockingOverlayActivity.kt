package com.myapplication

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockingOverlayActivity : AppCompatActivity() {
    private val hideReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple full-screen, touch-consuming layout
        val container = FrameLayout(this).apply {
            setBackgroundColor(0xCC000000.toInt())
            isClickable = true
            isFocusable = true
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding * 3, padding, padding)
        }

        val title = TextView(this).apply {
            text = "Pause reached"
            textSize = 22f
            setTextColor(Color.WHITE)
        }
        val body = TextView(this).apply {
            text = intent?.getStringExtra("message") ?: "Take a mindful pause"
            textSize = 16f
            setTextColor(Color.WHITE)
        }
        val scan = Button(this).apply {
            text = "Scan QR to continue"
            setOnClickListener {
                @Suppress("DEPRECATION")
                startActivityForResult(Intent(this@BlockingOverlayActivity, QrScanActivity::class.java), 1001)
            }
        }
        val dismiss = Button(this).apply {
            text = "Dismiss"
            setOnClickListener { finish() }
        }

        // Stack vertically and center
        val stack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (12 * resources.displayMetrics.density).toInt()
            bottomMargin = (12 * resources.displayMetrics.density).toInt()
        }
        stack.addView(title, lp)
        stack.addView(body, lp)
        stack.addView(scan, lp)
        stack.addView(dismiss, lp)
        container.addView(stack, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setContentView(container)

        // Register receiver to close when HIDE is sent
        val filter = IntentFilter("com.myapplication.HIDE_BLOCKING_OVERLAY")
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                registerReceiver(hideReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(hideReceiver, filter)
            }
        } catch (_: Exception) { }
    }

    override fun onDestroy() {
        try { unregisterReceiver(hideReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            // Close overlay after scanner returns (QR validity is handled in main flow elsewhere)
            finish()
        }
    }
}


