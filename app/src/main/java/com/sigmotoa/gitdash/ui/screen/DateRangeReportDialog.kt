package com.sigmotoa.gitdash.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Full-screen dialog for selecting a date range.
 *
 * [onGenerate] is a plain (non-suspend) callback invoked when the user taps
 * "Generate" with a valid range. The caller is responsible for showing the
 * rewarded interstitial and then generating the PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeReportDialog(
    username: String,
    onDismiss: () -> Unit,
    onGenerate: (LocalDate, LocalDate) -> Unit
) {
    val rangeState  = rememberDateRangePickerState()

    val startMillis = rangeState.selectedStartDateMillis
    val endMillis   = rangeState.selectedEndDateMillis
    val canGenerate = startMillis != null && endMillis != null

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                TopAppBar(
                    title = {
                        Column {
                            Text("Activity Report")
                            Text(
                                text  = "@$username",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (startMillis != null && endMillis != null) {
                                    val start = Instant.ofEpochMilli(startMillis)
                                        .atZone(ZoneOffset.UTC).toLocalDate()
                                    val end   = Instant.ofEpochMilli(endMillis)
                                        .atZone(ZoneOffset.UTC).toLocalDate()
                                    onGenerate(start, end)
                                }
                            },
                            enabled = canGenerate
                        ) {
                            Text("Generate")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                DateRangePicker(
                    state    = rangeState,
                    modifier = Modifier.weight(1f),
                    title    = {
                        Text(
                            text     = "Select the date range for the report",
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                            style    = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }
    }
}