import androidx.compose.runtime.Composable
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

actual fun getPlatformName(): String = "Android"

@Composable fun MainView() = App()

// Generate QR code bitmap
private fun generateQrCodeBitmap(text: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val cellSize = size / bitMatrix.width
        
        for (y in 0 until bitMatrix.height) {
            for (x in 0 until bitMatrix.width) {
                if (bitMatrix[x, y]) {
                    canvas.drawRect(
                        x * cellSize.toFloat(),
                        y * cellSize.toFloat(),
                        (x + 1) * cellSize.toFloat(),
                        (y + 1) * cellSize.toFloat(),
                        paint
                    )
                }
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

// Actual QR code display on Android
@Composable
actual fun QrCodeDisplay(text: String, modifier: Modifier) {
    val sizePx = with(LocalDensity.current) { 200.dp.roundToPx() }
    val bmp = generateQrCodeBitmap(text, sizePx)
    if (bmp != null) {
        Image(bmp.asImageBitmap(), contentDescription = "QR", modifier = modifier)
    }
}

// PDF saver that renders an actual QR code image. Returns file path.
actual fun saveQrPdf(qrText: String, message: String): String {
    // Save under app-specific external files directory if available
    val directory = File(Environment.getExternalStorageDirectory(), "Download")
    if (!directory.exists()) directory.mkdirs()
    val file = File(directory, "pausepoint_qr_${System.currentTimeMillis()}.pdf")

    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint().apply { 
        textSize = 28f
        isFakeBoldText = true
    }
    
    // Title
    canvas.drawText("PausePoint QR Code", 72f, 96f, paint)
    
    // Message
    paint.textSize = 20f
    paint.isFakeBoldText = false
    canvas.drawText("Message: $message", 72f, 140f, paint)
    
    // Generate and draw QR code image
    val qrBitmap = generateQrCodeBitmap(qrText, 200)
    if (qrBitmap != null) {
        // Draw QR code image
        canvas.drawBitmap(qrBitmap, 72f, 180f, null)
        
        // Instructions below QR code
        paint.textSize = 14f
        canvas.drawText("Instructions:", 72f, 400f, paint)
        canvas.drawText("1. Print this page", 72f, 430f, paint)
        canvas.drawText("2. Place the printed QR code around your home", 72f, 460f, paint)
        canvas.drawText("3. When your app time limit is reached,", 72f, 490f, paint)
        canvas.drawText("   walk to scan this QR code to unlock apps", 72f, 520f, paint)
    } else {
        // Fallback to text if QR generation fails
        paint.textSize = 16f
        canvas.drawText("QR Code: $qrText", 72f, 180f, paint)
        
        paint.textSize = 14f
        canvas.drawText("Instructions:", 72f, 240f, paint)
        canvas.drawText("1. Print this page", 72f, 270f, paint)
        canvas.drawText("2. Place the printed QR code around your home", 72f, 300f, paint)
        canvas.drawText("3. When your app time limit is reached,", 72f, 330f, paint)
        canvas.drawText("   walk to scan this QR code to unlock apps", 72f, 360f, paint)
    }
    
    pdf.finishPage(page)
    FileOutputStream(file).use { out -> pdf.writeTo(out) }
    pdf.close()
    return file.absolutePath
}

actual fun startUsageTracking(
    trackedPackages: List<String>,
    limitMinutes: Int,
    onLimitReached: () -> Unit
) {
    // TODO: Implement using UsageStatsManager/AccessibilityService. Placeholder timer.
    // No-op in this sample.
}

actual fun showBlockingOverlay(message: String) {
    // TODO: Implement using Activity/Window overlay or a full-screen Activity with FLAG_SHOW_WHEN_LOCKED.
}

actual fun scanQrAndDismiss(expectedMessage: String): Boolean {
    // TODO: Implement using ML Kit barcode scanning or CameraX; return whether QR matches.
    // This function should:
    // 1. Open camera for QR scanning
    // 2. Scan QR code and get the text
    // 3. Validate the scanned text against saved QR codes using storage.validateQrCode()
    // 4. Return true if valid QR code is found and matches expected message
    return false
}

actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}
