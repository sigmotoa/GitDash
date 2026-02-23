package com.sigmotoa.gitdash.ui.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generates a single-page A4 PDF profile report and saves it to the app cache.
 * Uses Android's built-in PdfDocument — no external dependencies needed.
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
        topPushedRepo: String?
    ): File {
        val totalStars     = repos.sumOf { it.starCount }
        val lastWorkedRepo = repos
            .mapNotNull { r -> r.updatedAt?.let { r.name to it } }
            .maxByOrNull { it.second }
            ?.first
        val topLanguages = repos
            .mapNotNull { it.language }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        val commits = categoryCounts["Commits"] ?: 0

        val pdf = PdfDocument()
        val page = pdf.startPage(
            PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create()
        )
        drawPage(
            canvas        = page.canvas,
            user          = user,
            totalStars    = totalStars,
            commits       = commits,
            topPushedRepo = topPushedRepo,
            lastWorkedRepo = lastWorkedRepo,
            topLanguages  = topLanguages,
            categoryCounts = categoryCounts
        )
        pdf.finishPage(page)

        val dir  = File(context.cacheDir, "reports").also { it.mkdirs() }
        val file = File(dir, "gitdash_report_${user.username}.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    // ── Drawing ────────────────────────────────────────────────────────────

    private fun drawPage(
        canvas: Canvas,
        user: UnifiedUser,
        totalStars: Int,
        commits: Int,
        topPushedRepo: String?,
        lastWorkedRepo: String?,
        topLanguages: List<String>,
        categoryCounts: Map<String, Int>
    ) {
        var y = 0f

        // ── Header bar ────────────────────────────────────────────────────
        val headerH = 88f
        canvas.drawRect(RectF(0f, 0f, PAGE_W, headerH), fill(C_HEADER))

        canvas.drawText("GitDash", MARGIN, 44f, txt(C_WHITE, 26f, bold = true))
        canvas.drawText(
            "Profile Report  ·  ${user.platform.displayName}",
            MARGIN, 66f, txt(C_HEADER_SUB, 11f)
        )
        canvas.drawText(
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            PAGE_W - MARGIN, 66f,
            txt(C_HEADER_SUB, 11f, align = Paint.Align.RIGHT)
        )

        y = headerH + 26f

        // ── User info ─────────────────────────────────────────────────────
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

        // ── Profile statistics ────────────────────────────────────────────
        drawLabel(canvas, "PROFILE STATISTICS", y); y += 26f

        val c1 = MARGIN
        val c2 = MARGIN + CONTENT_W / 3f
        val c3 = MARGIN + CONTENT_W * 2f / 3f

        drawStat(canvas, c1, y, user.followers.toString(),  "Followers")
        drawStat(canvas, c2, y, user.following.toString(),  "Following")
        drawStat(canvas, c3, y, user.publicRepos.toString(), "Public Repos")
        y += 50f

        drawStat(canvas, c1, y, totalStars.toString(), "Total Stars")
        drawStat(canvas, c2, y, commits.toString(),    "Commits (last 4 mo)")
        y += 50f

        drawDivider(canvas, y); y += 20f

        // ── Activity breakdown ────────────────────────────────────────────
        drawLabel(canvas, "ACTIVITY BREAKDOWN (LAST ~4 MONTHS)", y); y += 26f

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

        // ── Repositories ──────────────────────────────────────────────────
        drawLabel(canvas, "REPOSITORIES", y); y += 26f

        val kx = MARGIN           // key column x
        val vx = MARGIN + 130f    // value column x

        drawKeyValue(canvas, kx, vx, y, "Most active:", topPushedRepo ?: "—")
        y += 22f
        drawKeyValue(canvas, kx, vx, y, "Last worked on:", lastWorkedRepo ?: "—")
        y += 32f

        drawDivider(canvas, y); y += 20f

        // ── Top 3 Languages ───────────────────────────────────────────────
        drawLabel(canvas, "TOP 3 LANGUAGES", y); y += 26f

        if (topLanguages.isEmpty()) {
            canvas.drawText("—", MARGIN, y, txt(C_TEXT, 13f))
        } else {
            topLanguages.forEachIndexed { i, lang ->
                val bullet = when (i) { 0 -> "01"; 1 -> "02"; else -> "03" }
                canvas.drawText(bullet, MARGIN, y, txt(C_HEADER_SUB, 13f, bold = true))
                canvas.drawText(lang,   MARGIN + 34f, y, txt(C_TEXT, 14f, bold = true))
                y += 24f
            }
        }

        // ── Footer ────────────────────────────────────────────────────────
        drawDivider(canvas, PAGE_H - 38f)
        canvas.drawText(
            "Generated by GitDash  ·  ${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}",
            PAGE_W / 2f, PAGE_H - 18f,
            txt(C_GRAY, 9f, align = Paint.Align.CENTER)
        )
    }

    // ── Drawing helpers ────────────────────────────────────────────────────

    private fun drawDivider(canvas: Canvas, y: Float) =
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, stroke(C_DIVIDER, 0.8f))

    private fun drawLabel(canvas: Canvas, text: String, y: Float) =
        canvas.drawText(text, MARGIN, y, txt(C_GRAY, 9f))

    private fun drawStat(canvas: Canvas, x: Float, y: Float, value: String, label: String) {
        canvas.drawText(value, x, y,        txt(C_ACCENT, 22f, bold = true))
        canvas.drawText(label, x, y + 16f,  txt(C_GRAY, 10f))
    }

    private fun drawKeyValue(
        canvas: Canvas, kx: Float, vx: Float, y: Float, key: String, value: String
    ) {
        canvas.drawText(key,   kx, y, txt(C_GRAY, 11f))
        canvas.drawText(value, vx, y, txt(C_TEXT, 13f, bold = true))
    }

    // ── Paint factories ────────────────────────────────────────────────────

    private fun txt(
        color: Int,
        size: Float,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color        = color
        this.textSize     = size
        this.isFakeBoldText = bold
        this.textAlign    = align
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
