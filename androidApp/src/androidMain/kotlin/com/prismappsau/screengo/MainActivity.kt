package com.prismappsau.screengo

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
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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

}