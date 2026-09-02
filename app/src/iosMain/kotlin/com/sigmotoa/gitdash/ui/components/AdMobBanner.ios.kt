package com.sigmotoa.gitdash.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import com.sigmotoa.gitdash.ui.platform.IosAdBridge
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Banner de Google Mobile Ads en iOS: envuelve la `GADBannerView` que crea Swift
 * ([IosAdBridge.bannerFactory]). Si Swift no lo instaló, no muestra nada.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalForeignApi::class)
@Composable
actual fun AdMobBanner(modifier: Modifier) {
    val makeBanner = IosAdBridge.bannerFactory ?: return
    UIKitView(
        factory = { makeBanner() },
        modifier = modifier.fillMaxWidth().height(50.dp),
    )
}
