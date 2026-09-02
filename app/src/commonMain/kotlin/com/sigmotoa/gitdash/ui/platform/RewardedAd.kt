package com.sigmotoa.gitdash.ui.platform

import androidx.compose.runtime.Composable

/**
 * Muestra un anuncio recompensado y ejecuta la acción premiada.
 *
 * - Android: `RewardedInterstitialAd` de Google Mobile Ads.
 * - iOS: por ahora concede la recompensa directamente (D4 pendiente).
 *
 * [onReward] se invoca al ganar la recompensa **o** si no hay anuncio disponible.
 * [onCancelled] se invoca si el usuario cierra el anuncio sin ganarla.
 */
fun interface RewardedAdController {
    fun show(onReward: () -> Unit, onCancelled: () -> Unit)
}

@Composable
expect fun rememberRewardedAdController(): RewardedAdController
