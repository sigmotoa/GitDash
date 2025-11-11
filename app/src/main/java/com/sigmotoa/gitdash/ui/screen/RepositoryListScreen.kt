package com.sigmotoa.gitdash.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.ui.components.AdMobBanner
import com.sigmotoa.gitdash.ui.components.GitHubSearchBar
import com.sigmotoa.gitdash.ui.utils.LanguageColors
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryListScreen(
    viewModel: GitHubViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repositories") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Shared Search Section
            GitHubSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.loadUser(it) },
                placeholder = "Try 'sigmotoa' to see repositories"
            )

            // Content Section with Pull to Refresh
            when {
                uiState.user == null && !uiState.isLoading -> {
                    EmptyRepositoryState()
                }

                uiState.error != null && uiState.repos.isEmpty() -> {
                    ErrorRepositoryState(error = uiState.error!!)
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            if (uiState.user != null) {
                                isRefreshing = true
                                viewModel.loadUser(uiState.user!!.login)
                                // Simulate refresh delay
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.isLoading && uiState.repos.isEmpty()) {
                            LoadingRepositoryState()
                        } else {
                            RepositoryList(
                                repos = uiState.repos,
                                username = uiState.user?.login
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRepositoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No repositories yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Search for a GitHub user to view their repositories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorRepositoryState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error Loading Repositories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingRepositoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading repositories...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RepositoryList(
    repos: List<GitHubRepo>,
    username: String?
) {
    Column {
        // Header
        if (username != null && repos.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@$username's Repositories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${repos.size} repos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (repos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No public repositories found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(repos, key = { it.id }) { repo ->
                    RepositoryCard(repo = repo)
                }

                // AdMob Banner at the bottom
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    AdMobBanner()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(repo: GitHubRepo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Repository Name
            Text(
                text = repo.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Description
            if (!repo.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Badge
                repo.language?.let { language ->
                    LanguageBadge(language = language)
                }

                // Stars Badge
                if (repo.stargazersCount > 0) {
                    StatBadge(
                        icon = "⭐",
                        count = repo.stargazersCount,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }

                // Forks Badge
                if (repo.forksCount > 0) {
                    StatBadge(
                        icon = "🍴",
                        count = repo.forksCount,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageBadge(language: String) {
    val languageColor = LanguageColors.getColor(language)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = languageColor.copy(alpha = 0.15f),
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(languageColor)
            )

            Text(
                text = language,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = languageColor.copy(alpha = 1f)
            )
        }
    }
}

@Composable
private fun StatBadge(
    icon: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = formatCount(count),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}