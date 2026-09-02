package com.sigmotoa.gitdash.ui.utils

import kotlin.math.roundToInt

/**
 * Formatea un número con un decimal sin depender de `java.lang.String.format`,
 * que no existe en `commonMain`. Ej.: `66.666f.oneDecimal()` -> `"66.7"`.
 */
fun Float.oneDecimal(): String = ((this * 10).roundToInt() / 10.0).toString()
