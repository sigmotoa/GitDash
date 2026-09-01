package com.sigmotoa.gitdash.data.model

data class UnifiedRepo(
    val id: Int,
    val name: String,
    val fullName: String,
    val description: String?,
    val htmlUrl: String,
    val starCount: Int,
    val forksCount: Int,
    val language: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val owner: Owner,
    val platform: Platform
) {
    data class Owner(
        val login: String,
        val avatarUrl: String
    )

    companion object {
        fun fromGitHub(repo: GitHubRepo): UnifiedRepo {
            return UnifiedRepo(
                id = repo.id,
                name = repo.name,
                fullName = repo.fullName,
                description = repo.description,
                htmlUrl = repo.htmlUrl,
                starCount = repo.stargazersCount,
                forksCount = repo.forksCount,
                language = repo.language,
                createdAt = repo.createdAt,
                updatedAt = repo.updatedAt,
                owner = Owner(
                    login = repo.owner.login,
                    avatarUrl = repo.owner.avatarUrl
                ),
                platform = Platform.GITHUB
            )
        }

        fun fromGitLab(project: GitLabProject): UnifiedRepo {
            return UnifiedRepo(
                id = project.id,
                name = project.name,
                fullName = project.pathWithNamespace,
                description = project.description,
                htmlUrl = project.webUrl,
                starCount = project.starCount,
                forksCount = project.forksCount,
                language = null, // GitLab doesn't provide primary language in list endpoint
                createdAt = project.createdAt,
                updatedAt = project.lastActivityAt,
                owner = Owner(
                    login = project.namespace.name,
                    avatarUrl = project.namespace.avatarUrl ?: ""
                ),
                platform = Platform.GITLAB
            )
        }
    }
}
