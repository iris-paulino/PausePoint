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
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.content.ComponentName
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

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
    // TODO: Implement using UsageStatsManager/AccessibilityService for real app usage tracking.
    // For now, this is a placeholder that would need proper Android permissions and implementation.
    // The actual tracking logic is handled in the common code with session-based timing.
    println("DEBUG: Starting usage tracking for packages: $trackedPackages with limit: $limitMinutes minutes")
    
    // In a real implementation, you would:
    // 1. Request USAGE_STATS permission
    // 2. Use UsageStatsManager to query app usage
    // 3. Monitor app foreground/background events
    // 4. Calculate actual usage time per app
    // 5. Call onLimitReached when limits are exceeded
}

actual fun showBlockingOverlay(message: String) {
    // TODO: Implement using Activity/Window overlay or a full-screen Activity with FLAG_SHOW_WHEN_LOCKED.
}

actual suspend fun scanQrAndDismiss(expectedMessage: String): Boolean {
    // Launch a small activity that wraps ZXing scanner and await result
    val activity = currentActivityRef?.get() ?: return false
    val scanned = suspendCancellableCoroutine<String?> { cont ->
        QrResultHolder.continuation = cont
        @Suppress("DEPRECATION")
        activity.startActivityForResult(Intent(activity, com.myapplication.QrScanActivity::class.java), QR_REQUEST_CODE)
    }
    val storage = createAppStorage()
    val match = scanned?.let { storage.validateQrCode(it) }
    return match != null && match.message == expectedMessage
}

private const val QR_REQUEST_CODE = 9001

private var currentActivityRef: WeakReference<Activity>? = null
fun registerCurrentActivity(activity: Activity) { currentActivityRef = WeakReference(activity) }

object QrResultHolder {
    var continuation: kotlinx.coroutines.CancellableContinuation<String?>? = null
}

fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == QR_REQUEST_CODE) {
        val text = if (resultCode == Activity.RESULT_OK) data?.getStringExtra("qr_text") else null
        QrResultHolder.continuation?.resume(text)
        QrResultHolder.continuation = null
    }
}

actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}

actual fun openAccessibilitySettings() {
    val activity = currentActivityRef?.get() ?: return
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivity(intent)
    } catch (_: Exception) {
    }
}

actual fun isAccessibilityServiceEnabled(): Boolean {
    val activity = currentActivityRef?.get()
    println("DEBUG: isAccessibilityServiceEnabled - activity: $activity")
    if (activity == null) {
        println("DEBUG: isAccessibilityServiceEnabled - no activity, returning false")
        return false
    }
    val pkg = activity.packageName
    val expected = ComponentName(pkg, "$pkg.ForegroundAppAccessibilityService")
    val enabledServices = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    println("DEBUG: isAccessibilityServiceEnabled - pkg: $pkg, expected: $expected, enabledServices: $enabledServices")
    if (enabledServices.isNullOrEmpty()) {
        println("DEBUG: isAccessibilityServiceEnabled - no enabled services, returning false")
        return false
    }
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        if (componentName.equals(expected.flattenToString(), ignoreCase = true) ||
            componentName.endsWith("/ForegroundAppAccessibilityService", ignoreCase = true) ||
            componentName.contains(pkg, ignoreCase = true) && componentName.contains("ForegroundAppAccessibilityService", ignoreCase = true)
        ) {
            println("DEBUG: isAccessibilityServiceEnabled - found matching service, returning true")
            return true
        }
    }
    println("DEBUG: isAccessibilityServiceEnabled - no matching service found, returning false")
    return false
}

actual fun getCurrentForegroundApp(): String? {
    // TODO: Implement using ActivityManager or AccessibilityService to get current foreground app
    // For now, return null to indicate no foreground app detection
    // In a real implementation, you would:
    // 1. Use ActivityManager.getRunningTasks() (deprecated but still works)
    // 2. Use UsageStatsManager.queryUsageStats() for recent usage
    // 3. Use AccessibilityService to monitor window state changes
    // 4. Use ActivityManager.getRunningAppProcesses() and check importance
    return null
}
