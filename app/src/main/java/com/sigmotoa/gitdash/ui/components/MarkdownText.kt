package com.sigmotoa.gitdash.ui.components

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableScheduler
import io.noties.markwon.image.ImageSizeResolver
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.svg.SvgMediaDecoder
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Composable that renders a Markdown string as formatted rich text.
 *
 * Uses Markwon under the hood via an AndroidView (native TextView) because
 * Jetpack Compose does not yet ship a built-in Markdown renderer.
 *
 * Supported syntax:
 *  - Headings (#, ##, ###, …)
 *  - Bold / italic / strikethrough
 *  - Inline code and fenced code blocks
 *  - Ordered and unordered lists
 *  - Block-quotes
 *  - Links (auto-linkified + Markdown syntax)
 *  - HTML tags (basic)
 *  - Tables (GFM)
 *  - Images (PNG, JPEG, SVG) with aspect-ratio scaling
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    // Build Markwon once per composition context — never recreate on recomposition.
    val markwon = remember(context) { buildMarkwon(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                textSize = 14f
            }
        },
        update = { textView ->
            // beforeSetText from ImagesPlugin cancels prior loads; afterSetText starts new ones.
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            markwon.setMarkdown(textView, markdown)
        },
        // Cancel every pending image-load when the view leaves composition, so background
        // threads never try to invalidate a detached or recycled view.
        onRelease = { textView ->
            AsyncDrawableScheduler.unschedule(textView)
        }
    )
}

private fun buildMarkwon(context: android.content.Context): Markwon =
    Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(ImagesPlugin.create { plugin ->
            plugin.addMediaDecoder(SvgMediaDecoder.create(context.resources))
            // Silently replace any image that fails (bad URL, unsupported SVG feature, etc.)
            // with an invisible 1×1 drawable instead of crashing the screen.
            plugin.errorHandler { _, _ ->
                ColorDrawable(Color.TRANSPARENT).also { it.setBounds(0, 0, 1, 1) }
            }
        })
        // Fit every image to the available width while preserving aspect ratio.
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.imageSizeResolver(object : ImageSizeResolver() {
                    override fun resolveImageSize(drawable: AsyncDrawable): Rect {
                        val canvasWidth = drawable.lastKnownCanvasWidth
                        if (!drawable.hasResult()) {
                            return Rect(0, 0, canvasWidth, 1)
                        }
                        val result = drawable.result
                        val w = result?.intrinsicWidth?.takeIf { it > 0 } ?: canvasWidth
                        val h = result?.intrinsicHeight?.takeIf { it > 0 } ?: w
                        return if (w > canvasWidth && canvasWidth > 0) {
                            val scaledH = (h.toLong() * canvasWidth / w).toInt()
                            Rect(0, 0, canvasWidth, maxOf(1, scaledH))
                        } else {
                            Rect(0, 0, w, maxOf(1, h))
                        }
                    }
                })
            }
        })
        .usePlugin(LinkifyPlugin.create())
        .build()
