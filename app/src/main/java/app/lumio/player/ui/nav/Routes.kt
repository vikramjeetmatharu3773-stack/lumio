package app.lumio.player.ui.nav

import android.net.Uri
import app.lumio.player.data.model.CollectionType

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library?tab={tab}"
    const val SEARCH = "search"
    const val COLLECTION = "collection/{type}/{key}"
    const val SETTINGS = "settings"
    const val SET_APPEARANCE = "settings/appearance"
    const val SET_PLAYBACK = "settings/playback"
    const val SET_LIBRARY = "settings/library"
    const val SET_LYRICS = "settings/lyrics"
    const val SET_WIDGETS = "settings/widgets"
    const val SET_PRIVACY = "settings/privacy"
    const val SET_ABOUT = "settings/about"
    const val FOLDERS = "folders"
    const val EQUALIZER = "equalizer"
    const val HISTORY = "history"
    const val STATS = "stats"

    fun library(tab: Int = 0) = "library?tab=$tab"
    fun collection(type: CollectionType, key: String = "-") = "collection/${type.route}/${Uri.encode(key)}"
}
