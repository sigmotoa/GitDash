package com.sigmotoa.gitdash.data.repository

import com.sigmotoa.gitdash.data.model.Platform
import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.remote.CommitResponse
import com.sigmotoa.gitdash.data.remote.ContributorStatsResponse
import com.sigmotoa.gitdash.data.remote.GitHubApiService
import com.sigmotoa.gitdash.data.remote.GitLabApiService
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Info about the most-recent push captured in the events window. */
data class LastCommitInfo(
    val repoName: String,       // short name (after last "/")
    val repoFullName: String,   // "owner/reponame"
    val date: String            // ISO "yyyy-MM-dd"
)

/** One push event record retained for date-range filtering. */
data class RawEventRecord(
    val date: String,          // YYYY-MM-DD
    val repoShortName: String, // e.g. "my-repo"
    val repoFullName: String,  // "owner/my-repo" — empty for GitLab
    val commitCount: Int
)

data class ContributionData(
    val dateMap: Map<String, Int>,
    val categoryCounts: Map<String, Int>,
    val topPushedRepo: String? = null,
    val topReposByPushes: List<Pair<String, Int>> = emptyList(),
    val lastCommitInfo: LastCommitInfo? = null,
    val rawPushEvents: List<RawEventRecord> = emptyList()
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
                    val linkHeader = response.headers["Link"]
                    val bodySize = runCatching { response.body<List<CommitResponse>>().size }.getOrNull() ?: 0
                    val count = if (linkHeader != null) {
                        val lastPageRegex = """page=(\d+)>; rel="last"""".toRegex()
                        val match = lastPageRegex.find(linkHeader)
                        match?.groupValues?.get(1)?.toInt() ?: bodySize
                    } else {
                        bodySize
                    }
                    Result.success(count)
                }
                Platform.GITLAB -> {
                    if (repoId == null) {
                        Result.failure(Exception("Repository ID required for GitLab"))
                    } else {
                        val response = gitlabApiService.getProjectCommits(repoId, perPage = 1)
                        val totalPages = response.headers["X-Total-Pages"]?.toIntOrNull() ?: 0
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
            val repoPushCount = mutableMapOf<String, Int>()
            val rawPushEvents = mutableListOf<RawEventRecord>()

            var lastPushDate: String? = null
            var lastPushRepoName: String? = null
            var lastPushRepoFull: String? = null

            when (platform) {
                Platform.GITHUB -> {
                    for (page in 1..3) {
                        val events = githubApiService.getUserEvents(username, perPage = 100, page = page)
                        if (events.isEmpty()) break
                        events.forEach { event ->
                            val date = event.createdAt.take(10)
                            dateCount[date] = (dateCount[date] ?: 0) + 1
                            val category = categorizeGitHubEvent(event.type)
                            if (event.type == "PushEvent") {
                                val commitCount = (event.payload?.size ?: 0).let { if (it > 0) it else 1 }
                                categoryCount[category] = (categoryCount[category] ?: 0) + commitCount
                                val repoFullName = event.repo?.name ?: ""
                                val repoName = repoFullName.substringAfterLast("/")
                                if (repoName.isNotEmpty()) {
                                    repoPushCount[repoName] = (repoPushCount[repoName] ?: 0) + commitCount
                                    rawPushEvents.add(RawEventRecord(date, repoName, repoFullName, commitCount))
                                    if (lastPushDate == null) {
                                        lastPushDate = date
                                        lastPushRepoName = repoName
                                        lastPushRepoFull = repoFullName
                                    }
                                }
                            } else {
                                categoryCount[category] = (categoryCount[category] ?: 0) + 1
                            }
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
                            if (category == "Commits") {
                                val projectId = event.projectId?.toString() ?: ""
                                if (projectId.isNotEmpty()) {
                                    rawPushEvents.add(RawEventRecord(date, "project/$projectId", "", 1))
                                }
                            }
                        }
                        if (events.size < 100) break
                    }
                }
            }

            val topReposByPushes = repoPushCount.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key to it.value }
            val topPushedRepo = topReposByPushes.firstOrNull()?.first
            val lastCommitInfo = if (lastPushRepoName != null && lastPushDate != null)
                LastCommitInfo(lastPushRepoName!!, lastPushRepoFull ?: "", lastPushDate!!)
            else null

            Result.success(ContributionData(dateCount, categoryCount, topPushedRepo, topReposByPushes, lastCommitInfo, rawPushEvents))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sums lines added (GitHub only) for [username] across [repoFullNames] within the window
     * [[startEpochSeconds], [endEpochSeconds]) (both in seconds since the Unix epoch, UTC).
     */
    suspend fun getLinesAddedInRange(
        username: String,
        repoFullNames: List<String>,
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        platform: Platform
    ): Int {
        if (platform != Platform.GITHUB) return 0
        val startEpoch = startEpochSeconds
        val endEpoch   = endEpochSeconds
        val weekSecs   = 7L * 24 * 3600
        var totalLines = 0

        for (fullName in repoFullNames.take(5)) {
            val parts = fullName.split("/")
            if (parts.size != 2) continue
            val (owner, repo) = parts
            try {
                val response = githubApiService.getContributorStats(owner, repo)
                if (!response.status.isSuccess() || response.status.value == 202) continue
                val allStats = runCatching { response.body<List<ContributorStatsResponse>>() }.getOrNull() ?: continue
                val userStat = allStats.find { it.author?.login.equals(username, ignoreCase = true) } ?: continue
                totalLines += userStat.weeks
                    .filter { w -> w.weekTimestamp < endEpoch && w.weekTimestamp + weekSecs > startEpoch }
                    .sumOf { it.additions }
            } catch (_: Exception) { /* skip repo on error */ }
        }
        return totalLines
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun getReadme(owner: String, repoName: String, platform: Platform, repoId: Int? = null, defaultBranch: String? = null): Result<String> {
        return try {
            when (platform) {
                Platform.GITHUB -> {
                    val readmeResponse = githubApiService.getRepoReadme(owner, repoName)
                    val decodedContent = if (readmeResponse.encoding == "base64") {
                        val cleanContent = readmeResponse.content.replace("\n", "")
                        Base64.decode(cleanContent).decodeToString()
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
                                Base64.decode(cleanContent).decodeToString()
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
                                    Base64.decode(cleanContent).decodeToString()
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
