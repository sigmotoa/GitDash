package com.sigmotoa.gitdash.version

import com.sigmotoa.gitdash.data.remote.VersionCheckService
import com.sigmotoa.gitdash.platform.PlatformInfo

/**
 * Compara la versión instalada (vía [PlatformInfo]) con la última publicada
 * (`version.json`) y describe si hay actualización disponible.
 */
class VersionCheckManager(
    private val platformInfo: PlatformInfo,
    private val versionCheckService: VersionCheckService,
) {

    fun getCurrentVersionCode(): Int = platformInfo.appVersionCode

    fun getCurrentVersionName(): String =
        platformInfo.appVersionName.ifEmpty { "Unknown" }

    /** Devuelve `null` si la comprobación falla. */
    suspend fun checkForUpdate(): VersionUpdateInfo? = try {
        val latest = versionCheckService.checkVersion()
        val currentCode = getCurrentVersionCode()

        VersionUpdateInfo(
            currentVersion = getCurrentVersionName(),
            latestVersion = latest.versionName,
            versionCode = latest.versionCode,
            releaseNotes = latest.releaseNotes.takeIf { latest.versionCode > currentCode },
            downloadUrl = latest.downloadUrl.takeIf { latest.versionCode > currentCode },
            isMandatory = latest.isMandatory && latest.versionCode > currentCode,
            isUpdateAvailable = latest.versionCode > currentCode,
        )
    } catch (e: Exception) {
        null
    }
}
