expect class AdManager {
    fun initialize()
    fun showInterstitialAd(onAdClosed: () -> Unit, onAdFailedToLoad: () -> Unit)
    fun isAdLoaded(): Boolean
    fun loadAd()
}

expect fun createAdManager(): AdManager
