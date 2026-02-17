package com.sigmotoa.gitdash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitLabProject(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("path_with_namespace")
    val pathWithNamespace: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("web_url")
    val webUrl: String,
    @SerialName("star_count")
    val starCount: Int = 0,
    @SerialName("forks_count")
    val forksCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("last_activity_at")
    val lastActivityAt: String? = null,
    @SerialName("namespace")
    val namespace: Namespace,
    @SerialName("default_branch")
    val defaultBranch: String? = null
) {
    @Serializable
    data class Namespace(
        @SerialName("name")
        val name: String,
        @SerialName("path")
        val path: String,
        @SerialName("avatar_url")
        val avatarUrl: String? = null
    )
}
