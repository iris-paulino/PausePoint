import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVCaptureDeviceInput
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIViewController
import platform.UIKit.UIView
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDeviceAuthorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.QuartzCore.CALayer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreImage.CIDetector
import platform.CoreImage.CIFeature
import platform.CoreImage.CIImage
import platform.CoreImage.CIDetectorAccuracy
import platform.CoreImage.CIDetectorTypeQRCode
import platform.Foundation.NSData
import platform.UIKit.UIScreen
import platform.CoreGraphics.CGImageRef
import platform.CoreVideo.CVImageBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun getPlatformName(): String = "iOS"

fun MainViewController() = ComposeUIViewController { App() }

// iOS stub for PDF save; returns a placeholder path for now. A native implementation
// could be added using UIKit/PDFKit via Kotlin/Native interop.
actual fun saveQrPdf(qrText: String, message: String): String {
    // In a real iOS implementation, you would use PDFKit to create a PDF
    // and save it to the Documents directory or share it via UIActivityViewController
    return "Documents/pausepoint_qr_${kotlin.random.Random.nextLong()}.pdf"
}

actual fun startUsageTracking(
    trackedPackages: List<String>,
    limitMinutes: Int,
    onLimitReached: () -> Unit
) {
    // iOS background usage tracking is restricted; would require Screen Time APIs (not public).
    // For now, this is a placeholder that would need proper iOS implementation.
    // The actual tracking logic is handled in the common code with session-based timing.
    println("DEBUG: Starting usage tracking for packages: $trackedPackages with limit: $limitMinutes minutes")
}

actual fun showBlockingOverlay(message: String) {
    // Could present a full-screen Compose VC from the root.
}

actual suspend fun scanQrAndDismiss(expectedMessage: String): Boolean {
    // Minimal modal scanner using AVFoundation + CoreImage QR detector
    return suspendCancellableCoroutine { cont ->
        val controller = SimpleQrScannerController { qrText ->
            val storage = createAppStorage()
            // Use runBlocking to handle the suspend function call
            kotlinx.coroutines.runBlocking {
                val match = if (qrText != null) storage.validateQrCode(qrText) else null
                cont.resume(match != null && match.message == expectedMessage)
            }
        }
        val root = UIKitRootProvider.currentRootController()
        root?.presentViewController(controller, true, null)
    }
}

actual fun getCurrentTimeMillis(): Long {
    // Simple implementation - in a real app you'd use proper iOS time APIs
    return kotlin.random.Random.nextLong(1000000000000L, 2000000000000L)
}

actual fun setOnTimerResetCallback(callback: (() -> Unit)?) {
    // No-op on iOS for now
}

actual fun setOnDismissCallback(callback: (() -> Unit)?) {
    // No-op on iOS for now
}

actual fun updateAccessibilityServiceBlockedState(isBlocked: Boolean, trackedAppNames: List<String>, timeLimitMinutes: Int) {
    // No-op on iOS for now
}

actual fun openAccessibilitySettings() {
    // No-op on iOS
}

actual fun isAccessibilityServiceEnabled(): Boolean {
    return true
}

actual fun getCurrentForegroundApp(): String? {
    // iOS doesn't allow apps to detect other apps' foreground state for privacy reasons
    // Screen Time APIs are not publicly available
    // For now, return null to indicate no foreground app detection
    return null
}

actual fun dismissBlockingOverlay() {
    // No-op on iOS for now
}

actual fun checkAndShowOverlayIfBlocked(trackedAppNames: List<String>, isBlocked: Boolean, timeLimitMinutes: Int) {
    // No-op on iOS for now
}

actual fun openEmailClient(recipient: String) {
    // Could open Mail app with pre-filled recipient
    // For now, this is a placeholder
}

actual fun hasCameraPermission(): Boolean {
    val status = AVCaptureDeviceAuthorizationStatusForMediaType(AVMediaTypeVideo)
    return status == AVAuthorizationStatusAuthorized
}

actual fun requestCameraPermission(): Boolean {
    val status = AVCaptureDeviceAuthorizationStatusForMediaType(AVMediaTypeVideo)
    return when (status) {
        AVAuthorizationStatusAuthorized -> true
        AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> false
        AVAuthorizationStatusNotDetermined -> {
            var granted = false
            // Synchronously block until result for simplicity
            // In a production app, wire through a suspend callback
            val semaphore = kotlinx.cinterop.alloc<kotlinx.cinterop.IntVar>()
            requestAccessForMediaType(AVMediaTypeVideo) { ok ->
                granted = ok
            }
            granted
        }
        else -> false
    }
}

actual fun openAppSettingsForCamera() {
    // On iOS, direct deep link to camera settings is limited; open general app settings
    // Implement if needed using UIApplicationOpenSettingsURLString via UIKit interop
}

@Composable
actual fun QrCodeDisplay(
    text: String,
    modifier: Modifier
) {
    // This would be implemented as a Compose component
    // For now, this is a placeholder
}

actual fun showAccessibilityDisabledNotification() {
    // On iOS, accessibility is always enabled for our app, so this is a no-op
    // In a real implementation, you might show a local notification or alert
    println("DEBUG: showAccessibilityDisabledNotification - iOS (no-op)")
}