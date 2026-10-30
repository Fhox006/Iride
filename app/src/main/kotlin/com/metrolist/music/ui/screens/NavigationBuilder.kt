/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.metrolist.music.ui.screens

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.CompositionLocalProvider
import com.metrolist.music.ui.component.LocalAnimatedVisibilityScope
import com.metrolist.music.ui.component.CollectionExpansionPage
import com.metrolist.music.ui.component.LocalCollectionSourceEntry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.navArgument
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.repository.ChartPlaylistIdentity
import com.metrolist.music.ui.component.IrideArtistPageSurface
import com.metrolist.music.ui.screens.artist.ArtistAlbumsScreen
import com.metrolist.music.ui.screens.artist.ArtistDiscographyScreen
import com.metrolist.music.ui.screens.artist.ArtistGameScreen
import com.metrolist.music.ui.screens.artist.ArtistItemsScreen
import com.metrolist.music.ui.screens.artist.ArtistScreen
import com.metrolist.music.ui.screens.artist.ArtistSongsScreen
import com.metrolist.music.ui.screens.equalizer.EqEditorScreen
import com.metrolist.music.ui.screens.library.LibraryCategory
import com.metrolist.music.ui.screens.library.LibraryScreen
import com.metrolist.music.ui.screens.playlist.AutoPlaylistScreen
import com.metrolist.music.ui.screens.playlist.DailyChartScreen
import com.metrolist.music.ui.screens.playlist.DailyChartsWorldScreen
import com.metrolist.music.ui.screens.playlist.LocalPlaylistScreen
import com.metrolist.music.ui.screens.playlist.OnlinePlaylistScreen
import com.metrolist.music.ui.screens.playlist.TopPlaylistScreen
import com.metrolist.music.ui.screens.podcast.OnlinePodcastScreen
import com.metrolist.music.ui.screens.recognition.RecognitionHistoryScreen
import com.metrolist.music.ui.screens.recognition.RecognitionScreen
import com.metrolist.music.ui.screens.search.OnlineSearchResult
import com.metrolist.music.ui.screens.search.SearchScreen
import com.metrolist.music.ui.screens.search.RockModuleScreen
import com.metrolist.music.ui.screens.settings.AboutScreen
import com.metrolist.music.ui.screens.settings.AiSettings
import com.metrolist.music.ui.screens.settings.AndroidAutoSettings
import com.metrolist.music.ui.screens.settings.BackupAndRestore
import com.metrolist.music.ui.screens.settings.ContentSettings
import com.metrolist.music.ui.screens.settings.DesignTestScreen
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.ForceDarkTheme
import com.metrolist.music.ui.screens.settings.DiscordLoginScreen
import com.metrolist.music.ui.screens.settings.InterfaceSettings
import com.metrolist.music.ui.screens.settings.PlayerSettings
import com.metrolist.music.ui.screens.settings.MyAccountScreen
import com.metrolist.music.ui.screens.settings.NotesScreen
import com.metrolist.music.ui.screens.settings.PrivacySettings
import com.metrolist.music.ui.screens.settings.RomanizationSettings
import com.metrolist.music.ui.screens.settings.SettingsScreen
import com.metrolist.music.ui.screens.settings.StorageSettings
import com.metrolist.music.ui.screens.settings.UpdaterScreen
import com.metrolist.music.ui.screens.SpotifyImportScreen
import com.metrolist.music.ui.screens.settings.integrations.DiscordSettings
import com.metrolist.music.ui.screens.settings.integrations.IntegrationScreen
import com.metrolist.music.ui.screens.settings.integrations.LastFMSettings
import com.metrolist.music.ui.screens.settings.integrations.ListenTogetherSettings
import com.metrolist.music.ui.screens.wrapped.WrappedScreen
import com.metrolist.music.ui.screens.GenresScreen
import com.metrolist.music.ui.screens.IrideScreen
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.NavigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    portraitComposable("onboarding") {
        OnboardingScreen(navController = navController)
    }

    portraitComposable(Screens.Home.route) {
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            HomeScreen(navController = navController, snackbarHostState = snackbarHostState)
        }
    }

    portraitComposable(
        route = Screens.Search.route,
    ) { backStackEntry ->
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()

        val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
            ForceDarkTheme ||
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }
        val pureBlack = remember(pureBlackEnabled, useDarkTheme) {
            pureBlackEnabled && useDarkTheme
        }

        SearchScreen(
            navController = navController,
            pureBlack = pureBlack,
            savedStateHandle = backStackEntry.savedStateHandle
        )
    }

    portraitComposable(Screens.Library.route) {
        LibraryScreen(navController)
    }

    portraitComposable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController, showTopBar = false)
    }

    portraitComposable(route = "listen_together_from_topbar") {
        ListenTogetherScreen(navController, showTopBar = true)
    }

    portraitComposable("history") { HistoryScreen(navController) }
    // Redirect restored navigation state from the retired News tab.
    portraitComposable("new_releases") {
        LaunchedEffect(Unit) {
            navController.navigate(Screens.Home.route) {
                popUpTo("new_releases") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    portraitComposable("all_charts") { com.metrolist.music.ui.screens.playlist.AllChartsScreen(navController) }
    portraitComposable("stats") { StatsScreen(navController) }
    dialog(
        route = "reverie/{month}",
        arguments = listOf(navArgument("month") { type = NavType.StringType }),
        dialogProperties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
        ),
    ) { entry ->
        val month = entry.arguments?.getString("month")?.let { value ->
            runCatching { java.time.YearMonth.parse(value) }.getOrNull()
        }
        if (month != null) {
            ReverieScreen(navController, month)
        } else {
            LaunchedEffect(Unit) { navController.navigateUp() }
        }
    }
    portraitComposable("notes") { NotesScreen(navController) }
    portraitComposable("iride") {
        var readyToAnimate by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { delay(320); readyToAnimate = true }
        IrideScreen(navController, readyToAnimate = readyToAnimate)
    }
    portraitComposable(
        route = "video_player/{videoId}?title={title}",
        arguments = listOf(
            navArgument("videoId") { type = NavType.StringType },
            navArgument("title") { type = NavType.StringType; nullable = true },
        )
    ) {
        VideoPlayerScreen(
            navController = navController,
            videoId = it.arguments?.getString("videoId").orEmpty(),
            initialTitle = it.arguments?.getString("title"),
        )
    }
    portraitComposable("mood_and_genres") { MoodAndGenresScreen(navController) }
    portraitComposable("genres_screen") { GenresScreen(navController = navController) }
    portraitComposable("rock_module") { RockModuleScreen(navController = navController) }
    portraitComposable("account") { AccountScreen(navController) }

    portraitComposable(
        route = "browse/{browseId}",
        arguments = listOf(navArgument("browseId") { type = NavType.StringType })
    ) {
        BrowseScreen(
            navController,
            it.arguments?.getString("browseId")
        )
    }

    portraitComposable(
        route = "search/{query}",
        arguments = listOf(navArgument("query") { type = NavType.StringType }),
    ) { backStackEntry ->
        OnlineSearchResult(
            navController = navController,
            savedStateHandle = backStackEntry.savedStateHandle
        )
    }

    portraitComposable(
        route = "album/{albumId}",
        arguments = listOf(navArgument("albumId") { type = NavType.StringType })
    ) {
        AlbumScreen(navController)
    }

    portraitComposable(
        route = "artist/{artistId}?isPodcastChannel={isPodcastChannel}&portraitSource={portraitSource}",
        arguments = listOf(
            navArgument("artistId") { type = NavType.StringType },
            navArgument("isPodcastChannel") { type = NavType.BoolType; defaultValue = false },
            navArgument("portraitSource") { type = NavType.StringType; nullable = true; defaultValue = null },
        )
    ) {
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            ArtistScreen(navController, portraitSourceKey = it.arguments?.getString("portraitSource"))
        }
    }

    portraitComposable(
        route = "artist/{artistId}/songs",
        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
    ) { entry ->
        IrideArtistPageSurface(requireNotNull(entry.arguments?.getString("artistId"))) {
            ArtistSongsScreen(navController)
        }
    }

    portraitComposable(
        route = "artist/{artistId}/game",
        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
    ) { entry ->
        IrideArtistPageSurface(requireNotNull(entry.arguments?.getString("artistId"))) {
            ArtistGameScreen(navController)
        }
    }

    portraitComposable(
        route = "artist/{artistId}/albums",
        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
    ) { entry ->
        IrideArtistPageSurface(requireNotNull(entry.arguments?.getString("artistId"))) {
            ArtistAlbumsScreen(navController, scrollBehavior)
        }
    }

    portraitComposable(
        route = "artist/{artistId}/discography",
        arguments = listOf(navArgument("artistId") { type = NavType.StringType })
    ) { entry ->
        IrideArtistPageSurface(requireNotNull(entry.arguments?.getString("artistId"))) {
            ArtistDiscographyScreen(navController)
        }
    }

    portraitComposable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments = listOf(
            navArgument("artistId") { type = NavType.StringType },
            navArgument("browseId") { type = NavType.StringType; nullable = true },
            navArgument("params") { type = NavType.StringType; nullable = true }
        )
    ) { entry ->
        IrideArtistPageSurface(requireNotNull(entry.arguments?.getString("artistId"))) {
            ArtistItemsScreen(navController)
        }
    }

    portraitComposable("daily_charts") {
        DailyChartsWorldScreen(navController)
    }

    portraitComposable(
        route = "new_releases_chart/{market}",
        arguments = listOf(navArgument("market") { type = NavType.StringType }),
    ) {
        DailyChartScreen(navController, englishTitle = true)
    }

    portraitComposable(
        route = "daily_chart/{market}",
        arguments = listOf(navArgument("market") { type = NavType.StringType }),
    ) {
        DailyChartScreen(navController)
    }

    portraitComposable(
        route = "online_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
    ) {
        OnlinePlaylistScreen(navController)
    }

    portraitComposable(
        route = "online_podcast/{podcastId}",
        arguments = listOf(navArgument("podcastId") { type = NavType.StringType })
    ) {
        OnlinePodcastScreen(navController, scrollBehavior)
    }

    portraitComposable(
        route = "local_playlist/{playlistId}",
        arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
    ) { backStackEntry ->
        val market = ChartPlaylistIdentity.market(backStackEntry.arguments?.getString("playlistId").orEmpty())
        if (market == null) {
            LocalPlaylistScreen(navController)
        } else {
            LaunchedEffect(backStackEntry.id) {
                if (navController.currentBackStackEntry == backStackEntry) {
                    navController.navigate("new_releases_chart/${market.code}") {
                        popUpTo(backStackEntry.destination.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    portraitComposable(
        route = "auto_playlist/{playlist}",
        arguments = listOf(navArgument("playlist") { type = NavType.StringType })
    ) {
        AutoPlaylistScreen(navController)
    }

    portraitComposable(
        route = "cache_playlist/{playlist}",
        arguments = listOf(navArgument("playlist") { type = NavType.StringType })
    ) {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.CACHE, initialCache = true)
    }

    portraitComposable(
        route = "top_playlist/{top}",
        arguments = listOf(navArgument("top") { type = NavType.StringType })
    ) {
        TopPlaylistScreen(navController)
    }

    portraitComposable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments = listOf(
            navArgument("browseId") { type = NavType.StringType; nullable = true },
            navArgument("params") { type = NavType.StringType; nullable = true }
        )
    ) {
        YouTubeBrowseScreen(navController)
    }

    portraitComposable("settings") {
        SettingsScreen(navController, latestVersionName, activity, snackbarHostState)
    }

    portraitComposable("settings/my_account") {
        MyAccountScreen(navController)
    }

    portraitComposable("music_import") {
        MusicImportScreen(navController)
    }

    portraitComposable("apple_music_import") {
        AppleMusicImportScreen(navController)
    }

    portraitComposable("settings/interface") {
        InterfaceSettings(navController, activity)
    }

    portraitComposable("settings/content") {
        ContentSettings(navController)
    }

    portraitComposable("settings/content/design-test") {
        DesignTestScreen(navController)
    }

    portraitComposable("settings/content/romanization") {
        RomanizationSettings(navController)
    }

    portraitComposable("settings/ai") {
        AiSettings(navController)
    }

    portraitComposable("settings/player") {
        PlayerSettings(navController)
    }

    portraitComposable("settings/storage") {
        StorageSettings(navController)
    }

    portraitComposable("settings/privacy") {
        PrivacySettings(navController)
    }

    portraitComposable("settings/backup_restore") {
        BackupAndRestore(navController)
    }

    portraitComposable("settings/integrations") {
        IntegrationScreen(navController)
    }

    portraitComposable("settings/integrations/discord") {
        DiscordSettings(navController, snackbarHostState)
    }

    portraitComposable("settings/integrations/lastfm") {
        LastFMSettings(navController)
    }

    portraitComposable(route = "settings/integrations/listen_together") {
        ListenTogetherSettings(navController)
    }

    portraitComposable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }

    portraitComposable("settings/updater") {
        UpdaterScreen(navController)
    }

    portraitComposable("settings/about") {
        AboutScreen(navController)
    }

    portraitComposable("settings/equalizer") {
        EqEditorScreen(navController)
    }

    portraitComposable("login") {
        LoginScreen(navController)
    }

    portraitComposable("wrapped") {
        WrappedScreen(navController)
    }

    portraitComposable(
        route = "recognition?autoStart={autoStart}",
        arguments = listOf(navArgument("autoStart") { type = NavType.BoolType; defaultValue = false })
    ) {
        RecognitionScreen(navController, it.arguments?.getBoolean("autoStart") ?: false)
    }

    portraitComposable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }

    portraitComposable("settings/android_auto") {
        AndroidAutoSettings(navController, scrollBehavior)
    }

    portraitComposable("spotify_import") {
        SpotifyImportScreen(navController = navController)
    }

    portraitComposable("library_playlists") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.PLAYLISTS)
    }
    portraitComposable("library_songs") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.SONGS)
    }
    portraitComposable("library_songs_local") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.LOCAL_SONGS)
    }
    portraitComposable("library_albums") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.ALBUMS)
    }
    portraitComposable("library_artists") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.ARTISTS)
    }
    portraitComposable("library_playlists_offline") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.PLAYLISTS, initialOffline = true)
    }
    portraitComposable("library_songs_offline") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.SONGS, initialOffline = true)
    }
    portraitComposable("library_albums_offline") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.ALBUMS, initialOffline = true)
    }
    portraitComposable("library_artists_offline") {
        LibraryScreen(navController = navController, initialCategory = LibraryCategory.ARTISTS, initialOffline = true)
    }
}

private fun NavGraphBuilder.portraitComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) {
    composable(route = route, arguments = arguments) { entry ->
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this, LocalCollectionSourceEntry provides entry.id) {
            CollectionExpansionPage(entry) { content(entry) }
        }
    }
}
