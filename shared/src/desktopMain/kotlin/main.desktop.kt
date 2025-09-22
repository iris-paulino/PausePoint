import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileWriter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.Color
import java.awt.image.BufferedImage

actual fun getPlatformName(): String = "Desktop"

@Composable fun MainView() = App()

@Preview
@Composable
fun AppPreview() {
    App()
}

actual fun saveQrPdf(qrText: String, message: String): String {
    // Desktop implementation - save as a text file with QR code representation
    // In a production app, you would use a PDF library like iText or Apache PDFBox
    val file = File(System.getProperty("user.home"), "Downloads/pausepoint_qr_${System.currentTimeMillis()}.txt")
    file.parentFile?.mkdirs()
    FileWriter(file).use { writer ->
        writer.write("PausePoint QR Code\n")
        writer.write("==================\n\n")
        writer.write("Message: $message\n")
        writer.write("QR Text: $qrText\n\n")
        
        // Generate a simple text-based QR representation
        writer.write("QR Code (Text Representation):\n")
        writer.write("┌─────────────────────────────────┐\n")
        writer.write("│  ████  ██  ████  ██  ████  ██  │\n")
        writer.write("│  ██  ██  ██  ██  ██  ██  ██  ██  │\n")
        writer.write("│  ████  ██  ████  ██  ████  ██  │\n")
        writer.write("│  ██  ██  ██  ██  ██  ██  ██  ██  │\n")
        writer.write("│  ████  ██  ████  ██  ████  ██  │\n")
        writer.write("│  ██  ██  ██  ██  ██  ██  ██  ██  │\n")
        writer.write("│  ████  ██  ████  ██  ████  ██  │\n")
        writer.write("│  ██  ██  ██  ██  ██  ██  ██  ██  │\n")
        writer.write("│  ████  ██  ████  ██  ████  ██  │\n")
        writer.write("└─────────────────────────────────┘\n\n")
        
        writer.write("Instructions:\n")
        writer.write("1. Print this file\n")
        writer.write("2. Place the printed QR code around your home\n")
        writer.write("3. When your app time limit is reached, walk to scan this QR code\n")
        writer.write("4. Use any QR code scanner app to scan the pattern above\n")
    }
    return file.absolutePath
}

actual fun startUsageTracking(
    trackedPackages: List<String>,
    limitMinutes: Int,
    onLimitReached: () -> Unit
) {
    // Desktop implementation - placeholder
}

actual fun showBlockingOverlay(message: String) {
    // Desktop implementation - placeholder
}

actual fun scanQrAndDismiss(expectedMessage: String): Boolean {
    // Desktop implementation - placeholder
    // This function should:
    // 1. Open camera for QR scanning (could use webcam libraries)
    // 2. Scan QR code and get the text
    // 3. Validate the scanned text against saved QR codes using storage.validateQrCode()
    // 4. Return true if valid QR code is found and matches expected message
    return false
}

actual fun getCurrentTimeMillis(): Long {
    return System.currentTimeMillis()
}

private fun generateQrImage(text: String, size: Int): BufferedImage? {
    return try {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val matrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val graphics = image.graphics
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, size, size)
        graphics.color = Color.BLACK
        val cell = size / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    graphics.fillRect(x * cell, y * cell, cell, cell)
                }
            }
        }
        image
    } catch (e: Exception) { null }
}

@Composable
actual fun QrCodeDisplay(text: String, modifier: Modifier) {
    val px = with(LocalDensity.current) { 200.dp.roundToPx() }
    val img = generateQrImage(text, px)
    if (img != null) {
        Image(img.toComposeImageBitmap(), contentDescription = "QR", modifier = modifier)
    }
}