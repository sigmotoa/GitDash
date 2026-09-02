package com.sigmotoa.gitdash.ui.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * `Dispatchers.IO` no existe en `commonMain` (solo en JVM y Native), así que se
 * expone a través de expect/actual.
 */
expect val ioDispatcher: CoroutineDispatcher
