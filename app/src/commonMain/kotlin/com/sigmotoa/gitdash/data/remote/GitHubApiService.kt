package com.sigmotoa.gitdash.data.remote

import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.data.model.GitHubUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.appendPathSegments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cliente de la API REST de GitHub sobre Ktor.
 *
 * Los endpoints cuyo repositorio necesita leer cabeceras o el código de estado
 * (paginación por `Link`, `202 Accepted` en estadísticas) devuelven [HttpResponse]
 * en crudo; el resto devuelve el cuerpo ya deserializado.
 */
class GitHubApiService(private val client: HttpClient) {

    suspend fun getUser(username: String): GitHubUser =
        client.get(BASE_URL) {
            url { appendPathSegments("users", username) }
        }.body()

    suspend fun getUserRepos(
        username: String,
        perPage: Int = 50,
        sort: String = "updated",
    ): List<GitHubRepo> =
        client.get(BASE_URL) {
            url { appendPathSegments("users", username, "repos") }
            parameter("per_page", perPage)
            parameter("sort", sort)
        }.body()

    suspend fun getRepoCommits(owner: String, repo: String, perPage: Int = 1): HttpResponse =
        client.get(BASE_URL) {
            url { appendPathSegments("repos", owner, repo, "commits") }
            parameter("per_page", perPage)
        }

    suspend fun getContributorStats(owner: String, repo: String): HttpResponse =
        client.get(BASE_URL) {
            url { appendPathSegments("repos", owner, repo, "stats", "contributors") }
        }

    suspend fun getRepoBranches(owner: String, repo: String, perPage: Int = 100): List<BranchResponse> =
        client.get(BASE_URL) {
            url { appendPathSegments("repos", owner, repo, "branches") }
            parameter("per_page", perPage)
        }.body()

    suspend fun getRepoReadme(owner: String, repo: String): ReadmeResponse =
        client.get(BASE_URL) {
            url { appendPathSegments("repos", owner, repo, "readme") }
        }.body()

    suspend fun getUserEvents(username: String, perPage: Int = 100, page: Int = 1): List<GitHubEventResponse> =
        client.get(BASE_URL) {
            url { appendPathSegments("users", username, "events") }
            parameter("per_page", perPage)
            parameter("page", page)
        }.body()

    companion object {
        const val BASE_URL = "https://api.github.com"
    }
}

@Serializable
data class GitHubEventResponse(
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("type")
    val type: String = "",
    @SerialName("repo")
    val repo: GitHubEventRepo? = null,
    @SerialName("payload")
    val payload: GitHubEventPayload? = null
)

@Serializable
data class GitHubEventRepo(
    @SerialName("name")
    val name: String = ""   // format: "owner/reponame"
)

@Serializable
data class GitHubEventPayload(
    @SerialName("size")
    val size: Int? = null   // number of commits in a PushEvent
)

@Serializable
data class CommitResponse(
    val sha: String
)

@Serializable
data class BranchResponse(
    @SerialName("name")
    val name: String,
    @SerialName("protected")
    val protected: Boolean = false
)

@Serializable
data class ReadmeResponse(
    @SerialName("name")
    val name: String,
    @SerialName("path")
    val path: String,
    @SerialName("content")
    val content: String,
    @SerialName("encoding")
    val encoding: String,
    @SerialName("download_url")
    val downloadUrl: String? = null
)

@Serializable
data class ContributorStatsResponse(
    @SerialName("author")
    val author: ContributorAuthor? = null,
    @SerialName("total")
    val total: Int = 0,
    @SerialName("weeks")
    val weeks: List<WeekStat> = emptyList()
)

@Serializable
data class ContributorAuthor(
    @SerialName("login")
    val login: String = ""
)

@Serializable
data class WeekStat(
    @SerialName("w")
    val weekTimestamp: Long = 0,   // Unix epoch seconds of the Sunday starting the week
    @SerialName("a")
    val additions: Int = 0,
    @SerialName("d")
    val deletions: Int = 0,
    @SerialName("c")
    val commits: Int = 0
)
