package com.sigmotoa.gitdash.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Banner de anuncios. Implementación real en Android (Google Mobile Ads);
 * en iOS es un hueco vacío hasta integrar el SDK de iOS (paso D4).
 */
@Composable
expect fun AdMobBanner(modifier: Modifier = Modifier)
