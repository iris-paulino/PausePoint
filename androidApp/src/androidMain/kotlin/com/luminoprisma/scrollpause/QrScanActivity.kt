package com.luminoprisma.scrollpause

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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

class QrScanActivity : AppCompatActivity() {
    private var expectedMessage: String? = null // no longer used for validation; any QR counts

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
                    launchQrScanner()
                }
            }
        } else {
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
        expectedMessage = intent?.getStringExtra("expected_message")
        
        // Check camera permission before launching scanner
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            launchQrScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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


