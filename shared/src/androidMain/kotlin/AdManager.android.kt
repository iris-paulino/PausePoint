import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

actual class AdManager(private val context: Context) {
    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    
    // Test ad unit ID - replace with your actual ad unit ID for production
    private val adUnitId = "ca-app-pub-7430598136060306/3131995256" // Test ID for development
    
    actual fun initialize() {
        if (!isInitialized) {
            println("DEBUG: AdManager.initialize() called")
            MobileAds.initialize(context) {
                println("DEBUG: MobileAds.initialize() completed")
            }
            isInitialized = true
            loadAd()
        } else {
            println("DEBUG: AdManager already initialized")
        }
    }
    
    actual fun showInterstitialAd(onAdClosed: () -> Unit, onAdFailedToLoad: () -> Unit) {
        println("DEBUG: showInterstitialAd() called - interstitialAd is ${if (interstitialAd != null) "loaded" else "null"}")
        if (interstitialAd != null) {
            println("DEBUG: Showing interstitial ad")
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad was properly closed by user - proceed with dismiss
                    interstitialAd = null
                    onAdClosed()
                    // Load a new ad for next time
                    loadAd()
                }
                
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Ad failed to show - still proceed with dismiss
                    interstitialAd = null
                    onAdFailedToLoad()
                    // Load a new ad for next time
                    loadAd()
                }
                
                override fun onAdShowedFullScreenContent() {
                    // Ad is now showing - user must watch it
                    println("DEBUG: Ad is now showing - user must watch it")
                }
            }
            interstitialAd?.show(context as android.app.Activity)
        } else {
            // No ad available - proceed with dismiss anyway
            println("DEBUG: No ad available - calling onAdFailedToLoad")
            onAdFailedToLoad()
        }
    }
    
    actual fun isAdLoaded(): Boolean {
        return interstitialAd != null
    }
    
    actual fun loadAd() {
        println("DEBUG: loadAd() called with adUnitId: $adUnitId")
        val adRequest = AdRequest.Builder()
            .setRequestAgent("ScrollPause-Android")
            .build()
        
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    println("DEBUG: Ad failed to load: ${adError.message}")
                    println("DEBUG: Ad error code: ${adError.code}")
                    println("DEBUG: Ad error domain: ${adError.domain}")
                    println("DEBUG: Ad unit ID: $adUnitId")
                    
                    // Common error codes and their meanings
                    when (adError.code) {
                        0 -> println("DEBUG: ERROR_CODE_INTERNAL_ERROR - Internal error")
                        1 -> println("DEBUG: ERROR_CODE_INVALID_REQUEST - Invalid ad request")
                        2 -> println("DEBUG: ERROR_CODE_NETWORK_ERROR - Network error")
                        3 -> println("DEBUG: ERROR_CODE_NO_FILL - No ad available")
                        else -> println("DEBUG: Unknown error code: ${adError.code}")
                    }
                }
                
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    println("DEBUG: Ad loaded successfully")
                }
            }
        )
    }
}

// Global AdManager instance
private var globalAdManager: AdManager? = null

actual fun createAdManager(): AdManager {
    return globalAdManager ?: throw IllegalStateException("AdManager not initialized. Call initializeAdManager() first.")
}

fun initializeAdManager(context: Context) {
    println("DEBUG: initializeAdManager() called with context: ${context.packageName}")
    globalAdManager = AdManager(context)
    println("DEBUG: AdManager instance created")
    globalAdManager?.initialize()
    println("DEBUG: AdManager initialization completed")
}
