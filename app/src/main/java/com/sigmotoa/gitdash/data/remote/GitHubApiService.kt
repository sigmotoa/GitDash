package com.sigmotoa.gitdash.data.remote

import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.data.model.GitHubUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {

    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getUserRepos(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 50,
        @Query("sort") sort: String = "updated"
    ): List<GitHubRepo>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getRepoCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 1
    ): Response<List<CommitResponse>>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getRepoBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): List<BranchResponse>

    @GET("repos/{owner}/{repo}/readme")
    suspend fun getRepoReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): ReadmeResponse

    @GET("users/{username}/events")
    suspend fun getUserEvents(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<GitHubEventResponse>

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}

@Serializable
data class GitHubEventResponse(
    @SerialName("created_at")
    val createdAt: String
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