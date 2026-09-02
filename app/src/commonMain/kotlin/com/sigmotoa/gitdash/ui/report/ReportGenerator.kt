package com.sigmotoa.gitdash.ui.report

import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.remote.ApiClient
import com.sigmotoa.gitdash.data.repository.LastCommitInfo
import com.sigmotoa.gitdash.data.repository.RawEventRecord
import com.sigmotoa.gitdash.ui.utils.MONTHS_SHORT
import com.sigmotoa.gitdash.ui.utils.oneDecimal
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn

/**
 * Genera los PDF de informes. El **layout** es compartido; solo el dibujado de
 * primitivas es específico de plataforma (`renderSinglePagePdf` / [PdfCanvas]).
 *
 * Página A4 a 72 dpi (595 x 842 pt), una sola página.
 */
object ReportGenerator {

    private const val PAGE_W = 595f
    private const val PAGE_H = 842f
    private const val MARGIN = 44f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN

    private const val WHITE = 0xFFFFFFFFL
    private const val TEXT = 0xFF212121L
    private const val GRAY = 0xFF757575L
    private const val DIVIDER = 0xFFBDBDBDL

    // ─────────────────────────────────────────────────────────────────────────
    //  Profile report
    // ─────────────────────────────────────────────────────────────────────────

    private const val P_HEADER = 0xFF1A237EL
    private const val P_ACCENT = 0xFF1565C0L
    private const val P_SUB = 0xFF90CAF9L

    suspend fun profileReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        categoryCounts: Map<String, Int>,
        dateMap: Map<String, Int>,
        topReposByPushes: List<Pair<String, Int>>,
        lastCommitInfo: LastCommitInfo?,
    ): ByteArray {
        val totalStars = repos.sumOf { it.starCount }
        val commits = categoryCounts["Commits"] ?: 0
        val topLanguages = repos.mapNotNull { it.language }
            .groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(3).map { it.key }
        val topReposByStars = repos.sortedByDescending { it.starCount }.take(3)

        val windowStart = dateMap.keys.minOrNull()
        val windowEnd = today().toString()
        val lastCommitLang = lastCommitInfo?.let { info ->
            repos.find { it.name.equals(info.repoName, ignoreCase = true) }?.language
        }
        val avatar = user.avatarUrl?.takeIf { it.isNotBlank() }?.let { ApiClient.downloadBytes(it) }

        return renderSinglePagePdf(PAGE_W, PAGE_H) {
            var y: Float

            val headerH = 88f
            val avatarR = 26f
            val avatarCx = PAGE_W - MARGIN - avatarR
            val avatarCy = headerH / 2f

            fillRect(0f, 0f, PAGE_W, headerH, P_HEADER)
            text("GitDash", MARGIN, 44f, 26f, WHITE, bold = true)
            text("Profile Report  ·  ${user.platform.displayName}", MARGIN, 66f, 11f, P_SUB)
            val dateRight = if (avatar != null) avatarCx - avatarR - 8f else PAGE_W - MARGIN
            text(longDate(today()), dateRight, 66f, 11f, P_SUB, align = PdfAlign.RIGHT)
            if (avatar != null) circleImage(avatar, avatarCx, avatarCy, avatarR)

            y = headerH + 26f
            text("@${user.username}", MARGIN, y, 22f, P_ACCENT, bold = true); y += 26f
            user.name?.let { text(it, MARGIN, y, 14f, TEXT); y += 20f }
            user.location?.let { text(it, MARGIN, y, 11f, GRAY); y += 18f }
            user.company?.let { text(it, MARGIN, y, 11f, GRAY); y += 18f }

            y += 10f; divider(y); y += 20f

            label("PROFILE STATISTICS", y); y += 26f
            val c1 = MARGIN
            val c2 = MARGIN + CONTENT_W / 3f
            val c3 = MARGIN + CONTENT_W * 2f / 3f
            stat(c1, y, user.followers.toString(), "Followers", P_ACCENT, 22f)
            stat(c2, y, user.following.toString(), "Following", P_ACCENT, 22f)
            stat(c3, y, user.publicRepos.toString(), "Public Repos", P_ACCENT, 22f)
            y += 50f
            stat(c1, y, totalStars.toString(), "Total Stars", P_ACCENT, 22f)
            stat(c2, y, commits.toString(), "Commits (window)", P_ACCENT, 22f)
            y += 50f
            divider(y); y += 20f

            val windowLabel = if (windowStart != null) {
                "${mediumDate(windowStart)} – ${mediumDate(windowEnd)}"
            } else {
                "Until ${mediumDate(windowEnd)}"
            }
            text("ACTIVITY BREAKDOWN", MARGIN, y, 9f, GRAY)
            text(windowLabel, PAGE_W - MARGIN, y, 9f, GRAY, align = PdfAlign.RIGHT)
            y += 26f
            val statKeys = listOf("Commits", "PRs", "Issues", "Comments", "Other")
            val colW = CONTENT_W / statKeys.size
            statKeys.forEachIndexed { i, key ->
                stat(MARGIN + i * colW, y, (categoryCounts[key] ?: 0).toString(), key, P_ACCENT, 22f)
            }
            y += 50f
            divider(y); y += 20f

            label("MOST RECENT PUSH", y); y += 26f
            if (lastCommitInfo != null) {
                val parts = buildList {
                    add(lastCommitInfo.repoName)
                    add(mediumDate(lastCommitInfo.date))
                    lastCommitLang?.let { add(it) }
                }
                text(parts.joinToString("   ·   "), MARGIN, y, 12f, TEXT, bold = true)
            } else {
                text("—", MARGIN, y, 12f, TEXT)
            }
            y += 30f
            divider(y); y += 20f

            label("TOP REPOSITORIES", y); y += 22f
            val colLeft = MARGIN
            val colRight = MARGIN + CONTENT_W / 2f + 10f
            text("By Stars", colLeft, y, 10f, P_ACCENT, bold = true)
            text("By Activity", colRight, y, 10f, P_ACCENT, bold = true)
            y += 20f
            val rows = maxOf(topReposByStars.size, topReposByPushes.size).coerceAtMost(3)
            repeat(rows) { i ->
                topReposByStars.getOrNull(i)?.let {
                    text("${i + 1}. ${it.name.take(20)}  (${it.starCount} stars)", colLeft, y, 10f, TEXT)
                }
                topReposByPushes.getOrNull(i)?.let { (name, count) ->
                    text("${i + 1}. ${name.take(20)}  ($count commits)", colRight, y, 10f, TEXT)
                }
                y += 20f
            }
            y += 8f; divider(y); y += 20f

            label("TOP 3 LANGUAGES", y); y += 26f
            if (topLanguages.isEmpty()) {
                text("—", MARGIN, y, 13f, TEXT)
            } else {
                topLanguages.forEachIndexed { i, lang ->
                    text("0${i + 1}", MARGIN, y, 13f, P_SUB, bold = true)
                    text(lang, MARGIN + 34f, y, 14f, TEXT, bold = true)
                    y += 22f
                }
            }

            footer()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Diff / activity-range report
    // ─────────────────────────────────────────────────────────────────────────

    private const val D_HEADER = 0xFF1B5E20L
    private const val D_ACCENT = 0xFF2E7D32L
    private const val D_SUB = 0xFFA5D6A7L

    suspend fun diffReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        rawPushEvents: List<RawEventRecord>,
        startDateIso: String,
        endDateIso: String,
        linesAdded: Int,
    ): ByteArray {
        val start = LocalDate.parse(startDateIso)
        val end = LocalDate.parse(endDateIso)

        val inRange = rawPushEvents.filter { it.date in startDateIso..endDateIso }
        val totalCommits = inRange.sumOf { it.commitCount }
        val repoCommits = inRange.groupBy { it.repoShortName }
            .mapValues { (_, l) -> l.sumOf { it.commitCount } }
            .entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        val mostActiveRepo = repoCommits.firstOrNull()
        val dateToCommits = inRange.groupBy { it.date }
            .mapValues { (_, l) -> l.sumOf { it.commitCount } }
        val mostActiveDay = dateToCommits.maxByOrNull { it.value }
        val activeDays = dateToCommits.keys.size
        val totalDays = start.daysUntil(end) + 1
        val dailyAvg = if (totalDays > 0) totalCommits.toFloat() / totalDays else 0f
        val topLanguages = repos.mapNotNull { it.language }
            .groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(3).map { it.key }
        val avatar = user.avatarUrl?.takeIf { it.isNotBlank() }?.let { ApiClient.downloadBytes(it) }

        return renderSinglePagePdf(PAGE_W, PAGE_H) {
            var y: Float

            val headerH = 96f
            val avatarR = 26f
            val avatarCx = PAGE_W - MARGIN - avatarR
            val avatarCy = headerH / 2f

            fillRect(0f, 0f, PAGE_W, headerH, D_HEADER)
            text("GitDash", MARGIN, 38f, 26f, WHITE, bold = true)
            text("Activity Report  ·  ${user.platform.displayName}", MARGIN, 56f, 11f, D_SUB)
            text("@${user.username}", MARGIN, 78f, 13f, D_SUB, bold = true)
            val dateRight = if (avatar != null) avatarCx - avatarR - 8f else PAGE_W - MARGIN
            text("${mediumDate(start)}  →  ${mediumDate(end)}", dateRight, 78f, 11f, D_SUB, align = PdfAlign.RIGHT)
            if (avatar != null) circleImage(avatar, avatarCx, avatarCy, avatarR)

            y = headerH + 28f
            label("ACTIVITY SUMMARY", y); y += 26f
            val c1 = MARGIN
            val c2 = MARGIN + CONTENT_W / 3f
            val c3 = MARGIN + CONTENT_W * 2f / 3f
            stat(c1, y, totalCommits.toString(), "Commits in range", D_ACCENT, 14f)
            val repoLabel = (mostActiveRepo?.first?.take(18) ?: "—") +
                (mostActiveRepo?.let { " (${it.second})" } ?: "")
            stat(c2, y, repoLabel, "Most active repo", D_ACCENT, 14f)
            stat(
                c3, y,
                if (linesAdded > 0) "+$linesAdded" else "N/A",
                if (linesAdded > 0) "Lines added" else "Lines added (GitHub)",
                D_ACCENT, 14f,
            )
            y += 56f
            divider(y); y += 20f

            label("TOP REPOSITORIES IN PERIOD", y); y += 26f
            if (repoCommits.isEmpty()) {
                text("No commit data in this range", MARGIN, y, 11f, GRAY); y += 22f
            } else {
                val max = repoCommits.first().second
                repoCommits.forEachIndexed { i, (name, count) ->
                    val barW = (count.toFloat() / max) * (CONTENT_W * 0.4f)
                    text("${i + 1}.", MARGIN, y, 11f, D_ACCENT, bold = true)
                    text(name.take(22), MARGIN + 22f, y, 11f, TEXT)
                    text("$count commits", PAGE_W - MARGIN, y, 10f, GRAY, align = PdfAlign.RIGHT)
                    fillRect(MARGIN + 22f, y + 4f, barW, 5f, D_ACCENT)
                    y += 22f
                }
            }
            y += 6f; divider(y); y += 20f

            label("ACTIVITY HIGHLIGHTS", y); y += 26f
            stat(c1, y, "${dailyAvg.oneDecimal()} commits/day", "Daily average", D_ACCENT, 14f)
            stat(c2, y, "$activeDays active days out of $totalDays days", "Commitment rate", D_ACCENT, 14f)
            stat(
                c3, y,
                mostActiveDay?.let { "${it.value} commits" } ?: "—",
                mostActiveDay?.let { "Peak: ${mediumDate(it.key)}" } ?: "Most active day",
                D_ACCENT, 14f,
            )
            y += 56f
            divider(y); y += 20f

            label("TOP LANGUAGES (ALL REPOS)", y); y += 26f
            if (topLanguages.isEmpty()) {
                text("—", MARGIN, y, 13f, TEXT)
            } else {
                topLanguages.forEachIndexed { i, lang ->
                    text("0${i + 1}", MARGIN, y, 13f, D_SUB, bold = true)
                    text(lang, MARGIN + 34f, y, 14f, TEXT, bold = true)
                    y += 22f
                }
            }

            footer()
        }
    }

    // ── Drawing helpers (extensions on the platform canvas) ───────────────────

    private fun PdfCanvas.divider(y: Float) =
        line(MARGIN, y, PAGE_W - MARGIN, y, DIVIDER, 0.8f)

    private fun PdfCanvas.label(s: String, y: Float) =
        text(s, MARGIN, y, 9f, GRAY)

    private fun PdfCanvas.stat(x: Float, y: Float, value: String, label: String, accent: Long, valueSize: Float) {
        text(value, x, y, valueSize, accent, bold = true)
        text(label, x, y + 16f, if (valueSize >= 20f) 10f else 9f, GRAY)
    }

    private fun PdfCanvas.footer() {
        divider(PAGE_H - 38f)
        text(
            "Generated by GitDash  ·  ${today()}",
            PAGE_W / 2f, PAGE_H - 18f, 9f, GRAY, align = PdfAlign.CENTER,
        )
    }

    // ── Dates ────────────────────────────────────────────────────────────────

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun longDate(d: LocalDate): String =
        "${monthName(d.monthNumber)} ${d.dayOfMonth}, ${d.year}"

    private fun mediumDate(d: LocalDate): String =
        "${MONTHS_SHORT[d.monthNumber - 1]} ${d.dayOfMonth}, ${d.year}"

    private fun mediumDate(iso: String): String =
        runCatching { mediumDate(LocalDate.parse(iso)) }.getOrDefault(iso)

    private fun monthName(m: Int): String = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )[m - 1]
}
