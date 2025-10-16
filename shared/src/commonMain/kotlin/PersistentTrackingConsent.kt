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
                        text = "ScrollPause uses Android's Accessibility Service to provide enhanced digital wellbeing features that continue working even when the main app is closed.",
                        fontSize = 15.sp,
                        color = Color(0xFFCCCCCC),
                        lineHeight = 22.sp
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
                                text = "📱 How Accessibility Service is Used:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "• Monitor which apps you're currently using",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• Show pause screens when time limits are reached",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• Continue blocking apps even when ScrollPause is closed",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• Help you maintain healthy digital habits",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                        }
                    }
                    
                    Text(
                        text = "What this means:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✓ Your app usage continues to be tracked",
                            fontSize = 14.sp,
                            color = Color(0xFFCCCCCC)
                        )
                        Text(
                            text = "✓ Blocked apps remain blocked",
                            fontSize = 14.sp,
                            color = Color(0xFFCCCCCC)
                        )
                        Text(
                            text = "✓ Receive notifications about blocked apps",
                            fontSize = 14.sp,
                            color = Color(0xFFCCCCCC)
                        )
                        Text(
                            text = "✓ Tracking auto-resumes after app restarts",
                            fontSize = 14.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                    
                    Card(
                        backgroundColor = Color(0xFF2A2A2A),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "🔒 Privacy & Control",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "• All app usage data stays on your device",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• No personal data is collected or transmitted",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• You can disable accessibility service anytime",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                            Text(
                                text = "• Only monitors app names, not app content",
                                fontSize = 12.sp,
                                color = Color(0xFFCCCCCC)
                            )
                        }
                    }
                    
                    Text(
                        text = "You can disable this feature anytime in Android Settings > Accessibility Services.",
                        fontSize = 13.sp,
                        color = Color(0xFF999999),
                        lineHeight = 18.sp
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Button(
                        onClick = onAllow,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF6200EE)
                        )
                    ) {
                        Text(
                            "Enable Accessibility Service",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
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

