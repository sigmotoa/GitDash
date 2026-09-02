package com.sigmotoa.gitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private const val WEEKS = 18
private val GAP = 2.dp

// java.time daba nombres de mes localizados; kotlinx-datetime no, así que se fija ES/EN corto.
private val MONTH_ABBR = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

// Day-of-week labels: Sun(0)..Sat(6) — show label only on odd rows to save space
private val DAY_ROW_LABELS = listOf("S", "", "T", "", "T", "", "S")

// Canonical display order for event category stats
private val STAT_KEYS = listOf("Commits", "PRs", "Issues", "Comments", "Other")

@Composable
fun ContributionGraph(
    contributionMap: Map<String, Int>,
    categoryCounts: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Align to the Sunday of the current week (isoDayNumber: Mon=1..Sun=7)
    val currentSunday = today.minus(today.dayOfWeek.isoDayNumber % 7, DateTimeUnit.DAY)
    val startSunday = currentSunday.minus(WEEKS - 1, DateTimeUnit.WEEK)

    // Build a list of weeks, each containing 7 (date, count) pairs.
    // count == -1 means the date is in the future — rendered transparent.
    val weeks: List<List<Pair<LocalDate, Int>>> = (0 until WEEKS).map { weekIndex ->
        val weekStart = startSunday.plus(weekIndex, DateTimeUnit.WEEK)
        (0..6).map { day ->
            val date = weekStart.plus(day, DateTimeUnit.DAY)
            date to if (date <= today) (contributionMap[date.toString()] ?: 0) else -1
        }
    }

    // Pre-compute which week column should show a month label
    val monthLabels: Map<Int, String> = buildMap {
        var lastMonth = -1
        weeks.forEachIndexed { idx, week ->
            val first = week.firstOrNull { (_, c) -> c >= 0 }?.first ?: return@forEachIndexed
            if (first.monthNumber != lastMonth) {
                put(idx, MONTH_ABBR[first.monthNumber - 1])
                lastMonth = first.monthNumber
            }
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // BoxWithConstraints lets us compute a cell size that fills the full available width
    BoxWithConstraints(modifier = modifier) {
        val dayLabelWidth = 22.dp              // 20dp label column + 2dp spacer
        val gapsTotal: Dp = GAP * (WEEKS - 1)
        val cellSize: Dp = maxOf((maxWidth - dayLabelWidth - gapsTotal) / WEEKS, 6.dp)

        Column {

            // ── Month labels ──────────────────────────────────────────────
            Row {
                Spacer(modifier = Modifier.width(dayLabelWidth))
                weeks.forEachIndexed { idx, _ ->
                    Box(modifier = Modifier.width(cellSize + GAP)) {
                        monthLabels[idx]?.let { label ->
                            Text(
                                text = label,
                                fontSize = 7.sp,
                                color = onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // ── Day labels + grid ─────────────────────────────────────────
            Row(verticalAlignment = Alignment.Top) {

                // Day-of-week labels (Sun, Mon … Sat)
                Column(
                    verticalArrangement = Arrangement.spacedBy(GAP),
                    modifier = Modifier.width(20.dp)
                ) {
                    DAY_ROW_LABELS.forEach { label ->
                        Box(
                            modifier = Modifier.size(cellSize),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (label.isNotEmpty()) {
                                Text(
                                    text = label,
                                    fontSize = 7.sp,
                                    color = onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Grid of cells
                Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                            week.forEach { (_, count) ->
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(contributionColor(count, primary, empty))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── Legend ────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 22.dp)
            ) {
                Text(text = "Less", fontSize = 8.sp, color = onSurfaceVariant)
                Spacer(modifier = Modifier.width(3.dp))
                listOf(
                    empty,
                    primary.copy(alpha = 0.30f),
                    primary.copy(alpha = 0.55f),
                    primary.copy(alpha = 0.80f),
                    primary
                ).forEach { color ->
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "More", fontSize = 8.sp, color = onSurfaceVariant)
            }

            // ── Event category totals ─────────────────────────────────────
            if (categoryCounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    STAT_KEYS.forEach { key ->
                        val count = categoryCounts[key] ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = key,
                                fontSize = 9.sp,
                                color = onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun contributionColor(count: Int, primary: Color, empty: Color): Color = when {
    count < 0  -> Color.Transparent   // future date
    count == 0 -> empty
    count <= 2 -> primary.copy(alpha = 0.30f)
    count <= 5 -> primary.copy(alpha = 0.55f)
    count <= 9 -> primary.copy(alpha = 0.80f)
    else       -> primary
}
