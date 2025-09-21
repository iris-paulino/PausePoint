import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@Composable
fun App() {
    MaterialTheme(
        colors = androidx.compose.material.MaterialTheme.colors.copy(
            background = Color(0xFF1A1A1A),
            surface = Color(0xFF2C2C2C),
            primary = Color(0xFF4CAF50),
            onBackground = Color.White,
            onSurface = Color.White,
            onPrimary = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF1A1A1A)
        ) {
            Box(Modifier.fillMaxSize()) {
                AppRoot()
            }
        }
    }
}

// Simple navigation and app state holder
private enum class Route { Onboarding, QrGenerator, Dashboard }

private data class TrackedApp(
    val name: String,
    val minutesUsed: Int,
    val limitMinutes: Int
)

@Composable
private fun AppRoot() {
    var route by remember { mutableStateOf(Route.Onboarding) }
    var qrMessage by remember { mutableStateOf("Take a mindful pause") }
    var qrId by remember { mutableStateOf<String?>(null) }
    var trackedApps by remember {
        mutableStateOf(
            listOf(
                TrackedApp("Instagram", 45, 60),
                TrackedApp("TikTok", 32, 45),
                TrackedApp("Facebook", 18, 30)
            )
        )
    }

    when (route) {  
        Route.Onboarding -> OnboardingFlow(
            onGenerateQr = { route = Route.QrGenerator }
        )
        Route.QrGenerator -> QrGeneratorScreen(
            message = qrMessage,
            onMessageChange = { qrMessage = it },
            onQrCreated = { id ->
                qrId = id
                route = Route.Dashboard
            }
        )
        Route.Dashboard -> DashboardScreen(
            qrId = qrId,
            message = qrMessage,
            trackedApps = trackedApps,
            onPauseTracking = {
                // Placeholder: would toggle background tracking in platform layer
            },
            onOpenQrGenerator = { route = Route.QrGenerator }
        )
    }
}

@Composable
private fun OnboardingFlow(onGenerateQr: () -> Unit) {
    OnboardingPager(
        pages = listOf(
            OnboardingPage(
                title = "Take Mindful Breaks",
                description = "Set limits on your app usage and take meaningful pauses when you reach them."
            ),
            OnboardingPage(
                title = "Walk to Unlock Apps",
                description = "Print QR codes and place them around your home. When time is up, you'll need to physically walk to scan them."
            ),
            OnboardingPage(
                title = "Accountability Partners",
                description = "Add trusted contacts as accountability partners. They can generate QR codes to help you unlock apps."
            ),
            OnboardingPage(
                title = "Create Your First QR Code",
                description = "Generate your first QR code to get started. You can print it and place it somewhere in your home.",
                primaryCta = "Get Started"
            )
        ),
        onDone = onGenerateQr
    )
}

// QR generator screen
@Composable
private fun QrGeneratorScreen(
    message: String,
    onMessageChange: (String) -> Unit,
    onQrCreated: (String) -> Unit
) {
    QrGeneratorContent(
        message = message,
        onMessageChange = onMessageChange,
        onDownloadPdf = { text ->
            // Save a PDF using a platform stub and then continue
            saveQrPdf(qrText = text, message = message)
        },
        onGenerate = {
            // Create a simple unique id
            val id = "pause-${System.currentTimeMillis()}"
            onQrCreated(id)
        }
    )
}

// Dashboard
@Composable
private fun DashboardScreen(
    qrId: String?,
    message: String,
    trackedApps: List<TrackedApp>,
    onPauseTracking: () -> Unit,
    onOpenQrGenerator: () -> Unit
) {
    DashboardContent(
        qrId = qrId ?: "",
        message = message,
        trackedApps = trackedApps,
        onPauseTracking = onPauseTracking,
        onOpenQrGenerator = onOpenQrGenerator
    )
}

// Expect declarations implemented per platform
expect fun getPlatformName(): String
expect fun saveQrPdf(qrText: String, message: String): String
// Extension points for platform features (no-op defaults in platform sources)
expect fun startUsageTracking(
    trackedPackages: List<String>,
    limitMinutes: Int,
    onLimitReached: () -> Unit
)
expect fun showBlockingOverlay(message: String)
expect fun scanQrAndDismiss(expectedMessage: String): Boolean

// UI implementations for onboarding, QR, and dashboard live in this file for brevity
// Lightweight, dependency-free visuals only

@Immutable
private data class OnboardingPage(
    val title: String,
    val description: String,
    val primaryCta: String = "Next"
)

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun OnboardingPager(
    pages: List<OnboardingPage>,
    onDone: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    val page = pages[index]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { i ->
                Box(
                    modifier = Modifier
                        .width(if (i == index) 24.dp else 8.dp)
                        .height(8.dp)
                        .background(
                            if (i == index) Color(0xFF6EE7B7) else Color(0xFF4B5563),
                            RoundedCornerShape(4.dp)
                        )
                )
                if (i < pages.lastIndex) Spacer(Modifier.width(8.dp))
            }
        }

        // Main content card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF222625),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Illustration - person walking on path through hills
                Image(
                    painter = painterResource("images/onboarding/mindful_breaks.png"),
                    contentDescription = "Mindful breaks illustration",
                    modifier = Modifier.size(200.dp)
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = page.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = page.description,
                    fontSize = 16.sp,
                    color = Color(0xFFD1D5DB),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Action buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (index < pages.lastIndex) index++ else onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6EE7B7)),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text(
                    page.primaryCta,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold
                ) 
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Skip",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onDone() },
                color = Color(0xFFD1D5DB)
            )
        }
    }
}

@Composable
private fun QrGeneratorContent(
    message: String,
    onMessageChange: (String) -> Unit,
    onDownloadPdf: (String) -> Unit,
    onGenerate: () -> Unit
) {
    var qrVersion by remember { mutableStateOf(0) } // Start at 0 so no QR shows initially
    var hasGeneratedQr by remember { mutableStateOf(false) }
    val qrText = remember(message, qrVersion) { "QR:$message:v$qrVersion" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("QR Code Generator", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Create QR codes to place around your home", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // QR Code Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("▦", fontSize = 16.sp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Your QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Spacer(Modifier.height(16.dp))
                
                // QR Code - only shows after generation
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasGeneratedQr) {
                        QrCodeDisplay(
                            text = qrText,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Click 'Generate New QR Code' to create your QR", 
                             color = Color(0xFF8C9C8D), 
                             fontSize = 14.sp,
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                
                if (hasGeneratedQr) {
                    Spacer(Modifier.height(12.dp))
                    Text(message, color = Color(0xFF4CAF50), fontSize = 16.sp)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ID: pause-${System.currentTimeMillis().toString().takeLast(6)}...", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("⧉", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Customize Message Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Customize Message", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("Personal Message", fontSize = 14.sp, color = Color(0xFFD1D5DB))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        textColor = Color.White,
                        cursorColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Download Button - only enabled when QR is generated
        Button(
            onClick = { onDownloadPdf(qrText) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (hasGeneratedQr) Color(0xFF4CAF50) else Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            enabled = hasGeneratedQr
        ) {
            Text("↓", color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Download PDF for Printing", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Generate New Button
        Button(
            onClick = { 
                qrVersion++
                hasGeneratedQr = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text("↻", color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Generate New QR Code", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Continue to Dashboard Button - only enabled when QR is generated
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (hasGeneratedQr) Color(0xFF4CAF50) else Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            enabled = hasGeneratedQr
        ) {
            Text("✓", color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Continue to Dashboard", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DashboardContent(
    qrId: String,
    message: String,
    trackedApps: List<TrackedApp>,
    onPauseTracking: () -> Unit,
    onOpenQrGenerator: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("PausePoint", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Friday, Sep 19", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
            Row {
                Text("☀", fontSize = 24.sp, color = Color(0xFFD1D5DB))
                Spacer(Modifier.width(16.dp))
                Text("⚙", fontSize = 24.sp, color = Color(0xFFD1D5DB))
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Current Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕐", fontSize = 16.sp, color = Color(0xFF4CAF50))
                        Spacer(Modifier.width(8.dp))
                        Text("Current Status", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Apps Available", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Time remaining display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF388E3C), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0m", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("until combined limit is reached", fontSize = 14.sp, color = Color.White)
                        Text("All apps share a 15m total limit", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("2", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Text("times unblocked today", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1h 35m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Text("total usage today", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Pause tracking button
                Button(
                    onClick = onPauseTracking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3A3A3A)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text("⏸", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Pause Tracking", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // App Usage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 16.sp, color = Color(0xFF4CAF50))
                        Spacer(Modifier.width(8.dp))
                        Text("App Usage", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text("+", fontSize = 24.sp, color = Color.White)
                }
                
                Spacer(Modifier.height(16.dp))
                
                trackedApps.forEach { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.name, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${app.minutesUsed}m today", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("✏", color = Color(0xFFFF5252), fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Text("🗑", color = Color(0xFFFF5252), fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val percent = (app.minutesUsed.toFloat() / app.limitMinutes.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color(0xFF4B5563), RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = percent)
                                    .height(8.dp)
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Quick Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Quick Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onOpenQrGenerator,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("▦", color = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("👥", color = Color.White)
                    }
                }
            }
        }
    }
}