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

/**
 * Full-screen dialog for selecting a date range.
 *
 * [onGenerate] receives the selected start/end as UTC-midnight epoch millis
 * (exactly what the Material date-range picker produces). The caller is
 * responsible for showing the rewarded interstitial and generating the PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeReportDialog(
    username: String,
    onDismiss: () -> Unit,
    onGenerate: (startMillis: Long, endMillis: Long) -> Unit
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
                                    onGenerate(startMillis, endMillis)
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