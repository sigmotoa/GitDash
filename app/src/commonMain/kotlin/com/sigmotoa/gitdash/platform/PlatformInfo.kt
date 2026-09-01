package com.sigmotoa.gitdash.platform

/**
 * Información de la plataforma y de la instalación actual de la app.
 *
 * Es un `expect class`: `commonMain` solo declara la forma (propiedades), y cada
 * target aporta su implementación real en `androidMain` / `iosMain`.
 *
 * No se declara constructor en común a propósito: en Android se necesita un
 * `Context` para construirlo y en iOS no. El código de `commonMain` que necesite
 * esta información debe recibir una instancia ya creada (inyección), no instanciarla.
 */
expect class PlatformInfo {

    /** Nombre del sistema operativo, p. ej. "Android" o "iOS". */
    val osName: String

    /** Versión del sistema operativo, p. ej. "14" o "17.5". */
    val osVersion: String

    /** Nombre de versión de la app instalada (versionName / CFBundleShortVersionString). */
    val appVersionName: String

    /** Código de versión de la app instalada (versionCode / CFBundleVersion). */
    val appVersionCode: Int
}

/** Etiqueta legible del tipo "Android 14" / "iOS 17.5". */
val PlatformInfo.label: String
    get() = "$osName $osVersion"
