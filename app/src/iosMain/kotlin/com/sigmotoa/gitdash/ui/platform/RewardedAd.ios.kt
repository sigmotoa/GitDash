package com.sigmotoa.gitdash.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// TODO(D4): integrar anuncios recompensados de Google Mobile Ads en iOS.
// Mientras tanto se concede la recompensa directamente.
@Composable
actual fun rememberRewardedAdController(): RewardedAdController = remember {
    RewardedAdController { onReward, _ -> onReward() }
}
