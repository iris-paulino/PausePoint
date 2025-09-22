import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import java.io.File
import java.io.FileWriter

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
    return false
}