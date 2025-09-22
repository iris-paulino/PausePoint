import androidx.compose.ui.window.ComposeUIViewController

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
}

actual fun showBlockingOverlay(message: String) {
    // Could present a full-screen Compose VC from the root.
}

actual fun scanQrAndDismiss(expectedMessage: String): Boolean {
    // Could use AVFoundation barcode scanning via interop. Placeholder.
    return false
}