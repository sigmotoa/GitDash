package com.sigmotoa.gitdash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val WEEKS = 18
private val CELL = 10.dp
private val GAP = 2.dp

// Day-of-week labels: Sun(0)..Sat(6) — show label only on odd rows to save space
private val DAY_ROW_LABELS = listOf("S", "", "T", "", "T", "", "S")

@Composable
fun ContributionGraph(
    contributionMap: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    // Align to the Sunday of the current week (DayOfWeek values: Mon=1..Sun=7)
    val currentSunday = today.minusDays(today.dayOfWeek.value.toLong() % 7)
    val startSunday = currentSunday.minusWeeks((WEEKS - 1).toLong())

    // Build a list of weeks, each containing 7 (date, count) pairs.
    // count == -1 means the date is in the future — rendered transparent.
    val weeks: List<List<Pair<LocalDate, Int>>> = (0 until WEEKS).map { weekIndex ->
        val weekStart = startSunday.plusWeeks(weekIndex.toLong())
        (0..6).map { day ->
            val date = weekStart.plusDays(day.toLong())
            date to if (!date.isAfter(today)) (contributionMap[date.toString()] ?: 0) else -1
        }
    }

    // Pre-compute which week column should show a month label
    val monthLabels: Map<Int, String> = buildMap {
        var lastMonth = -1
        weeks.forEachIndexed { idx, week ->
            val first = week.firstOrNull { (_, c) -> c >= 0 }?.first ?: return@forEachIndexed
            if (first.monthValue != lastMonth) {
                put(idx, first.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                lastMonth = first.monthValue
            }
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {

        // ── Month labels ──────────────────────────────────────────────
        Row {
            Spacer(modifier = Modifier.width(22.dp))
            weeks.forEachIndexed { idx, _ ->
                Box(modifier = Modifier.width(CELL + GAP)) {
                    monthLabels[idx]?.let { label ->
                        Text(
                            text = label,
                            fontSize = 7.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        modifier = Modifier.size(CELL),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                fontSize = 7.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    .size(CELL)
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
            Text(
                text = "Less",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Text(
                text = "More",
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
