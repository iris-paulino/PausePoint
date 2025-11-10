package com.luminoprisma.scrollpause

import MainView
import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import initializeAppStorage
import initializeInstalledAppsProvider
import registerCurrentActivity
import initializeAdManager

class MainActivity : AppCompatActivity() {
    private var isDismissing = false // Flag to prevent camera permission requests during dismiss
    private lateinit var appRestartDetector: AppRestartDetector
    private lateinit var notificationManager: WellbeingNotificationManager
    
    private var dismissCallback: (() -> Unit)? = null
    
    private val resetTimerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            println("DEBUG: MainActivity - RESET_TIMER_AND_CONTINUE broadcast received")
            // Call the dismiss callback to restart tracking
            dismissCallback?.invoke()
        }
    }
    
    fun setDismissCallback(callback: (() -> Unit)?) {
        dismissCallback = callback
        println("DEBUG: MainActivity - setDismissCallback called")
    }
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isDismissing) {
            println("DEBUG: MainActivity - camera permission result received but app is dismissing, ignoring")
            return@registerForActivityResult
        }
        // Permission result is handled by the QR scanner itself
        // This launcher is just to ensure we have the permission before scanning
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        
        super.onCreate(savedInstanceState)
        
        // Initialize the app storage with the activity context
        initializeAppStorage(this)
        
        // Initialize the installed apps provider with the activity context
        initializeInstalledAppsProvider(this)
        
        // Initialize the ad manager with the activity context
        initializeAdManager(this)

        // Initialize new monitoring components
        appRestartDetector = AppRestartDetector(this)
        notificationManager = WellbeingNotificationManager(this)
        
        // Detect app restart and auto-resume if needed
        appRestartDetector.onAppStart()

        registerCurrentActivity(this)
        
        // Register broadcast receiver for RESET_TIMER_AND_CONTINUE
        val filter = IntentFilter("com.luminoprisma.scrollpause.RESET_TIMER_AND_CONTINUE")
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(resetTimerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(resetTimerReceiver, filter)
            }
            println("DEBUG: MainActivity - registered RESET_TIMER_AND_CONTINUE broadcast receiver")
        } catch (e: Exception) {
            println("DEBUG: MainActivity - error registering broadcast receiver: ${e.message}")
        }

        setContent {
            MainView()
        }
        
        // Handle intent extras for pause screen
        handleIntentExtras()
    }
    
    fun requestCameraPermissionIfNeeded(): Boolean {
        if (isDismissing) {
            println("DEBUG: MainActivity - requestCameraPermissionIfNeeded called but app is dismissing, skipping")
            return false
        }
        return when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                false
            }
        }
    }
    
    fun setDismissingState(dismissing: Boolean) {
        isDismissing = dismissing
        println("DEBUG: MainActivity - setDismissingState: $dismissing")
    }
    
    private fun handleIntentExtras() {
        val message = intent?.getStringExtra("message")
        if (message != null) {
            println("DEBUG: MainActivity - received message from intent: $message")
            // The MainView will automatically show PauseScreen when route = Route.Pause
            // This happens when the time limit is reached in the main app logic
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras()
    }
    
    fun resetDismissingStateIfNeeded() {
        if (isDismissing) {
            isDismissing = false
            println("DEBUG: MainActivity - resetDismissingStateIfNeeded: reset dismissing state to false")
        }
    }

    // Accessibility & Overlay helpers
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val expected = ComponentName(this, ForegroundAppAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.split(":").any { it.equals(expected.flattenToString(), ignoreCase = true) }
        } catch (_: Exception) {
            false
        }
    }

    fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    // REMOVED: Overlay functionality for policy compliance
    // Blocking now happens within the app itself
    
    override fun onDestroy() {
        super.onDestroy()
        appRestartDetector.cleanup()
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(resetTimerReceiver)
            println("DEBUG: MainActivity - unregistered RESET_TIMER_AND_CONTINUE broadcast receiver")
        } catch (e: Exception) {
            println("DEBUG: MainActivity - error unregistering broadcast receiver: ${e.message}")
        }
    }

}