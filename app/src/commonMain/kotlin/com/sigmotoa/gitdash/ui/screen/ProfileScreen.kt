package com.sigmotoa.gitdash.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sigmotoa.gitdash.data.model.GitHubUser
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.repository.LastCommitInfo
import com.sigmotoa.gitdash.data.repository.RawEventRecord
import com.sigmotoa.gitdash.ui.components.AdMobBanner
import com.sigmotoa.gitdash.ui.components.ContributionGraph
import com.sigmotoa.gitdash.ui.components.GitHubSearchBar
import com.sigmotoa.gitdash.ui.platform.rememberFileSharer
import com.sigmotoa.gitdash.ui.platform.rememberRewardedAdController
import com.sigmotoa.gitdash.ui.platform.sharePdf
import com.sigmotoa.gitdash.ui.report.ReportGenerator
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: GitHubViewModel,
    onUserInteraction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope   = rememberCoroutineScope()
    val fileSharer = rememberFileSharer()
    val rewardedAd = rememberRewardedAdController()

    var contributionMap      by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var categoryCounts       by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var topPushedRepo        by remember { mutableStateOf<String?>(null) }
    var topReposByPushes     by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var lastCommitInfo       by remember { mutableStateOf<LastCommitInfo?>(null) }
    var rawPushEvents        by remember { mutableStateOf<List<RawEventRecord>>(emptyList()) }
    var contributionLoading  by remember { mutableStateOf(false) }
    var isGeneratingReport   by remember { mutableStateOf(false) }
    var showDateRangeDialog  by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.unifiedUser) {
        val user = uiState.unifiedUser
        if (user != null) {
            contributionMap   = emptyMap()
            categoryCounts    = emptyMap()
            topPushedRepo     = null
            topReposByPushes  = emptyList()
            lastCommitInfo    = null
            rawPushEvents     = emptyList()
            contributionLoading = true
            viewModel.getContributions(user.username, user.platform, user.id).fold(
                onSuccess = { data ->
                    contributionMap   = data.dateMap
                    categoryCounts    = data.categoryCounts
                    topPushedRepo     = data.topPushedRepo
                    topReposByPushes  = data.topReposByPushes
                    lastCommitInfo    = data.lastCommitInfo
                    rawPushEvents     = data.rawPushEvents
                    contributionLoading = false
                },
                onFailure = {
                    contributionLoading = false
                }
            )
        } else {
            contributionMap   = emptyMap()
            categoryCounts    = emptyMap()
            topPushedRepo     = null
            topReposByPushes  = emptyList()
            lastCommitInfo    = null
            rawPushEvents     = emptyList()
            contributionLoading = false
        }
    }

    // Generates the PDF on IO thread then fires the system share sheet.
    // Called from the reward callback (or as fallback if ad fails to load).
    val doSharePdf: () -> Unit = {
        val currentUser = uiState.unifiedUser
        if (currentUser != null) {
            scope.launch(Dispatchers.Main) {
                try {
                    // El renderer de PDF de iOS usa UIKit y debe correr en el hilo
                    // principal; la descarga del avatar es suspend y no bloquea.
                    val pdf = ReportGenerator.profileReport(
                        user             = currentUser,
                        repos            = uiState.unifiedRepos,
                        categoryCounts   = categoryCounts,
                        dateMap          = contributionMap,
                        topReposByPushes = topReposByPushes,
                        lastCommitInfo   = lastCommitInfo
                    )
                    fileSharer.sharePdf(pdf, "GitDash-Profile-${currentUser.username}.pdf")
                } finally {
                    isGeneratingReport = false
                }
            }
        } else {
            isGeneratingReport = false
        }
    }

    // Entry point: show rewarded ad first; the PDF is the reward.
    val generateAndShareReport = {
        val user = uiState.unifiedUser
        if (user != null && !isGeneratingReport) {
            isGeneratingReport = true
            rewardedAd.show(
                onReward    = { doSharePdf() },
                onCancelled = { isGeneratingReport = false },
            )
        }
    }

    // ── Date-range diff report: PDF generation (the reward action) ─────────
    // start/end are UTC-midnight epoch millis, straight from the date-range picker.
    val doShareDiffPdf: (Long, Long) -> Unit = { startMillis, endMillis ->
        val currentUser = uiState.unifiedUser
        if (currentUser != null) {
            scope.launch(Dispatchers.Main) {
                try {
                    val startIso = Instant.fromEpochMilliseconds(startMillis)
                        .toLocalDateTime(TimeZone.UTC).date.toString()
                    val endIso = Instant.fromEpochMilliseconds(endMillis)
                        .toLocalDateTime(TimeZone.UTC).date.toString()

                    val reposInRange = rawPushEvents
                        .filter { it.date in startIso..endIso }
                        .groupBy { it.repoFullName }
                        .mapValues { (_, list) -> list.sumOf { it.commitCount } }
                        .entries
                        .sortedByDescending { it.value }
                        .take(5)
                        .map { it.key }
                        .filter { it.isNotEmpty() }

                    val linesAdded = viewModel.getLinesAddedInRange(
                        currentUser.username,
                        reposInRange,
                        startMillis / 1000,
                        endMillis / 1000 + 86_400,   // end date + 1 day (exclusive)
                        currentUser.platform
                    )

                    val pdf = ReportGenerator.diffReport(
                        user          = currentUser,
                        repos         = uiState.unifiedRepos,
                        rawPushEvents = rawPushEvents,
                        startDateIso  = startIso,
                        endDateIso    = endIso,
                        linesAdded    = linesAdded
                    )
                    fileSharer.sharePdf(
                        pdf,
                        "GitDash-Activity-${currentUser.username}-${startIso}_$endIso.pdf",
                    )
                } finally {
                    isGeneratingReport = false
                }
            }
        } else {
            isGeneratingReport = false
        }
    }

    // Entry point for diff report: rewarded ad → PDF as reward.
    val generateAndShareDiffReport: (Long, Long) -> Unit = { startMillis, endMillis ->
        val user = uiState.unifiedUser
        if (user != null && !isGeneratingReport) {
            isGeneratingReport   = true
            showDateRangeDialog  = false
            rewardedAd.show(
                onReward    = { doShareDiffPdf(startMillis, endMillis) },
                onCancelled = { isGeneratingReport = false },
            )
        }
    }

    // ── Date-range diff report dialog ──────────────────────────────────────
    if (showDateRangeDialog) {
        val currentUser = uiState.unifiedUser
        if (currentUser != null) {
            DateRangeReportDialog(
                username  = currentUser.username,
                onDismiss = { showDateRangeDialog = false },
                onGenerate = generateAndShareDiffReport,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${uiState.selectedPlatform.displayName} Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (uiState.unifiedUser != null) {
                        if (isGeneratingReport) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(end = 12.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            // Date-range diff report
                            IconButton(
                                onClick = { showDateRangeDialog = true },
                                enabled = rawPushEvents.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Activity Report by Date Range",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            // Full profile report
                            IconButton(onClick = generateAndShareReport) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Generate PDF Report",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
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
                selectedPlatform = uiState.selectedPlatform,
                onPlatformChange = { viewModel.updatePlatform(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content Section
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }

                uiState.error != null -> {
                    ErrorContent(error = uiState.error!!)
                }

                uiState.unifiedUser != null -> {
                    UnifiedProfileContent(
                        user = uiState.unifiedUser!!,
                        contributionMap = contributionMap,
                        categoryCounts = categoryCounts,
                        contributionLoading = contributionLoading
                    )
                }

                else -> {
                    EmptyContent()
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
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
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Search for a user to view their profile",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UnifiedProfileContent(
    user: UnifiedUser,
    contributionMap: Map<String, Int> = emptyMap(),
    categoryCounts: Map<String, Int> = emptyMap(),
    contributionLoading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "User avatar",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name and Username
        Text(
            text = user.name ?: user.username,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "@${user.username}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Platform Badge
        SuggestionChip(
            onClick = { },
            label = { Text(user.platform.displayName) },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Bio
        user.bio?.let { bio ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Additional Info
        if (user.company != null || user.location != null || user.blog != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    user.company?.let {
                        InfoRow(label = "Company", value = it)
                    }
                    user.location?.let {
                        InfoRow(label = "Location", value = it)
                    }
                    user.blog?.let {
                        if (it.isNotBlank()) {
                            InfoRow(label = "Website/Email", value = it)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (user.publicRepos > 0) {
                        StatColumn(label = "Repositories", value = user.publicRepos)
                        VerticalDivider(modifier = Modifier.height(48.dp))
                    }
                    StatColumn(label = "Followers", value = user.followers)
                    VerticalDivider(modifier = Modifier.height(48.dp))
                    StatColumn(label = "Following", value = user.following)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contribution heatmap
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Activity (last ~4 months)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (contributionLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (contributionMap.isNotEmpty()) {
                    ContributionGraph(
                        contributionMap = contributionMap,
                        categoryCounts = categoryCounts,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (!contributionLoading) {
                    Text(
                        text = "No public activity found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AdMob Banner
        AdMobBanner(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileContent(user: GitHubUser) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "User avatar",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name and Username
        Text(
            text = user.name ?: user.login,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "@${user.login}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bio
        user.bio?.let { bio ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Additional Info
        if (user.company != null || user.location != null || user.blog != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    user.company?.let {
                        InfoRow(label = "Company", value = it)
                    }
                    user.location?.let {
                        InfoRow(label = "Location", value = it)
                    }
                    user.blog?.let {
                        if (it.isNotBlank()) {
                            InfoRow(label = "Website", value = it)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(label = "Repositories", value = user.publicRepos)
                    VerticalDivider(modifier = Modifier.height(48.dp))
                    StatColumn(label = "Followers", value = user.followers)
                    VerticalDivider(modifier = Modifier.height(48.dp))
                    StatColumn(label = "Following", value = user.following)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AdMob Banner
        AdMobBanner(
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatColumn(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun VerticalDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.width(1.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
    )
}