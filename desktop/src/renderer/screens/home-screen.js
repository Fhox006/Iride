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

    // TODO KT:668-716  orderedFeedSections (ordinamento trending/top 100/nuove uscite) -- DA FARE
    // TODO KT:717-723  pickedSongs -- DA FARE
    // TODO KT:725-742  homeColorScheme -- DA FARE
    // TODO KT:743-820+ MaterialTheme / Scaffold / BoxWithConstraints / ytGridItem -- DA FARE (prossima parte)

    return null; // placeholder finche' la parte 2 non e' tradotta
  }

  global.IrideHomeScreen = { HomeScreen, HomeScreenContent, SHOW_HOME_TEST_PLAYLIST };
})(typeof window !== 'undefined' ? window : globalThis);
