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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
private enum class Route { Onboarding, QrGenerator, Dashboard, AppSelection, DurationSetting, Pause, Settings, SavedQrCodes }

private data class TrackedApp(
    val name: String,
    val minutesUsed: Int,
    val limitMinutes: Int
)

private data class AvailableApp(
    val name: String,
    val category: String,
    val icon: String,
    val packageName: String,
    val isSelected: Boolean = false
)

@Composable
private fun AppRoot() {
    var route by remember { mutableStateOf<Route?>(null) } // Start with null to show loading
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
    
    var availableApps by remember { mutableStateOf<List<AvailableApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    
    var timeLimitMinutes by remember { mutableStateOf(15) }
    
    val storage = remember { createAppStorage() }
    val installedAppsProvider = remember { createInstalledAppsProvider() }
    val coroutineScope = rememberCoroutineScope()
    
    var isTracking by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    // Drive a simple combined-usage timer while tracking is active
    LaunchedEffect(isTracking) {
        if (!isTracking) return@LaunchedEffect
        while (isTracking) {
            delay(1000)
            elapsedSeconds += 1
            val minutes = (elapsedSeconds / 60)
            // Update tracked apps' minutesUsed with combined usage
            trackedApps = trackedApps.map { it.copy(minutesUsed = minutes.coerceAtMost(it.limitMinutes)) }
            // If limit reached for combined usage, show Pause and stop tracking
            if (minutes >= timeLimitMinutes) {
                isTracking = false
                // Navigate to Pause screen
                route = Route.Pause
            }
        }
    }

    // Function to set up default apps
    suspend fun setupDefaultApps() {
        isLoadingApps = true
        try {
            val installedApps = installedAppsProvider.getInstalledApps()
            println("DEBUG: Found ${installedApps.size} installed apps")
            
            // Define default apps to track (same list as in AppSelection)
            val defaultAppNames = listOf("Instagram", "TikTok", "Snapchat", "Chrome", "YouTube")
            
            // Filter to only include installed default apps
            val defaultTrackedApps = installedApps
                .filter { app -> defaultAppNames.any { defaultName -> 
                    app.appName.contains(defaultName, ignoreCase = true) || 
                    defaultName.contains(app.appName, ignoreCase = true)
                }}
                .map { app -> TrackedApp(app.appName, 0, 15) } // 15 minutes default
            
            trackedApps = defaultTrackedApps
            println("DEBUG: Set up ${trackedApps.size} default tracked apps")
            // Persist selected packages for defaults
            val selectedPackages = installedApps.filter { app ->
                defaultAppNames.any { defaultName ->
                    app.appName.contains(defaultName, ignoreCase = true) ||
                    defaultName.contains(app.appName, ignoreCase = true)
                }
            }.map { it.packageName }
            try { storage.saveSelectedAppPackages(selectedPackages) } catch (_: Exception) {}
            
        } catch (e: Exception) {
            println("DEBUG: Exception occurred while loading apps: ${e.message}")
            e.printStackTrace()
            // Fallback to some default apps even if detection fails
            trackedApps = listOf(
                TrackedApp("Instagram", 0, 15),
                TrackedApp("TikTok", 0, 15),
                TrackedApp("Snapchat", 0, 15),
                TrackedApp("Chrome", 0, 15),
                TrackedApp("YouTube", 0, 15)
            )
            // Persist fallback package identifiers so the choice survives restarts
            val fallbackPackages = listOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.snapchat.android",
                "com.android.chrome",
                "com.google.android.youtube"
            )
            try { storage.saveSelectedAppPackages(fallbackPackages) } catch (_: Exception) {}
        } finally {
            isLoadingApps = false
        }
    }
    
    // Check onboarding completion status on app start
    LaunchedEffect(Unit) {
        try {
            // Add a timeout to prevent hanging
            val isOnboardingCompleted = withTimeoutOrNull(5000) {
                storage.isOnboardingCompleted()
            } ?: false // Default to false if timeout occurs
            
            if (isOnboardingCompleted) {
                // Load persisted selections and time limit
                val savedTime = withTimeoutOrNull(3000) { storage.getTimeLimitMinutes() } ?: 15
                timeLimitMinutes = savedTime

                val savedPackages = withTimeoutOrNull(3000) { storage.getSelectedAppPackages() } ?: emptyList()
                if (savedPackages.isNotEmpty()) {
                    try {
                        val installed = installedAppsProvider.getInstalledApps()
                        val selected = installed.filter { it.packageName in savedPackages.toSet() }
                        trackedApps = selected.map { TrackedApp(it.appName, 0, timeLimitMinutes) }
                        // Pre-populate available apps to reflect saved selection when opening selection screen later
                        availableApps = installed.map { installedApp ->
                            val selectedSet = savedPackages.toSet()
                            AvailableApp(
                                name = installedApp.appName,
                                category = installedApp.category,
                                icon = installedApp.icon,
                                packageName = installedApp.packageName,
                                isSelected = installedApp.packageName in selectedSet
                            )
                        }
                    } catch (_: Exception) {
                        // Fallback to defaults if loading installed apps fails
                        setupDefaultApps()
                    }
                } else {
                    // No saved selection: set up defaults
                    setupDefaultApps()
                }
                route = Route.Dashboard
            } else {
                // If onboarding is not completed, show onboarding
                route = Route.Onboarding
            }
        } catch (e: Exception) {
            // If storage fails, default to onboarding
            route = Route.Onboarding
        }
    }
    
    // Load installed apps when navigating to AppSelection
    LaunchedEffect(route) {
        if (route == Route.AppSelection && !isLoadingApps) {
            // Reset apps when navigating to AppSelection to ensure fresh load
            if (availableApps.isEmpty()) {
                isLoadingApps = true
                try {
                    val installedApps = installedAppsProvider.getInstalledApps()
                    // Debug: Print the number of apps found
                    println("DEBUG: Found ${installedApps.size} installed apps")
                    if (installedApps.isNotEmpty()) {
                        // Use persisted package selections if available
                        val savedPackages = try { storage.getSelectedAppPackages() } catch (_: Exception) { emptyList() }
                        val savedSet = savedPackages.toSet()
                        availableApps = installedApps.map { installedApp ->
                            val isSelected = if (savedSet.isNotEmpty()) installedApp.packageName in savedSet else {
                                // Fallback to matching by current tracked apps
                                trackedApps.any { tracked ->
                                    tracked.name.equals(installedApp.appName, ignoreCase = true) ||
                                    tracked.name.contains(installedApp.appName, ignoreCase = true) ||
                                    installedApp.appName.contains(tracked.name, ignoreCase = true)
                                }
                            }
                            AvailableApp(
                                name = installedApp.appName,
                                category = installedApp.category,
                                icon = installedApp.icon,
                                packageName = installedApp.packageName,
                                isSelected = isSelected
                            )
                        }
                        println("DEBUG: Loaded ${availableApps.size} apps for selection")
                    } else {
                        // If no apps are detected, provide some common fallback apps
                        println("DEBUG: No apps detected, using fallback apps")
                        val fallback = listOf(
                            AvailableApp("Instagram", "Social Media", "📷", "com.instagram.android"),
                            AvailableApp("TikTok", "Social Media", "🎵", "com.zhiliaoapp.musically"),
                            AvailableApp("Facebook", "Social Media", "📘", "com.facebook.katana"),
                            AvailableApp("Twitter", "Social Media", "🐦", "com.twitter.android"),
                            AvailableApp("YouTube", "Entertainment", "📺", "com.google.android.youtube"),
                            AvailableApp("Snapchat", "Social Media", "👻", "com.snapchat.android"),
                            AvailableApp("Reddit", "Social Media", "🤖", "com.reddit.frontpage"),
                            AvailableApp("LinkedIn", "Professional", "💼", "com.linkedin.android")
                        )
                        availableApps = fallback.map { app ->
                            val isTracked = trackedApps.any { tracked ->
                                tracked.name.equals(app.name, ignoreCase = true) ||
                                tracked.name.contains(app.name, ignoreCase = true) ||
                                app.name.contains(tracked.name, ignoreCase = true)
                            }
                            app.copy(isSelected = isTracked)
                        }
                    }
                } catch (e: Exception) {
                    // If loading fails, provide fallback apps
                    println("DEBUG: Exception occurred while loading apps: ${e.message}")
                    e.printStackTrace()
                    val fallback = listOf(
                        AvailableApp("Instagram", "Social Media", "📷", "com.instagram.android"),
                        AvailableApp("TikTok", "Social Media", "🎵", "com.zhiliaoapp.musically"),
                        AvailableApp("Facebook", "Social Media", "📘", "com.facebook.katana"),
                        AvailableApp("Twitter", "Social Media", "🐦", "com.twitter.android"),
                        AvailableApp("YouTube", "Entertainment", "📺", "com.google.android.youtube"),
                        AvailableApp("Snapchat", "Social Media", "👻", "com.snapchat.android"),
                        AvailableApp("Reddit", "Social Media", "🤖", "com.reddit.frontpage"),
                        AvailableApp("LinkedIn", "Professional", "💼", "com.linkedin.android")
                    )
                    availableApps = fallback.map { app ->
                        val isTracked = trackedApps.any { tracked ->
                            tracked.name.equals(app.name, ignoreCase = true) ||
                            tracked.name.contains(app.name, ignoreCase = true) ||
                            app.name.contains(tracked.name, ignoreCase = true)
                        }
                        app.copy(isSelected = isTracked)
                    }
                } finally {
                    isLoadingApps = false
                }
            }
        }
    }

    when (route) {
        null -> {
            // Show loading state while checking onboarding status
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Loading...", color = Color.White)
                    Text("Checking onboarding status...", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                }
            }
        }
        Route.Onboarding -> OnboardingFlow(
            onGetStarted = { 
                coroutineScope.launch {
                    storage.setOnboardingCompleted(true)
                    setupDefaultApps()
                    route = Route.Dashboard
                }
            },
            onSkip = {
                coroutineScope.launch {
                    storage.setOnboardingCompleted(true)
                    setupDefaultApps()
                    route = Route.Dashboard
                }
            }
        )
        Route.QrGenerator -> QrGeneratorScreen(
            message = qrMessage,
            onMessageChange = { qrMessage = it },
            onQrCreated = { id ->
                qrId = id
                route = Route.AppSelection
            },
            onClose = { route = Route.Dashboard }
        )
        Route.Dashboard -> DashboardScreen(
            qrId = qrId,
            message = qrMessage,
            trackedApps = trackedApps,
            isTracking = isTracking,
            timeLimitMinutes = timeLimitMinutes,
            onToggleTracking = {
                isTracking = !isTracking
            },
            onOpenQrGenerator = { route = Route.QrGenerator },
            onOpenAppSelection = { route = Route.AppSelection },
            onScanQrCode = {
                // Trigger QR code scanning to unblock apps
                coroutineScope.launch {
                    val isValid = scanQrAndDismiss(qrMessage)
                    if (isValid) {
                        // QR code is valid, unblock apps
                        // In a real implementation, this would dismiss the blocking overlay
                        // and allow app usage to continue
                    }
                }
            },
            onOpenPause = {
                route = Route.Pause
            },
            onOpenDurationSetting = { route = Route.DurationSetting },
            onOpenSettings = { route = Route.Settings },
            onRemoveTrackedApp = { appName ->
                // Remove from tracked list (robust name matching)
                trackedApps = trackedApps.filterNot { tracked ->
                    tracked.name.equals(appName, ignoreCase = true) ||
                    tracked.name.contains(appName, ignoreCase = true) ||
                    appName.contains(tracked.name, ignoreCase = true)
                }
                // Also toggle off in available apps list so AppSelection reflects it
                availableApps = availableApps.map { app ->
                    val matches = app.name.equals(appName, ignoreCase = true) ||
                        app.name.contains(appName, ignoreCase = true) ||
                        appName.contains(app.name, ignoreCase = true)
                    if (matches) app.copy(isSelected = false) else app
                }
                // Persist updated selection
                coroutineScope.launch {
                    val selectedPackages = availableApps.filter { it.isSelected }.map { it.packageName }
                    storage.saveSelectedAppPackages(selectedPackages)
                }
            }
        )
        Route.Settings -> SettingsScreen(
            onBack = { route = Route.Dashboard },
            onOpenSavedQrCodes = { route = Route.SavedQrCodes }
        )
        Route.SavedQrCodes -> SavedQrCodesScreen(
            onBack = { route = Route.Settings }
        )
        Route.AppSelection -> AppSelectionScreen(
            availableApps = availableApps,
            isLoading = isLoadingApps,
            onAppToggle = { packageName ->
                availableApps = availableApps.map { app ->
                    if (app.packageName == packageName) app.copy(isSelected = !app.isSelected) else app
                }
                
                // Immediately update trackedApps to reflect changes on dashboard
                val selectedApps = availableApps.filter { it.isSelected }
                trackedApps = selectedApps.map { app ->
                    // Try to preserve existing usage data if app was already tracked
                    val existingApp = trackedApps.find { it.name == app.name }
                    TrackedApp(
                        name = app.name,
                        minutesUsed = existingApp?.minutesUsed ?: 0,
                        limitMinutes = existingApp?.limitMinutes ?: timeLimitMinutes
                    )
                }
                // Persist updated selection
                coroutineScope.launch {
                    storage.saveSelectedAppPackages(selectedApps.map { it.packageName })
                }
            },
            onContinue = {
                // Ensure dashboard reflects the current selections even if user didn't toggle
                val selectedApps = availableApps.filter { it.isSelected }
                trackedApps = selectedApps.map { app ->
                    val existingApp = trackedApps.find { it.name == app.name }
                    TrackedApp(
                        name = app.name,
                        minutesUsed = existingApp?.minutesUsed ?: 0,
                        limitMinutes = existingApp?.limitMinutes ?: timeLimitMinutes
                    )
                }
                // Persist selection
                coroutineScope.launch {
                    storage.saveSelectedAppPackages(selectedApps.map { it.packageName })
                }
                route = Route.Dashboard
            },
            onBack = { route = Route.Dashboard }
        )
        Route.DurationSetting -> DurationSettingScreen(
            timeLimitMinutes = timeLimitMinutes,
            onTimeLimitChange = { 
                timeLimitMinutes = it
                // Persist time limit immediately
                coroutineScope.launch { storage.saveTimeLimitMinutes(it) }
            },
            onCompleteSetup = {
                // Convert selected apps to tracked apps with the chosen time limit
                val selectedApps = availableApps.filter { it.isSelected }
                trackedApps = selectedApps.map { app ->
                    // Try to preserve existing usage data if app was already tracked
                    val existingApp = trackedApps.find { it.name == app.name }
                    TrackedApp(
                        name = app.name,
                        minutesUsed = existingApp?.minutesUsed ?: 0,
                        limitMinutes = timeLimitMinutes
                    )
                }
                // Ensure persisted values are saved
                coroutineScope.launch {
                    storage.saveSelectedAppPackages(selectedApps.map { it.packageName })
                    storage.saveTimeLimitMinutes(timeLimitMinutes)
                }
                route = Route.Dashboard
            },
            onBack = { route = Route.Dashboard }
        )
        Route.Pause -> PauseScreen(
            appName = trackedApps.firstOrNull()?.name ?: "Instagram",
            durationText = "1h 15m",
            onScanQr = {
                coroutineScope.launch {
                    val ok = scanQrAndDismiss(qrMessage)
                    if (ok) route = Route.Dashboard
                }
            },
            onClose = { route = Route.Dashboard }
        )
    }
}


// QR generator screen
@Composable
private fun QrGeneratorScreen(
    message: String,
    onMessageChange: (String) -> Unit,
    onQrCreated: (String) -> Unit,
    onClose: () -> Unit
) {
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    
    QrGeneratorContent(
        message = message,
        onMessageChange = onMessageChange,
        onDownloadPdf = { text ->
            // Save a PDF using a platform stub and then continue
            val filePath = saveQrPdf(qrText = text, message = message)
            
            // Save the QR code to storage for later validation
            coroutineScope.launch {
                val qrCode = SavedQrCode(
                    id = "pause-${kotlin.random.Random.nextLong()}",
                    qrText = text,
                    message = message,
                    createdAt = getCurrentTimeMillis(),
                    isActive = true
                )
                storage.saveQrCode(qrCode)
            }
            
            // Note: In a real app, you might want to show a toast or notification here
            // indicating the file was saved to the specified path
        },
        onGenerate = {
            // Create a simple unique id
            val id = "pause-${kotlin.random.Random.nextLong()}"
            onQrCreated(id)
        },
        onClose = onClose
    )
}

// Dashboard
@Composable
private fun DashboardScreen(
    qrId: String?,
    message: String,
    trackedApps: List<TrackedApp>,
    isTracking: Boolean,
    timeLimitMinutes: Int,
    onToggleTracking: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    onOpenAppSelection: () -> Unit,
    onScanQrCode: () -> Unit,
    onOpenPause: () -> Unit,
    onOpenDurationSetting: () -> Unit,
    onOpenSettings: () -> Unit,
    onRemoveTrackedApp: (String) -> Unit
) {
    DashboardContent(
        qrId = qrId ?: "",
        message = message,
        trackedApps = trackedApps,
        isTracking = isTracking,
        timeLimitMinutes = timeLimitMinutes,
        onToggleTracking = onToggleTracking,
        onOpenQrGenerator = onOpenQrGenerator,
        onOpenAppSelection = onOpenAppSelection,
        onScanQrCode = onScanQrCode,
        onOpenPause = onOpenPause,
        onOpenDurationSetting = onOpenDurationSetting,
        onOpenSettings = onOpenSettings,
        onRemoveTrackedApp = onRemoveTrackedApp
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
expect suspend fun scanQrAndDismiss(expectedMessage: String): Boolean
expect fun getCurrentTimeMillis(): Long

// Enhanced QR scanning function that validates against saved QR codes
suspend fun scanQrAndValidate(storage: AppStorage): Boolean {
    // This would be called from platform-specific implementations
    // For now, we'll implement a simple validation flow
    return false // Placeholder - will be implemented in platform layers
}

// Simple date formatting function
private fun formatDate(timestamp: Long): String {
    // Simple date formatting - just show relative time for now
    val now = getCurrentTimeMillis()
    val diff = now - timestamp
    val days = diff / (24 * 60 * 60 * 1000)
    
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        days < 30L -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}

// Expect actual QR code display. Implemented per platform.
@Composable
expect fun QrCodeDisplay(
    text: String,
    modifier: Modifier = Modifier
)

// UI implementations for onboarding, QR, and dashboard live in this file for brevity
// Lightweight, dependency-free visuals only

@Composable
private fun OnboardingFlow(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
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
                title = "Pause Partners",
                description = "Add trusted contacts as pause partners. They can generate QR codes to help you unlock apps.",
                primaryCta = "Get Started"
            )
        ),
        onDone = onGetStarted,
        onSkip = onSkip
    )
}

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
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    var index by remember { mutableStateOf(0) }
    var offsetX by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress indicators at top
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

        // Swipeable content area (centered)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Determine if swipe was significant enough to change page
                            val threshold = size.width * 0.2f // 20% of screen width
                            when {
                                offsetX > threshold && index > 0 -> {
                                    // Swipe right - go to previous page
                                    index--
                                }
                                offsetX < -threshold && index < pages.lastIndex -> {
                                    // Swipe left - go to next page
                                    index++
                                }
                            }
                            offsetX = 0f
                        }
                    ) { _, dragAmount ->
                        // Only allow horizontal swiping
                        offsetX += dragAmount.x
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Main content card with swipe offset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), 0) },
                backgroundColor = Color(0xFF222625),
                shape = RoundedCornerShape(16.dp)
            ) {
                val page = pages[index]
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
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = page.description,
                        fontSize = 16.sp,
                        color = Color(0xFFD1D5DB),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Action buttons at bottom
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
                    pages[index].primaryCta,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Bold
                ) 
            }
            // Only show Skip button if not on the last page
            if (index < pages.lastIndex) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Skip",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onSkip() },
                    color = Color(0xFFD1D5DB)
                )
            }
        }
    }
}

@Composable
private fun QrGeneratorContent(
    message: String,
    onMessageChange: (String) -> Unit,
    onDownloadPdf: (String) -> Unit,
    onGenerate: () -> Unit,
    onClose: () -> Unit
) {
    var qrVersion by remember { mutableStateOf(1) } // Start at 1 so QR shows initially
    var hasGeneratedQr by remember { mutableStateOf(true) } // Auto-generate QR on page load
    var downloadSuccess by remember { mutableStateOf(false) }
    var showAccountabilityDialog by remember { mutableStateOf(false) }
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
            Text(
                text = "×",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onClose() }
            )
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
                        Text("ID: pause-${kotlin.random.Random.nextLong().toString().takeLast(6)}...", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("⧉", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Generate New QR Code Button inside the card
                    Button(
                        onClick = { 
                            qrVersion++
                            hasGeneratedQr = true
                            downloadSuccess = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text("↻", color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate New QR Code", color = Color.White, fontWeight = FontWeight.Bold)
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
            onClick = { 
                if (downloadSuccess) {
                    // If already downloaded, navigate to dashboard
                    onClose()
                } else {
                    // Download PDF first
                    onDownloadPdf(qrText)
                    downloadSuccess = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (downloadSuccess) Color(0xFF2E7D32) else if (hasGeneratedQr) Color(0xFF4CAF50) else Color(0xFF4B5563)
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            enabled = hasGeneratedQr
        ) {
            Text(if (downloadSuccess) "✓" else "↓", color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                if (downloadSuccess) "Go to Dashboard" else "Download PDF for Printing", 
                color = Color.White, 
                fontWeight = FontWeight.Bold
            )
        }
        
        // Success message
        if (downloadSuccess) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "✓ File saved to Downloads folder",
                color = Color(0xFF4CAF50),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Don't have a printer? Button
        Button(
            onClick = { showAccountabilityDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text("?", color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Don't have a printer?", color = Color.White, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(32.dp))
        
        // How Physical Movement Unlocks Work Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "How Physical Movement Unlocks Work:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Step 1
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "1.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Download and print this QR code",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Step 2
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "2.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Place it somewhere you need to walk to (kitchen, bedroom, upstairs, etc.)",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Step 3
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "3.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "When your app time limit is reached, you'll need to physically get up and scan this code to unlock your apps",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Step 4
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "4.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "This forces you to move your body and step away from your phone, creating a natural pause",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Step 5
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "5.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "The physical effort makes you consider if you really need more screen time",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    
    // Pause Partners Dialog
    if (showAccountabilityDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showAccountabilityDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👥", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Coming Soon: Pause Partners",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "We're working on a feature that lets someone you trust generate QR codes on their phone for you to scan.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Your pause partner can help you think twice about your app usage by being the \"gatekeeper\" of your unlock codes.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAccountabilityDialog = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }
}

@Composable
private fun DashboardContent(
    qrId: String,
    message: String,
    trackedApps: List<TrackedApp>,
    isTracking: Boolean,
    timeLimitMinutes: Int,
    onToggleTracking: () -> Unit,
    onOpenQrGenerator: () -> Unit,
    onOpenAppSelection: () -> Unit,
    onScanQrCode: () -> Unit,
    onOpenPause: () -> Unit,
    onOpenDurationSetting: () -> Unit,
    onOpenSettings: () -> Unit,
    onRemoveTrackedApp: (String) -> Unit
) {
    var showAccountabilityDialog by remember { mutableStateOf(false) }
    var savedQrCodes by remember { mutableStateOf<List<SavedQrCode>>(emptyList()) }
    var savedQrLoaded by remember { mutableStateOf(false) }
    val storage = remember { createAppStorage() }
    val coroutineScope = rememberCoroutineScope()
    
    // Load saved QR codes when component mounts
    LaunchedEffect(Unit) {
        savedQrCodes = storage.getSavedQrCodes()
        savedQrLoaded = true
    }
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
                Text("PausePal", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Monday, Sep 22", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
            Row {
                Text("☀", fontSize = 24.sp, color = Color(0xFFD1D5DB))
                Spacer(Modifier.width(16.dp))
                Text("⚙", fontSize = 24.sp, color = Color(0xFFD1D5DB), modifier = Modifier.clickable { onOpenSettings() })
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
                        .clickable { onOpenDurationSetting() }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val minutesUsed = trackedApps.maxOfOrNull { it.minutesUsed } ?: 0
                        val remaining = (timeLimitMinutes - minutesUsed).coerceAtLeast(0)
                        Text("${remaining}m", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("minutes remaining until pause time", fontSize = 14.sp, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Time Limit: ${timeLimitMinutes} minutes", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "✏",
                                fontSize = 12.sp,
                                color = Color(0xFFD1D5DB),
                                modifier = Modifier.clickable { onOpenDurationSetting() }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Text("times unblocked today", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0h 0m", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                        Text("total usage today", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Start tracking button
                Button(
                    onClick = onToggleTracking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = if (isTracking) Color(0xFFFF9800) else Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(if (isTracking) "⏸" else "▶", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTracking) "Pause Tracking" else "Start Tracking", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onOpenPause,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF43A047)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text("⏸", color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Simulate Limit Reached (Open Pause Screen)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Ready to Walk Card (show only if no saved QR codes after load)
        if (savedQrLoaded && savedQrCodes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF2C2C2C),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("▦", fontSize = 16.sp, color = Color(0xFF4CAF50))
                        Spacer(Modifier.width(8.dp))
                        Text("Ready to Walk for Your Apps?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        "Print your personal QR codes and place them around your home. When your time limit is reached, you'll need to walk to scan one - encouraging healthy movement breaks!",
                        fontSize = 14.sp,
                        color = Color(0xFFD1D5DB),
                        lineHeight = 20.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onOpenQrGenerator,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("▦", color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate QR Codes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = { showAccountabilityDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4B5563)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text("👥", color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Partners", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    
                    // Removed scan action button per requirements
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Saved QR Codes card removed from Dashboard; now lives in Settings
        
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
                    Text(
                        text = "+", 
                        fontSize = 24.sp, 
                        color = Color.White,
                        modifier = Modifier.clickable { onOpenAppSelection() }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Empty state when no apps are selected
                if (trackedApps.isEmpty()) {
                    Text(
                        "No apps selected. Add apps to track",
                        color = Color(0xFFD1D5DB),
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onOpenAppSelection() }
                    )
                }

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
                                Text("0m today", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "🗑",
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { onRemoveTrackedApp(app.name) }
                                )
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
    }
    
    // Pause Partners Dialog
    if (showAccountabilityDialog) {
        androidx.compose.material.AlertDialog(
            onDismissRequest = { showAccountabilityDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👥", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Coming Soon: Pause Partners",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "We're working on a feature that lets someone you trust generate QR codes on their phone for you to scan.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Your pause partner can help you think twice about your app usage by being the \"gatekeeper\" of your unlock codes.",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAccountabilityDialog = false },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got it!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            backgroundColor = Color(0xFF1A1A1A),
            contentColor = Color.White
        )
    }
}

@Composable
private fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSavedQrCodes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("←", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Customize your digital wellness experience", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Notifications", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Push Notifications — Get notified when you reach time limits", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("App Behavior", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Strict Mode — Make it harder to bypass time limits", color = Color(0xFFD1D5DB), fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Show Usage Statistics — Display daily usage on dashboard", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSavedQrCodes() },
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Saved QR Codes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Manage, share, and protect your QR codes", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Appearance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Theme — Switch between light and dark mode", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Danger Zone", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                Spacer(Modifier.height(8.dp))
                Text("Reset All Data — This cannot be undone", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SavedQrCodesScreen(
    onBack: () -> Unit
) {
    val storage = remember { createAppStorage() }
    var savedQrCodes by remember { mutableStateOf<List<SavedQrCode>>(emptyList()) }
    LaunchedEffect(Unit) { savedQrCodes = storage.getSavedQrCodes() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 24.sp, color = Color.White, modifier = Modifier.clickable { onBack() })
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Saved QR Codes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Manage your QR codes", fontSize = 14.sp, color = Color(0xFFD1D5DB))
            }
        }
        Spacer(Modifier.height(24.dp))

        if (savedQrCodes.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF2C2C2C), shape = RoundedCornerShape(16.dp)) {
                Text("No saved QR codes yet", color = Color(0xFFD1D5DB), modifier = Modifier.padding(24.dp))
            }
            return@Column
        }

        savedQrCodes.forEach { qrCode ->
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), backgroundColor = Color(0xFF2C2C2C), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("▦", color = Color(0xFF4CAF50), fontSize = 16.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(qrCode.message, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Created ${formatDate(qrCode.createdAt)}", color = Color(0xFFD1D5DB), fontSize = 12.sp)
                    }
                    if (qrCode.isActive) Text("✓", color = Color(0xFF4CAF50), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PauseScreen(
    appName: String,
    durationText: String,
    onScanQr: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1111)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card-like container
            Card(
                backgroundColor = Color(0xFF16201B),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✦",
                        color = Color(0xFF6EE7B7),
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Time for a Pause",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Text("You've used ", color = Color(0xFFD1D5DB))
                        Text(appName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(" for", color = Color(0xFFD1D5DB))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = durationText,
                        color = Color(0xFF6EE7B7),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Choose how you'd like to take your mindful break",
                        color = Color(0xFFBFC7C2),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onScanQr,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF34D399)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("▣", color = Color(0xFF063B2D))
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Scan My QR Code", color = Color(0xFF063B2D), fontWeight = FontWeight.Bold)
                    Text("Get up and scan your printed QR code", color = Color(0xFF0B5E47), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "× Dismiss",
                color = Color(0xFFBFC7C2),
                modifier = Modifier.clickable { onClose() }
            )
        }
    }
}

@Composable
private fun AppSelectionScreen(
    availableApps: List<AvailableApp>,
    isLoading: Boolean,
    onAppToggle: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val selectedCount = availableApps.count { it.isSelected }
    
    // Show loading state if apps are being loaded
    if (isLoading || availableApps.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Loading...", color = Color.White, fontSize = 18.sp)
                Text("Scanning for installed apps...", color = Color(0xFFD1D5DB), fontSize = 14.sp)
            }
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Select Apps to Track",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Choose which apps you'd like to set limits for.",
                    fontSize = 14.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Selection count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📱", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "$selectedCount apps selected",
                fontSize = 16.sp,
                color = Color(0xFFD1D5DB)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Apps list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableApps) { app ->
                AppSelectionItem(
                    app = app,
                    onToggle = { onAppToggle(app.packageName) }
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Done button
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                "Done",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AppSelectionItem(
    app: AvailableApp,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF2C2C2C),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Text(
                text = app.icon,
                fontSize = 24.sp,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            // App info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = app.category,
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
            
            // Toggle switch
            Box(
                modifier = Modifier
                    .size(48.dp, 28.dp)
                    .background(
                        color = if (app.isSelected) Color(0xFF4CAF50) else Color(0xFF4B5563),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp, 24.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .offset(
                            x = if (app.isSelected) 10.dp else (-10).dp
                        )
                )
            }
        }
    }
}

@Composable
private fun DurationSettingScreen(
    timeLimitMinutes: Int,
    onTimeLimitChange: (Int) -> Unit,
    onCompleteSetup: () -> Unit,
    onBack: () -> Unit
) {
    val quickSelectOptions = listOf(5, 10, 15, 45, 60, 90, 120)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    "Set Time Limit",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "How long before you take a pause?",
                    fontSize = 14.sp,
                    color = Color(0xFFD1D5DB)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Current Limit Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕒", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Current Limit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Time display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeLimitMinutes.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "minutes",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Adjust controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { 
                            if (timeLimitMinutes > 1) {
                                onTimeLimitChange(timeLimitMinutes - 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "-",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "adjust",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(16.dp))
                    
                    Button(
                        onClick = { 
                            if (timeLimitMinutes < 480) { // Max 8 hours
                                onTimeLimitChange(timeLimitMinutes + 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "+",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Quick Select
        Text(
            "Quick Select",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(Modifier.height(12.dp))
        
        // Quick select buttons
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickSelectOptions.chunked(3)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { minutes ->
                        Button(
                            onClick = { onTimeLimitChange(minutes) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (timeLimitMinutes == minutes) 
                                    Color(0xFF4CAF50) else Color(0xFF2C2C2C)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                "${minutes}m",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    // Fill remaining space if row has less than 3 items
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Information card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF2C2C2C),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "All selected apps will be blocked after $timeLimitMinutes minutes of combined use. You have to walk and scan your saved QR codes to unblock them.",
                modifier = Modifier.padding(16.dp),
                color = Color.White,
                fontSize = 14.sp
            )
        }
        
        Spacer(Modifier.weight(1f))
        
        // Complete Setup button
        Button(
            onClick = onCompleteSetup,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            Text(
                "Done",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}