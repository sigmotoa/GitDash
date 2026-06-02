package com.sigmotoa.gitdash.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.repository.RawEventRecord
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Generates a single-page A4 PDF showing activity differences within a chosen date window.
 * Metrics: commits in range, most active repo, lines added (GitHub only), top repos, highlights.
 */
object DiffReportGenerator {

    private const val PAGE_W   = 595f
    private const val PAGE_H   = 842f
    private const val MARGIN   = 44f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN

    private val C_HEADER  = Color.parseColor("#1B5E20")
    private val C_ACCENT  = Color.parseColor("#2E7D32")
    private val C_SUB     = Color.parseColor("#A5D6A7")
    private val C_TEXT    = Color.parseColor("#212121")
    private val C_GRAY    = Color.parseColor("#757575")
    private val C_DIVIDER = Color.parseColor("#BDBDBD")
    private val C_WHITE   = Color.WHITE

    fun generate(
        context: Context,
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        rawPushEvents: List<RawEventRecord>,
        startDate: LocalDate,
        endDate: LocalDate,
        linesAdded: Int = 0
    ): File {
        val startStr = startDate.toString()
        val endStr   = endDate.toString()

        val eventsInRange = rawPushEvents.filter { it.date in startStr..endStr }

        val totalCommits = eventsInRange.sumOf { it.commitCount }

        val repoCommits: List<Pair<String, Int>> = eventsInRange
            .groupBy { it.repoShortName }
            .mapValues { (_, list) -> list.sumOf { it.commitCount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val mostActiveRepo = repoCommits.firstOrNull()

        val allDatesInRange = rawPushEvents
            .filter { it.date in startStr..endStr }
            .map { it.date }
            .distinct()

        val dateToCommits: Map<String, Int> = eventsInRange
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.commitCount } }

        val mostActiveDay = dateToCommits.maxByOrNull { it.value }
        val activeDays    = dateToCommits.keys.size
        val totalDays     = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val dailyAvg      = if (totalDays > 0) totalCommits.toFloat() / totalDays else 0f

        val topLanguages = repos.mapNotNull { it.language }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        val avatarBitmap: Bitmap? = user.avatarUrl?.let { url ->
            try { URL(url).openStream().use { BitmapFactory.decodeStream(it) } }
            catch (_: Exception) { null }
        }

        val pdf  = PdfDocument()
        val page = pdf.startPage(
            PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create()
        )

        drawPage(
            canvas         = page.canvas,
            user           = user,
            startDate      = startDate,
            endDate        = endDate,
            totalCommits   = totalCommits,
            linesAdded     = linesAdded,
            mostActiveRepo = mostActiveRepo,
            repoCommits    = repoCommits,
            mostActiveDay  = mostActiveDay,
            activeDays     = activeDays,
            totalDays      = totalDays,
            dailyAvg       = dailyAvg,
            topLanguages   = topLanguages,
            avatarBitmap   = avatarBitmap
        )

        pdf.finishPage(page)

        val dir  = File(context.cacheDir, "reports").also { it.mkdirs() }
        val file = File(dir, "gitdash_diff_${user.username}_${startDate}_${endDate}.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        avatarBitmap?.recycle()
        return file
    }

    @Suppress("LongParameterList")
    private fun drawPage(
        canvas: Canvas,
        user: UnifiedUser,
        startDate: LocalDate,
        endDate: LocalDate,
        totalCommits: Int,
        linesAdded: Int,
        mostActiveRepo: Pair<String, Int>?,
        repoCommits: List<Pair<String, Int>>,
        mostActiveDay: Map.Entry<String, Int>?,
        activeDays: Int,
        totalDays: Int,
        dailyAvg: Float,
        topLanguages: List<String>,
        avatarBitmap: Bitmap?
    ) {
        var y: Float

        // ── Header ─────────────────────────────────────────────────────────
        val headerH = 96f
        val avatarR = 26f
        val avatarCx = PAGE_W - MARGIN - avatarR
        val avatarCy = headerH / 2f

        canvas.drawRect(RectF(0f, 0f, PAGE_W, headerH), fill(C_HEADER))
        canvas.drawText("GitDash", MARGIN, 38f, txt(C_WHITE, 26f, bold = true))
        canvas.drawText(
            "Activity Report  ·  ${user.platform.displayName}",
            MARGIN, 56f, txt(C_SUB, 11f)
        )

        val rangeLabel = "${fmt(startDate)}  →  ${fmt(endDate)}"
        canvas.drawText("@${user.username}", MARGIN, 78f, txt(C_SUB, 13f, bold = true))

        val dateRight = if (avatarBitmap != null) avatarCx - avatarR - 8f else PAGE_W - MARGIN
        canvas.drawText(rangeLabel, dateRight, 78f, txt(C_SUB, 11f, align = Paint.Align.RIGHT))

        avatarBitmap?.let { drawCircleAvatar(canvas, it, avatarCx, avatarCy, avatarR) }

        y = headerH + 28f

        // ── Summary stats ───────────────────────────────────────────────────
        drawLabel(canvas, "ACTIVITY SUMMARY", y); y += 26f

        val c1 = MARGIN
        val c2 = MARGIN + CONTENT_W / 3f
        val c3 = MARGIN + CONTENT_W * 2f / 3f

        drawStat(canvas, c1, y, totalCommits.toString(), "Commits in range")

        val repoDisplay = mostActiveRepo?.first?.take(18) ?: "—"
        val repoSuffix  = mostActiveRepo?.let { " (${it.second})" } ?: ""
        drawStat(canvas, c2, y, repoDisplay + repoSuffix, "Most active repo")

        val linesDisplay = if (linesAdded > 0) "+$linesAdded" else "N/A"
        val linesLabel   = if (linesAdded > 0) "Lines added" else "Lines added (GitHub)"
        drawStat(canvas, c3, y, linesDisplay, linesLabel)
        y += 56f

        drawDivider(canvas, y); y += 20f

        // ── Top repos in range ──────────────────────────────────────────────
        drawLabel(canvas, "TOP REPOSITORIES IN PERIOD", y); y += 26f

        if (repoCommits.isEmpty()) {
            canvas.drawText("No commit data in this range", MARGIN, y, txt(C_GRAY, 11f))
            y += 22f
        } else {
            repoCommits.take(5).forEachIndexed { i, (name, count) ->
                val bullet  = "${i + 1}."
                val barW    = (count.toFloat() / (repoCommits.first().second)) * (CONTENT_W * 0.4f)
                canvas.drawText(bullet, MARGIN, y, txt(C_ACCENT, 11f, bold = true))
                canvas.drawText(name.take(22), MARGIN + 22f, y, txt(C_TEXT, 11f))
                canvas.drawText("$count commits", PAGE_W - MARGIN, y, txt(C_GRAY, 10f, align = Paint.Align.RIGHT))
                // mini bar
                canvas.drawRect(
                    RectF(MARGIN + 22f, y + 4f, MARGIN + 22f + barW, y + 9f),
                    fill(C_ACCENT)
                )
                y += 22f
            }
        }

        y += 6f
        drawDivider(canvas, y); y += 20f

        // ── Activity highlights ─────────────────────────────────────────────
        drawLabel(canvas, "ACTIVITY HIGHLIGHTS", y); y += 26f

        val avgStr  = "%.1f commits/day".format(dailyAvg)
        val daysSrc = "$activeDays active days out of $totalDays days"
        drawStat(canvas, c1, y, avgStr, "Daily average")
        drawStat(canvas, c2, y, daysSrc, "Commitment rate")

        val peakStr   = mostActiveDay?.let { "${it.value} commits" } ?: "—"
        val peakLabel = mostActiveDay?.let { "Peak: ${fmtStr(it.key)}" } ?: "Most active day"
        drawStat(canvas, c3, y, peakStr, peakLabel)
        y += 56f

        drawDivider(canvas, y); y += 20f

        // ── Top languages ───────────────────────────────────────────────────
        drawLabel(canvas, "TOP LANGUAGES (ALL REPOS)", y); y += 26f

        if (topLanguages.isEmpty()) {
            canvas.drawText("—", MARGIN, y, txt(C_TEXT, 13f))
        } else {
            topLanguages.forEachIndexed { i, lang ->
                val num = when (i) { 0 -> "01"; 1 -> "02"; else -> "03" }
                canvas.drawText(num,  MARGIN,       y, txt(C_SUB, 13f, bold = true))
                canvas.drawText(lang, MARGIN + 34f, y, txt(C_TEXT, 14f, bold = true))
                y += 22f
            }
        }

        // ── Footer ─────────────────────────────────────────────────────────
        drawDivider(canvas, PAGE_H - 38f)
        canvas.drawText(
            "Generated by GitDash  ·  ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}",
            PAGE_W / 2f, PAGE_H - 18f,
            txt(C_GRAY, 9f, align = Paint.Align.CENTER)
        )
    }

    private fun drawCircleAvatar(canvas: Canvas, src: Bitmap, cx: Float, cy: Float, radius: Float) {
        val size   = (radius * 2).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, size, size, true)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val m      = Matrix()
        m.setTranslate(cx - radius, cy - radius)
        shader.setLocalMatrix(m)
        paint.shader = shader
        canvas.drawCircle(cx, cy, radius, paint)
        if (scaled !== src) scaled.recycle()
    }

    private fun drawDivider(canvas: Canvas, y: Float) =
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, stroke(C_DIVIDER, 0.8f))

    private fun drawLabel(canvas: Canvas, text: String, y: Float) =
        canvas.drawText(text, MARGIN, y, txt(C_GRAY, 9f))

    private fun drawStat(canvas: Canvas, x: Float, y: Float, value: String, label: String) {
        canvas.drawText(value, x,      y,       txt(C_ACCENT, 14f, bold = true))
        canvas.drawText(label, x,      y + 16f, txt(C_GRAY, 9f))
    }

    private fun fmt(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

    private fun fmtStr(iso: String): String =
        try { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM d, yyyy")) }
        catch (_: Exception) { iso }

    private fun txt(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color          = color
            this.textSize       = size
            this.isFakeBoldText = bold
            this.textAlign      = align
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color       = color
        this.style       = Paint.Style.STROKE
        this.strokeWidth = width
    }
}