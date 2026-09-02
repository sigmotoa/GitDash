package com.sigmotoa.gitdash.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Usa la implementación real de Google Mobile Ads si Swift la instaló
 * ([IosAdBridge.rewardedFactory]); si no, concede la recompensa directamente.
 */
@Composable
actual fun rememberRewardedAdController(): RewardedAdController = remember {
    IosAdBridge.rewardedFactory?.invoke()
        ?: RewardedAdController { onReward, _ -> onReward() }
}
