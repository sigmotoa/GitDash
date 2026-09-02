package com.sigmotoa.gitdash.version

/** Resultado de comparar la versión instalada con la última publicada. */
data class VersionUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val versionCode: Int,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null,
    val isMandatory: Boolean = false,
    val isUpdateAvailable: Boolean
)
