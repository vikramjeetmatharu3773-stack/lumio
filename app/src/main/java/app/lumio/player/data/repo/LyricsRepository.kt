package app.lumio.player.data.repo

import android.content.Context
import android.net.Uri
import app.lumio.player.data.model.LyricLine
import app.lumio.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object LrcParser {
    private val stamp = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
    private val meta = Regex("^\\[[a-zA-Z]+:.*]$")

    /** Returns lines sorted by time. Plain text (no stamps) -> timeMs = -1 for every line. */
    fun parse(text: String): List<LyricLine> {
        var offset = 0L
        val out = ArrayList<LyricLine>()
        val plain = ArrayList<LyricLine>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[offset:", true)) {
                offset = line.substringAfter(':').trimEnd(']').trim().toLongOrNull() ?: 0L
                continue
            }
            val stamps = stamp.findAll(line).toList()
            if (stamps.isEmpty()) {
                if (!meta.matches(line)) plain.add(LyricLine(-1, line))
                continue
            }
            val body = line.replace(stamp, "").trim()
            for (m in stamps) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                out.add(LyricLine((min * 60 + sec) * 1000 + frac - offset, body))
            }
        }
        return if (out.isNotEmpty()) out.sortedBy { it.timeMs } else plain
    }
}

/** App-private lyrics storage. Spec: docs/08 (Lyrics). Folder auto-match = task T21. */
class LyricsRepository(private val context: Context) {
    private val dir get() = File(context.filesDir, "lyrics").apply { mkdirs() }

    private fun fileFor(songUri: String): File {
        val h = MessageDigest.getInstance("SHA-1").digest(songUri.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(dir, "$h.lrc")
    }

    suspend fun load(song: Song): List<LyricLine>? = withContext(Dispatchers.IO) {
        val f = fileFor(song.uri)
        if (f.exists()) LrcParser.parse(f.readText()).ifEmpty { null } else null
    }

    suspend fun import(song: Song, source: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext false
            fileFor(song.uri).writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun remove(song: Song) = withContext(Dispatchers.IO) { fileFor(song.uri).delete(); Unit }
}
