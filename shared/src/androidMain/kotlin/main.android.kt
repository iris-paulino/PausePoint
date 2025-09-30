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
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build

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
    canvas.drawText("Scroll Pause QR Code", 72f, 96f, paint)
    
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
    println("DEBUG: showBlockingOverlay called with message: $message")
    val activity = currentActivityRef?.get()
    if (activity != null) {
        try {
            // Use broadcast receiver to show the overlay
            val intent = Intent("com.luminoprisma.scrollpause.SHOW_BLOCKING_OVERLAY").apply {
                putExtra("message", message)
                setPackage(activity.packageName) // Explicitly set the package
            }
            activity.sendBroadcast(intent)
            println("DEBUG: showBlockingOverlay - sent SHOW_BLOCKING_OVERLAY broadcast with package: ${activity.packageName}")
        } catch (e: Exception) {
            println("DEBUG: showBlockingOverlay - error sending broadcast: ${e.message}")
        }
    } else {
        println("DEBUG: showBlockingOverlay - no activity available")
    }
}

actual fun dismissBlockingOverlay() {
    println("DEBUG: dismissBlockingOverlay called")
    val activity = currentActivityRef?.get()
    if (activity != null) {
        try {
            // Use broadcast receiver to hide the overlay
            val intent = Intent("com.luminoprisma.scrollpause.HIDE_BLOCKING_OVERLAY").apply {
                setPackage(activity.packageName) // Explicitly set the package
            }
            activity.sendBroadcast(intent)
            println("DEBUG: dismissBlockingOverlay - sent HIDE_BLOCKING_OVERLAY broadcast with package: ${activity.packageName}")
        } catch (e: Exception) {
            println("DEBUG: dismissBlockingOverlay - error sending broadcast: ${e.message}")
        }
    } else {
        println("DEBUG: dismissBlockingOverlay - no activity available")
    }
}

fun resetTimerAndContinueTracking() {
    println("DEBUG: resetTimerAndContinueTracking called")
    
    // Call the callback if it's set
    onTimerResetCallback?.invoke()
    
    val activity = currentActivityRef?.get()
    if (activity != null) {
        try {
            // Use broadcast receiver to reset timer and continue tracking
            val intent = Intent("com.luminoprisma.scrollpause.RESET_TIMER_AND_CONTINUE").apply {
                setPackage(activity.packageName) // Explicitly set the package
            }
            activity.sendBroadcast(intent)
            println("DEBUG: resetTimerAndContinueTracking - sent RESET_TIMER_AND_CONTINUE broadcast with package: ${activity.packageName}")
        } catch (e: Exception) {
            println("DEBUG: resetTimerAndContinueTracking - error sending broadcast: ${e.message}")
        }
    } else {
        println("DEBUG: resetTimerAndContinueTracking - no activity available")
    }
}

fun dismissAndContinueTracking() {
    println("DEBUG: dismissAndContinueTracking called")
    
    // Call the dismiss callback if it's set
    onDismissCallback?.invoke()
    
    val activity = currentActivityRef?.get()
    if (activity != null) {
        try {
            // Use broadcast receiver to dismiss and continue tracking
            val intent = Intent("com.luminoprisma.scrollpause.RESET_TIMER_AND_CONTINUE").apply {
                setPackage(activity.packageName) // Explicitly set the package
            }
            activity.sendBroadcast(intent)
            println("DEBUG: dismissAndContinueTracking - sent RESET_TIMER_AND_CONTINUE broadcast with package: ${activity.packageName}")
        } catch (e: Exception) {
            println("DEBUG: dismissAndContinueTracking - error sending broadcast: ${e.message}")
        }
    } else {
        println("DEBUG: dismissAndContinueTracking - no activity available")
    }
}

actual fun checkAndShowOverlayIfBlocked(trackedAppNames: List<String>, isBlocked: Boolean, timeLimitMinutes: Int) {
    if (!isBlocked) return
    
    println("DEBUG: checkAndShowOverlayIfBlocked called - isBlocked: $isBlocked, isQrScanningActive: $isQrScanningActive")
    
    // Don't show overlay if QR scanning is currently active
    if (isQrScanningActive) {
        println("DEBUG: checkAndShowOverlayIfBlocked - QR scanning is active, not showing overlay")
        return
    }
    
    // Get the current foreground app using the existing expect/actual function
    val currentForegroundApp = getCurrentForegroundApp()
    println("DEBUG: checkAndShowOverlayIfBlocked - currentForegroundApp: $currentForegroundApp")
    
    if (currentForegroundApp != null) {
        // Check if the current foreground app is one of the tracked apps
        val isTrackedApp = trackedAppNames.any { appName ->
            val expectedPackage = when (appName.lowercase()) {
                "chrome" -> "com.android.chrome"
                "youtube" -> "com.google.android.youtube"
                "messages" -> "com.google.android.apps.messaging"
                "gmail" -> "com.google.android.gm"
                "whatsapp" -> "com.whatsapp"
                "youtube music" -> "com.google.android.apps.youtube.music"
                else -> appName.lowercase().replace(" ", "")
            }
            currentForegroundApp == expectedPackage
        }
        
        println("DEBUG: checkAndShowOverlayIfBlocked - isTrackedApp: $isTrackedApp")
        
        if (isTrackedApp) {
            // User is trying to use a tracked app while blocked, show overlay
            println("DEBUG: checkAndShowOverlayIfBlocked - showing overlay for blocked tracked app")
            showBlockingOverlay("Take a mindful pause - you've reached your time limit of ${timeLimitMinutes} minutes")
        }
    }
}

private var currentActivityRef: WeakReference<Activity>? = null
fun registerCurrentActivity(activity: Activity) { currentActivityRef = WeakReference(activity) }

private var isQrScanningActive = false
fun setQrScanningActive(active: Boolean) { isQrScanningActive = active }

private var onTimerResetCallback: (() -> Unit)? = null
private var onDismissCallback: (() -> Unit)? = null

actual suspend fun scanQrAndDismiss(expectedMessage: String): Boolean {
    val activity = currentActivityRef?.get() ?: return false

    return suspendCancellableCoroutine { cont ->
        try {
            // Receiver to capture scan result
            val filter = IntentFilter("com.luminoprisma.scrollpause.QR_SCAN_RESULT")
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        activity.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    val qrText = intent?.getStringExtra("qr_text")
                    // Treat any non-empty QR text as a valid scan
                    val ok = !qrText.isNullOrEmpty()
                    if (!cont.isCompleted) cont.resume(ok) {}
                }
            }
            activity.registerReceiver(receiver, filter)

            // Launch QR scanner activity and pass expected message for validation
            val intent = Intent(activity, Class.forName("com.luminoprisma.scrollpause.QrScanActivity")).apply {
                putExtra("expected_message", expectedMessage)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            if (!cont.isCompleted) cont.resume(false) {}
        }
    }
}


actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}

actual fun setOnTimerResetCallback(callback: (() -> Unit)?) {
    onTimerResetCallback = callback
}

actual fun setOnDismissCallback(callback: (() -> Unit)?) {
    onDismissCallback = callback
}

actual fun updateAccessibilityServiceBlockedState(isBlocked: Boolean, trackedAppNames: List<String>, timeLimitMinutes: Int) {
    println("DEBUG: updateAccessibilityServiceBlockedState - blocked: $isBlocked, apps: $trackedAppNames, limit: $timeLimitMinutes")
    try {
        // Use reflection to call the accessibility service method safely
        val serviceClass = Class.forName("com.luminoprisma.scrollpause.ForegroundAppAccessibilityService")
        val companionClass = serviceClass.getDeclaredClasses().find { it.simpleName == "Companion" }
        if (companionClass != null) {
            val companionInstance = companionClass.getDeclaredField("INSTANCE").get(null)
            val setBlockedStateMethod = companionClass.getDeclaredMethod("setBlockedState", Boolean::class.java, List::class.java, Int::class.java)
            setBlockedStateMethod.invoke(companionInstance, isBlocked, trackedAppNames, timeLimitMinutes)
            println("DEBUG: updateAccessibilityServiceBlockedState - successfully updated accessibility service")
        } else {
            println("DEBUG: updateAccessibilityServiceBlockedState - could not find Companion class")
        }
    } catch (e: Exception) {
        println("DEBUG: updateAccessibilityServiceBlockedState - error updating accessibility service: ${e.message}")
    }
}

actual fun openEmailClient(recipient: String) {
    val activity = currentActivityRef?.get() ?: return
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:$recipient")
        }
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        } else {
            // Fallback to generic email intent if no email app is available
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            }
            activity.startActivity(Intent.createChooser(fallbackIntent, "Send Email"))
        }
    } catch (e: Exception) {
        println("DEBUG: openEmailClient - error opening email client: ${e.message}")
    }
}

actual fun openAccessibilitySettings() {
    val activity = currentActivityRef?.get() ?: return
    try {
        // Try to open the specific accessibility service settings first
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        // Add the specific component name to navigate directly to our app's service
        val pkg = activity.packageName
        val componentName = ComponentName(pkg, "$pkg.ForegroundAppAccessibilityService")
        intent.putExtra("componentName", componentName.flattenToString())
        activity.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to general accessibility settings if specific navigation fails
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            activity.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

actual fun isAccessibilityServiceEnabled(): Boolean {
    val activity = currentActivityRef?.get()
    println("DEBUG: isAccessibilityServiceEnabled - activity: $activity")
    if (activity == null) {
        println("DEBUG: isAccessibilityServiceEnabled - no activity, returning false")
        return false
    }
    // cache application context for receiver registration later
    appContextRef = activity.applicationContext
    val pkg = activity.packageName
    
    // Create both possible component name formats that Android might use
    val expectedWithDot = ComponentName(pkg, ".ForegroundAppAccessibilityService")
    val expectedWithFullPackage = ComponentName(pkg, "$pkg.ForegroundAppAccessibilityService")
    
    val enabledServices = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    println("DEBUG: isAccessibilityServiceEnabled - pkg: $pkg")
    println("DEBUG: isAccessibilityServiceEnabled - expectedWithDot: ${expectedWithDot.flattenToString()}")
    println("DEBUG: isAccessibilityServiceEnabled - expectedWithFullPackage: ${expectedWithFullPackage.flattenToString()}")
    println("DEBUG: isAccessibilityServiceEnabled - enabledServices: $enabledServices")
    
    if (enabledServices.isNullOrEmpty()) {
        println("DEBUG: isAccessibilityServiceEnabled - no enabled services, returning false")
        return false
    }
    
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        println("DEBUG: isAccessibilityServiceEnabled - checking component: $componentName")
        
        // Check for exact matches with both formats
        if (componentName.equals(expectedWithDot.flattenToString(), ignoreCase = true) ||
            componentName.equals(expectedWithFullPackage.flattenToString(), ignoreCase = true) ||
            // Also check for partial matches as fallback
            componentName.endsWith("/ForegroundAppAccessibilityService", ignoreCase = true) ||
            (componentName.contains(pkg, ignoreCase = true) && componentName.contains("ForegroundAppAccessibilityService", ignoreCase = true))
        ) {
            println("DEBUG: isAccessibilityServiceEnabled - found matching service: $componentName, returning true")
            return true
        }
    }
    println("DEBUG: isAccessibilityServiceEnabled - no matching service found, returning false")
    return false
}

private object ForegroundAppCache {
    @Volatile
    var currentPackage: String? = null
}

private var receiverRegistered = false
private var appContextRef: Context? = null

private fun ensureReceiverRegistered(context: Context) {
    if (receiverRegistered) return
    try {
        val filter = IntentFilter("com.luminoprisma.scrollpause.FOREGROUND_APP_CHANGED")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val pkg = intent?.getStringExtra("pkg")
                ForegroundAppCache.currentPackage = pkg
                println("DEBUG: ForegroundAppCache - received pkg: $pkg")
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
        println("DEBUG: ForegroundAppCache - receiver registered with context=$context")
    } catch (e: Exception) {
        println("DEBUG: ForegroundAppCache - register error: ${e.message}")
    }
}

actual fun getCurrentForegroundApp(): String? {
    println("DEBUG: *** ANDROID FUNCTION CALLED *** getCurrentForegroundApp - function called")
    val activity = currentActivityRef?.get()
    val ctx = activity?.applicationContext ?: appContextRef
    if (ctx != null) {
        ensureReceiverRegistered(ctx)
    } else {
        println("DEBUG: getCurrentForegroundApp - no context available to register receiver")
    }
    val cached = ForegroundAppCache.currentPackage
    println("DEBUG: getCurrentForegroundApp - cached: $cached")
    return cached
}

private fun getCurrentForegroundAppFallback(activity: Activity): String? {
    return try {
        val activityManager = activity.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningProcesses = activityManager.getRunningAppProcesses()
        
        println("DEBUG: getCurrentForegroundAppFallback - found ${runningProcesses?.size ?: 0} running processes")
        runningProcesses?.forEach { process ->
            println("DEBUG: getCurrentForegroundAppFallback - process: ${process.processName}, importance: ${process.importance}")
        }
        
        // Find the process with the highest importance (most likely to be foreground)
        val ourPackageName = activity.packageName
        val foregroundApp = runningProcesses
            ?.filter { it.processName != ourPackageName } // Exclude our own app
            ?.maxByOrNull { it.importance } // Get the process with highest importance
            ?.processName
        
        println("DEBUG: getCurrentForegroundAppFallback - our package: $ourPackageName, returning: $foregroundApp")
        foregroundApp
    } catch (e: Exception) {
        println("DEBUG: getCurrentForegroundAppFallback - error: ${e.message}")
        null
    }
}
