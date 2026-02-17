package com.sigmotoa.gitdash.version

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.sigmotoa.gitdash.data.model.AppVersion
import com.sigmotoa.gitdash.data.remote.VersionCheckService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VersionCheckManager(
    private val context: Context,
    private val versionCheckService: VersionCheckService
) {

    /**
     * Gets the current app version code
     */
    fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Gets the current app version name
     */
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Checks if there's a new version available
     * Returns null if check fails or if there's an error
     */
    suspend fun checkForUpdate(): VersionUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val latestVersion = versionCheckService.checkVersion()
                val currentVersionCode = getCurrentVersionCode()

                if (latestVersion.versionCode > currentVersionCode) {
                    VersionUpdateInfo(
                        currentVersion = getCurrentVersionName(),
                        latestVersion = latestVersion.versionName,
                        versionCode = latestVersion.versionCode,
                        releaseNotes = latestVersion.releaseNotes,
                        downloadUrl = latestVersion.downloadUrl,
                        isMandatory = latestVersion.isMandatory,
                        isUpdateAvailable = true
                    )
                } else {
                    VersionUpdateInfo(
                        currentVersion = getCurrentVersionName(),
                        latestVersion = latestVersion.versionName,
                        versionCode = latestVersion.versionCode,
                        isUpdateAvailable = false
                    )
                }
            } catch (e: Exception) {
                // Log error or handle silently
                null
            }
        }
    }
}

data class VersionUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val versionCode: Int,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null,
    val isMandatory: Boolean = false,
    val isUpdateAvailable: Boolean
)
