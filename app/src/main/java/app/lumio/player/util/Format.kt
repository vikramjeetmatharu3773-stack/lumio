package app.lumio.player.util

import java.text.Normalizer
import java.util.Locale

fun formatDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000).toInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatTotal(ms: Long): String {
    val mins = (ms / 60_000).toInt()
    return if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min"
}

private val diacritics = Regex("\\p{M}+")

/** lowercase + strip accents, for search. */
fun normalizeText(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(diacritics, "").lowercase()

fun sortKey(title: String, ignoreArticles: Boolean): String {
    var t = title.trim().lowercase()
    if (ignoreArticles) {
        for (a in listOf("the ", "a ", "an ")) if (t.startsWith(a) && t.length > a.length) { t = t.removePrefix(a); break }
    }
    return t
}

fun firstLetter(title: String): Char {
    val c = title.trim().firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}
