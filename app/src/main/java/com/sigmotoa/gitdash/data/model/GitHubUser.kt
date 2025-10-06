package com.sigmotoa.gitdash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUser(
    @SerialName("login")
    val login: String,
    @SerialName("id")
    val id: Int,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("company")
    val company: String? = null,
    @SerialName("blog")
    val blog: String? = null,
    @SerialName("location")
    val location: String? = null,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("public_repos")
    val publicRepos: Int = 0,
    @SerialName("followers")
    val followers: Int = 0,
    @SerialName("following")
    val following: Int = 0
)