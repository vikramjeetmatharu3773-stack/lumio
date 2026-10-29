package app.lumio.player.ui.screens

import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.container
import app.lumio.player.data.db.ScanFolderEntity
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.ScanMode
import app.lumio.player.ui.components.*
import kotlinx.coroutines.launch

/** Settings > Library & scan > Folders. Spec: docs/07 and docs/04 (4.10). */
@Composable
fun FoldersScreen(onBack: () -> Unit) {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val on = MaterialTheme.colorScheme.onSurface
    val s by c.settings.flow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val folders by c.db.folders().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scan by c.scanner.state.collectAsStateWithLifecycle()
    var pendingMode by remember { mutableIntStateOf(ScanFolderEntity.INCLUDE) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch {
            c.scanner.addFolder(uri, pendingMode, deep = pendingMode == ScanFolderEntity.INCLUDE)
            c.scanner.scan()
        }
    }

    ScreenScaffold("Folders & scan", onBack = onBack) {
        item {
            SettingsSection("Scan mode") {
                SettingsChoice(
                    "Look for music in",
                    listOf(ScanMode.ALL_DEVICE to "Whole device", ScanMode.SELECTED_ONLY to "Only selected folders"),
                    s.scanMode,
                ) { m -> scope.launch { c.settings.update { copy(scanMode = m) }; c.scanner.scan() } }
                Text(
                    if (s.scanMode == ScanMode.ALL_DEVICE) "Every song on the device is included, except folders you exclude below."
                    else "Only songs inside the folders you include below are shown. Excluded folders always win.",
                    style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f), modifier = Modifier.padding(16.dp),
                )
            }
        }
        if (s.scanMode == ScanMode.SELECTED_ONLY && folders.none { it.mode == ScanFolderEntity.INCLUDE }) {
            item {
                SettingsSection(null) {
                    Text("No folders included yet — your library will be empty. Add a folder below.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }
        }
        item {
            SettingsSection("Folders") {
                if (folders.isEmpty()) Text("No folders added.", color = on.copy(alpha = 0.7f), modifier = Modifier.padding(16.dp))
                folders.forEachIndexed { i, f ->
                    if (i > 0) SettingsDivider()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(f.displayPath, style = MaterialTheme.typography.bodyLarge, color = on)
                        val last = if (f.lastScan == 0L) "not scanned" else "scanned " + DateUtils.getRelativeTimeSpanString(f.lastScan)
                        Text("${if (f.mode == ScanFolderEntity.INCLUDE) "Include" else "Exclude"} · ${f.songCount} songs · $last", style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { scope.launch { c.db.folders().setMode(f.treeUri, if (f.mode == ScanFolderEntity.INCLUDE) ScanFolderEntity.EXCLUDE else ScanFolderEntity.INCLUDE); c.scanner.scan() } }) {
                                Text(if (f.mode == ScanFolderEntity.INCLUDE) "Make exclude" else "Make include")
                            }
                            if (f.mode == ScanFolderEntity.INCLUDE) {
                                TextButton(onClick = { scope.launch { c.db.folders().setDeep(f.treeUri, !f.deepScan); c.scanner.scan() } }) {
                                    Text(if (f.deepScan) "Deep scan: on" else "Deep scan: off")
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { scope.launch { c.scanner.removeFolder(f.treeUri); c.scanner.scan() } }) { Icon(Icons.Rounded.Delete, "Remove folder", tint = on) }
                        }
                    }
                }
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("Include folder", { pendingMode = ScanFolderEntity.INCLUDE; picker.launch(null) }, icon = Icons.Rounded.Add, filled = true)
                    GlassButton("Exclude", { pendingMode = ScanFolderEntity.EXCLUDE; picker.launch(null) }, icon = Icons.Rounded.Block)
                }
            }
        }
        item {
            SettingsSection("Scan") {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (scan.running) {
                        CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                        Spacer(Modifier.width(14.dp))
                        Column { Text(scan.phase, color = on); Text("${scan.found} songs found", style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f)) }
                    } else {
                        Text(scan.summary.ifBlank { "Ready" }, color = on.copy(alpha = 0.85f))
                    }
                }
                Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { scope.launch { c.scanner.scan() } }, enabled = !scan.running) { Text("Scan now") }
                    TextButton(onClick = { scope.launch { c.scanner.scan(rebuild = true) } }, enabled = !scan.running) { Text("Full rebuild") }
                }
                Text("Full rebuild clears the song list and re-reads everything. Favorites and play counts are kept.", style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.65f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}
