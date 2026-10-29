package app.lumio.player.data.repo

import app.lumio.player.data.model.Song
import app.lumio.player.data.settings.SortField
import app.lumio.player.util.sortKey

fun sortSongs(list: List<Song>, field: SortField, asc: Boolean, ignoreArticles: Boolean): List<Song> {
    val cmp: Comparator<Song> = when (field) {
        SortField.TITLE -> compareBy { sortKey(it.title, ignoreArticles) }
        SortField.ARTIST -> compareBy<Song> { sortKey(it.artist, ignoreArticles) }.thenBy { sortKey(it.title, ignoreArticles) }
        SortField.ALBUM -> compareBy<Song> { sortKey(it.album, ignoreArticles) }.thenBy { it.track }
        SortField.DATE_ADDED -> compareBy { it.dateAdded }
        SortField.DURATION -> compareBy { it.durationMs }
        SortField.PLAY_COUNT -> compareBy { it.playCount }
        SortField.YEAR -> compareBy { it.year }
    }
    val sorted = list.sortedWith(cmp)
    return if (asc) sorted else sorted.asReversed()
}
