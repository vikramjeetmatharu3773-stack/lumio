package app.lumio.player.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.lumio.player.container
import app.lumio.player.data.model.CollectionType
import app.lumio.player.data.settings.AppSettings
import app.lumio.player.data.settings.ThemeMode
import app.lumio.player.ui.components.GlassDock
import app.lumio.player.ui.components.LocalOverlays
import app.lumio.player.ui.components.OverlayController
import app.lumio.player.ui.components.OverlayHost
import app.lumio.player.ui.screens.CollectionScreen
import app.lumio.player.ui.screens.EqualizerScreen
import app.lumio.player.ui.screens.FoldersScreen
import app.lumio.player.ui.screens.HomeScreen
import app.lumio.player.ui.screens.LibraryScreen
import app.lumio.player.ui.screens.NowPlayingSheet
import app.lumio.player.ui.screens.SearchScreen
import app.lumio.player.ui.screens.SettingsAboutScreen
import app.lumio.player.ui.screens.SettingsAppearanceScreen
import app.lumio.player.ui.screens.SettingsLandingScreen
import app.lumio.player.ui.screens.SettingsLibraryScreen
import app.lumio.player.ui.screens.SettingsLyricsScreen
import app.lumio.player.ui.screens.SettingsPlaybackScreen
import app.lumio.player.ui.screens.SettingsPrivacyScreen
import app.lumio.player.ui.screens.SettingsWidgetsScreen
import app.lumio.player.ui.theme.BackdropEffect
import app.lumio.player.ui.theme.BackdropState
import app.lumio.player.ui.theme.LocalBackdrop
import app.lumio.player.ui.theme.LocalIsDark
import app.lumio.player.ui.theme.LocalMotion
import app.lumio.player.ui.theme.LumioColors
import app.lumio.player.ui.theme.LumioTheme
import app.lumio.player.ui.theme.drawBackdrop
import app.lumio.player.util.ArtworkTheme
import app.lumio.player.util.ArtworkThemeBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.onSizeChanged as layoutOnSizeChanged

@Composable
fun AppRoot(openPlayerRequests: StateFlow<Int>, onReady: () -> Unit = {}) {
    val ctx = LocalContext.current
    val c = ctx.container
    val loaded by remember { c.settings.flow.map<AppSettings, AppSettings?> { it } }.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(loaded != null) { if (loaded != null) onReady() }
    val s = loaded ?: return Box(Modifier.fillMaxSize().drawBehind { drawRect(LumioColors.BgDark) })

    val systemDark = isSystemInDarkTheme()
    val dark = s.themeMode == ThemeMode.DARK || s.themeMode == ThemeMode.AMOLED || (s.themeMode == ThemeMode.SYSTEM && systemDark)
    val baseBg = when {
        s.themeMode == ThemeMode.AMOLED -> Color.Black
        dark -> LumioColors.BgDark
        else -> LumioColors.BgLight
    }

    val backdrop = remember { BackdropState() }
    val player by c.player.state.collectAsStateWithLifecycle()
    val currentUri = player.current?.uri
    var artTheme by remember { mutableStateOf<ArtworkTheme?>(null) }
    LaunchedEffect(currentUri, s.glassBlur, s.backdropStyle, s.customAccent, baseBg) {
        try {
            artTheme = withContext(Dispatchers.Default) {
                ArtworkThemeBuilder.build(ctx, currentUri, s.glassBlur, s.backdropStyle, s.customAccent, baseBg.toArgb())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            app.lumio.player.crash.CrashReporter.logNonFatal("ArtworkTheme", e)
        }
    }
    val backdropBitmap = remember(artTheme) { artTheme?.backdrop?.asImageBitmap() }
    val permissionGranted = true

    LumioTheme(s, artTheme?.let { Color(it.accent) }, backdrop) {
        BackdropEffect(backdrop, backdropBitmap)
        Box(
            Modifier.fillMaxSize().layoutOnSizeChanged { backdrop.rootSize = it },
        ) {
            LiquidBackground(backdrop, baseBg, dark, s.themeMode == ThemeMode.AMOLED)
            MainContent(s, openPlayerRequests)
        }
    }
}

@Composable
private fun LiquidBackground(backdrop: BackdropState, base: Color, dark: Boolean, amoled: Boolean) {
    val scrim = when { amoled -> Color.Black.copy(alpha = 0.45f); dark -> Color.Black.copy(alpha = 0.35f); else -> Color.White.copy(alpha = 0.25f) }
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(base)
            drawBackdrop(backdrop)
            drawRect(scrim)
        }
    )
}

@Composable
private fun MainContent(s: AppSettings, openPlayerRequests: StateFlow<Int>) {
    val c = LocalContext.current.container
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val overlays = remember { OverlayController() }
    val player by c.player.state.collectAsStateWithLifecycle()
    val motion = LocalMotion.current
    val scope = rememberCoroutineScope()
    val expand = remember { Animatable(0f) }
    val expanded by remember { derivedStateOf { expand.value > 0.5f } }

    // scan on launch
    LaunchedEffect(Unit) { if (s.autoScanOnLaunch) c.scanner.scan(deep = false) }
    // external "open now playing" requests (notification tap, VIEW intents)
    LaunchedEffect(Unit) {
        openPlayerRequests.collect { n -> if (n > 0) expand.animateTo(1f, motion.smooth()) }
    }
    // keep screen on while Now Playing is open (optional setting)
    val view = LocalView.current
    DisposableEffect(expanded, s.keepScreenOn) {
        view.keepScreenOn = expanded && s.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    fun navigate(r: String) {
        scope.launch { if (expand.value > 0f) expand.animateTo(0f, motion.smooth()) }
        nav.navigate(r) { launchSingleTop = true }
    }
    fun navigateTab(r: String) {
        nav.navigate(r) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalOverlays provides overlays) {
        Box(Modifier.fillMaxSize()) {
            AppNavHost(nav, ::navigate)

            val showDock = route != Routes.EQUALIZER && route != Routes.FOLDERS && route?.startsWith("settings/") != true
            if (showDock) {
                GlassDock(
                    currentRoute = route,
                    dockStyle = s.dockStyle,
                    song = player.current,
                    isPlaying = player.isPlaying,
                    position = c.player.position,
                    durationMs = player.durationMs,
                    onHome = { navigateTab(Routes.HOME) },
                    onLibrary = { navigateTab(Routes.library()) },
                    onSearch = { navigateTab(Routes.SEARCH) },
                    onOpenPlayer = { scope.launch { expand.animateTo(1f, motion.smooth()) } },
                    onToggle = { c.player.togglePlay() },
                    onNext = { c.player.next() },
                    onPrevious = { c.player.previous() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            NowPlayingSheet(progress = expand, onNavigate = ::navigate)
            OverlayHost(overlays) { r -> navigate(r) }
        }
    }
}

private fun fwd(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { fadeIn(tween(260)) + slideInHorizontally(tween(300)) { it / 12 } }
private fun fwdExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { fadeOut(tween(160)) }
private fun back(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { fadeIn(tween(260)) }
private fun backExit(): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 12 } }

@Composable
private fun AppNavHost(nav: NavHostController, go: (String) -> Unit) {
    val s = LocalContext.current.container
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        modifier = Modifier.fillMaxSize(),
        enterTransition = fwd(), exitTransition = fwdExit(), popEnterTransition = back(), popExitTransition = backExit(),
    ) {
        composable(Routes.HOME) { HomeScreen(go) }
        composable(
            Routes.LIBRARY,
            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 }),
        ) { e -> LibraryScreen(initialTab = e.arguments?.getInt("tab") ?: 0, onNavigate = go) }
        composable(Routes.SEARCH) { SearchScreen(go) }
        composable(
            Routes.COLLECTION,
            arguments = listOf(navArgument("type") { type = NavType.StringType }, navArgument("key") { type = NavType.StringType }),
        ) { e ->
            CollectionScreen(
                type = CollectionType.from(e.arguments?.getString("type").orEmpty()),
                key = e.arguments?.getString("key").orEmpty(),
                onBack = { nav.popBackStack() },
                onNavigate = go,
            )
        }
        composable(Routes.SETTINGS) { SettingsLandingScreen(onBack = { nav.popBackStack() }, onNavigate = go) }
        composable(Routes.SET_APPEARANCE) { SettingsAppearanceScreen { nav.popBackStack() } }
        composable(Routes.SET_PLAYBACK) { SettingsPlaybackScreen { nav.popBackStack() } }
        composable(Routes.SET_LIBRARY) { SettingsLibraryScreen(onBack = { nav.popBackStack() }, onNavigate = go) }
        composable(Routes.SET_LYRICS) { SettingsLyricsScreen { nav.popBackStack() } }
        composable(Routes.SET_WIDGETS) { SettingsWidgetsScreen { nav.popBackStack() } }
        composable(Routes.SET_PRIVACY) { SettingsPrivacyScreen { nav.popBackStack() } }
        composable(Routes.SET_ABOUT) { SettingsAboutScreen { nav.popBackStack() } }
        composable(Routes.FOLDERS) { FoldersScreen { nav.popBackStack() } }
        composable(Routes.EQUALIZER) { EqualizerScreen { nav.popBackStack() } }
        // T20 will replace these placeholders with real History / Stats screens
        composable(Routes.HISTORY) { app.lumio.player.ui.screens.PlaceholderScreen("History") { nav.popBackStack() } }
        composable(Routes.STATS) { app.lumio.player.ui.screens.PlaceholderScreen("Stats") { nav.popBackStack() } }
    }
}
