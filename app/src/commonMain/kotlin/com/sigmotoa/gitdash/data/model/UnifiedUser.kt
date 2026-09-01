package com.sigmotoa.gitdash.data.model

data class UnifiedUser(
    val id: Int,
    val username: String,
    val name: String?,
    val avatarUrl: String,
    val bio: String?,
    val location: String?,
    val company: String?,
    val blog: String?,
    val publicRepos: Int,
    val followers: Int,
    val following: Int,
    val platform: Platform
) {
    companion object {
        fun fromGitHub(user: GitHubUser): UnifiedUser {
            return UnifiedUser(
                id = user.id,
                username = user.login,
                name = user.name,
                avatarUrl = user.avatarUrl,
                bio = user.bio,
                location = user.location,
                company = user.company,
                blog = user.blog,
                publicRepos = user.publicRepos,
                followers = user.followers,
                following = user.following,
                platform = Platform.GITHUB
            )
        }

        fun fromGitLab(user: GitLabUser): UnifiedUser {
            return UnifiedUser(
                id = user.id,
                username = user.username,
                name = user.name,
                avatarUrl = user.avatarUrl ?: "",
                bio = user.bio,
                location = user.location,
                company = user.organization,
                blog = user.publicEmail,
                publicRepos = 0, // GitLab doesn't provide this in user endpoint
                followers = user.followers ?: 0,
                following = user.following ?: 0,
                platform = Platform.GITLAB
            )
        }
    }
}
