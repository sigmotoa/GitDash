package com.sigmotoa.gitdash.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sigmotoa.gitdash.data.model.GitHubRepo
import com.sigmotoa.gitdash.ui.utils.LanguageColors
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryDetailScreen(
    repoId: Int,
    viewModel: GitHubViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val repo = uiState.repos.find { it.id == repoId }
    var commitCount by remember { mutableStateOf<Int?>(null) }
    var isLoadingCommits by remember { mutableStateOf(false) }
    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingBranches by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(repo) {
        if (repo != null) {
            isLoadingCommits = true
            isLoadingBranches = true
            scope.launch {
                val commitResult = viewModel.getCommitCount(repo.owner.login, repo.name)
                commitResult.onSuccess { count ->
                    commitCount = count
                }
                isLoadingCommits = false
            }
            scope.launch {
                val branchResult = viewModel.getBranches(repo.owner.login, repo.name)
                branchResult.onSuccess { branchList ->
                    branches = branchList
                }
                isLoadingBranches = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo?.name ?: "Repository Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (repo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Repository not found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = repo.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!repo.description.isNullOrBlank()) {
                            Text(
                                text = repo.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Star,
                        label = "Stars",
                        value = repo.stargazersCount.toString(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        icon = Icons.Default.Commit,
                        label = "Commits",
                        value = if (isLoadingCommits) "..." else (commitCount?.toString() ?: "N/A"),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        HorizontalDivider()

                        // Language
                        if (repo.language != null) {
                            DetailRow(
                                icon = Icons.Default.Code,
                                label = "Language",
                                value = repo.language
                            ) {
                                val languageColor = LanguageColors.getColor(repo.language)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(languageColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = repo.language,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Created Date
                        if (repo.createdAt != null) {
                            DetailRow(
                                icon = Icons.Default.CalendarToday,
                                label = "Created",
                                value = formatDate(repo.createdAt)
                            )
                        }

                        // Forks
                        DetailRow(
                            icon = null,
                            iconText = "🍴",
                            label = "Forks",
                            value = repo.forksCount.toString()
                        )
                    }
                }

                // Branches Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Branches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLoadingBranches) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "${branches.size} total",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        if (branches.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                branches.forEach { branchName ->
                                    BranchItem(name = branchName)
                                }
                            }
                        } else if (!isLoadingBranches) {
                            Text(
                                text = "No branches found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Open in GitHub Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repo.htmlUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error silently or show a toast
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View on GitHub")
                }
            }
        }
    }
}

@Composable
private fun BranchItem(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🌿",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector? = null,
    iconText: String? = null,
    label: String,
    value: String? = null,
    customValue: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else if (iconText != null) {
            Text(
                text = iconText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (customValue != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    customValue()
                }
            } else if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}
