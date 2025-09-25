package com.prismappsau.screengo

import MainView
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import initializeAppStorage
import initializeInstalledAppsProvider
import registerCurrentActivity

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

}