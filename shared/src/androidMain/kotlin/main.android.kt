import androidx.compose.runtime.Composable
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

actual fun getPlatformName(): String = "Android"

@Composable fun MainView() = App()

// Very simple PDF saver that renders a placeholder QR box as text; in a real
// implementation we would render a bitmap of the QR image. Returns file path.
actual fun saveQrPdf(qrText: String, message: String): String {
    // Save under app-specific external files directory if available
    val directory = File(Environment.getExternalStorageDirectory(), "Download")
    if (!directory.exists()) directory.mkdirs()
    val file = File(directory, "pausepoint_qr_${System.currentTimeMillis()}.pdf")

    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint().apply { textSize = 24f }
    canvas.drawText("PausePoint QR", 72f, 96f, paint)
    paint.textSize = 18f
    canvas.drawText(message, 72f, 140f, paint)
    paint.textSize = 14f
    canvas.drawText(qrText, 72f, 180f, paint)
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
    return false
}
