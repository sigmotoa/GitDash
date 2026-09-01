package com.sigmotoa.gitdash.platform

import android.content.Context
import android.os.Build

/**
 * Implementación Android de [PlatformInfo].
 *
 * Requiere un [Context] (por ejemplo el `applicationContext`) para leer el
 * `PackageManager`. Los tipos de Android quedan encapsulados aquí; `commonMain`
 * nunca los ve.
 */
actual class PlatformInfo(context: Context) {

    private val packageInfo =
        context.packageManager.getPackageInfo(context.packageName, 0)

    actual val osName: String = "Android"

    actual val osVersion: String =
        Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()

    actual val appVersionName: String = packageInfo.versionName ?: ""

    actual val appVersionCode: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
}
