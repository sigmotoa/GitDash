package com.sigmotoa.gitdash.ui.components

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
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
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val textColor   = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor   = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                // Allow links to be tapped
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                textSize = 14f

                val markwon = buildMarkwon(context)
                markwon.setMarkdown(this, markdown)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            val markwon = buildMarkwon(textView.context)
            markwon.setMarkdown(textView, markdown)
        }
    )
}

private fun buildMarkwon(context: android.content.Context): Markwon =
    Markwon.builder(context)
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TablePlugin.create(context))
        .usePlugin(HtmlPlugin.create())
        .usePlugin(LinkifyPlugin.create())
        .build()
