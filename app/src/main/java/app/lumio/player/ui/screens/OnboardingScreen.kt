package app.lumio.player.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumio.player.container
import app.lumio.player.data.db.ScanFolderEntity
import app.lumio.player.data.settings.ScanMode
import app.lumio.player.ui.components.GlassButton
import app.lumio.player.ui.components.GlassSurface
import app.lumio.player.util.hasAudioPermission
import app.lumio.player.util.onboardingPermissions
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen() {
    val ctx = LocalContext.current
    val c = ctx.container
    val scope = rememberCoroutineScope()
    val on = MaterialTheme.colorScheme.onSurface
    val scan by c.scanner.state.collectAsStateWithLifecycle()
    val folders by c.db.folders().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    var step by remember { mutableIntStateOf(if (hasAudioPermission(ctx)) 1 else 0) }
    var denied by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasAudioPermission(ctx)) step = 1 else denied = true
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) scope.launch { c.scanner.addFolder(uri, ScanFolderEntity.INCLUDE, true) }
    }

    fun startScan(mode: ScanMode) {
        step = 2
        scope.launch {
            c.settings.update { copy(scanMode = mode) }
            c.scanner.scan(deep = true)
            c.settings.update { copy(onboardingDone = true) }
        }
    }

    val breathe by rememberInfiniteTransition(label = "orb").animateFloat(1f, 1.06f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "orbScale")

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlassSurface(Modifier.size(120.dp).graphicsLayer { scaleX = breathe; scaleY = breathe }, shape = CircleShape, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = on, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Lumio", style = MaterialTheme.typography.displayLarge, color = on)
        Text("Your music. Pure glass.", style = MaterialTheme.typography.bodyLarge, color = on.copy(alpha = 0.75f))
        Spacer(Modifier.height(28.dp))

        AnimatedContent(
            step,
            label = "onboardStep",
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)),
        ) { s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                when (s) {
                    0 -> {
                        FeatureRow(Icons.Rounded.Lock, "Offline & private", "No account, no internet, no tracking.")
                        FeatureRow(Icons.Rounded.Folder, "Any folder you choose", "Scan everything or only the folders you pick.")
                        FeatureRow(Icons.Rounded.GraphicEq, "Hi-fi playback", "Gapless, equalizer, lyrics, widgets.")
                        Spacer(Modifier.height(8.dp))
                        if (denied) {
                            Text("Music access was denied. Lumio needs it to find your songs.", color = on, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                            GlassButton("Open app settings", {
                                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                            })
                        }
                        GlassButton("Allow music access", { permLauncher.launch(onboardingPermissions()) }, filled = true)
                    }
                    1 -> {
                        Text("How should I find your music?", style = MaterialTheme.typography.titleLarge, color = on, textAlign = TextAlign.Center)
                        ChoiceCard(Icons.Rounded.PhoneAndroid, "Whole device", "Find every song on this phone.") { startScan(ScanMode.ALL_DEVICE) }
                        ChoiceCard(Icons.Rounded.FolderOpen, "Pick folders", "Only scan the folders you choose.") { step = 3 }
                    }
                    3 -> {
                        Text("Choose your music folders", style = MaterialTheme.typography.titleLarge, color = on, textAlign = TextAlign.Center)
                        folders.forEach { f -> Text("• ${f.displayPath}", color = on.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium) }
                        GlassButton("Add folder", { folderPicker.launch(null) }, icon = Icons.Rounded.Add)
                        if (folders.isNotEmpty()) GlassButton("Start scan", { startScan(ScanMode.SELECTED_ONLY) }, filled = true)
                    }
                    else -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(scan.phase.ifBlank { "Scanning…" }, style = MaterialTheme.typography.titleMedium, color = on)
                        Text("${scan.found} songs found", style = MaterialTheme.typography.bodyMedium, color = on.copy(alpha = 0.75f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    val on = MaterialTheme.colorScheme.onSurface
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = on)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val on = MaterialTheme.colorScheme.onSurface
    GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), onClick = onClick) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = on)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f))
            }
        }
    }
}

/** Temporary screen until T20 (History/Stats). */
@Composable
fun PlaceholderScreen(title: String, onBack: () -> Unit) {
    app.lumio.player.ui.components.ScreenScaffold(title = title, onBack = onBack) {
        item { app.lumio.player.ui.components.EmptyState(title, "Coming in task T20.") }
    }
}
