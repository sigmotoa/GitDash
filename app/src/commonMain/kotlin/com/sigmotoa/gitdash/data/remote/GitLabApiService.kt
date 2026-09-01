package com.sigmotoa.gitdash.data.remote

import com.sigmotoa.gitdash.data.model.GitLabProject
import com.sigmotoa.gitdash.data.model.GitLabUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.appendPathSegments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cliente de la API REST v4 de GitLab sobre Ktor.
 */
class GitLabApiService(private val client: HttpClient) {

    // GitLab no soporta GET /users/{username} — hay que usar ?username= que
    // devuelve una lista; el repositorio toma la primera coincidencia.
    suspend fun searchUsers(username: String): List<GitLabUser> =
        client.get(BASE_URL) {
            url { appendPathSegments("users") }
            parameter("username", username)
        }.body()

    suspend fun getUserProjects(
        username: String,
        perPage: Int = 50,
        orderBy: String = "last_activity_at",
    ): List<GitLabProject> =
        client.get(BASE_URL) {
            url { appendPathSegments("users", username, "projects") }
            parameter("per_page", perPage)
            parameter("order_by", orderBy)
        }.body()

    suspend fun getProjectCommits(projectId: Int, perPage: Int = 1): HttpResponse =
        client.get(BASE_URL) {
            url { appendPathSegments("projects", projectId.toString(), "repository", "commits") }
            parameter("per_page", perPage)
        }

    suspend fun getProjectBranches(projectId: Int, perPage: Int = 100): List<GitLabBranchResponse> =
        client.get(BASE_URL) {
            url { appendPathSegments("projects", projectId.toString(), "repository", "branches") }
            parameter("per_page", perPage)
        }.body()

    suspend fun getProjectReadme(projectId: Int, ref: String = "main"): GitLabReadmeResponse =
        client.get(BASE_URL) {
            url {
                appendPathSegments(
                    "projects", projectId.toString(), "repository", "files", "README.md",
                )
            }
            parameter("ref", ref)
        }.body()

    suspend fun getUserEvents(userId: Int, perPage: Int = 100, page: Int = 1): List<GitLabEventResponse> =
        client.get(BASE_URL) {
            url { appendPathSegments("users", userId.toString(), "events") }
            parameter("per_page", perPage)
            parameter("page", page)
        }.body()

    companion object {
        const val BASE_URL = "https://gitlab.com/api/v4"
    }
}

@Serializable
data class GitLabEventResponse(
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("action_name")
    val actionName: String = "",
    @SerialName("target_type")
    val targetType: String? = null,
    @SerialName("project_id")
    val projectId: Int? = null
)

@Serializable
data class GitLabCommitResponse(
    @SerialName("id")
    val id: String
)

@Serializable
data class GitLabBranchResponse(
    @SerialName("name")
    val name: String,
    @SerialName("protected")
    val protected: Boolean = false
)

@Serializable
data class GitLabReadmeResponse(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("file_path")
    val filePath: String,
    @SerialName("content")
    val content: String,
    @SerialName("encoding")
    val encoding: String
)
