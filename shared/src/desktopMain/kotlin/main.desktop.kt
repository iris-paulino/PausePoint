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
    // Desktop implementation - save as a simple text file for now
    val file = File(System.getProperty("user.home"), "Downloads/pausepoint_qr_${System.currentTimeMillis()}.txt")
    file.parentFile?.mkdirs()
    FileWriter(file).use { writer ->
        writer.write("PausePoint QR Code\n")
        writer.write("Message: $message\n")
        writer.write("QR Text: $qrText\n")
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