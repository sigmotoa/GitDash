package com.sigmotoa.gitdash.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitLabUser(
    @SerialName("id")
    val id: Int,
    @SerialName("username")
    val username: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("web_url")
    val webUrl: String? = null,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("location")
    val location: String? = null,
    @SerialName("public_email")
    val publicEmail: String? = null,
    @SerialName("organization")
    val organization: String? = null,
    @SerialName("followers")
    val followers: Int? = null,
    @SerialName("following")
    val following: Int? = null
)
