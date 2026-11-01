/*
 * Traduzione riga per riga di HomeScreen (parte 1 di N)
 * Sorgente: app/src/main/kotlin/com/metrolist/music/ui/screens/HomeScreen.kt
 *           (tag v0.1.0-alpha33, righe 219-664 tradotte; 664-2829 DA FARE)
 *
 * Ogni blocco porta il numero di riga Kotlin in un commento "KT:".
 * Le dipendenze sono in home-components.js (stub con riferimento KT).
 * Questo file NON e' ancora caricato da index.html.
 *
 * Convenzioni (come library-albums.js):
 *  - Stato Compose (remember / collectAsState / preference) -> valori o oggetto `state`.
 *  - LaunchedEffect -> chiamata diretta alla funzione di avvio (una volta).
 *  - Composable -> funzione JS che restituisce un nodo DOM.
 */
(function (global) {
  'use strict';

  const C = global.IrideHomeComponents; // home-components.js

  // KT:219  private const val SHOW_HOME_TEST_PLAYLIST = false
  const SHOW_HOME_TEST_PLAYLIST = false;

  // KT:365-391  fun HomeScreen(navController, snackbarHostState, viewModel)
  // Avvolge HomeScreenContent con il local LocalHomeContentSnapshot.
  function HomeScreen(props) {
    const navController = props.navController;
    const snackbarHostState = props.snackbarHostState;
    const viewModel = props.viewModel; // HomeViewModel (vedi stub)

    // KT:370  val lifecycle = LocalLifecycleOwner.current.lifecycle
    const lifecycle = C.LocalLifecycleOwner().lifecycle;
    // KT:371-387  DisposableEffect(lifecycle, viewModel) { observer ON_START/ON_STOP ... }
    const dispose = C.disposableLifecycleEffect(lifecycle, viewModel, (event) => {
      if (event === 'ON_START') viewModel.contentSnapshot.setVisible(true);   // KT:374
      else if (event === 'ON_STOP') viewModel.contentSnapshot.setVisible(false); // KT:375
    }, () => {
      viewModel.contentSnapshot.setVisible(false); // KT:385
    });
    // KT:379-381  setVisible(lifecycle.currentState.isAtLeast(STARTED))
    viewModel.contentSnapshot.setVisible(C.lifecycleIsAtLeastStarted(lifecycle));

    // KT:388-390  CompositionLocalProvider(LocalHomeContentSnapshot provides viewModel.contentSnapshot)
    return C.provideLocals({ LocalHomeContentSnapshot: viewModel.contentSnapshot }, () =>
      HomeScreenContent({ navController, snackbarHostState, viewModel }));
  }

  // KT:399-403  private fun HomeScreenContent(navController, snackbarHostState, viewModel)
  function HomeScreenContent(props) {
    const navController = props.navController;
    const snackbarHostState = props.snackbarHostState;
    const viewModel = props.viewModel;

    // KT:404  val menuState = LocalMenuState.current
    const menuState = C.LocalMenuState();
    // KT:405-406  chartViewModel, reverieViewModel (hiltViewModel)
    const chartViewModel = C.hiltViewModel('ChartDirectoryViewModel');
    const reverieViewModel = C.hiltViewModel('MonthlyReverieViewModel');
    // KT:407-408  reverieMonths, firstFullReverieMonth (collectAsStateWithLifecycle)
    const reverieMonths = reverieViewModel.archiveMonths();
    const firstFullReverieMonth = reverieViewModel.firstFullMonth();
    // KT:409  PreloadHomeCharts(chartViewModel)
    C.PreloadHomeCharts(chartViewModel);
    // KT:410  followedReleasesPosition = rememberSaveable { Random.nextInt(1, 6) }  (1..5)
    const followedReleasesPosition = C.rememberSaveable('followedReleasesPosition', () => C.randomInt(1, 6));
    // KT:411-414  playerConnection, database, context, listenTogetherManager
    const playerConnection = C.LocalPlayerConnection();
    const database = C.LocalDatabase();
    const context = C.LocalContext();
    const listenTogetherManager = C.LocalListenTogetherManager();
    // KT:415  isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    const isListenTogetherGuest = listenTogetherManager
      ? (listenTogetherManager.isInRoom && !listenTogetherManager.isHost)
      : false;

    // KT:417-421  isPlayingState = playerConnection?.isEffectivelyPlaying ?: MutableStateFlow(false)
    const isPlayingState = playerConnection ? playerConnection.isEffectivelyPlaying : C.MutableStateFlow(false);
    // KT:422-426  mediaMetadataState = playerConnection?.mediaMetadata ?: MutableStateFlow(null)
    const mediaMetadataState = playerConnection ? playerConnection.mediaMetadata : C.MutableStateFlow(null);

    // KT:428-434  flag di caricamento / completamento (collectAsStateWithLifecycle)
    const isHomeFeedLoading = viewModel.isHomeFeedLoading();
    const isForYouLoading = viewModel.isForYouLoading();
    const isAccountPlaylistsLoading = viewModel.isAccountPlaylistsLoading();
    const dailyDiscoverComplete = viewModel.phase2DailyDiscoverDone();
    const communityComplete = viewModel.phase2CommunityDone();
    const similarComplete = viewModel.phase2SimilarDone();

    // KT:436-439  communityPlaylists (collectDeferredHomeState, hasContent: distinct id >= 4)
    const communityPlaylists = C.collectDeferredHomeState(viewModel.communityPlaylists, 'community_playlists',
      (it) => (it || []).filter((x, i, a) => a.findIndex((y) => y.playlist.id === x.playlist.id) === i).length >= 4);
    // KT:440-443  dailyDiscover (distinct recommendation.id >= 4)
    const dailyDiscover = C.collectDeferredHomeState(viewModel.dailyDiscover, 'daily_discover',
      (it) => (it || []).filter((x, i, a) => a.findIndex((y) => y.recommendation.id === x.recommendation.id) === i).length >= 4);
    // KT:444-447  accountPlaylists (distinct id >= 4)
    const accountPlaylists = C.collectDeferredHomeState(viewModel.accountPlaylists, 'account_playlists',
      (it) => (it || []).filter((x, i, a) => a.findIndex((y) => y.id === x.id) === i).length >= 4);
    // KT:448  homePersonalMixes
    const homePersonalMixes = C.collectDeferredHomeState(viewModel.homePersonalMixes, 'personal_mixes');
    // KT:449-450  starredPlaylistThumbnails
    const starredPlaylistThumbnails = C.collectDeferredHomeState(viewModel.starredPlaylistThumbnails, 'starred_thumbnails');
    // KT:451-457  similarRecommendations (mergeWhileVisible -> appendLoadedHomeSections)
    const similarRecommendations = C.collectDeferredHomeState(viewModel.similarRecommendations, 'similar_recommendations', {
      mergeWhileVisible: (current, incoming) =>
        C.appendLoadedHomeSections(current || [], incoming || [], (it) => it.title.id),
    });
    // KT:458-464  quickPicks (no video, no episode, distinct id >= 4)
    const quickPicks = C.collectDeferredHomeState(viewModel.quickPicks, 'quick_picks', (it) =>
      (it || [])
        .filter((song) => !(song.song.isVideo || song.song.isEpisode))
        .filter((x, i, a) => a.findIndex((y) => y.id === x.id) === i).length >= 4);
    // KT:465-473  forYouShelves (mergeWhileVisible artist.id, hasContent >= 4)
    const forYouShelves = C.collectDeferredHomeState(viewModel.forYouShelves, 'for_you', {
      mergeWhileVisible: (current, incoming) =>
        C.appendLoadedHomeSections(current, incoming, (it) => it.artist.id),
      hasContent: (it) => it.length >= 4,
    });
    // KT:474-475  homePage (hasContent: sections non vuote)
    const homePage = C.collectDeferredHomeState(viewModel.homePage, 'feed', (it) => !!(it && it.sections && it.sections.length));
    // KT:476-478  phase1Complete, isHeroCarouselEnabled, isHeroCarouselLoading
    const phase1Complete = viewModel.phase1Complete();
    const isHeroCarouselEnabled = viewModel.isHeroCarouselEnabled();
    const isHeroCarouselLoading = viewModel.isHeroCarouselLoading();
    // KT:479-493  heroCarouselItems (mergeWhileVisible: copertine ArtistRadio)
    const heroCarouselItems = C.collectDeferredHomeState(viewModel.heroCarouselItems, 'hero_carousel', {
      mergeWhileVisible: (current, incoming) => {
        const covers = C.associateBy(
          incoming.filter((it) => it.type === 'ArtistRadio'),
          (it) => it.artistId);
        return current.map((item) => {
          if (item.type === 'ArtistRadio') {
            const c = covers[item.artistId];
            return (c && c.coverUrl) ? C.copy(item, { coverUrl: c.coverUrl }) : item;
          }
          return item;
        });
      },
    });

    // KT:495-501  accountName (AccountName != "Guest" ? flow : pref)
    const accountNameFlow = viewModel.accountName();
    const syncState = viewModel.syncState();
    const syncBannerLaunchCount = viewModel.syncBannerLaunchCount();
    // KT:498  innerTubeCookie = rememberPreference(InnerTubeCookieKey, "")
    const innerTubeCookie = C.rememberPreference('InnerTubeCookieKey', '');
    // KT:499  isLoggedIn = "SAPISID" in parseCookieString(innerTubeCookie)
    const isLoggedIn = C.parseCookieString(innerTubeCookie).has('SAPISID');
    // KT:500  accountNamePref = rememberPreference(AccountNameKey, "")
    const accountNamePref = C.rememberPreference('AccountNameKey', '');
    // KT:501  accountName = if (accountNameFlow != "Guest") accountNameFlow else accountNamePref
    const accountName = accountNameFlow !== 'Guest' ? accountNameFlow : accountNamePref;

    // KT:503-505  irideStart, irideListItemStart, irideGridItemStart = 20.dp
    const irideStart = 20;
    const irideListItemStart = 20;
    const irideGridItemStart = 20;
    // KT:506-508  hideExplicit, hideVideoSongs, hideYoutubeShorts (default false)
    const hideExplicit = C.rememberPreference('HideExplicitKey', false);
    const hideVideoSongs = C.rememberPreference('HideVideoSongsKey', false);
    const hideYoutubeShorts = C.rememberPreference('HideYoutubeShortsKey', false);
    // KT:509  currentGridHeight = currentGridThumbnailHeight()
    const currentGridHeight = C.currentGridThumbnailHeight();

    // KT:511-512  lazyListState, homeReducedMotion
    const lazyListState = C.rememberLazyListState();
    const homeReducedMotion = C.rememberReducedMotion();
    // KT:513-521  moodContent (identity params, hasContent items, mergeWhileVisible posters)
    const moodContent = C.collectDeferredHomeState(viewModel.homeMoodContent, 'mood', {
      identity: (it) => it.params,
      hasContent: (it) => !!(it.items && it.items.length),
      mergeWhileVisible: (current, incoming) =>
        C.copy(current, { posters: current.posters.concat(incoming.posters) }),
    });
    // KT:522  moodMixItems = moodContent.items
    const moodMixItems = moodContent.items;
    // KT:523-533  testPlaylist (solo se SHOW_HOME_TEST_PLAYLIST)
    const testPlaylist = SHOW_HOME_TEST_PLAYLIST
      ? (moodMixItems && moodMixItems[0]) || (homePersonalMixes[0]) || C.firstPlaylistItem(homePage && homePage.sections)
      : null;
    // KT:534-535  testPlaylistTrackThumbnails
    const testPlaylistTrackThumbnails = viewModel.testPlaylistTrackThumbnails();
    // KT:536-540  LaunchedEffect(testPlaylist?.id) { loadTestPlaylistArtwork }
    if (SHOW_HOME_TEST_PLAYLIST && testPlaylist) viewModel.loadTestPlaylistArtwork(testPlaylist.id);
    // KT:541  moodPosterUrls = moodContent.posters
    const moodPosterUrls = moodContent.posters;
    // KT:542-543  moodPosterSize = currentGridHeight in px, coerceIn(128, 768)
    const moodPosterSize = C.clamp(C.toPx(currentGridHeight), 128, 768);
    // KT:544-548  isMoodLoading, isMoodError, loadedMoodParams, preferredMoodParams, isMoodPreferenceReady
    const isMoodLoading = viewModel.isMoodLoading();
    const isMoodError = viewModel.isMoodError();
    const loadedMoodParams = moodContent.params;
    const preferredMoodParams = viewModel.preferredMoodParams();
    const isMoodPreferenceReady = viewModel.isMoodPreferenceReady();
    // KT:549-551  var selectedMoodCategory: HomePage.Chip? = null
    const selectedMood = C.useState('selectedMoodCategory', null);
    // KT:552  cachedMoodSnapshot
    const cachedMoodSnapshot = viewModel.cachedMoodSnapshot();
    // KT:553-576  moodChips = remember(homePage?.chips, cachedMoodSnapshot) { ... }
    const moodChips = C.remember([homePage && homePage.chips, cachedMoodSnapshot], () => {
      const feedChips = C.MoodMixes.chips(homePage && homePage.chips);            // KT:555
      let availableChips = feedChips;                                             // KT:556-557
      if (!feedChips.length && cachedMoodSnapshot && cachedMoodSnapshot.chipParams) { // KT:558-559
        availableChips = [{                                                       // KT:561-571
          title: cachedMoodSnapshot.chipTitle,
          endpoint: { browseId: 'FEmusic_home', params: cachedMoodSnapshot.chipParams },
          deselectEndPoint: null,
        }];
      }
      return availableChips.map((it) => [it, it.title]);                          // KT:575
    });
    // KT:577  moodMixesState = rememberLazyListState()
    const moodMixesState = C.rememberLazyListState();
    // KT:578-579  moodLifecycleOwner, moodContext
    const moodLifecycleOwner = C.LocalLifecycleOwner();
    const moodContext = C.LocalContext();
    // KT:580-617  LaunchedEffect(moodLifecycleOwner, selectedMood, hideExplicit, hideVideoSongs, hideYoutubeShorts, moodPosterSize)
    C.launchRepeatOnLifecycle(moodLifecycleOwner.lifecycle, 'STARTED', async () => {
      if (selectedMood.value !== null) {                                          // KT:589
        viewModel.loadMoodPage(selectedMood.value.endpoint && selectedMood.value.endpoint.params,  // KT:590-598
          selectedMood.value.title, hideExplicit, hideVideoSongs, hideYoutubeShorts, moodPosterSize, false);
      }
      // KT:600-615  collectLatest su isMoodError: retry ogni 30 s se internet disponibile
      await viewModel.isMoodError.collectLatest(async (failed) => {
        while (failed && viewModel.isMoodError.value()) {
          await C.delay(30000);                                                   // KT:602
          if (C.isInternetAvailable(moodContext)) {                               // KT:603
            viewModel.loadMoodPage(selectedMood.value && selectedMood.value.endpoint && selectedMood.value.endpoint.params,
              selectedMood.value && selectedMood.value.title, hideExplicit, hideVideoSongs, hideYoutubeShorts, moodPosterSize, true);
          }
        }
      });
    }, [moodLifecycleOwner, selectedMood.value, hideExplicit, hideVideoSongs, hideYoutubeShorts, moodPosterSize]);

    // KT:619-634  LaunchedEffect(moodChips, isMoodPreferenceReady) -> scelta categoria
    C.launchEffect([moodChips, isMoodPreferenceReady], () => {
      if (isMoodPreferenceReady && moodChips.length) {                            // KT:620
        const current = (moodChips.find((p) =>                                    // KT:621-626
          String(p[0].title).toLowerCase() === String(selectedMood.value && selectedMood.value.title).toLowerCase()) || [])[0] || null;
        selectedMood.value = current                                              // KT:627-632
          || ((moodChips.find((p) => p[0].endpoint && p[0].endpoint.params === preferredMoodParams) || [])[0] || null)
          || moodChips[0][0];
      }
    });
    // KT:635-637  LaunchedEffect(selectedMoodCategory) { moodMixesState.scrollToItem(0) }
    C.launchEffect([selectedMood.value], () => moodMixesState.scrollToItem(0));

    // KT:639-649  screenProgress: IrideTabEntrance.wasRevealed("home") ? 1 : rememberEnterProgress(...)
    const screenProgress = C.IrideTabEntrance.wasRevealed('home')
      ? 1
      : C.markRevealedWhenDone(C.rememberEnterProgress({
          play: true,
          durationMillis: C.IrideMotion.Short,
          easing: C.IrideMotion.EaseOutQuart,
        }), 'home');                                                              // KT:648
    // KT:650  scope = rememberCoroutineScope()
    const scope = C.rememberCoroutineScope();
    // KT:651-653  backStackEntry, scrollToTop (savedStateHandle "scrollToTop")
    const backStackEntry = C.LocalViewModelStoreOwner();
    const scrollToTop = backStackEntry && backStackEntry.savedStateHandle ? backStackEntry.savedStateHandle.getStateFlow('scrollToTop', false) : null;
    // KT:654-660  LaunchedEffect(scrollToTop?.value) { if true -> scroll to 0; set false }
    C.launchEffect([scrollToTop && scrollToTop.value], async () => {
      if (scrollToTop && scrollToTop.value === true) {
        if (homeReducedMotion) await lazyListState.scrollToItem(0);               // KT:656
        else await lazyListState.animateScrollToItem(0);                          // KT:657
        backStackEntry.savedStateHandle.set('scrollToTop', false);                // KT:658
      }
    });

    // KT:662-666  LaunchedEffect(Unit) { onSectionBecameVisible x3 }
    C.launchEffect([], () => {
      viewModel.onSectionBecameVisible('daily_discover');
      viewModel.onSectionBecameVisible('from_the_community');
      viewModel.onSectionBecameVisible('similar_recommendation_0');
    });

    // KT:668-716  orderedFeedSections = remember(homePage?.sections) { ... }
    const orderedFeedSections = C.remember([homePage && homePage.sections], () => {
      const sections = (homePage && homePage.sections) || [];                    // KT:670
      const isItalian = (s) => ['italia', 'italy', 'italiano', 'italian'].some((w) => s.title.toLowerCase().includes(w) || (s.label || '').toLowerCase().includes(w)); // KT:678-680
      const selectedTrendingIndex = (() => {                                      // KT:671-696
        const top = sections.map((section, index) => ({ section, index }))
          .filter(({ section }) => {                                              // KT:674-681
            const title = section.title.toLowerCase();
            const label = (section.label || '').toLowerCase();
            return (title.includes('trending') || title.includes('top 100')) &&
              !['italia', 'italy', 'italiano', 'italian'].some((w) => w && (title.includes(w) || label.includes(w)));
          })
          .find(({ section }) => section.title.toLowerCase().includes('top 100'));  // KT:682-684
        if (top) return top.index;                                                // KT:685
        const fb = sections.map((section, index) => ({ section, index })).find(({ section }) => { // KT:686-696
          const title = section.title.toLowerCase();
          const label = (section.label || '').toLowerCase();
          return title.includes('trending') &&
            !['italia', 'italy', 'italiano', 'italian'].some((w) => title.includes(w) || label.includes(w));
        });
        return fb ? fb.index : undefined;                                         // KT:696
      })();
      return sections                                                             // KT:698-715
        .map((section, index) => ({ index, value: section }))                     // KT:699-707
        .filter(({ index, value }) => {                                           // KT:700-704
          const title = value.title.toLowerCase();
          const isTrending = title.includes('trending') || title.includes('top 100');
          return !isTrending || index === selectedTrendingIndex;
        })
        .map((indexed) => ({ index: indexed.index, value: indexed.value }))        // KT:705-707
        .sort((a, b) => {                                                         // KT:708-715
          const rank = (v) => {
            const title = v.value.title.toLowerCase();
            if (title.includes('trending') || title.includes('top 100')) return 0;
            if (title.includes('new release') || title.includes('nuove uscite')) return 1;
            return 2;
          };
          return rank(a) - rank(b);                                               // sortedBy stabile
        });
    });
    // KT:717-723  pickedSongs = remember(quickPicks) { quickPicks ... distinctBy id }
    const pickedSongs = C.remember([quickPicks], () =>
      (quickPicks || [])
        .filter((it) => !(it.song.isVideo || it.song.isEpisode))
        .filter((x, i, a) => a.findIndex((y) => y.id === x.id) === i));

    // KT:725-742  homeColorScheme = MaterialTheme.colorScheme.let { colors -> colors.copy(...) }
    const colors = C.MaterialTheme.colorScheme();
    const homeColorScheme = C.copy(colors, {
      primary: colors.onSurface,                  // KT:728
      onPrimary: colors.surface,                  // KT:729
      primaryContainer: colors.surfaceContainerHighest, // KT:730
      onPrimaryContainer: colors.onSurface,       // KT:731
      inversePrimary: colors.onSurface,           // KT:732
      secondary: colors.onSurfaceVariant,         // KT:733
      onSecondary: colors.surface,                // KT:734
      secondaryContainer: colors.surfaceContainerHighest, // KT:735
      onSecondaryContainer: colors.onSurface,     // KT:736
      tertiary: colors.onSurfaceVariant,          // KT:737
      onTertiary: colors.surface,                 // KT:738
      tertiaryContainer: colors.surfaceContainerHighest, // KT:739
      onTertiaryContainer: colors.onSurface,      // KT:740
    });

    // KT:743-749  MaterialTheme(colorScheme = homeColorScheme, typography = irideFolioShelfTypography()) { IrideFolioDisplayHeadings { Scaffold { ... } } }
    return C.MaterialTheme({ colorScheme: homeColorScheme, typography: C.irideFolioShelfTypography() }, () =>
      C.IrideFolioDisplayHeadings({ menuState }, () =>
        C.Scaffold({ modifier: {}, containerColor: 'transparent', contentWindowInsets: C.WindowInsets(0) }, (paddingValues) =>
          C.BoxWithConstraints({ modifier: C.Modifier.fillMaxSize() }, () => {    // KT:750

            // KT:751-862  ytGridItem: YouTubeGridItem + irideFolioCombinedClickable (click + long-click)
            const ytGridItem = (item) => C.CompositionLocalProvider({ LocalItemHorizontalPadding: false }, () =>
              C.YouTubeGridItem({                                                  // KT:753
                item,
                isActive: [mediaMetadataState.value && mediaMetadataState.value.album && mediaMetadataState.value.album.id,
                  mediaMetadataState.value && mediaMetadataState.value.id].includes(item.id), // KT:755-760
                isPlaying: isPlayingState.value,                                  // KT:761
                coroutineScope: scope,                                            // KT:762
                thumbnailRatio: (item.type === 'SongItem' && item.isVideoSong) ? 16 / 9 : 1, // KT:763-764
                forceThumbnailRatio: item.type === 'SongItem' && item.isVideoSong, // KT:765
                size: currentGridHeight,                                          // KT:766
                modifier: C.irideFolioCombinedClickable({
                  onClick: () => {                                                // KT:769-817
                    switch (item.type) {
                      case 'SongItem':                                            // KT:771-788
                        if (!isListenTogetherGuest) {
                          if (item.isVideoSong) {                                  // KT:773-776
                            if (playerConnection && playerConnection.videoPlayback) playerConnection.videoPlayback.playVideo(C.toMediaMetadata(item));
                          } else {                                                // KT:777-786
                            if (playerConnection) playerConnection.playQueue(C.YouTubeQueue(item.endpoint || C.WatchEndpoint({ videoId: item.id }), C.toMediaMetadata(item)));
                          }
                        }
                        break;
                      case 'AlbumItem': navController.navigate(`album/${item.id}`); break;   // KT:789-790
                      case 'ArtistItem': navController.navigate(`artist/${item.id}`); break; // KT:791-792
                      case 'PlaylistItem': navController.navigate(`online_playlist/${item.id.replace(/^VL/, '')}`); break; // KT:793-796
                      case 'PodcastItem': navController.navigate(`online_podcast/${item.id}`); break; // KT:797-800
                      case 'EpisodeItem':                                          // KT:801-815
                        if (!isListenTogetherGuest && playerConnection) {
                          playerConnection.playQueue(C.ListQueue({ title: item.title, items: [C.toMediaItem(C.toMediaMetadata(item))] }));
                        }
                        break;
                    }
                  },
                  onLongClick: () => {                                            // KT:818-858
                    menuState.show({ anchored: true }, () => {                    // KT:819
                      switch (item.type) {                                        // KT:820-856
                        case 'SongItem': return C.YouTubeSongMenu({ song: item, navController, onDismiss: menuState.dismiss });  // KT:821-826
                        case 'AlbumItem': return C.YouTubeAlbumMenu({ albumItem: item, navController, onDismiss: menuState.dismiss }); // KT:827-832
                        case 'ArtistItem': return C.YouTubeArtistMenu({ artist: item, onDismiss: menuState.dismiss }); // KT:833-837
                        case 'PlaylistItem': return C.YouTubePlaylistMenu({ playlist: item, coroutineScope: scope, onDismiss: menuState.dismiss }); // KT:838-843
                        case 'PodcastItem': return C.YouTubePlaylistMenu({ playlist: C.asPlaylistItem(item), coroutineScope: scope, onDismiss: menuState.dismiss }); // KT:844-849
                        case 'EpisodeItem': return C.YouTubeSongMenu({ song: C.asSongItem(item), navController, onDismiss: menuState.dismiss }); // KT:850-855
                      }
                    });
                  },
                }),
              }));

            // KT:864-912  ytSongItem(song, itemWidth): YouTubeListItem + menu button + combined clickable
            const ytSongItem = (song, itemWidth) => {
              const showSongMenu = () => menuState.show({ anchored: true }, () =>   // KT:865-873
                C.YouTubeSongMenu({ song, navController, onDismiss: menuState.dismiss }));
              return C.CompositionLocalProvider({ LocalItemHorizontalPadding: false }, () =>
                C.YouTubeListItem({                                               // KT:875
                  item: song,
                  isActive: song.id === (mediaMetadataState.value && mediaMetadataState.value.id), // KT:877
                  isPlaying: isPlayingState.value,                                // KT:878
                  isSwipeable: false,                                             // KT:879
                  showDivider: false,                                             // KT:880
                  trailingContent: () => C.SongMenuIconButton({ onClick: showSongMenu }, () =>  // KT:881-887
                    C.Icon(C.painterResource('more_vert'), C.stringResource('more_options'))),
                  modifier: C.irideFolioCombinedClickable({                       // KT:889-909
                    width: itemWidth,
                    onClick: () => {                                              // KT:892-907
                      if (!isListenTogetherGuest) {
                        if (song.id === (mediaMetadataState.value && mediaMetadataState.value.id)) {
                          if (playerConnection) playerConnection.togglePlayPause();
                        } else if (playerConnection) {
                          playerConnection.playQueue(C.YouTubeQueue(song.endpoint || C.WatchEndpoint({ videoId: song.id }), C.toMediaMetadata(song)));
                        }
                      }
                    },
                    onLongClick: showSongMenu,                                    // KT:908
                  }),
                }));
            };

            // KT:914-917  forYouBoxScale / forYouCellGap / forYouBoxSize / forYouCellSize
            const forYouBoxScale = 1.35;                                          // KT:914
            const forYouCellGap = C.IrideFolio.ShelfMosaicGap;                    // KT:915
            const forYouBoxSize = currentGridHeight * forYouBoxScale;             // KT:916
            const forYouCellSize = (forYouBoxSize - forYouCellGap) / 2;           // KT:917
            // KT:918-933  forYouOnClick(cell)
            const forYouOnClick = (cell) => {
              switch (cell.type) {
                case 'Song':                                                      // KT:920-927
                  if (!isListenTogetherGuest) {
                    if (cell.id === (mediaMetadataState.value && mediaMetadataState.value.id)) {
                      if (playerConnection) playerConnection.togglePlayPause();
                    } else if (playerConnection) {
                      playerConnection.startRadioForSong(C.toMediaMetadata(cell));
                    }
                  }
                  break;
                case 'Album': navController.navigate(`album/${cell.id}`); break;  // KT:929
                case 'Artist': navController.navigate(`artist/${cell.id}`); break; // KT:930
                case 'Playlist': break;                                           // KT:931
              }
            };
            // KT:934-1067  forYouMiniTile(cell): tile quadrata con portrait artista + menu/click
            const forYouMiniTile = (cell) => {
              const tileShape = cell.type === 'Artist' ? C.CircleShape : C.FolioShape(6);   // KT:935
              const collectionTile = cell.type === 'Album'                           // KT:936-940
                ? C.rememberCollectionExpansionTile('Album', cell.id, { artworkUrl: cell.thumbnailUrl, title: cell.title })
                : { first: C.Modifier(), second: C.Modifier() };
              const thumbnail = cell.type === 'Artist'                               // KT:941-944
                ? C.rememberArtistThumbnail(cell.id, cell.title, cell.thumbnailUrl)
                : cell.thumbnailUrl;
              const portraitNavigation = C.LocalArtistPortraitNavigation();          // KT:945
              const portraitKey = C.rememberSaveable('portraitKey', () => C.uuid());  // KT:946
              const portraitBounds = C.useState('portraitBounds', C.Rect.Zero);       // KT:947
              const portrait = cell.type === 'Artist'                                 // KT:948-962
                ? C.rememberIrideArtistArtwork({
                    artistId: cell.id, url: cell.thumbnailUrl, artistName: cell.title,
                    channelId: cell.artist.channelId,
                    externalEnabled: !cell.artist.isLocal && !cell.artist.isPodcastChannel && !cell.artist.isPrivatelyOwnedArtist,
                    highResolution: false,
                  }).value
                : null;
              const portraitScope = C.LocalAnimatedVisibilityScope();                 // KT:963
              const portraitInFlight = cell.type === 'Artist' && C.LocalArtistPortraitTransitionActive() &&   // KT:964-967
                portraitScope != null && !homeReducedMotion &&
                portraitNavigation && portraitNavigation.seed && portraitNavigation.seed.sourceKey === portraitKey;
              const homePortraitProgress = portraitInFlight                           // KT:968-974
                ? C.animateFloat(portraitScope, (state) => (state === 'Visible' ? 1 : 0), 'homePortraitVisibility')
                : 1;
              // KT:975-976  commento: questi tile sono circa meta' della griglia reale, bordi piu' sottili
              return C.Box({                                                          // KT:977-1039
                modifier: C.Modifier.size(forYouCellSize)
                  .then(collectionTile.first).then(collectionTile.second)
                  .clip(tileShape)
                  .irideFolioCombinedClickable({
                    onClick: () => {                                                   // KT:984-1000
                      if (cell.type === 'Artist' && portrait != null) {
                        if (portraitNavigation) portraitNavigation.select(portraitKey, cell.id, cell.title, portrait, thumbnail, portraitBounds.value);
                        navController.navigate(`artist/${cell.id}?portraitSource=${portraitKey}`);
                      } else {
                        forYouOnClick(cell);
                      }
                    },
                    onLongClick: () => menuState.show({ anchored: true }, () => {     // KT:1001-1029
                      switch (cell.type) {
                        case 'Song': return C.SongMenu({ originalSong: cell, navController, onDismiss: menuState.dismiss }); // KT:1004-1009
                        case 'Album': return C.AlbumMenu({ originalAlbum: cell, navController, onDismiss: menuState.dismiss }); // KT:1010-1015
                        case 'Artist': return C.ArtistMenu({ originalArtist: cell, coroutineScope: scope, onDismiss: menuState.dismiss }); // KT:1016-1021
                        case 'Playlist': return C.PlaylistMenu({ playlist: cell, coroutineScope: scope, onDismiss: menuState.dismiss }); // KT:1022-1027
                      }
                    }),
                  })
                  .border({ width: 0.5, color: cell.type === 'Song' ? C.strokeItemSoft() : C.strokeCard(), shape: tileShape }), // KT:1032-1038
                children: () => {                                                     // KT:1040-1066
                  if (cell.type === 'Artist' && portrait != null) {
                    const displayedPortrait = (portraitNavigation && portraitNavigation.seed && portraitNavigation.seed.sourceKey === portraitKey && portraitNavigation.seed.artwork) || portrait; // KT:1041
                    return C.Image({                                                  // KT:1042-1051
                      bitmap: displayedPortrait.bitmap, contentDescription: cell.title, contentScale: 'Crop',
                      modifier: C.Modifier.fillMaxSize().onGloballyPositioned((it) => { portraitBounds.value = it.boundsInWindow(); })
                        .alpha(portraitInFlight && homePortraitProgress < 1 ? 0 : 1).clip(tileShape),
                    });
                  }
                  return C.AsyncImage({                                               // KT:1053-1064
                    model: C.ImageRequest({ data: C.resize(thumbnail, 300, 300), memoryCachePolicy: 'ENABLED', diskCachePolicy: 'ENABLED', networkCachePolicy: 'ENABLED' }),
                    contentDescription: cell.title, contentScale: 'Crop',
                    modifier: C.Modifier.fillMaxSize().clip(tileShape),
                  });
                },
              });
            };
            // KT:1068-1111  forYouRecommendedTile(tile)
            const forYouRecommendedTile = (tile) => {
              if (tile.type === 'Local') return forYouMiniTile(tile.item);            // KT:1070
              const album = tile.item;                                                // KT:1072
              const collectionTile = C.rememberCollectionExpansionTile('Album', album.id, { artworkUrl: album.thumbnail, title: album.title }); // KT:1073-1075
              return C.AsyncImage({                                                   // KT:1076-1108
                model: C.ImageRequest({ data: C.resize(album.thumbnail, 300, 300), memoryCachePolicy: 'ENABLED', diskCachePolicy: 'ENABLED', networkCachePolicy: 'ENABLED' }),
                contentDescription: album.title, contentScale: 'Crop',
                modifier: C.Modifier.size(forYouCellSize).then(collectionTile.first).then(collectionTile.second)
                  .irideArtworkOverlayBorder(C.IrideSoftBorderWidth, C.strokeCard(), C.FolioShape(6))
                  .clip(C.FolioShape(6))
                  .irideFolioCombinedClickable({
                    onClick: () => navController.navigate(`album/${album.id}`),        // KT:1097
                    onLongClick: () => menuState.show({ anchored: true }, () =>        // KT:1098-1106
                      C.YouTubeAlbumMenu({ albumItem: album, navController, onDismiss: menuState.dismiss })),
                  }),
              });
            };
            // KT:1112-1149  forYouBlock(shelf): 2x2 mosaico (artista + 3 tile casuali) + nome artista
            const forYouBlock = (shelf) => {
              const cells = C.remember([shelf], () =>                                 // KT:1113-1119
                [shelf.artist].concat(C.shuffled(shelf.tiles, C.Random(C.hashCode(shelf.artist.id))).slice(0, 3)));
              return C.Column({ modifier: C.Modifier.width(forYouBoxSize) }, () => [  // KT:1120
                C.Column({ verticalArrangement: C.spacedBy(forYouCellGap) }, () =>   // KT:1121-1134
                  C.chunked(cells, 2).map((rowCells) =>                              // KT:1122
                    C.Row({ horizontalArrangement: C.spacedBy(forYouCellGap) }, () =>
                      rowCells.map((cell) => (cell.type === 'Artist' ? forYouMiniTile(cell) : forYouRecommendedTile(cell)))))), // KT:1126-1131
                C.Spacer({ modifier: C.Modifier.height(6) }),                        // KT:1135
                C.Text({                                                              // KT:1136-1147
                  text: shelf.artist.artist.name,
                  style: { ...C.typography('bodyLarge'), fontFamily: C.folioTypewriterFontFamily() },
                  fontWeight: 'Bold', maxLines: 1, overflow: 'Ellipsis', textAlign: 'Center',
                  modifier: C.Modifier.fillMaxWidth(),
                }),
              ]);
            };
            // KT:1150  forYouLoading = isForYouLoading && forYouShelves.isEmpty()
            const forYouLoading = isForYouLoading && forYouShelves.length === 0;
            // KT:1151-1231  forYouSection(shelfOrder): IrideFolioSection + LazyRow di forYouBlock
            const forYouSection = (shelfOrder) => {
              if (!(forYouLoading || forYouShelves.length > 0)) return null;        // KT:1152
              shelfOrder.added({ hasFourRows: false });                              // KT:1153
              return C.item({ key: 'for_you_section' }, () =>                       // KT:1154
                C.IrideFolioSection({                                                // KT:1155-1181
                  modifier: C.Modifier.animateItem({
                    fadeInSpec: null,
                    placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                    fadeOutSpec: null,
                  }),
                  sectionKey: 'for_you_section',
                  ready: !forYouLoading,
                  artwork: forYouShelves.slice(0, 2).flatMap((shelf) =>               // KT:1167-1177
                    [C.resize(shelf.artist.thumbnailUrl, 300, 300)].concat(
                      shelf.tiles.map((tile) => (tile.type === 'Local' ? C.resize(tile.item.thumbnailUrl, 300, 300) : C.resize(tile.item.thumbnail, 300, 300))))),
                  placeholder: () => C.IrideFolioSectionSkeleton(forYouBoxSize, { mosaic: true }), // KT:1178-1180
                }, () => {
                  C.IrideFolioShelfHeader({ title: C.stringResource('for_you_artists_title'), showNavigationArrow: false }); // KT:1182-1185
                  const shelves = forYouShelves;                                      // KT:1187
                  const forYouState = C.rememberLazyListState();                      // KT:1188
                  C.LaunchedEffect([forYouState, shelves.length, isForYouLoading], () => { // KT:1189-1205
                    C.snapshotFlow(() => C.lastVisibleIndex(forYouState)).collect((lastVisible) => {
                      if (lastVisible != null && lastVisible >= shelves.length - 3 && viewModel.forYouShelves.value.length <= shelves.length) {
                        viewModel.loadMoreForYouShelves();                            // KT:1202
                      }
                    });
                  });
                  return C.LazyRow({                                                  // KT:1206-1227
                    state: forYouState,
                    contentPadding: { horizontal: irideGridItemStart },              // KT:1208-1209
                    horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing), // KT:1210-1211
                    overscrollEffect: null,                                           // KT:1212
                    modifier: C.rubberBandOverscroll('Horizontal', forYouState),     // KT:1213-1217
                    itemsIndexed: shelves.map((shelf, index) => ({ key: `for_you_${index}_${shelf.artist.id}`, content: () => forYouBlock(shelf) })), // KT:1219-1226
                  });
                }));
            };

            // TODO KT:1233-2829  IridePageLayout(title = homePageTitle(), ...) e tutto il resto della pagina -- PARTE 3

            return null;                                                          // placeholder fino alla parte 3
          }))));
  }

  global.IrideHomeScreen = { HomeScreen, HomeScreenContent, SHOW_HOME_TEST_PLAYLIST };
})(typeof window !== 'undefined' ? window : globalThis);
