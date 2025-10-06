package com.sigmotoa.gitdash.data.remote

import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.data.model.GitHubUser
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubApiService {

    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getUserRepos(@Path("username") username: String): List<GitHubRepo>

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}