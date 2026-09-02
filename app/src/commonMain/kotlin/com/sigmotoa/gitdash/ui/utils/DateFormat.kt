package com.sigmotoa.gitdash.ui.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal val MONTHS_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/**
 * `"2024-01-15T10:30:00Z"` -> `"Jan 15, 2024"` (en UTC).
 * Devuelve el string original si no se puede parsear.
 *
 * kotlinx-datetime no da nombres de mes localizados, así que se usan los cortos
 * en inglés (igual que hace el gráfico de contribuciones).
 */
fun isoInstantToMediumDate(iso: String): String = try {
    val d = Instant.parse(iso).toLocalDateTime(TimeZone.UTC).date
    "${MONTHS_SHORT[d.monthNumber - 1]} ${d.dayOfMonth.toString().padStart(2, '0')}, ${d.year}"
} catch (e: Exception) {
    iso
}
