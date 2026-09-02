package com.sigmotoa.gitdash.ui.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// `Dispatchers.IO` sigue siendo `internal` en Kotlin/Native con coroutines 1.9.
// `Default.limitedParallelism` da un dispatcher con paralelismo propio, apto para
// trabajo bloqueante puntual.
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(64)
