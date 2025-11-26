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

    suspend fun getCommitCount(owner: String, repo: String): Result<Int> {
        return try {
            val response = apiService.getRepoCommits(owner, repo, perPage = 1)
            // Parse the Link header to get total count
            val linkHeader = response.headers()["Link"]
            val count = if (linkHeader != null) {
                // Extract last page number from Link header
                val lastPageRegex = """page=(\d+)>; rel="last"""".toRegex()
                val match = lastPageRegex.find(linkHeader)
                match?.groupValues?.get(1)?.toInt() ?: response.body()?.size ?: 0
            } else {
                response.body()?.size ?: 0
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBranches(owner: String, repo: String): Result<List<String>> {
        return try {
            val branches = apiService.getRepoBranches(owner, repo)
            Result.success(branches.map { it.name })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}