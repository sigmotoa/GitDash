package com.sigmotoa.gitdash.ui.platform

import platform.UIKit.UIView

/**
 * Puente entre el código común de anuncios y la implementación real de Google
 * Mobile Ads, que vive en Swift (`iosApp/Ads.swift`) porque el SDK se añade por
 * SPM en el proyecto Xcode, no como binding de Kotlin/Native.
 *
 * Swift rellena estas fábricas al arrancar la app. Si quedan a `null` (p. ej. en
 * tests), los `actual` de iOS degradan a un banner vacío y a conceder la
 * recompensa directamente.
 */
object IosAdBridge {

    /** Crea una `RewardedAdController` respaldada por `GADRewardedInterstitialAd`. */
    var rewardedFactory: (() -> RewardedAdController)? = null

    /** Crea la `UIView` de un banner (`GADBannerView` ya configurado y cargando). */
    var bannerFactory: (() -> UIView)? = null
}
