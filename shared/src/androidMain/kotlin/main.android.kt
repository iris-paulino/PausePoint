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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.net.Uri
import android.app.AppOpsManager

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
    // Enforce accessibility service requirement before starting tracking
    val activity = currentActivityRef?.get()
    val accessibilityEnabled = isAccessibilityServiceEnabled()
    println("DEBUG: startUsageTracking - accessibilityEnabled=$accessibilityEnabled, packages=$trackedPackages, limit=$limitMinutes")
    if (!accessibilityEnabled) {
        // Inform user and route to settings; do not start tracking
        try {
            if (activity != null) {
                android.widget.Toast.makeText(
                    activity,
                    "Enable Accessibility for ScrollPause to start tracking.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } catch (_: Exception) {}
        openAccessibilitySettings()
        return
    }

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
            // Instead of showing overlay, redirect to our pause screen
            val intent = Intent().apply {
                setClassName(activity.packageName, "com.luminoprisma.scrollpause.PauseOverlayActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("message", message)
            }
            activity.startActivity(intent)
            println("DEBUG: showBlockingOverlay - redirected to PauseOverlayActivity with message: $message")
        } catch (e: Exception) {
            println("DEBUG: showBlockingOverlay - error redirecting to pause screen: ${e.message}")
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
            // Set dismissing state in MainActivity to prevent camera permission requests
            try {
                val method = activity::class.java.methods.firstOrNull { it.name == "setDismissingState" && it.parameterTypes.size == 1 && it.parameterTypes[0] == Boolean::class.java }
                if (method != null) {
                    method.invoke(activity, true)
                    println("DEBUG: dismissBlockingOverlay - set dismissing state in MainActivity")
                }
            } catch (e: Exception) {
                println("DEBUG: dismissBlockingOverlay - error setting dismissing state: ${e.message}")
            }
            
            // Send broadcast to finish any running QrScanActivity
            val qrScanIntent = Intent("com.luminoprisma.scrollpause.FINISH_QR_SCAN_ACTIVITY").apply {
                setPackage(activity.packageName) // Explicitly set the package
            }
            activity.sendBroadcast(qrScanIntent)
            println("DEBUG: dismissBlockingOverlay - sent FINISH_QR_SCAN_ACTIVITY broadcast with package: ${activity.packageName}")
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
            // Reset dismissing state in MainActivity
            try {
                val method = activity::class.java.methods.firstOrNull { it.name == "setDismissingState" && it.parameterTypes.size == 1 && it.parameterTypes[0] == Boolean::class.java }
                if (method != null) {
                    method.invoke(activity, false)
                    println("DEBUG: resetTimerAndContinueTracking - reset dismissing state in MainActivity")
                }
            } catch (e: Exception) {
                println("DEBUG: resetTimerAndContinueTracking - error resetting dismissing state: ${e.message}")
            }
            
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
            // Keep dismissing state for a longer period to prevent camera permission requests
            // from Settings screen polling that happens after dismiss
            try {
                val method = activity::class.java.methods.firstOrNull { it.name == "setDismissingState" && it.parameterTypes.size == 1 && it.parameterTypes[0] == Boolean::class.java }
                if (method != null) {
                    // Don't reset immediately - let it stay true for a bit longer
                    // The state will be reset when the user actually interacts with the app
                    println("DEBUG: dismissAndContinueTracking - keeping dismissing state true to prevent camera permission requests")
                }
            } catch (e: Exception) {
                println("DEBUG: dismissAndContinueTracking - error managing dismissing state: ${e.message}")
            }
            
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
    
    // Suppress overlay while QR scanner is active to avoid bouncing back to pause screen
    if (isQrScanningActive) {
        println("DEBUG: checkAndShowOverlayIfBlocked - scanning active, suppressing overlay")
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
    
    // Also set the callback in MainActivity so the broadcast receiver can call it
    val activity = currentActivityRef?.get()
    if (activity != null) {
        try {
            val method = activity::class.java.methods.firstOrNull { it.name == "setDismissCallback" && it.parameterTypes.size == 1 }
            if (method != null) {
                method.invoke(activity, callback)
                println("DEBUG: setOnDismissCallback - set callback in MainActivity")
            }
        } catch (e: Exception) {
            println("DEBUG: setOnDismissCallback - error setting callback in MainActivity: ${e.message}")
        }
    }
}

actual fun updateAccessibilityServiceBlockedState(isBlocked: Boolean, trackedAppNames: List<String>, timeLimitMinutes: Int) {
    println("DEBUG: updateAccessibilityServiceBlockedState - blocked: $isBlocked, apps: $trackedAppNames, limit: $timeLimitMinutes")
    try {
        // Choose a safe context (application if activity missing)
        val activity = currentActivityRef?.get()
        val ctx: Context? = activity?.applicationContext ?: appContextRef

        // Persist state so the AccessibilityService can restore it after app UI is killed
        if (ctx != null) {
            try {
                val prefs = ctx.getSharedPreferences("scrollpause_prefs", Context.MODE_PRIVATE)
                val csv = trackedAppNames.joinToString(",")
                prefs.edit()
                    .putBoolean("blocked", isBlocked)
                    .putString("tracked_apps_csv", csv)
                    .putInt("time_limit_minutes", timeLimitMinutes)
                    .apply()
                println("DEBUG: updateAccessibilityServiceBlockedState - persisted to SharedPreferences (app ctx): blocked=$isBlocked, apps=$csv, limit=$timeLimitMinutes")
            } catch (e: Exception) {
                println("DEBUG: updateAccessibilityServiceBlockedState - prefs persist error: ${e.message}")
            }
        } else {
            println("DEBUG: updateAccessibilityServiceBlockedState - no context to persist prefs")
        }

        // Send broadcast to notify accessibility service of state change
        if (ctx != null) {
            try {
                val intent = Intent("com.luminoprisma.scrollpause.STATE_CHANGED").apply {
                    setPackage(ctx.packageName)
                    putExtra("isBlocked", isBlocked)
                    putExtra("trackedApps", trackedAppNames.joinToString(","))
                    putExtra("timeLimit", timeLimitMinutes)
                }
                ctx.sendBroadcast(intent)
                println("DEBUG: updateAccessibilityServiceBlockedState - sent STATE_CHANGED broadcast (app ctx)")
            } catch (e: Exception) {
                println("DEBUG: updateAccessibilityServiceBlockedState - error sending broadcast: ${e.message}")
            }
        } else {
            println("DEBUG: updateAccessibilityServiceBlockedState - no context to send broadcast")
        }
    } catch (e: Exception) {
        println("DEBUG: updateAccessibilityServiceBlockedState - error updating accessibility service: ${e.message}")
    }
}

@Suppress("unused")
actual fun openLastTrackedApp(trackedAppIdentifiers: List<String>) {
    try {
        val activity = currentActivityRef?.get() ?: return
        // Try to launch the most recent tracked app seen by the Accessibility Service
        val pkg = ForegroundAppCache.getLastTrackedForegroundPackage(trackedAppIdentifiers)
        if (pkg != null) {
            val intent = activity.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
                println("DEBUG: openLastTrackedApp - launched $pkg")
                return
            }
        }
        println("DEBUG: openLastTrackedApp - no tracked package to launch")
    } catch (e: Exception) {
        println("DEBUG: openLastTrackedApp - error: ${e.message}")
    }
}

@Suppress("unused")
actual fun showCongratulationsOverlay() {
    try {
        val activity = currentActivityRef?.get()
        val ctx = activity?.applicationContext ?: appContextRef
        val intent = android.content.Intent().apply {
            setClassName(
                ctx?.packageName ?: "com.luminoprisma.scrollpause",
                "com.luminoprisma.scrollpause.CongratulationsOverlayActivity"
            )
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        ctx?.startActivity(intent)
        println("DEBUG: showCongratulationsOverlay - launched overlay activity")
    } catch (e: Exception) {
        println("DEBUG: showCongratulationsOverlay - error: ${e.message}")
    }
}

actual fun showStreakMilestone(milestone: String) {
    try {
        val activity = currentActivityRef?.get()
        val ctx = activity?.applicationContext ?: appContextRef
        val intent = android.content.Intent().apply {
            setClassName(
                ctx?.packageName ?: "com.luminoprisma.scrollpause",
                "com.luminoprisma.scrollpause.StreakMilestoneActivity"
            )
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("milestone", milestone)
        }
        ctx?.startActivity(intent)
        println("DEBUG: showStreakMilestone - launched milestone activity with: $milestone")
    } catch (e: Exception) {
        println("DEBUG: showStreakMilestone - error: ${e.message}")
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

actual fun hasCameraPermission(): Boolean {
    val activity = currentActivityRef?.get() ?: return false
    return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

actual fun requestCameraPermission(): Boolean {
    val activity = currentActivityRef?.get() ?: return false
    return try {
        // Check dismissing state first before doing anything
        try {
            val dismissingField = activity::class.java.declaredFields.firstOrNull { it.name == "isDismissing" }
            if (dismissingField != null) {
                dismissingField.isAccessible = true
                val isDismissing = dismissingField.getBoolean(activity)
                if (isDismissing) {
                    println("DEBUG: requestCameraPermission - called but app is dismissing, skipping")
                    return false
                }
            }
        } catch (e: Exception) {
            println("DEBUG: requestCameraPermission - error checking dismissing state: ${e.message}")
        }
        
        // Reset dismissing state when user explicitly requests camera permission
        try {
            val resetMethod = activity::class.java.methods.firstOrNull { it.name == "resetDismissingStateIfNeeded" && it.parameterTypes.isEmpty() }
            if (resetMethod != null) {
                resetMethod.invoke(activity)
                println("DEBUG: requestCameraPermission - reset dismissing state before requesting permission")
            }
        } catch (e: Exception) {
            println("DEBUG: requestCameraPermission - error resetting dismissing state: ${e.message}")
        }
        
        // Delegate to MainActivity helper if available to use ActivityResult API
        val method = activity::class.java.methods.firstOrNull { it.name == "requestCameraPermissionIfNeeded" && it.parameterTypes.isEmpty() }
        if (method != null) {
            val result = method.invoke(activity) as? Boolean
            result ?: false
        } else {
            // Fallback: if not granted, cannot synchronously request here; return current status
            hasCameraPermission()
        }
    } catch (_: Exception) {
        hasCameraPermission()
    }
}

actual fun openAppSettingsForCamera() {
    val activity = currentActivityRef?.get() ?: return
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    } catch (_: Exception) {}
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

actual fun openUsageAccessSettings() {
    val activity = currentActivityRef?.get() ?: return
    try {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        activity.startActivity(intent)
        println("DEBUG: openUsageAccessSettings - opened usage access settings")
    } catch (e: Exception) {
        println("DEBUG: openUsageAccessSettings - error: ${e.message}")
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
    // Register accessibility status change receiver
    ensureAccessibilityReceiverRegistered(activity.applicationContext)
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

    fun getLastTrackedForegroundPackage(identifiers: List<String>): String? {
        val pkg = currentPackage ?: return null
        // identifiers may include names and package ids; match by package id subset
        val isMatch = identifiers.any { id ->
            val normalized = id.trim()
            normalized.isNotEmpty() && (normalized == pkg || (normalized.contains('.') && pkg == normalized))
        }
        return if (isMatch) pkg else null
    }
}

private var receiverRegistered = false
private var accessibilityReceiverRegistered = false
private var usageAccessReceiverRegistered = false
private var appContextRef: Context? = null
private var accessibilityStatusCallback: ((Boolean) -> Unit)? = null
private var usageAccessStatusCallback: ((Boolean) -> Unit)? = null

private fun ensureReceiverRegistered(context: Context) {
    if (receiverRegistered) return
    try {
        val filter = IntentFilter("com.luminoprisma.scrollpause.FOREGROUND_APP_CHANGED")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val pkg = intent?.getStringExtra("pkg")
                ForegroundAppCache.currentPackage = pkg
                println("DEBUG: ForegroundAppCache - received pkg: $pkg")
                println("DEBUG: ForegroundAppCache - appChangeCallback is null: ${appChangeCallback == null}")
                
                // Call the app change callback for event-driven tracking
                appChangeCallback?.invoke(pkg)
                println("DEBUG: ForegroundAppCache - callback invoked with pkg: $pkg")
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

private fun ensureAccessibilityReceiverRegistered(context: Context) {
    if (accessibilityReceiverRegistered) return
    try {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                println("DEBUG: AccessibilityReceiver - received intent: ${intent?.action}")
                // Check accessibility status when package changes occur
                val isEnabled = isAccessibilityServiceEnabled()
                println("DEBUG: AccessibilityReceiver - accessibility status changed to: $isEnabled")
                accessibilityStatusCallback?.invoke(isEnabled)
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        accessibilityReceiverRegistered = true
        println("DEBUG: AccessibilityReceiver - receiver registered with context=$context")
    } catch (e: Exception) {
        println("DEBUG: AccessibilityReceiver - register error: ${e.message}")
    }
}

private fun ensureUsageAccessReceiverRegistered(context: Context) {
    if (usageAccessReceiverRegistered) return
    try {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                println("DEBUG: UsageAccessReceiver - received intent: ${intent?.action}")
                // Check usage access status when package changes occur
                val isGranted = isUsageAccessPermissionGranted()
                println("DEBUG: UsageAccessReceiver - usage access status changed to: $isGranted")
                usageAccessStatusCallback?.invoke(isGranted)
            }
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        usageAccessReceiverRegistered = true
        println("DEBUG: UsageAccessReceiver - receiver registered with context=$context")
    } catch (e: Exception) {
        println("DEBUG: UsageAccessReceiver - register error: ${e.message}")
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

actual fun showAccessibilityDisabledNotification() {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: showAccessibilityDisabledNotification - no activity available")
        return
    }
    
    try {
        // Use the new notification manager for better user experience
        val notificationManager = Class.forName("com.luminoprisma.scrollpause.BlockingNotificationManager")
            .getDeclaredConstructor(Context::class.java)
            .newInstance(activity) as Any
        
        val showMethod = notificationManager::class.java.getDeclaredMethod("showAccessibilityDisabledNotification")
        showMethod.invoke(notificationManager)
        
        println("DEBUG: showAccessibilityDisabledNotification - notification shown")
    } catch (e: Exception) {
        // Fallback to toast if notification manager fails
        try {
            android.widget.Toast.makeText(
                activity,
                "Tracking stopped: Accessibility access was disabled. Please re-enable it in Settings to continue tracking.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (toastException: Exception) {
            println("DEBUG: showAccessibilityDisabledNotification - error: ${e.message}")
        }
    }
}

actual fun showUsageAccessDisabledNotification() {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: showUsageAccessDisabledNotification - no activity available")
        return
    }
    
    try {
        // Use the new notification manager for better user experience
        val notificationManager = Class.forName("com.luminoprisma.scrollpause.BlockingNotificationManager")
            .getDeclaredConstructor(Context::class.java)
            .newInstance(activity) as Any
        
        val showMethod = notificationManager::class.java.getDeclaredMethod("showUsageAccessDisabledNotification")
        showMethod.invoke(notificationManager)
        
        println("DEBUG: showUsageAccessDisabledNotification - notification shown")
    } catch (e: Exception) {
        // Fallback to toast if notification manager fails
        try {
            android.widget.Toast.makeText(
                activity,
                "Tracking stopped: App usage access was disabled. Please re-enable it in Settings to continue tracking.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (toastException: Exception) {
            println("DEBUG: showUsageAccessDisabledNotification - error: ${e.message}")
        }
    }
}

private var appChangeCallback: ((String?) -> Unit)? = null

actual fun setOnAppChangeCallback(callback: ((String?) -> Unit)?) {
    appChangeCallback = callback
    println("DEBUG: setOnAppChangeCallback - callback registered: ${callback != null}")
    
    // Also ensure receiver is registered when callback is set
    val activity = currentActivityRef?.get()
    val ctx = activity?.applicationContext ?: appContextRef
    if (ctx != null) {
        ensureReceiverRegistered(ctx)
        println("DEBUG: setOnAppChangeCallback - ensured receiver is registered")
    } else {
        println("DEBUG: setOnAppChangeCallback - no context available to register receiver")
    }
}

actual fun setOnAccessibilityStatusChangeCallback(callback: ((Boolean) -> Unit)?) {
    accessibilityStatusCallback = callback
    println("DEBUG: setOnAccessibilityStatusChangeCallback - callback registered: ${callback != null}")
    
    // Also ensure accessibility receiver is registered when callback is set
    val activity = currentActivityRef?.get()
    val ctx = activity?.applicationContext ?: appContextRef
    if (ctx != null) {
        ensureAccessibilityReceiverRegistered(ctx)
        println("DEBUG: setOnAccessibilityStatusChangeCallback - ensured accessibility receiver is registered")
    } else {
        println("DEBUG: setOnAccessibilityStatusChangeCallback - no context available to register receiver")
    }
}

actual fun setOnUsageAccessStatusChangeCallback(callback: ((Boolean) -> Unit)?) {
    usageAccessStatusCallback = callback
    println("DEBUG: setOnUsageAccessStatusChangeCallback - callback registered: ${callback != null}")
    
    // Also ensure usage access receiver is registered when callback is set
    val activity = currentActivityRef?.get()
    val ctx = activity?.applicationContext ?: appContextRef
    if (ctx != null) {
        ensureUsageAccessReceiverRegistered(ctx)
        println("DEBUG: setOnUsageAccessStatusChangeCallback - ensured usage access receiver is registered")
    } else {
        println("DEBUG: setOnUsageAccessStatusChangeCallback - no context available to register receiver")
    }
}

// Start periodic accessibility monitoring to catch changes that BroadcastReceiver might miss
actual fun startAccessibilityMonitoring() {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: startAccessibilityMonitoring - no activity available")
        return
    }
    
    // This will be called from the main app to start monitoring
    // The actual monitoring is done in the main app's LaunchedEffect
    println("DEBUG: startAccessibilityMonitoring - monitoring started")
}

// Check if usage access permission is actually granted by the system
actual fun isUsageAccessPermissionGranted(): Boolean {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: isUsageAccessPermissionGranted - no activity available")
        return false
    }
    
    return try {
        val appOps = activity.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            activity.packageName
        )
        val isGranted = mode == AppOpsManager.MODE_ALLOWED
        println("DEBUG: isUsageAccessPermissionGranted - mode: $mode, granted: $isGranted, packageName: ${activity.packageName}")
        isGranted
    } catch (e: Exception) {
        println("DEBUG: isUsageAccessPermissionGranted - error: ${e.message}")
        false
    }
}

// Platform-specific implementations for service management
actual fun startAppMonitoringForegroundService() {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: startAppMonitoringForegroundService - no activity available")
        return
    }
    
    try {
        // Use reflection to call the service start method
        val serviceClass = Class.forName("com.luminoprisma.scrollpause.AppMonitoringForegroundService")
        val startMethod = serviceClass.getDeclaredMethod("startService", Context::class.java)
        startMethod.invoke(null, activity)
        println("DEBUG: startAppMonitoringForegroundService - service started")
    } catch (e: Exception) {
        println("DEBUG: startAppMonitoringForegroundService - error: ${e.message}")
    }
}

actual fun stopAppMonitoringForegroundService() {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: stopAppMonitoringForegroundService - no activity available")
        return
    }
    
    try {
        // Use reflection to call the service stop method
        val serviceClass = Class.forName("com.luminoprisma.scrollpause.AppMonitoringForegroundService")
        val stopMethod = serviceClass.getDeclaredMethod("stopService", Context::class.java)
        stopMethod.invoke(null, activity)
        println("DEBUG: stopAppMonitoringForegroundService - service stopped")
    } catch (e: Exception) {
        println("DEBUG: stopAppMonitoringForegroundService - error: ${e.message}")
    }
}

actual fun saveTrackingStateForRestart(isTracking: Boolean, isBlocked: Boolean, trackedApps: List<String>, timeLimit: Int) {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: saveTrackingStateForRestart - no activity available")
        return
    }
    
    try {
        // Use reflection to call the restart detector save method
        val detectorClass = Class.forName("com.luminoprisma.scrollpause.AppRestartDetector")
        val constructor = detectorClass.getDeclaredConstructor(Context::class.java)
        val detector = constructor.newInstance(activity)
        val saveMethod = detectorClass.getDeclaredMethod("saveTrackingState", Boolean::class.java, Boolean::class.java, List::class.java, Int::class.java)
        saveMethod.invoke(detector, isTracking, isBlocked, trackedApps, timeLimit)
        println("DEBUG: saveTrackingStateForRestart - state saved")
    } catch (e: Exception) {
        println("DEBUG: saveTrackingStateForRestart - error: ${e.message}")
    }
}

actual fun showPersistentBlockingNotification(trackedApps: List<String>, timeLimit: Int) {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: showPersistentBlockingNotification - no activity available")
        return
    }
    
    try {
        // Use reflection to call the notification manager
        val managerClass = Class.forName("com.luminoprisma.scrollpause.BlockingNotificationManager")
        val constructor = managerClass.getDeclaredConstructor(Context::class.java)
        val manager = constructor.newInstance(activity)
        val showMethod = managerClass.getDeclaredMethod("showPersistentBlockingNotification", List::class.java, Int::class.java)
        showMethod.invoke(manager, trackedApps, timeLimit)
        println("DEBUG: showPersistentBlockingNotification - showed notification for ${trackedApps.size} apps")
    } catch (e: Exception) {
        println("DEBUG: showPersistentBlockingNotification - error: ${e.message}")
    }
}

actual fun clearPersistentBlockingNotification() {
    val activity = currentActivityRef?.get()
    if (activity == null) {
        println("DEBUG: clearPersistentBlockingNotification - no activity available")
        return
    }
    
    try {
        // Use reflection to call the notification manager
        val managerClass = Class.forName("com.luminoprisma.scrollpause.BlockingNotificationManager")
        val constructor = managerClass.getDeclaredConstructor(Context::class.java)
        val manager = constructor.newInstance(activity)
        val clearMethod = managerClass.getDeclaredMethod("clearPersistentBlockingNotification")
        clearMethod.invoke(manager)
        println("DEBUG: clearPersistentBlockingNotification - cleared notification")
    } catch (e: Exception) {
        println("DEBUG: clearPersistentBlockingNotification - error: ${e.message}")
    }
}

actual fun startCompliantAppBlocking(trackedApps: List<String>, timeLimit: Int) {
    try {
        val activity = currentActivityRef?.get()
        if (activity != null) {
            // Use reflection to call the compliant app blocking service
            val serviceClass = Class.forName("com.luminoprisma.scrollpause.CompliantBlockingService")
            val getInstanceMethod = serviceClass.getDeclaredMethod("getInstance", Context::class.java)
            val blockingService = getInstanceMethod.invoke(null, activity)
            val startBlockingMethod = serviceClass.getDeclaredMethod("startBlocking", List::class.java, Int::class.java)
            startBlockingMethod.invoke(blockingService, trackedApps, timeLimit)
            println("DEBUG: startCompliantAppBlocking - started blocking ${trackedApps.size} apps")
        } else {
            println("DEBUG: startCompliantAppBlocking - no activity available")
        }
    } catch (e: Exception) {
        println("DEBUG: startCompliantAppBlocking - error: ${e.message}")
    }
}

actual fun stopCompliantAppBlocking() {
    try {
        val activity = currentActivityRef?.get()
        if (activity != null) {
            // Use reflection to call the compliant app blocking service
            val serviceClass = Class.forName("com.luminoprisma.scrollpause.CompliantBlockingService")
            val getInstanceMethod = serviceClass.getDeclaredMethod("getInstance", Context::class.java)
            val blockingService = getInstanceMethod.invoke(null, activity)
            val stopBlockingMethod = serviceClass.getDeclaredMethod("stopBlocking")
            stopBlockingMethod.invoke(blockingService)
            println("DEBUG: stopCompliantAppBlocking - stopped blocking")
        } else {
            println("DEBUG: stopCompliantAppBlocking - no activity available")
        }
    } catch (e: Exception) {
        println("DEBUG: stopCompliantAppBlocking - error: ${e.message}")
    }
}

actual fun isCompliantAppBlockingEnabled(): Boolean {
    return try {
        val activity = currentActivityRef?.get()
        if (activity != null) {
            // Use reflection to call the compliant app blocking service
            val serviceClass = Class.forName("com.luminoprisma.scrollpause.CompliantBlockingService")
            val getInstanceMethod = serviceClass.getDeclaredMethod("getInstance", Context::class.java)
            val blockingService = getInstanceMethod.invoke(null, activity)
            val isBlockingMethod = serviceClass.getDeclaredMethod("isBlocking")
            isBlockingMethod.invoke(blockingService) as Boolean
        } else {
            false
        }
    } catch (e: Exception) {
        println("DEBUG: isCompliantAppBlockingEnabled - error: ${e.message}")
        false
    }
}

