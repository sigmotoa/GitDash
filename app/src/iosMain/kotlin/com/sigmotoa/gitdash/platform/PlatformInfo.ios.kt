package com.sigmotoa.gitdash.platform

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

/**
 * Implementación iOS de [PlatformInfo].
 *
 * Lee la versión del SO de `UIDevice` y la versión de la app del `Info.plist`
 * (`NSBundle.mainBundle`). No necesita parámetros.
 */
actual class PlatformInfo {

    private val bundle = NSBundle.mainBundle

    actual val osName: String = UIDevice.currentDevice.systemName()

    actual val osVersion: String = UIDevice.currentDevice.systemVersion()

    actual val appVersionName: String =
        bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: ""

    actual val appVersionCode: Int =
        (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toIntOrNull() ?: 0
}
