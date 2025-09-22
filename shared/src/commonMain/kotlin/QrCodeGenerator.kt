import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.common.BitMatrix

@Composable
fun QrCodeDisplay(
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val bitMatrix = remember(text) { 
        generateQrCodeMatrix(text, 200) // Fixed size for QR generation
    }
    
    bitMatrix?.let { matrix ->
        Canvas(
            modifier = modifier
        ) {
            drawQrCode(this, matrix, size.value.toInt())
        }
    }
}

private fun generateQrCodeMatrix(text: String, size: Int): BitMatrix? {
    return try {
        val writer = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        
        writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    } catch (e: Exception) {
        null
    }
}

private fun drawQrCode(drawScope: DrawScope, bitMatrix: BitMatrix, @Suppress("UNUSED_PARAMETER") size: Int) {
    val canvasSize = drawScope.size.minDimension
    val cellSize = canvasSize / bitMatrix.width
    
    // Draw white background
    drawScope.drawRect(
        color = Color.White,
        topLeft = androidx.compose.ui.geometry.Offset.Zero,
        size = drawScope.size
    )
    
    // Calculate offset to center the QR code
    val qrSize = bitMatrix.width * cellSize
    val offsetX = (drawScope.size.width - qrSize) / 2
    val offsetY = (drawScope.size.height - qrSize) / 2
    
    // Draw QR code pattern
    for (y in 0 until bitMatrix.height) {
        for (x in 0 until bitMatrix.width) {
            if (bitMatrix[x, y]) {
                drawScope.drawRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        offsetX + x * cellSize,
                        offsetY + y * cellSize
                    ),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                )
            }
        }
    }
}