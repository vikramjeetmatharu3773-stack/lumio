package app.lumio.player.data.scan

import android.net.Uri
import android.provider.DocumentsContract

/** Folder-key helpers. Key format: "<volume>:<relativePath>/"  e.g. "primary:Music/Rock/". See docs/07. */
object FolderUtils {
    data class ParsedTree(val volume: String, val relativePath: String, val display: String)

    fun normalizeVolume(v: String?): String = when {
        v.isNullOrBlank() -> "primary"
        v.equals("external_primary", true) || v.equals("external", true) || v.equals("primary", true) -> "primary"
        else -> v.lowercase()
    }

    private fun dirWithSlash(rel: String): String {
        val r = rel.replace('\\', '/').trim('/')
        return if (r.isEmpty()) "" else "$r/"
    }

    fun keyFromRelative(volumeName: String?, relativePath: String?): String =
        "${normalizeVolume(volumeName)}:${dirWithSlash(relativePath ?: "")}"

    /** API 26-28: derive key from the DATA file path. */
    fun keyFromData(path: String?): String {
        if (path.isNullOrBlank()) return "primary:"
        val dir = path.replace('\\', '/').substringBeforeLast('/', "")
        for (pre in listOf("/storage/emulated/0", "/sdcard", "/storage/self/primary")) {
            if (dir == pre) return "primary:"
            if (dir.startsWith("$pre/")) return "primary:" + dirWithSlash(dir.removePrefix(pre))
        }
        if (dir.startsWith("/storage/")) {
            val rest = dir.removePrefix("/storage/")
            return "${rest.substringBefore('/').lowercase()}:" + dirWithSlash(rest.substringAfter('/', ""))
        }
        return "primary:"
    }

    /** SAF document id of a DIRECTORY, e.g. "primary:Music/Rock" -> "primary:Music/Rock/". */
    fun keyFromDirDocId(docId: String): String {
        val vol = docId.substringBefore(':')
        val rel = docId.substringAfter(':', "")
        return "${normalizeVolume(vol)}:${dirWithSlash(rel)}"
    }

    fun parseTree(treeUri: Uri): ParsedTree {
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val vol = docId.substringBefore(':')
        val rel = dirWithSlash(docId.substringAfter(':', ""))
        val display = (if (vol.equals("primary", true)) "Internal storage" else "SD $vol") + "/" + rel.trimEnd('/')
        return ParsedTree(normalizeVolume(vol), rel, display.trimEnd('/'))
    }

    fun isInside(songFolderKey: String, folderKey: String): Boolean =
        songFolderKey.startsWith(folderKey, ignoreCase = true)

    fun displayName(folderKey: String): String {
        val rel = folderKey.substringAfter(':', "").trim('/')
        return if (rel.isEmpty()) (if (folderKey.startsWith("primary")) "Internal storage" else folderKey.substringBefore(':'))
        else rel.substringAfterLast('/')
    }
}
