package com.sigmotoa.gitdash.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sigmotoa.gitdash.data.model.Platform

@Composable
fun GitHubSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    selectedPlatform: Platform,
    onPlatformChange: (Platform) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Platform Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Platform:",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )

                FilterChip(
                    selected = selectedPlatform == Platform.GITHUB,
                    onClick = { onPlatformChange(Platform.GITHUB) },
                    label = { Text("GitHub") },
                    leadingIcon = if (selectedPlatform == Platform.GITHUB) {
                        {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )

                FilterChip(
                    selected = selectedPlatform == Platform.GITLAB,
                    onClick = { onPlatformChange(Platform.GITLAB) },
                    label = { Text("GitLab") },
                    leadingIcon = if (selectedPlatform == Platform.GITLAB) {
                        {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }

            // Search Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter ${selectedPlatform.displayName} username") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.isNotBlank()) {
                                onSearch(query)
                            }
                        }
                    )
                )

                Button(
                    onClick = {
                        if (query.isNotBlank()) {
                            onSearch(query)
                        }
                    },
                    enabled = query.isNotBlank(),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Search")
                }
            }
        }
    }
}