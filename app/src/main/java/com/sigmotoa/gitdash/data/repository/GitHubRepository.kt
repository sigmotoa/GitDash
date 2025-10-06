package com.sigmotoa.gitdash.data.repository

import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.data.model.GitHubUser
import com.sigmotoa.gitdash.data.remote.GitHubApiService

class GitHubRepository(private val apiService: GitHubApiService) {

    suspend fun getUser(username: String): Result<GitHubUser> {
        return try {
            val user = apiService.getUser(username)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRepos(username: String): Result<List<GitHubRepo>> {
        return try {
            val repos = apiService.getUserRepos(username)
            Result.success(repos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}