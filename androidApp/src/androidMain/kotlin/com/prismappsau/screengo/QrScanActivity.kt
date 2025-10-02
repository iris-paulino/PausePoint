package com.prismappsau.screengo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QrScanActivity : AppCompatActivity() {
    private var scannerLaunched = false
    
    private val launcher = registerForActivityResult(ScanContract()) { result ->
        val data = Intent()
        if (result != null && result.contents != null) {
            data.putExtra("qr_text", result.contents)
            setResult(Activity.RESULT_OK, data)
        } else {
            setResult(Activity.RESULT_CANCELED)
        }
        finish()
    }
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && !scannerLaunched) {
            launchQrScanner()
        } else if (!isGranted) {
            // Permission denied, finish activity
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check camera permission before launching scanner
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            if (!scannerLaunched) {
                launchQrScanner()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun launchQrScanner() {
        if (scannerLaunched) {
            return // Prevent duplicate launches
        }
        scannerLaunched = true
        
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Scan your Pause QR code")
            .setBeepEnabled(false)
            .setBarcodeImageEnabled(false)
            .setCameraId(0) // Prefer back camera
            .setOrientationLocked(true) // reduce layout churn
            .setTimeout(30000) // 30 second timeout to prevent hanging
            .setCaptureActivity(CaptureActivity::class.java)

        launcher.launch(options)
    }
}


