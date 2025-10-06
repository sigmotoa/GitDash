package com.sigmotoa.gitdash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepo(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("stargazers_count")
    val stargazersCount: Int = 0,
    @SerialName("forks_count")
    val forksCount: Int = 0,
    @SerialName("language")
    val language: String? = null,
    @SerialName("owner")
    val owner: Owner
) {
    @Serializable
    data class Owner(
        @SerialName("login")
        val login: String,
        @SerialName("avatar_url")
        val avatarUrl: String
    )
}