package com.sigmotoa.gitdash.ui.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

/**
 * El controlador de vista sobre el que presentar (share sheet, anuncios…).
 *
 * `UIApplication.keyWindow` está deprecado y devuelve `nil` en apps basadas en
 * escenas (SwiftUI), así que se busca por `connectedScenes`.
 */
internal fun topmostViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes
        .firstOrNull { it is UIWindowScene } as? UIWindowScene
        ?: return null

    var vc: UIViewController? = scene.keyWindow?.rootViewController
        ?: (scene.windows.firstOrNull() as? UIWindow)?.rootViewController

    while (vc?.presentedViewController != null) {
        vc = vc.presentedViewController
    }
    return vc
}
