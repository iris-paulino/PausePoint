package com.luminoprisma.scrollpause

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.Toast
import createAppStorage
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import setQrScanningActive

class QrScanActivity : AppCompatActivity() {
    private var expectedMessage: String? = null // no longer used for validation; any QR counts
    private var isFinishing = false // Flag to prevent camera permission launcher after finish broadcast
    private var cameraPermissionLauncherInProgress = false // Flag to track if camera permission launcher is in progress

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            println("DEBUG: QrScanActivity - received FINISH_QR_SCAN_ACTIVITY broadcast")
            isFinishing = true
            cameraPermissionLauncherInProgress = false // Cancel any pending camera permission request
            setQrScanningActive(false)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private val launcher = registerForActivityResult(ScanContract()) { result ->
        val data = Intent()
        if (result != null && result.contents != null) {
            val scanned = result.contents

            lifecycleScope.launch {
                val isValid = try {
                    val storage = createAppStorage()
                    storage.validateQrCode(scanned) != null
                } catch (_: Exception) { false }

                if (isValid) {
                    // End scanning session on success
                    setQrScanningActive(false)
                    data.putExtra("qr_text", scanned)
                    setResult(Activity.RESULT_OK, data)
                    try {
                        val intent = Intent("com.luminoprisma.scrollpause.QR_SCAN_RESULT").apply {
                            putExtra("qr_text", scanned)
                            setPackage(applicationContext.packageName)
                        }
                        applicationContext.sendBroadcast(intent)
                    } catch (_: Exception) {}
                    finish()
                } else {
                    Toast.makeText(this@QrScanActivity, "Invalid QR code. Please scan a valid Pause QR.", Toast.LENGTH_SHORT).show()
                    // Relaunch scanner without finishing, so the camera stays open
                    // Keep scanning active between attempts
                    setQrScanningActive(true)
                    launchQrScanner()
                }
            }
        } else {
            // End scanning session on cancel/no result
            setQrScanningActive(false)
            setResult(Activity.RESULT_CANCELED)
            try {
                val intent = Intent("com.luminoprisma.scrollpause.QR_SCAN_RESULT").apply {
                    setPackage(applicationContext.packageName)
                }
                applicationContext.sendBroadcast(intent)
            } catch (_: Exception) {}
            finish()
        }
    }
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionLauncherInProgress = false // Reset flag when result is received
        if (isFinishing) {
            println("DEBUG: QrScanActivity - camera permission result received but activity is finishing, ignoring")
            return@registerForActivityResult
        }
        if (isGranted) {
            launchQrScanner()
        } else {
            // Permission denied, finish activity
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mark scanning active; lifecycle methods will keep this accurate
        setQrScanningActive(true)
        expectedMessage = intent?.getStringExtra("expected_message")
        
        // Register receiver to finish when dismiss is called
        val filter = IntentFilter("com.luminoprisma.scrollpause.FINISH_QR_SCAN_ACTIVITY")
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(finishReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(finishReceiver, filter)
            }
        } catch (_: Exception) { }
        
        // Check camera permission before launching scanner
        if (isFinishing) {
            println("DEBUG: QrScanActivity - onCreate called but activity is finishing, skipping camera permission check")
            return
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            launchQrScanner()
        } else {
            // Add a small delay to allow finish broadcast to be received if it's coming
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (!isFinishing && !cameraPermissionLauncherInProgress) {
                    cameraPermissionLauncherInProgress = true // Set flag before launching
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    println("DEBUG: QrScanActivity - skipping camera permission launcher due to finishing state")
                }
            }, 100) // 100ms delay
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) {
            println("DEBUG: QrScanActivity - onResume called but activity is finishing, skipping")
            return
        }
        // Scanner is visible/interactive
        setQrScanningActive(true)
    }

    override fun onPause() {
        // Scanner is no longer in the foreground; allow overlays on tracked apps
        setQrScanningActive(false)
        super.onPause()
    }

    override fun onDestroy() {
        // Safety: ensure we clear the flag if the activity is destroyed unexpectedly
        setQrScanningActive(false)
        try { unregisterReceiver(finishReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
    
    private fun launchQrScanner() {
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Scan your Pause QR code")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
            .setCaptureActivity(CaptureActivity::class.java)

        launcher.launch(options)
    }
}


