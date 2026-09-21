package com.sigmotoa.gitdash.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.data.model.GitHubUser
import com.sigmotoa.gitdash.data.model.Platform
import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.repository.ContributionData
import com.sigmotoa.gitdash.data.repository.GitHubRepository
import com.sigmotoa.gitdash.data.repository.UnifiedRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Traduce excepciones de red/HTTP a mensajes legibles. Sin esto, un error de
 * la API (ej. límite de rate-limit sin autenticación, o un usuario que no
 * existe) se mostraba como el texto crudo de la excepción de Ktor.
 */
private fun friendlyErrorMessage(exception: Throwable): String {
    val status = (exception as? ResponseException)?.response?.status
    return when (status) {
        HttpStatusCode.NotFound -> "User not found. Check the username and try again."
        HttpStatusCode.Forbidden, HttpStatusCode.TooManyRequests ->
            "Too many requests right now — please wait a moment and try again."
        null -> "Couldn't connect. Check your internet connection and try again."
        else -> "Something went wrong. Please try again."
    }
}

data class GitHubUiState(
    val user: GitHubUser? = null,
    val repos: List<GitHubRepo> = emptyList(),
    val unifiedUser: UnifiedUser? = null,
    val unifiedRepos: List<UnifiedRepo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedPlatform: Platform = Platform.GITHUB
)

class GitHubViewModel(
    private val repository: GitHubRepository,
    private val unifiedRepository: UnifiedRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GitHubUiState())
    val uiState: StateFlow<GitHubUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updatePlatform(platform: Platform) {
        _uiState.value = _uiState.value.copy(selectedPlatform = platform)
    }

    fun loadUser(username: String) {
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                searchQuery = username
            )

            val platform = _uiState.value.selectedPlatform
            unifiedRepository.getUser(username, platform).fold(
                onSuccess = { unifiedUser ->
                    _uiState.value = _uiState.value.copy(
                        unifiedUser = unifiedUser,
                        isLoading = false
                    )
                    loadUserRepos(username, platform)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = friendlyErrorMessage(exception),
                        isLoading = false
                    )
                }
            )
        }
    }

    private fun loadUserRepos(username: String, platform: Platform) {
        viewModelScope.launch {
            unifiedRepository.getUserRepos(username, platform).fold(
                onSuccess = { repos ->
                    _uiState.value = _uiState.value.copy(unifiedRepos = repos)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = friendlyErrorMessage(exception)
                    )
                }
            )
        }
    }

    suspend fun getCommitCount(owner: String, repo: String, platform: Platform, repoId: Int? = null): Result<Int> {
        return unifiedRepository.getCommitCount(owner, repo, platform, repoId)
    }

    suspend fun getBranches(owner: String, repo: String, platform: Platform, repoId: Int? = null): Result<List<String>> {
        return unifiedRepository.getBranches(owner, repo, platform, repoId)
    }

    suspend fun getReadme(owner: String, repo: String, platform: Platform, repoId: Int? = null, defaultBranch: String? = null): Result<String> {
        return unifiedRepository.getReadme(owner, repo, platform, repoId, defaultBranch)
    }

    suspend fun getContributions(username: String, platform: Platform, userId: Int): Result<ContributionData> {
        return unifiedRepository.getContributions(username, platform, userId)
    }

    suspend fun getLinesAddedInRange(
        username: String,
        repos: List<String>,
        startEpochSeconds: Long,
        endEpochSeconds: Long,
        platform: Platform
    ): Int = unifiedRepository.getLinesAddedInRange(username, repos, startEpochSeconds, endEpochSeconds, platform)
}