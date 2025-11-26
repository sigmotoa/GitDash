package com.sigmotoa.gitdash.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.sigmotoa.gitdash.BuildConfig

/**
 * Manager for handling interstitial ads following AdMob best practices:
 * - Preloads ads before showing them
 * - Reloads ads after they are shown
 * - Handles all lifecycle callbacks properly
 * - Manages click tracking to show ads every 5 clicks
 */
class InterstitialAdManager(private val activity: Activity) {

    private var interstitialAd: InterstitialAd? = null
    private var isLoadingAd = false
    private var clickCount = 0

    companion object {
        private const val TAG = "InterstitialAdManager"
        private const val CLICKS_THRESHOLD = 5
        private const val AD_UNIT_ID = BuildConfig.AD_UNIT_INTERSTITIAL
    }

    init {
        // Preload the first ad
        loadAd()
    }

    /**
     * Loads an interstitial ad in the background
     */
    private fun loadAd() {
        if (isLoadingAd || interstitialAd != null) {
            return
        }

        isLoadingAd = true
        Log.d(TAG, "Loading interstitial ad...")

        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoadingAd = false
                    setupAdCallbacks()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: ${loadAdError.message}")
                    interstitialAd = null
                    isLoadingAd = false

                    // Retry loading after a delay (optional)
                    // You could implement exponential backoff here
                }
            }
        )
    }

    /**
     * Sets up callbacks for when the ad is shown
     */
    private fun setupAdCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed")
                interstitialAd = null
                // Preload the next ad
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                interstitialAd = null
                // Preload a new ad
                loadAd()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content")
            }
        }
    }

    /**
     * Registers a click. Shows ad when threshold is reached.
     * Call this method from UI components that should count towards showing the ad.
     */
    fun registerClick() {
        clickCount++
        Log.d(TAG, "Click registered: $clickCount/$CLICKS_THRESHOLD")

        if (clickCount >= CLICKS_THRESHOLD) {
            showAdIfAvailable()
            clickCount = 0  // Reset counter
        }
    }

    /**
     * Shows the interstitial ad if it's loaded
     */
    private fun showAdIfAvailable() {
        if (interstitialAd != null) {
            Log.d(TAG, "Showing interstitial ad")
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Ad wasn't loaded yet. Loading now...")
            // If ad isn't loaded, load it for next time
            loadAd()
        }
    }

    /**
     * Gets the current click count (useful for debugging)
     */
    fun getCurrentClickCount(): Int = clickCount

    /**
     * Manually resets the click counter
     */
    fun resetClickCounter() {
        clickCount = 0
        Log.d(TAG, "Click counter reset")
    }

    /**
     * Call this when the activity is being destroyed
     */
    fun destroy() {
        interstitialAd = null
        isLoadingAd = false
    }
}