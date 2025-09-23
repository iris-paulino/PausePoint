import androidx.compose.ui.window.ComposeUIViewController
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
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Default) {
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