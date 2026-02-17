package com.sigmotoa.gitdash.data.remote

import com.sigmotoa.gitdash.data.model.GitLabProject
import com.sigmotoa.gitdash.data.model.GitLabUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitLabApiService {

    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitLabUser

    @GET("users/{username}/projects")
    suspend fun getUserProjects(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 50,
        @Query("order_by") orderBy: String = "last_activity_at"
    ): List<GitLabProject>

    @GET("projects/{id}/repository/commits")
    suspend fun getProjectCommits(
        @Path("id") projectId: Int,
        @Query("per_page") perPage: Int = 1
    ): Response<List<GitLabCommitResponse>>

    @GET("projects/{id}/repository/branches")
    suspend fun getProjectBranches(
        @Path("id") projectId: Int,
        @Query("per_page") perPage: Int = 100
    ): List<GitLabBranchResponse>

    @GET("projects/{id}/repository/files/README.md")
    suspend fun getProjectReadme(
        @Path("id") projectId: Int,
        @Query("ref") ref: String = "main"
    ): GitLabReadmeResponse

    companion object {
        const val BASE_URL = "https://gitlab.com/api/v4/"
    }
}

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
