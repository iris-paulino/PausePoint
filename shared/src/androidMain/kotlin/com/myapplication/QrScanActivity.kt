package com.myapplication

import android.app.Activity
import android.os.Bundle

/**
 * Minimal placeholder activity for QR scanning.
 * This immediately finishes without a result so the app can build and run.
 * Replace with a real scanner UI as needed.
 */
class QrScanActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        finish()
    }
}


