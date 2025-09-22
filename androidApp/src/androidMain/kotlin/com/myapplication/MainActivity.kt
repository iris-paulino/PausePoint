package com.myapplication

import MainView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import initializeAppStorage
import initializeInstalledAppsProvider
import registerCurrentActivity
import handleActivityResult

class MainActivity : AppCompatActivity() {
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

    @Deprecated("startActivityForResult is deprecated but fine for this simple bridge")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleActivityResult(requestCode, resultCode, data)
    }
}