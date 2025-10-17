package com.luminoprisma.scrollpause

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class CongratulationsOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen overlay-style
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

        // Build a simple view hierarchy (no Compose dependency here)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A1A"))
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "Congratulations!"
            setTextColor(Color.WHITE)
            textSize = 28f
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 48
        })

        val subtitle = TextView(this).apply {
            text = "Great job getting your steps in."
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 16f
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 48
        })

        val dashboardBtn = Button(this).apply {
            text = "Back to Dashboard"
            setBackgroundColor(Color.parseColor("#2C4877"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                try {
                    val intent = android.content.Intent().apply {
                        setClassName(
                            "com.luminoprisma.scrollpause",
                            "com.luminoprisma.scrollpause.MainActivity"
                        )
                        addFlags(
                            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
                finish()
            }
        }
        root.addView(dashboardBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 24
        })

        val lastAppBtn = Button(this).apply {
            text = "Back to last app"
            setBackgroundColor(Color.parseColor("#2C2C2C"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                try { launchLastTrackedAppFromPrefs() } catch (_: Exception) {}
                finish()
            }
        }
        root.addView(lastAppBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(root)
    }

    private fun getAllTrackedAppIdentifiersFromPrefs(): List<String> {
        return try {
            val prefs = applicationContext.getSharedPreferences("scrollpause_prefs", android.content.Context.MODE_PRIVATE)
            val csv = prefs.getString("tracked_apps_csv", "") ?: ""
            if (csv.isBlank()) emptyList() else csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        } catch (_: Exception) { emptyList() }
    }

    private fun launchLastTrackedAppFromPrefs() {
        val identifiers = getAllTrackedAppIdentifiersFromPrefs()
        if (identifiers.isEmpty()) return
        // Try to pick the first identifier that looks like a package name
        val pkg = identifiers.firstOrNull { it.contains('.') }
        if (pkg != null) {
            try {
                val intent = packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }
    }
}


