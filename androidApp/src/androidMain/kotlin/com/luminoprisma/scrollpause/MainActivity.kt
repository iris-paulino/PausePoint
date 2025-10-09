package com.luminoprisma.scrollpause

import MainView
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import initializeAppStorage
import initializeInstalledAppsProvider
import registerCurrentActivity

class MainActivity : AppCompatActivity() {
    private var isDismissing = false // Flag to prevent camera permission requests during dismiss
    
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
        super.onCreate(savedInstanceState)
        
        // Initialize the app storage with the activity context
        initializeAppStorage(this)
        
        // Initialize the installed apps provider with the activity context
        initializeInstalledAppsProvider(this)

        registerCurrentActivity(this)

        setContent {
            MainView()
        }
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
    
    fun resetDismissingStateIfNeeded() {
        if (isDismissing) {
            isDismissing = false
            println("DEBUG: MainActivity - resetDismissingStateIfNeeded: reset dismissing state to false")
        }
    }

}