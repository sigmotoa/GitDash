package com.sigmotoa.gitdash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppVersion(
    @SerialName("version_code")
    val versionCode: Int,
    @SerialName("version_name")
    val versionName: String,
    @SerialName("release_notes")
    val releaseNotes: String? = null,
    @SerialName("download_url")
    val downloadUrl: String? = null,
    @SerialName("is_mandatory")
    val isMandatory: Boolean = false
)
