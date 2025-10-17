import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PersistentTrackingConsentDialog(
    isVisible: Boolean,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
            Card(
                backgroundColor = Color(0xFF1A1A1A),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🔒 Enhanced Digital Wellbeing Protection",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = "Enable enhanced tracking that continues working even when ScrollPause is closed.",
                        fontSize = 15.sp,
                        color = Color(0xFFCCCCCC),
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Card(
                        backgroundColor = Color(0xFF2A2A2A),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "📱 What this enables:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "• Continue tracking and blocking apps when ScrollPause is closed",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• Show pause screens when time limits are reached",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• Auto-resume tracking after app restarts",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                        }
                    }
                    
                    Text(
                        text = "All data stays on your device. You can disable this anytime in Android Settings.",
                        fontSize = 13.sp,
                        color = Color(0xFF999999),
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onAllow,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text(
                                "Enable Accessibility Service",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(3.dp))
                    
                    TextButton(
                        onClick = onDeny,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Use Standard Mode Only",
                            color = Color(0xFFCCCCCC),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

