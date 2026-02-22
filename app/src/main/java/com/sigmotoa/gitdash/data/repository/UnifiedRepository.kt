package com.sigmotoa.gitdash.data.repository

import android.util.Base64
import com.sigmotoa.gitdash.data.model.Platform
import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.remote.GitHubApiService
import com.sigmotoa.gitdash.data.remote.GitLabApiService
import java.net.URLEncoder

data class ContributionData(
    val dateMap: Map<String, Int>,
    val categoryCounts: Map<String, Int>
)

private fun categorizeGitHubEvent(type: String): String = when (type) {
    "PushEvent"                                -> "Commits"
    "PullRequestEvent"                         -> "PRs"
    "IssuesEvent"                              -> "Issues"
    "IssueCommentEvent", "CommitCommentEvent",
    "PullRequestReviewCommentEvent",
    "PullRequestReviewEvent"                   -> "Comments"
    else                                       -> "Other"
}

private fun categorizeGitLabEvent(actionName: String, targetType: String?): String = when {
    actionName.contains("push", ignoreCase = true)   -> "Commits"
    targetType == "MergeRequest"                      -> "PRs"
    actionName in listOf("accepted", "merged")        -> "PRs"
    targetType == "Issue"                             -> "Issues"
    actionName.contains("comment", ignoreCase = true) -> "Comments"
    else                                              -> "Other"
}

class UnifiedRepository(
    private val githubApiService: GitHubApiService,
    private val gitlabApiService: GitLabApiService
) {

    suspend fun getUser(username: String, platform: Platform): Result<UnifiedUser> {
        return try {
            when (platform) {
                Platform.GITHUB -> {
                    val user = githubApiService.getUser(username)
                    Result.success(UnifiedUser.fromGitHub(user))
                }
                Platform.GITLAB -> {
                    // GitLab API requires ?username= query — returns a list; take the first match
                    val results = gitlabApiService.searchUsers(username)
                    val user = results.firstOrNull()
                        ?: return Result.failure(Exception("GitLab user '$username' not found"))
                    Result.success(UnifiedUser.fromGitLab(user))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRepos(username: String, platform: Platform): Result<List<UnifiedRepo>> {
        return try {
            when (platform) {
                Platform.GITHUB -> {
                    val repos = githubApiService.getUserRepos(username)
                    Result.success(repos.map { UnifiedRepo.fromGitHub(it) })
                }
                Platform.GITLAB -> {
                    val projects = gitlabApiService.getUserProjects(username)
                    Result.success(projects.map { UnifiedRepo.fromGitLab(it) })
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommitCount(owner: String, repoName: String, platform: Platform, repoId: Int? = null): Result<Int> {
        return try {
            when (platform) {
                Platform.GITHUB -> {
                    val response = githubApiService.getRepoCommits(owner, repoName, perPage = 1)
                    val linkHeader = response.headers()["Link"]
                    val count = if (linkHeader != null) {
                        val lastPageRegex = """page=(\d+)>; rel="last"""".toRegex()
                        val match = lastPageRegex.find(linkHeader)
                        match?.groupValues?.get(1)?.toInt() ?: response.body()?.size ?: 0
                    } else {
                        response.body()?.size ?: 0
                    }
                    Result.success(count)
                }
                Platform.GITLAB -> {
                    if (repoId == null) {
                        Result.failure(Exception("Repository ID required for GitLab"))
                    } else {
                        val response = gitlabApiService.getProjectCommits(repoId, perPage = 1)
                        val totalPages = response.headers()["X-Total-Pages"]?.toIntOrNull() ?: 0
                        Result.success(totalPages)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBranches(owner: String, repoName: String, platform: Platform, repoId: Int? = null): Result<List<String>> {
        return try {
            when (platform) {
                Platform.GITHUB -> {
                    val branches = githubApiService.getRepoBranches(owner, repoName)
                    Result.success(branches.map { it.name })
                }
                Platform.GITLAB -> {
                    if (repoId == null) {
                        Result.failure(Exception("Repository ID required for GitLab"))
                    } else {
                        val branches = gitlabApiService.getProjectBranches(repoId)
                        Result.success(branches.map { it.name })
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getContributions(
        username: String,
        platform: Platform,
        userId: Int
    ): Result<ContributionData> {
        return try {
            val dateCount = mutableMapOf<String, Int>()
            val categoryCount = mutableMapOf<String, Int>()
            when (platform) {
                Platform.GITHUB -> {
                    for (page in 1..3) {
                        val events = githubApiService.getUserEvents(username, perPage = 100, page = page)
                        if (events.isEmpty()) break
                        events.forEach { event ->
                            val date = event.createdAt.take(10)
                            dateCount[date] = (dateCount[date] ?: 0) + 1
                            val category = categorizeGitHubEvent(event.type)
                            categoryCount[category] = (categoryCount[category] ?: 0) + 1
                        }
                        if (events.size < 100) break
                    }
                }
                Platform.GITLAB -> {
                    for (page in 1..3) {
                        val events = gitlabApiService.getUserEvents(userId, perPage = 100, page = page)
                        if (events.isEmpty()) break
                        events.forEach { event ->
                            val date = event.createdAt.take(10)
                            dateCount[date] = (dateCount[date] ?: 0) + 1
                            val category = categorizeGitLabEvent(event.actionName, event.targetType)
                            categoryCount[category] = (categoryCount[category] ?: 0) + 1
                        }
                        if (events.size < 100) break
                    }
                }
            }
            Result.success(ContributionData(dateCount, categoryCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReadme(owner: String, repoName: String, platform: Platform, repoId: Int? = null, defaultBranch: String? = null): Result<String> {
        return try {
            when (platform) {
                Platform.GITHUB -> {
                    val readmeResponse = githubApiService.getRepoReadme(owner, repoName)
                    val decodedContent = if (readmeResponse.encoding == "base64") {
                        val cleanContent = readmeResponse.content.replace("\n", "")
                        String(Base64.decode(cleanContent, Base64.DEFAULT))
                    } else {
                        readmeResponse.content
                    }
                    Result.success(decodedContent)
                }
                Platform.GITLAB -> {
                    if (repoId == null) {
                        Result.failure(Exception("Repository ID required for GitLab"))
                    } else {
                        val branch = defaultBranch ?: "main"
                        try {
                            val readmeResponse = gitlabApiService.getProjectReadme(repoId, branch)
                            val decodedContent = if (readmeResponse.encoding == "base64") {
                                val cleanContent = readmeResponse.content.replace("\n", "")
                                String(Base64.decode(cleanContent, Base64.DEFAULT))
                            } else {
                                readmeResponse.content
                            }
                            Result.success(decodedContent)
                        } catch (e: Exception) {
                            // Try with master branch if main fails
                            try {
                                val readmeResponse = gitlabApiService.getProjectReadme(repoId, "master")
                                val decodedContent = if (readmeResponse.encoding == "base64") {
                                    val cleanContent = readmeResponse.content.replace("\n", "")
                                    String(Base64.decode(cleanContent, Base64.DEFAULT))
                                } else {
                                    readmeResponse.content
                                }
                                Result.success(decodedContent)
                            } catch (e2: Exception) {
                                Result.failure(e2)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
