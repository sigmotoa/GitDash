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
import com.sigmotoa.gitdash.data.repository.LastCommitInfo
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generates a single-page A4 PDF profile report and saves it to the app cache.
 * Uses Android's built-in PdfDocument — no external dependencies needed.
 *
 * Layout (top → bottom):
 *   Header bar  |  @username + bio info  |  Profile stats
 *   Activity breakdown (with date window range)
 *   Most recent push  |  Top repos (stars + activity side-by-side)
 *   Top 3 languages  |  Footer
 */
object ProfileReportGenerator {

    private const val PAGE_W = 595f   // A4 at 72 dpi
    private const val PAGE_H = 842f
    private const val MARGIN = 44f
    private const val CONTENT_W = PAGE_W - 2 * MARGIN

    // ── Palette ────────────────────────────────────────────────────────────
    private val C_HEADER     = Color.parseColor("#1A237E")   // deep blue
    private val C_ACCENT     = Color.parseColor("#1565C0")   // medium blue
    private val C_HEADER_SUB = Color.parseColor("#90CAF9")   // light blue
    private val C_TEXT       = Color.parseColor("#212121")   // near-black
    private val C_GRAY       = Color.parseColor("#757575")   // medium gray
    private val C_DIVIDER    = Color.parseColor("#BDBDBD")   // light gray
    private val C_WHITE      = Color.WHITE

    // ── Public entry point ─────────────────────────────────────────────────

    fun generate(
        context: Context,
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        categoryCounts: Map<String, Int>,
        dateMap: Map<String, Int>,
        topReposByPushes: List<Pair<String, Int>>,
        lastCommitInfo: LastCommitInfo?
    ): File {
        val totalStars     = repos.sumOf { it.starCount }
        val commits        = categoryCounts["Commits"] ?: 0
        val topLanguages   = repos
            .mapNotNull { it.language }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        val topReposByStars = repos.sortedByDescending { it.starCount }.take(3)

        // Date window boundaries
        val windowStart = dateMap.keys.minOrNull()
        val windowEnd   = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Language of the last-commit repo (look it up in the already-loaded repo list)
        val lastCommitLang = lastCommitInfo?.let { info ->
            repos.find { it.name.equals(info.repoName, ignoreCase = true) }?.language
        }

        // Download avatar bitmap — best-effort, null if unavailable
        val avatarBitmap: Bitmap? = user.avatarUrl?.let { url ->
            try { URL(url).openStream().use { BitmapFactory.decodeStream(it) } }
            catch (_: Exception) { null }
        }

        val pdf  = PdfDocument()
        val page = pdf.startPage(
            PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create()
        )
        drawPage(
            canvas           = page.canvas,
            user             = user,
            totalStars       = totalStars,
            commits          = commits,
            topLanguages     = topLanguages,
            topReposByStars  = topReposByStars,
            topReposByPushes = topReposByPushes,
            lastCommitInfo   = lastCommitInfo,
            lastCommitLang   = lastCommitLang,
            windowStart      = windowStart,
            windowEnd        = windowEnd,
            avatarBitmap     = avatarBitmap,
            categoryCounts   = categoryCounts
        )
        pdf.finishPage(page)

        val dir  = File(context.cacheDir, "reports").also { it.mkdirs() }
        val file = File(dir, "gitdash_report_${user.username}.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        avatarBitmap?.recycle()
        return file
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    private fun drawPage(
        canvas: Canvas,
        user: UnifiedUser,
        totalStars: Int,
        commits: Int,
        topLanguages: List<String>,
        topReposByStars: List<UnifiedRepo>,
        topReposByPushes: List<Pair<String, Int>>,
        lastCommitInfo: LastCommitInfo?,
        lastCommitLang: String?,
        windowStart: String?,
        windowEnd: String,
        avatarBitmap: Bitmap?,
        categoryCounts: Map<String, Int>
    ) {
        var y: Float

        // ── Header bar ─────────────────────────────────────────────────────
        val headerH    = 88f
        val avatarR    = 26f          // radius of the avatar circle
        val avatarCx   = PAGE_W - MARGIN - avatarR
        val avatarCy   = headerH / 2f

        canvas.drawRect(RectF(0f, 0f, PAGE_W, headerH), fill(C_HEADER))

        canvas.drawText("GitDash", MARGIN, 44f, txt(C_WHITE, 26f, bold = true))
        canvas.drawText(
            "Profile Report  ·  ${user.platform.displayName}",
            MARGIN, 66f, txt(C_HEADER_SUB, 11f)
        )
        // Date pushed left of the avatar so they don't overlap
        val dateRightEdge = if (avatarBitmap != null) avatarCx - avatarR - 8f else PAGE_W - MARGIN
        canvas.drawText(
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            dateRightEdge, 66f,
            txt(C_HEADER_SUB, 11f, align = Paint.Align.RIGHT)
        )

        avatarBitmap?.let {
            drawCircleAvatar(canvas, it, avatarCx, avatarCy, avatarR)
        }

        y = headerH + 26f

        // ── User info ──────────────────────────────────────────────────────
        canvas.drawText("@${user.username}", MARGIN, y, txt(C_ACCENT, 22f, bold = true))
        y += 26f

        user.name?.let {
            canvas.drawText(it, MARGIN, y, txt(C_TEXT, 14f))
            y += 20f
        }
        user.location?.let {
            canvas.drawText(it, MARGIN, y, txt(C_GRAY, 11f))
            y += 18f
        }
        user.company?.let {
            canvas.drawText(it, MARGIN, y, txt(C_GRAY, 11f))
            y += 18f
        }

        y += 10f
        drawDivider(canvas, y); y += 20f

        // ── Profile statistics ─────────────────────────────────────────────
        drawLabel(canvas, "PROFILE STATISTICS", y); y += 26f

        val c1 = MARGIN
        val c2 = MARGIN + CONTENT_W / 3f
        val c3 = MARGIN + CONTENT_W * 2f / 3f

        drawStat(canvas, c1, y, user.followers.toString(),  "Followers")
        drawStat(canvas, c2, y, user.following.toString(),  "Following")
        drawStat(canvas, c3, y, user.publicRepos.toString(), "Public Repos")
        y += 50f

        drawStat(canvas, c1, y, totalStars.toString(), "Total Stars")
        drawStat(canvas, c2, y, commits.toString(),    "Commits (window)")
        y += 50f

        drawDivider(canvas, y); y += 20f

        // ── Activity breakdown (with date window) ──────────────────────────
        val windowLabel = if (windowStart != null)
            "${formatDate(windowStart)} – ${formatDate(windowEnd)}"
        else
            "Until ${formatDate(windowEnd)}"

        canvas.drawText("ACTIVITY BREAKDOWN", MARGIN, y, txt(C_GRAY, 9f))
        canvas.drawText(
            windowLabel, PAGE_W - MARGIN, y,
            txt(C_GRAY, 9f, align = Paint.Align.RIGHT)
        )
        y += 26f

        val statKeys = listOf("Commits", "PRs", "Issues", "Comments", "Other")
        val colW = CONTENT_W / statKeys.size
        statKeys.forEachIndexed { i, key ->
            drawStat(
                canvas,
                MARGIN + i * colW,
                y,
                (categoryCounts[key] ?: 0).toString(),
                key
            )
        }
        y += 50f

        drawDivider(canvas, y); y += 20f

        // ── Most recent push ───────────────────────────────────────────────
        drawLabel(canvas, "MOST RECENT PUSH", y); y += 26f

        if (lastCommitInfo != null) {
            val repoLabel = buildString {
                append(lastCommitInfo.repoName)
                append("   \u00B7   ")          // ·
                append(formatDate(lastCommitInfo.date))
                if (lastCommitLang != null) {
                    append("   \u00B7   ")
                    append(lastCommitLang)
                }
            }
            canvas.drawText(repoLabel, MARGIN, y, txt(C_TEXT, 12f, bold = true))
        } else {
            canvas.drawText("\u2014", MARGIN, y, txt(C_TEXT, 12f))  // —
        }
        y += 30f

        drawDivider(canvas, y); y += 20f

        // ── Top repositories (two columns) ─────────────────────────────────
        drawLabel(canvas, "TOP REPOSITORIES", y); y += 22f

        val colLeft  = MARGIN
        val colRight = MARGIN + CONTENT_W / 2f + 10f

        canvas.drawText("By Stars",    colLeft,  y, txt(C_ACCENT, 10f, bold = true))
        canvas.drawText("By Activity", colRight, y, txt(C_ACCENT, 10f, bold = true))
        y += 20f

        val rowCount = maxOf(topReposByStars.size, topReposByPushes.size).coerceAtMost(3)
        repeat(rowCount) { i ->
            topReposByStars.getOrNull(i)?.let { repo ->
                val starLine = "${i + 1}. ${repo.name.take(20)}  (${repo.starCount} stars)"
                canvas.drawText(starLine, colLeft, y, txt(C_TEXT, 10f))
            }
            topReposByPushes.getOrNull(i)?.let { (name, count) ->
                val pushLine = "${i + 1}. ${name.take(20)}  ($count commits)"
                canvas.drawText(pushLine, colRight, y, txt(C_TEXT, 10f))
            }
            y += 20f
        }

        y += 8f
        drawDivider(canvas, y); y += 20f

        // ── Top 3 languages ────────────────────────────────────────────────
        drawLabel(canvas, "TOP 3 LANGUAGES", y); y += 26f

        if (topLanguages.isEmpty()) {
            canvas.drawText("\u2014", MARGIN, y, txt(C_TEXT, 13f))
        } else {
            topLanguages.forEachIndexed { i, lang ->
                val bullet = when (i) { 0 -> "01"; 1 -> "02"; else -> "03" }
                canvas.drawText(bullet,      MARGIN,        y, txt(C_HEADER_SUB, 13f, bold = true))
                canvas.drawText(lang,        MARGIN + 34f,  y, txt(C_TEXT, 14f, bold = true))
                y += 22f
            }
        }

        // ── Footer ─────────────────────────────────────────────────────────
        drawDivider(canvas, PAGE_H - 38f)
        canvas.drawText(
            "Generated by GitDash  \u00B7  ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}",
            PAGE_W / 2f, PAGE_H - 18f,
            txt(C_GRAY, 9f, align = Paint.Align.CENTER)
        )
    }

    // ── Avatar helper ──────────────────────────────────────────────────────

    /**
     * Draws [src] cropped to a circle of [radius], centred at ([cx], [cy]).
     * Uses BitmapShader with a translation matrix so the bitmap aligns with
     * the circle's bounding box on the PDF canvas.
     */
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

    // ── Drawing helpers ────────────────────────────────────────────────────

    private fun drawDivider(canvas: Canvas, y: Float) =
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, stroke(C_DIVIDER, 0.8f))

    private fun drawLabel(canvas: Canvas, text: String, y: Float) =
        canvas.drawText(text, MARGIN, y, txt(C_GRAY, 9f))

    private fun drawStat(canvas: Canvas, x: Float, y: Float, value: String, label: String) {
        canvas.drawText(value, x,       y,        txt(C_ACCENT, 22f, bold = true))
        canvas.drawText(label, x,       y + 16f,  txt(C_GRAY, 10f))
    }

    // ── Date formatting ────────────────────────────────────────────────────

    private fun formatDate(isoDate: String): String =
        try {
            LocalDate.parse(isoDate)
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (_: Exception) {
            isoDate
        }

    // ── Paint factories ────────────────────────────────────────────────────

    private fun txt(
        color: Int,
        size: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
