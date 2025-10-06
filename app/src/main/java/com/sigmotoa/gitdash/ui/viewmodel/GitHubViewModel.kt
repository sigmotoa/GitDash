package com.sigmotoa.gitdash.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.data.model.GitHubUser
import com.sigmotoa.gitdash.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GitHubUiState(
    val user: GitHubUser? = null,
    val repos: List<GitHubRepo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GitHubViewModel(private val repository: GitHubRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GitHubUiState())
    val uiState: StateFlow<GitHubUiState> = _uiState.asStateFlow()

    fun loadUser(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getUser(username).fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(user = user, isLoading = false)
                    loadUserRepos(username)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Unknown error",
                        isLoading = false
                    )
                }
            )
        }
    }

    private fun loadUserRepos(username: String) {
        viewModelScope.launch {
            repository.getUserRepos(username).fold(
                onSuccess = { repos ->
                    _uiState.value = _uiState.value.copy(repos = repos)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Failed to load repos"
                    )
                }
            )
        }
    }
}