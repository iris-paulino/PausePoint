package com.prismappsau.screengo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class QrScanActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Scan your Pause QR code")
            .setBeepEnabled(false)
            .setOrientationLocked(false)
            .setCaptureActivity(CaptureActivity::class.java)

        launcher.launch(options)
    }
}


