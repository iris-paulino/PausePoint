actual class AdManager {
    actual fun initialize() {
        // iOS implementation would go here
        // For now, just a stub
    }
    
    actual fun showInterstitialAd(onAdClosed: () -> Unit, onAdFailedToLoad: () -> Unit) {
        // iOS implementation would go here
        // For now, just call onAdClosed immediately
        onAdClosed()
    }
    
    actual fun isAdLoaded(): Boolean {
        // iOS implementation would go here
        return false
    }
    
    actual fun loadAd() {
        // iOS implementation would go here
    }
}

actual fun createAdManager(): AdManager {
    return AdManager()
}
