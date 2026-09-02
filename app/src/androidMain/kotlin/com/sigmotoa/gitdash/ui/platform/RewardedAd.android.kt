package com.sigmotoa.gitdash.ui.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.sigmotoa.gitdash.BuildConfig

@Composable
actual fun rememberRewardedAdController(): RewardedAdController {
    val activity = LocalContext.current as? Activity
    return remember(activity) {
        RewardedAdController { onReward, onCancelled ->
            if (activity == null) {
                onReward()
                return@RewardedAdController
            }
            RewardedInterstitialAd.load(
                activity,
                BuildConfig.AD_UNIT_REWARDED,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        var earned = false
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                if (!earned) onCancelled()
                            }

                            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                                onReward()
                            }
                        }
                        ad.show(activity) { _ ->
                            earned = true
                            onReward()
                        }
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onReward()
                    }
                },
            )
        }
    }
}
