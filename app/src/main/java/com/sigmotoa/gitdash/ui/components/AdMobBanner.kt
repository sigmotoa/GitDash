package com.sigmotoa.gitdash.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sigmotoa.gitdash.BuildConfig

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                // Ad Unit ID loaded from local.properties via BuildConfig
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}