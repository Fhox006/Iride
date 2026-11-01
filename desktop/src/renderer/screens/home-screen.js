/*
 * Traduzione riga per riga di HomeScreen (parte 1 di N)
 * Sorgente: app/src/main/kotlin/com/metrolist/music/ui/screens/HomeScreen.kt
 *           (tag v0.1.0-alpha33, righe 219-2829 tradotte; dipendenze in home-components.js)
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


  // KT:219-229  IrideMoodChipsRow (chip per categorie umore, indicatore che scorre)
  // Traduzione 1:1 della struttura; le misure (labelBoundsPx, animateFloatAsState) sono STUB in home-components.js.
  function IrideMoodChipsRow(props) {                                             // KT:231-237
    const chips = props.chips;
    const currentValue = props.currentValue;
    const onValueUpdate = props.onValueUpdate;
    const horizontalPadding = props.horizontalPadding != null ? props.horizontalPadding : 20; // KT:236
    const labelBoundsPx = C.mutableStateMapOf();                                  // KT:239-241
    const selectedIndex = chips.findIndex((p) => p[0] === currentValue);          // KT:242
    const targetBounds = selectedIndex >= 0 ? labelBoundsPx[selectedIndex] : null; // KT:243
    const reducedMotion = C.rememberReducedMotion();                              // KT:244
    const chipScrollState = C.rememberScrollState();                              // KT:245
    const indicatorAnimSpec = C.tween({ durationMillis: reducedMotion ? 0 : C.IrideMotion.Short, easing: C.IrideMotion.EaseOutQuart }); // KT:246-250
    const indicatorX = C.animateFloatAsState(targetBounds ? targetBounds[0] : 0, indicatorAnimSpec, 'moodChipIndicatorX'); // KT:251-256
    const indicatorWidth = C.animateFloatAsState(targetBounds ? targetBounds[1] : 0, indicatorAnimSpec, 'moodChipIndicatorWidth'); // KT:257-262
    return C.Column({                                                             // KT:264-313
      modifier: C.Modifier.fillMaxWidth().heightIn(40).rubberBandOverscroll('Horizontal', chipScrollState)
        .horizontalScroll(chipScrollState, { overscrollEffect: null }),           // KT:265-270
    }, () => [
      C.Row({ verticalAlignment: 'CenterVertically' }, () => [                    // KT:272
        C.Spacer({ modifier: C.Modifier.width(horizontalPadding) }),              // KT:273
        ...chips.map(([value, label], index) => {                                 // KT:274-298
          const isSelected = currentValue === value;                              // KT:275
          return [
            C.Text({                                                              // KT:276-296
              text: label,
              style: C.MaterialTheme.typography('labelLarge').copy({ fontSize: 14 }), // KT:278
              color: isSelected ? C.MaterialTheme.colorScheme().textPrimary : C.MaterialTheme.colorScheme().textTertiary, // KT:279-281
              fontWeight: isSelected ? 'SemiBold' : 'Normal',                     // KT:282
              maxLines: 1,                                                        // KT:283
              modifier: C.Modifier.clickable({ interactionSource: C.remember([], () => C.MutableInteractionSource()), indication: null }, () => onValueUpdate(value)) // KT:285-290
                .padding({ vertical: 6 })                                         // KT:291
                .onGloballyPositioned((coords) => { labelBoundsPx[index] = [coords.positionInParent().x, coords.size.width]; }), // KT:292-295
            }),
            index !== chips.length - 1 ? C.Spacer({ modifier: C.Modifier.width(20) }) : null, // KT:297
          ];
        }).flat(),
        C.Spacer({ modifier: C.Modifier.width(horizontalPadding) }),              // KT:299
      ]),
      C.Spacer({ modifier: C.Modifier.height(3) }),                               // KT:301
      targetBounds != null                                                        // KT:302-312
        ? C.Box({ modifier: C.Modifier.offset(() => ({ x: Math.round(indicatorX.value), y: 0 })).width(indicatorWidth.value).height(2).background(C.MaterialTheme.colorScheme().textPrimary) }) // KT:303-309
        : C.Spacer({ modifier: C.Modifier.height(2) }),                           // KT:311
    ]);
  }

  // KT:316-319  ForYouShelfSkeleton(boxSize, contentPadding)
  function ForYouShelfSkeleton(boxSize, contentPadding, modifier) {
    return C.IrideFolioShelfSkeleton(boxSize, modifier, contentPadding, { tilesPerSide: 2 }); // KT:318
  }

  // KT:322-357  HomeSimilarityTitle(name, thumbnailUrl, isArtist, artistId, modifier, onClick)
  function HomeSimilarityTitle(props) {
    const name = props.name;
    const thumbnailUrl = props.thumbnailUrl;
    const isArtist = props.isArtist;
    const artistId = props.artistId != null ? props.artistId : null;
    const onClick = props.onClick || null;
    const shape = isArtist ? C.CircleShape : C.FolioShape(5);                    // KT:330
    const resolvedThumbnail = (isArtist && artistId != null)                     // KT:331-334
      ? C.rememberArtistThumbnail(artistId, name, thumbnailUrl) : thumbnailUrl;
    const portraitTile = (isArtist && artistId != null && onClick != null)       // KT:335-337
      ? C.rememberArtistPortraitTile(artistId, name, resolvedThumbnail, null, true)
      : { first: C.Modifier(), second: C.Modifier() };
    return C.IrideFolioShelfHeader({                                              // KT:338-356
      label: C.stringResource('home_similar_to'),                                 // KT:339
      title: C.compactHomeTitle(name),                                            // KT:340
      thumbnail: () => C.ItemThumbnail({                                          // KT:341-351
        thumbnailUrl: resolvedThumbnail, isActive: false, isPlaying: false, shape,
        artistArtwork: isArtist, requestResolution: 192, hairlineBorder: true,
        modifier: C.Modifier.size(48).then(portraitTile.second),
      }),
      onClick,                                                                    // KT:353
      modifier: portraitTile.first,                                               // KT:354
      showNavigationArrow: false,                                                 // KT:355
    });
  }

  // KT:2638-2644  HomeFeedLoadingShelf(thumbnailSize)
  function HomeFeedLoadingShelf(thumbnailSize) {
    return C.IrideFolioSection({
      sectionKey: 'home_feed_loading', ready: false,
      placeholder: () => C.IrideFolioSectionSkeleton(thumbnailSize, { songs: true }), // KT:2643
    }, () => null);
  }

  // KT:2647-2711  IrideFeatureCard(title, subtitle, iconRes, onClick, modifier)
  function IrideFeatureCard(props) {
    return C.Surface({                                                            // KT:2655-2660
      onClick: props.onClick,                                                     // KT:2656
      modifier: C.Modifier.fillMaxWidth().then(props.modifier || C.Modifier()),   // KT:2657
      shape: C.FolioShape(6),                                                     // KT:2658
      color: 'transparent',                                                       // KT:2659
      border: { width: 1, color: C.MaterialTheme.colorScheme().strokeCard },      // KT:2660
    }, () => C.Row({                                                              // KT:2662-2709
      modifier: C.Modifier.fillMaxWidth().height(112).padding({ horizontal: 18, vertical: 16 }), // KT:2663-2666
      verticalAlignment: 'CenterVertically',
      horizontalArrangement: C.spacedBy(16),                                      // KT:2668
    }, () => [
      C.Box({                                                                     // KT:2670-2679
        modifier: C.Modifier.size(44).border(1, C.MaterialTheme.colorScheme().strokeCard, C.FolioShape(5)),
        contentAlignment: 'Center',
      }, () => C.Icon({ painter: C.painterResource(props.iconRes), contentDescription: null, tint: C.MaterialTheme.colorScheme().textPrimary, modifier: C.Modifier.size(22) })), // KT:2680-2685
      C.Column({ modifier: C.Modifier.weight(1), verticalArrangement: C.spacedBy(4) }, () => [ // KT:2687-2690
        C.Text({ text: props.title, style: C.MaterialTheme.typography('titleMedium'), fontWeight: 'SemiBold', color: C.MaterialTheme.colorScheme().textPrimary }), // KT:2691-2696
        C.Text({ text: props.subtitle, style: C.MaterialTheme.typography('bodySmall'), color: C.MaterialTheme.colorScheme().textSecondary }), // KT:2697-2701
      ]),
      C.Icon({ painter: C.painterResource('arrow_forward'), contentDescription: null, tint: C.MaterialTheme.colorScheme().textSecondary, modifier: C.Modifier.size(20) }), // KT:2703-2708
    ]));
  }

  // KT:2713-2829  SyncBanner(syncState, useIrideStyle)
  function SyncBanner(syncState, useIrideStyle) {
    const ops = [syncState.likedSongs, syncState.librarySongs, syncState.uploadedSongs, syncState.likedAlbums, syncState.uploadedAlbums, syncState.artists, syncState.playlists]; // KT:2715-2724
    const completedCount = ops.filter((it) => it === 'Completed').length;         // KT:2725
    const progress = completedCount / ops.length;                                 // KT:2726
    if (useIrideStyle) {                                                          // KT:2728
      return C.Column({ modifier: C.Modifier.fillMaxWidth().padding({ horizontal: 20, vertical: 10 }) }, () => [ // KT:2729
        C.Row({ verticalAlignment: 'CenterVertically', horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing) }, () => [ // KT:2730-2733
          C.CircularProgressIndicator({ modifier: C.Modifier.size(16), strokeWidth: 2, color: C.MaterialTheme.colorScheme().textSecondary }), // KT:2734-2737
          C.Column({ modifier: C.Modifier.weight(1) }, () => [                    // KT:2739
            C.Text({ text: C.stringResource('syncing_library').toUpperCase(), style: C.MaterialTheme.typography('labelLarge').copy({ fontFamily: C.folioTypewriterFontFamily(), fontSize: 13, letterSpacing: -0.1 }), fontWeight: 'Bold', color: C.MaterialTheme.colorScheme().textSecondary }), // KT:2740-2749
            C.Text({ text: C.stringResource('network_sync_warning'), style: C.MaterialTheme.typography('bodySmall').copy({ fontFamily: C.folioTypewriterFontFamily() }), color: C.MaterialTheme.colorScheme().textTertiary }), // KT:2751-2758
          ]),
        ]),
        C.Spacer({ modifier: C.Modifier.height(10) }),                            // KT:2761
        C.LinearProgressIndicator({ progress: () => progress, modifier: C.Modifier.fillMaxWidth().height(2).clip(C.CircleShape), color: C.MaterialTheme.colorScheme().textSecondary, trackColor: C.MaterialTheme.colorScheme().fillSelected }), // KT:2762-2767
        syncState.currentOperation && syncState.currentOperation.length > 0 ? [   // KT:2768-2778
          C.Spacer({ modifier: C.Modifier.height(4) }),
          C.Text({ text: syncState.currentOperation, style: C.MaterialTheme.typography('labelSmall').copy({ fontFamily: C.folioTypewriterFontFamily() }), color: C.MaterialTheme.colorScheme().textTertiary }),
        ] : null,
      ]);                                                                         // KT:2780 return
    }
    return C.Card({                                                               // KT:2783-2787
      modifier: C.Modifier.fillMaxWidth().padding({ horizontal: 16, vertical: 8 }),
      shape: C.FolioShape(20),
      colors: C.CardDefaults.cardColors({ containerColor: C.MaterialTheme.colorScheme().secondaryContainer }),
    }, () => C.Column({ modifier: C.Modifier.padding({ horizontal: 16, vertical: 12 }) }, () => [ // KT:2789
      C.Row({ verticalAlignment: 'CenterVertically', horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing) }, () => [ // KT:2790-2793
        C.CircularProgressIndicator({ modifier: C.Modifier.size(18), strokeWidth: 2, color: C.MaterialTheme.colorScheme().onSecondaryContainer }), // KT:2794-2798
        C.Column({ modifier: C.Modifier.weight(1) }, () => [                      // KT:2799
          C.Text({ text: C.stringResource('syncing_your_library'), style: C.MaterialTheme.typography('titleSmall'), color: C.MaterialTheme.colorScheme().onSecondaryContainer }), // KT:2800-2804
          C.Text({ text: C.stringResource('network_sync_warning'), style: C.MaterialTheme.typography('bodySmall'), color: C.MaterialTheme.colorScheme().onSecondaryContainer.copy({ alpha: 0.7 }) }), // KT:2805-2809
        ]),
      ]),
      C.Spacer({ modifier: C.Modifier.height(10) }),                              // KT:2812
      C.LinearProgressIndicator({ progress: () => progress, modifier: C.Modifier.fillMaxWidth().height(4).clip(C.CircleShape), color: C.MaterialTheme.colorScheme().onSecondaryContainer, trackColor: C.MaterialTheme.colorScheme().onSecondaryContainer.copy({ alpha: 0.2 }) }), // KT:2813-2818
      syncState.currentOperation && syncState.currentOperation.length > 0 ? [     // KT:2819-2827
        C.Spacer({ modifier: C.Modifier.height(4) }),
        C.Text({ text: syncState.currentOperation, style: C.MaterialTheme.typography('labelSmall'), color: C.MaterialTheme.colorScheme().onSecondaryContainer.copy({ alpha: 0.55 }) }),
      ] : null,
    ]));
  }

  // KT:1895-2038  testPlaylist (SHOW_HOME_TEST_PLAYLIST): item("home_test_playlist_section") con mosaico 2x2 e etichetta.
  // Blocco di test disattivato; la struttura e' tradotta, i dettagli grafici sono STUB.
  function homeTestPlaylistSection(playlist, ctx) {
    return C.item({ key: 'home_test_playlist_section' }, () =>
      C.IrideFolioSection({
        modifier: C.Modifier.animateItem({ fadeInSpec: null, placementSpec: ctx.homeReducedMotion ? null : C.IrideMotion.PlacementSpec, fadeOutSpec: null }),
        sectionKey: 'home_test_playlist_section', ready: true,
      }, () => [
        C.IrideFolioShelfHeader({ title: C.stringResource('home_test_section'), showNavigationArrow: false }),
        C.LazyRow({
          contentPadding: { horizontal: ctx.irideGridItemStart },
          horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing),
          overscrollEffect: null, modifier: C.Modifier.fillMaxWidth(),
          items: [C.item({ key: `home_test_playlist_${playlist.id}` }, () => {
            const artwork = ctx.testPlaylistTrackThumbnails;                      // KT:1918
            const collectionTile = C.rememberCollectionExpansionTile('Playlist', playlist.id, { artworkUrl: artwork[0] || playlist.thumbnail, title: playlist.title }); // KT:1919-1922
            return C.Column({ modifier: C.Modifier.width(ctx.currentGridHeight).then(collectionTile.first).irideFolioCombinedClickable({ onClick: () => ctx.navController.navigate(`online_playlist/${playlist.id}`) }) }, () => [ // KT:1923-1937
              C.Box({ modifier: C.Modifier.size(ctx.currentGridHeight).then(collectionTile.second).clip(C.FolioShape(8)), children: () => [ // KT:1932-1975
                artwork.length === 0
                  ? C.AsyncImage({ model: playlist.thumbnail, contentDescription: null, contentScale: 'Crop', modifier: C.Modifier.fillMaxSize() }) // KT:1938-1943
                  : C.Column({ modifier: C.Modifier.fillMaxSize() }, () => [      // KT:1945-1974 mosaico 2x2
                      C.Row({ modifier: C.Modifier.weight(1).fillMaxWidth() }, () => [0, 1].map((i) => C.AsyncImage({ model: artwork.length > i ? artwork[i] : artwork[0], contentDescription: null, contentScale: 'Crop', modifier: C.Modifier.weight(1).fillMaxHeight() }))),
                      C.Row({ modifier: C.Modifier.weight(1).fillMaxWidth() }, () => [2, 3].map((i) => C.AsyncImage({ model: artwork.length > i ? artwork[i] : artwork[0], contentDescription: null, contentScale: 'Crop', modifier: C.Modifier.weight(1).fillMaxHeight() }))),
                    ]),
                C.Box({ modifier: C.Modifier.fillMaxSize().background(C.Color.Black.copy({ alpha: 0.34 })), contentAlignment: 'Center', children: () => [ // KT:1976-2025
                  C.Column({ horizontalAlignment: 'CenterHorizontally' }, () => [
                    C.Text({ text: C.stringResource('home_test_mood_label'), color: 'White', fontFamily: C.SatoshiFontFamily, fontWeight: 'Black', fontSize: 24 * C.IrideFolio.SatoshiTextHeightScale, letterSpacing: -0.6, textAlign: 'Center', maxLines: 1 }), // KT:1981-1991
                    C.Text({ text: C.stringResource('home_test_mood_type'), color: 'White', fontFamily: C.SatoshiFontFamily, fontWeight: 'SemiBold', fontSize: 10 * C.IrideFolio.SatoshiTextHeightScale, letterSpacing: 3, textAlign: 'Center' }), // KT:1992-2000
                  ]),
                  C.Row({ modifier: C.Modifier.align('TopEnd').padding({ top: 4, end: 5 }), verticalAlignment: 'CenterVertically', horizontalArrangement: C.spacedBy(4) }, () => [ // KT:2003-2008
                    C.Image({ painter: C.painterResource('ic_logo'), contentDescription: null, colorFilter: C.ColorFilter.tint('White'), modifier: C.Modifier.size(14) }), // KT:2009-2014
                    C.Text({ text: C.stringResource('home_test_brand_label'), color: 'White', fontFamily: C.SatoshiFontFamily, fontWeight: 'SemiBold', fontSize: 10 * C.IrideFolio.SatoshiTextHeightScale, letterSpacing: 0.2, maxLines: 1 }), // KT:2015-2023
                  ]),
                ] }),
              ] }),
              C.Text({ text: playlist.title, style: C.MaterialTheme.typography('bodyMedium'), maxLines: 2, overflow: 'Ellipsis', modifier: C.Modifier.padding({ top: 8 }) }), // KT:2026-2032
            ]);
          })],
        }),
      ]));
  }

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
        while (failed && viewModel.isMoodError.value) {
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

            // KT:1233-1268  IridePageLayout(title, state, reducedMotion, bottomPadding, listModifier, overlay) { ... }
            return C.IridePageLayout({
              title: C.homePageTitle(),                                             // KT:1234
              state: lazyListState,                                               // KT:1235
              reducedMotion: homeReducedMotion,                                   // KT:1236
              bottomPadding: C.LocalPlayerAwareWindowInsets().asPaddingValues().calculateBottomPadding(), // KT:1237-1240
              listModifier: C.irideDepthEnter(() => screenProgress, { intensity: 0.4 }), // KT:1241-1242
              overlay: () => {                                                    // KT:1243-1267
                if (isLoggedIn && syncState.overallStatus === 'Syncing' && syncBannerLaunchCount < 3) { // KT:1244-1248
                  return C.Text({                                                  // KT:1249-1265
                    text: C.stringResource('syncing_library'),                     // KT:1250
                    style: C.MaterialTheme.typography('labelSmall'),               // KT:1251
                    color: C.MaterialTheme.colorScheme().textSecondary,            // KT:1252
                    modifier: C.Modifier.align('TopEnd')                           // KT:1254
                      .windowInsetsPadding(C.WindowInsets.statusBars)              // KT:1255
                      .padding({ horizontal: C.IrideFolio.HorizontalPadding, vertical: 8 }) // KT:1256-1259
                      .background(C.MaterialTheme.colorScheme().background, C.FolioShape(4)) // KT:1260-1263
                      .padding(4),                                                 // KT:1264
                  });
                }
                return null;
              },
            }, () => {                                                            // KT:1268 body
              // KT:1269  val shelfOrder = HomeShelfOrder()
              const shelfOrder = C.HomeShelfOrder();
              const body = [];

              // KT:1270-1321  hero carousel
              if (isHeroCarouselEnabled) {
                body.push(C.item({ key: 'hero_carousel' }, () => {                // KT:1271
                  const radioItems = heroCarouselItems.filter((it) => it.type === 'ArtistRadio'); // KT:1272-1275
                  if (radioItems.length > 0 || isHeroCarouselLoading) {           // KT:1276
                    return C.IrideFolioSection({                                   // KT:1277-1294
                      modifier: C.Modifier.animateItem({
                        fadeInSpec: null,
                        placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                        fadeOutSpec: null,
                      }),
                      sectionKey: 'hero_carousel',                                  // KT:1286
                      ready: radioItems.length > 0,                                 // KT:1287
                      artwork: radioItems.slice(0, 1).map((it) => it.coverUrl),     // KT:1288
                      placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight, { hero: true }), // KT:1289-1294
                    }, () => C.HeroCarouselSection({                              // KT:1296-1317
                      items: radioItems,                                          // KT:1297
                      onCoverError: viewModel.repairHeroRadioCover,               // KT:1298
                      onArtistClick: (artistId) => navController.navigate(`artist/${artistId}`), // KT:1299-1301
                      onArtistLongClick: (artist) => menuState.show({ anchored: true }, () => // KT:1302-1315
                        C.YouTubeArtistMenu({                                     // KT:1304-1314
                          artist: C.ArtistItem({ id: artist.artistId, title: artist.artistName, thumbnail: artist.coverUrl, shuffleEndpoint: null, radioEndpoint: null }),
                          onDismiss: menuState.dismiss,
                        })),
                    }));
                  }
                  return null;                                                    // KT:1319-1320
                }));
              }

              // KT:1323-1443  picked for you (quick picks)
              if (quickPicks == null || pickedSongs.length > 0) {                 // KT:1323
                shelfOrder.added({ hasFourRows: true });                          // KT:1324
                body.push(C.item({ key: 'picked_for_you_section' }, () =>         // KT:1325
                  C.IrideFolioSection({                                           // KT:1326-1343
                    modifier: C.Modifier.animateItem({
                      fadeInSpec: null,
                      placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                      fadeOutSpec: null,
                    }),
                    sectionKey: 'picked_for_you_section',                         // KT:1335
                    ready: quickPicks != null,                                    // KT:1336
                    artwork: pickedSongs.slice(0, 4).map((it) => C.resize(it.song.thumbnailUrl, 448, 448)), // KT:1337-1340
                    placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight, { songs: true }), // KT:1341-1343
                  }, () => {                                                      // KT:1344
                    C.SharedHomeSectionTitle({ title: C.stringResource('quick_picks_section_title') }); // KT:1345-1347
                    return C.SharedQuickPicksCarousel({                           // KT:1349-1357
                      items: pickedSongs,                                         // KT:1350
                      key: (it) => `picked_${it.id}`,                             // KT:1351
                      loading: quickPicks == null,                                // KT:1352
                      contentPadding: irideListItemStart - 4,                     // KT:1353
                      usePager: true,                                             // KT:1354
                      minimumRows: 1,                                             // KT:1355
                      modifier: C.Modifier,                                       // KT:1356
                      content: (originalSong, itemWidth) => {                     // KT:1357-1439
                        const song = C.collectAsStateWithLifecycle(database.song(originalSong.id), originalSong.id, originalSong); // KT:1358-1363
                        const currentSong = song || originalSong;                 // KT:1364
                        return C.CompositionLocalProvider({ LocalItemHorizontalPadding: false }, () =>  // KT:1365-1367
                          C.SongListItem({                                        // KT:1368
                            song: currentSong,                                    // KT:1369
                            isActive: currentSong.id === (mediaMetadataState.value && mediaMetadataState.value.id), // KT:1370-1371
                            isPlaying: isPlayingState.value,                      // KT:1372
                            isSwipeable: false,                                   // KT:1373
                            showDivider: false,                                   // KT:1374
                            trailingContent: () => C.SongMenuIconButton({         // KT:1375-1397
                              onClick: () => menuState.show({ anchored: true }, () => // KT:1377-1384
                                C.SongMenu({ originalSong: currentSong, navController, onDismiss: menuState.dismiss })),
                            }, () => C.Icon({ painter: C.painterResource('more_vert'), contentDescription: C.stringResource('more_options') })),
                            modifier: C.Modifier.width(itemWidth).irideFolioCombinedClickable({ // KT:1399-1437
                              onClick: () => {                                    // KT:1402-1425
                                if (!isListenTogetherGuest) {
                                  if (currentSong.id === (mediaMetadataState.value && mediaMetadataState.value.id)) {
                                    if (playerConnection) playerConnection.togglePlayPause(); // KT:1409-1410
                                  } else if (playerConnection) {
                                    playerConnection.playQueue(C.YouTubeQueue(C.WatchEndpoint({ videoId: currentSong.id }), C.toMediaMetadata(currentSong))); // KT:1412-1422
                                  }
                                }
                              },
                              onLongClick: () => menuState.show({ anchored: true }, () => // KT:1426-1436
                                C.SongMenu({ originalSong: currentSong, navController, onDismiss: menuState.dismiss })),
                            }),
                          }));
                      },
                    });
                  })));
              }

              // KT:1445-1447  knownArtistIds / knownArtistNames
              const knownArtistIds = new Set(forYouShelves.map((s) => s.artist.id)); // KT:1445
              const knownArtistNames = new Set(forYouShelves.map((s) => s.artist.artist.name.toLowerCase())); // KT:1446-1447

              // KT:1448-1693  addFeedSection(indexed): filtri trending / nuove uscite / simili / video
              const addFeedSection = (indexed) => {                               // KT:1452
                const index = indexed.index;                                      // KT:1453
                const section = indexed.value;                                    // KT:1454
                const title = section.title.toLowerCase();                        // KT:1455
                const label = (section.label || '').toLowerCase();                // KT:1456
                const isSimilar =                                                 // KT:1457-1461
                  title.includes('similar to') || title.includes('simile a') ||
                  label.includes('similar to') || label.includes('simile a');
                const isTrending = title.includes('trending') || title.includes('top 100'); // KT:1462-1463
                const isNewRelease = title.includes('new release') || title.includes('nuove uscite'); // KT:1464-1465
                const isVideoSection = title.includes('video') || label.includes('video') || // KT:1466-1470
                  (section.items.length > 0 && section.items.every((it) => it.type === 'SongItem' && it.isVideoSong));
                if (!isTrending && !isNewRelease && !isSimilar && !isVideoSection) return null; // KT:1471-1472
                if (                                                              // KT:1473-1484
                  title.includes('listen again') || label.includes('listen again') ||
                  title.includes('long listens') || title === accountName.toLowerCase() ||
                  title.includes('picked for you') || title.includes('picked 4u') ||
                  title.includes('quick picks') || label.includes('picked for you') ||
                  (label.trim() === '' && knownArtistNames.has(title))
                ) return null;                                                    // KT:1484

                const sectionItems = C.distinctById(section.items).filter((item) => { // KT:1486-1505
                  const isMusicItem = item.type === 'SongItem' || item.type === 'AlbumItem' || item.type === 'ArtistItem';
                  return isMusicItem && (isVideoSection || !isNewRelease ||
                    (item.type === 'AlbumItem' && !['single', 'singolo'].includes(String(item.albumType || '').toLowerCase()) &&
                      (item.artists || []).some((artist) => artist.id != null && knownArtistIds.has(artist.id))));
                });
                if (label.trim() === '' && sectionItems.some((item) => {        // KT:1506-1527
                  if (item.type === 'SongItem') return item.artists.some((a) => a.name.toLowerCase() === section.title.toLowerCase());
                  if (item.type === 'AlbumItem') return (item.artists || []).some((a) => a.name.toLowerCase() === section.title.toLowerCase());
                  return false;
                })) return null;                                                  // KT:1528

                const rawName = isSimilar                                         // KT:1529-1537
                  ? section.title.substring(section.title.indexOf(':') + 1 > 0 ? section.title.indexOf(':') + 1 : 0).replace(/^(similar to|simile a)\s*/i, '').trim()
                  : section.title;
                const name = rawName.replace(/^(artist|artista|album|track|brano):\s*/i, '').trim(); // KT:1538-1544
                const displayItems = isSimilar                                    // KT:1545-1563
                  ? sectionItems.filter((item) => {
                      if (item.type === 'SongItem') return !item.artists.some((a) => a.name.toLowerCase() === name.toLowerCase());
                      if (item.type === 'AlbumItem') return !(item.title.toLowerCase() === name.toLowerCase() || (item.artists || []).some((a) => a.name.toLowerCase() === name.toLowerCase()));
                      if (item.type === 'ArtistItem') return !(item.title.toLowerCase() === name.toLowerCase());
                      return true;
                    })
                  : sectionItems;
                if (displayItems.length === 0 || (!isVideoSection && displayItems.length < 4)) return null; // KT:1564-1568
                const matchedArtist = sectionItems.find((it) => it.type === 'ArtistItem' && it.title.toLowerCase() === name.toLowerCase()); // KT:1569-1572
                const matchedAlbum = sectionItems.find((it) => it.type === 'AlbumItem' && it.title.toLowerCase() === name.toLowerCase()); // KT:1573-1576
                const isAlbumSource = title.includes('album:') || label.includes('album') || matchedAlbum != null; // KT:1577-1580
                const sourceThumbnail = section.thumbnail || (matchedArtist && matchedArtist.thumbnail) || (matchedAlbum && matchedAlbum.thumbnail); // KT:1581-1584
                const sourceArtistId = (matchedArtist && matchedArtist.id)        // KT:1585-1601
                  || (section.endpoint && section.endpoint.browseId && section.endpoint.browseId.startsWith('UC') ? section.endpoint.browseId : null)
                  || (() => {
                    const found = sectionItems.flatMap((item) => {
                      if (item.type === 'SongItem') return item.artists;
                      if (item.type === 'AlbumItem') return item.artists || [];
                      return [];
                    }).find((a) => a.name.toLowerCase() === name.toLowerCase());
                    return found ? found.id : null;
                  })();
                if (isSimilar && (name.trim() === '' || ((sourceThumbnail == null || String(sourceThumbnail).trim() === '') && (isAlbumSource || sourceArtistId == null)))) return null; // KT:1602-1608
                shelfOrder.added({ hasFourRows: false });                          // KT:1609
                const onSectionClick = section.endpoint ? () => {                 // KT:1610-1627
                  if (section.endpoint.browseId === 'FEmusic_moods_and_genres') navController.navigate('mood_and_genres'); // KT:1614-1615
                  else if (section.endpoint.params != null) navController.navigate(`youtube_browse/${section.endpoint.browseId}?params=${section.endpoint.params}`); // KT:1616-1619
                  else navController.navigate(`browse/${section.endpoint.browseId}`); // KT:1620-1623
                } : null;
                return C.item({ key: `home_section_${index}` }, () => C.IrideFolioSection({ // KT:1628-1643
                  modifier: C.Modifier.animateItem({
                    fadeInSpec: null,
                    placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                    fadeOutSpec: null,
                  }),
                  sectionKey: `home_section_${index}`,                             // KT:1638
                  artwork: displayItems.slice(0, 4).map((it) => C.resize(it.thumbnail, 448, 448)), // KT:1639-1642
                  placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight, { // KT:1643-1651
                    songs: displayItems.some((it) => it.type === 'SongItem' && !it.isVideoSong),
                  }),
                }, () => {                                                        // KT:1652
                  if (isSimilar) {                                                // KT:1653-1660
                    HomeSimilarityTitle({ name, thumbnailUrl: sourceThumbnail, artistId: sourceArtistId, isArtist: !isAlbumSource, onClick: onSectionClick });
                  } else {                                                        // KT:1661-1673
                    C.IrideFolioShelfHeader({
                      title: isVideoSection ? section.title : C.stringResource(isTrending ? 'home_trending_title' : 'home_new_albums_title'),
                      onClick: onSectionClick,
                      showNavigationArrow: false,
                    });
                  }
                  return C.IrideFolioMediaShelf({                                  // KT:1676-1690
                    items: isVideoSection
                      ? displayItems.map((item) => (item.type === 'SongItem' && !item.isVideoSong) ? C.copy(item, { musicVideoType: 'MUSIC_VIDEO_TYPE_OMV' }) : item) // KT:1679-1686
                      : displayItems,                                             // KT:1687
                    songContent: ytSongItem,                                      // KT:1688
                    tileContent: ytGridItem,                                      // KT:1689
                  });
                }));
              };

              // KT:1694-1713  if (isLoggedIn) { your_mood_section }
              if (isLoggedIn) {
                shelfOrder.added({ hasFourRows: false });                         // KT:1695
                body.push(C.item({ key: 'your_mood_section' }, () =>              // KT:1696
                  C.IrideFolioSection({                                           // KT:1697-1713
                    modifier: C.Modifier.animateItem({
                      fadeInSpec: null,
                      placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                      fadeOutSpec: null,
                    }),
                    sectionKey: 'your_mood_section',                              // KT:1706
                    ready: isMoodPreferenceReady,                                 // KT:1707
                    placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight, { controls: true }), // KT:1708-1713
                  }, () => {                                                      // KT:1714
                    C.IrideFolioShelfHeader({ title: C.stringResource('home_mood_youtube_music'), showNavigationArrow: false }); // KT:1715-1718
                    return C.Column({ modifier: C.Modifier.fillMaxWidth() }, () => { // KT:1720
                      if (moodChips.length > 0) {                                 // KT:1721
                        IrideMoodChipsRow({                                     // KT:1722-1739
                          chips: moodChips,                                       // KT:1723
                          currentValue: selectedMood.value,                       // KT:1724
                          onValueUpdate: (chip) => {                              // KT:1725-1737
                            if (chip === selectedMood.value) {                    // KT:1726
                              viewModel.loadMoodPage(chip.endpoint && chip.endpoint.params, chip.title, hideExplicit, hideVideoSongs, hideYoutubeShorts, moodPosterSize, true); // KT:1727-1735
                            } else selectedMood.value = chip;                     // KT:1736
                          },
                          horizontalPadding: irideStart,                          // KT:1738
                        });
                      }
                      const mixItems = moodMixItems || [];                        // KT:1742
                      return C.Box({                                              // KT:1743-1823
                        modifier: C.Modifier.fillMaxWidth().height(currentGridHeight + 64),  // KT:1744-1745
                        children: () => [
                          C.LazyRow({                                             // KT:1747-1823
                            state: moodMixesState,                                // KT:1748
                            contentPadding: { horizontal: irideGridItemStart },   // KT:1749-1750
                            horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing), // KT:1751-1754
                            overscrollEffect: null,                               // KT:1755
                            modifier: C.Modifier.fillMaxWidth().height(currentGridHeight + 64).padding({ bottom: 4 }) // KT:1756-1763
                              .rubberBandOverscroll('Horizontal', moodMixesState),
                            items: mixItems,                                      // KT:1765
                            key: (it) => it.id,                                   // KT:1765
                            content: (mix) => C.YouTubeGridItem({                 // KT:1766-1821
                              item: mix,                                          // KT:1767
                              thumbnailUrlOverride: moodPosterUrls[mix.id] || mix.thumbnail, // KT:1768-1769
                              thumbnailImageLoader: moodPosterUrls[mix.id] != null ? viewModel.moodPosterImageLoader : null, // KT:1770-1773
                              useThumbnailOverride: true,                         // KT:1774
                              thumbnailPlaceholder: () => C.MixPosterPlaceholder({ // KT:1775-1792
                                title: mix.title,                                 // KT:1777
                                mood: C.MixMood.fromMixTitle(mix.title)           // KT:1779
                                  || C.MixMood.fromChip((moodChips.find((p) => p[0].endpoint && p[0].endpoint.params === loadedMoodParams) || [])[1])  // KT:1780-1789
                                  || C.MixMood.CHILL,                             // KT:1790
                              }),
                              isActive: mix.id === (mediaMetadataState.value && mediaMetadataState.value.album && mediaMetadataState.value.album.id), // KT:1793-1795
                              isPlaying: isPlayingState.value,                    // KT:1796
                              coroutineScope: scope,                              // KT:1797
                              thumbnailRatio: 1,                                  // KT:1798
                              size: currentGridHeight,                            // KT:1799
                              showTitle: true,                                    // KT:1800
                              modifier: C.irideFolioCombinedClickable({           // KT:1802-1820
                                onClick: () => navController.navigate(`online_playlist/${mix.id}`), // KT:1803-1806
                                onLongClick: () => menuState.show({ anchored: true }, () => // KT:1808-1818
                                  C.YouTubePlaylistMenu({ playlist: mix, coroutineScope: scope, onDismiss: menuState.dismiss })),
                              }),
                            }),
                          }),
                          // KT:1824-1889  when { ... } : vuoto / skeleton / errore con Riprova / vuoto
                          (() => {
                            if (mixItems.length === 0 && moodChips.length === 0 && isMoodPreferenceReady && !isHomeFeedLoading) // KT:1825-1826
                              return C.Text({ text: C.stringResource('home_mood_empty'), color: C.MaterialTheme.colorScheme().textSecondary, modifier: C.Modifier.align('Center').padding({ horizontal: 24 }) }); // KT:1827-1831
                            if (mixItems.length === 0 && (isMoodLoading || isHomeFeedLoading || !isMoodPreferenceReady || selectedMood.value == null)) // KT:1832-1836
                              return C.IrideFolioShelfSkeleton(currentGridHeight); // KT:1837
                            if (isMoodError && mixItems.length === 0)              // KT:1838
                              return C.Column({ modifier: C.Modifier.align('Center').padding({ horizontal: 24 }), horizontalAlignment: 'CenterHorizontally' }, () => [ // KT:1839-1845
                                C.Text({ text: C.stringResource('home_mood_load_error'), color: C.MaterialTheme.colorScheme().textSecondary }), // KT:1846-1854
                                C.TextButton({                                    // KT:1855-1869
                                  onClick: () => {                                // KT:1856-1868
                                    if (selectedMood.value) {
                                      const chip = selectedMood.value;
                                      viewModel.loadMoodPage(chip.endpoint && chip.endpoint.params, chip.title, hideExplicit, hideVideoSongs, hideYoutubeShorts, moodPosterSize, true);
                                    }
                                  },
                                }, () => C.Text({ text: C.stringResource('home_mood_retry') })), // KT:1870-1874
                              ]);
                            if (mixItems.length === 0)                            // KT:1877
                              return C.Text({ text: C.stringResource('home_mood_empty'), color: C.MaterialTheme.colorScheme().textSecondary, modifier: C.Modifier.align('Center').padding({ horizontal: 24 }) }); // KT:1878-1888
                            return null;
                          })(),
                        ],
                      });
                    });
                  })));
              }

              // KT:1895-2038  if (SHOW_HOME_TEST_PLAYLIST) testPlaylist?.let { playlist -> item("home_test_playlist_section") }
              // Blocco di test, disattivato (SHOW_HOME_TEST_PLAYLIST = false). Tradotto come funzione separata.
              if (SHOW_HOME_TEST_PLAYLIST && testPlaylist) body.push(homeTestPlaylistSection(testPlaylist, { currentGridHeight, homeReducedMotion, irideGridItemStart, testPlaylistTrackThumbnails, navController }));

              // KT:2040  forYouSection(shelfOrder)
              body.push(forYouSection(shelfOrder));

              // KT:2041-2061  home_feed_status (sezioni feed vuote: loading o errore con Riprova)
              if (!homePage || !homePage.sections || homePage.sections.length === 0) {
                body.push(C.item({ key: 'home_feed_status' }, () => {             // KT:2042
                  if (isHomeFeedLoading) return HomeFeedLoadingShelf(currentGridHeight); // KT:2043-2044
                  return C.Column({ modifier: C.Modifier.fillMaxWidth().padding(24), horizontalAlignment: 'CenterHorizontally', verticalArrangement: C.spacedBy(12) }, () => [ // KT:2046-2050
                    C.Text({ text: C.stringResource('home_feed_unavailable'), color: C.MaterialTheme.colorScheme().textSecondary }), // KT:2051-2054
                    C.TextButton({ onClick: () => viewModel.loadHomeData() }, () => C.Text({ text: C.stringResource('home_mood_retry') })), // KT:2055-2057
                  ]);
                }));
              }

              // KT:2062-2146  home_personal_mixes_section (solo se loggato)
              if (isLoggedIn) shelfOrder.added({ hasFourRows: false });           // KT:2062
              if (isLoggedIn) {                                                    // KT:2063
                body.push(C.item({ key: 'home_personal_mixes_section' }, () =>    // KT:2064
                  C.IrideFolioSection({                                           // KT:2065-2079
                    modifier: C.Modifier.animateItem({
                      fadeInSpec: null,
                      placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                      fadeOutSpec: null,
                    }),
                    sectionKey: 'home_personal_mixes_section',
                    artwork: homePersonalMixes.slice(0, 2).map((it) => C.resize(it.thumbnail, 448, 448)), // KT:2076-2078
                    placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight), // KT:2079
                  }, () => {                                                      // KT:2080
                    C.IrideFolioShelfHeader({ title: C.stringResource('home_fresh_find_old_favorite'), showNavigationArrow: false }); // KT:2081-2085
                    const personalMixesState = C.rememberLazyListState();          // KT:2087
                    const starredPlaylistName = C.stringResource('starred');       // KT:2088
                    const starredPlaylist = C.remember([starredPlaylistName, starredPlaylistThumbnails], () => // KT:2089-2100
                      C.Playlist({ playlist: C.PlaylistEntity({ id: C.PlaylistEntity.LIKED_PLAYLIST_ID, name: starredPlaylistName }), songCount: 0, songThumbnails: starredPlaylistThumbnails }));
                    return C.LazyRow({                                            // KT:2101-2113
                      state: personalMixesState,
                      contentPadding: { horizontal: irideGridItemStart },         // KT:2103-2104
                      horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing), // KT:2105-2106
                      overscrollEffect: null,                                     // KT:2107
                      modifier: C.Modifier.fillMaxWidth().rubberBandOverscroll('Horizontal', personalMixesState), // KT:2109-2113
                      items: [                                                    // KT:2115-2143
                        C.item({ key: 'home_personal_mixes_starred' }, () => C.PlaylistGridItem({ // KT:2116-2136
                          playlist: starredPlaylist,
                          autoPlaylist: true,
                          modifier: C.irideFolioCombinedClickable({
                            onClick: () => navController.navigate('auto_playlist/liked'), // KT:2121-2124
                            onLongClick: () => menuState.show({ anchored: true }, () => // KT:2126-2134
                              C.PlaylistMenu({ playlist: starredPlaylist, coroutineScope: scope, onDismiss: menuState.dismiss })),
                          }),
                        })),
                      ].concat(homePersonalMixes.map((mix) => C.item({ key: `home_personal_mix_${mix.id}` }, () => ytGridItem(mix)))), // KT:2138-2143
                    });
                  }),
                ));
              }

              // KT:2147-2149  feedSection = { orderedFeedSections.forEach { addFeedSection(it) } }
              const feedSection = () => orderedFeedSections.map((it) => addFeedSection(it));

              // KT:2151-2201  dailySection: daily discover (distinct recommendation.id, non vuota)
              const dailySection = () => {
                const discoverList = (dailyDiscover || []).filter((x, i, a) => a.findIndex((y) => y.recommendation.id === x.recommendation.id) === i); // KT:2153
                if (!discoverList.length) return null;                            // KT:2154
                shelfOrder.added({ hasFourRows: false });                         // KT:2156
                return C.item({ key: 'daily_discover_section' }, () =>            // KT:2157
                  C.IrideFolioSection({                                           // KT:2158-2177
                    modifier: C.Modifier.animateItem({
                      fadeInSpec: null,
                      placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                      fadeOutSpec: null,
                    }),
                    sectionKey: 'daily_discover_section',
                    artwork: discoverList.slice(0, 4).map((it) => C.resize(it.recommendation.thumbnail, 448, 448)), // KT:2168-2171
                    placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight, { songs: true }), // KT:2172-2177
                  }, () => {                                                      // KT:2178
                    C.IrideFolioShelfHeader({ title: C.stringResource('your_daily_discover'), showNavigationArrow: false }); // KT:2179-2183
                    return C.SharedQuickPicksCarousel({                           // KT:2185-2191
                      items: discoverList,
                      key: (it) => it.recommendation.id,                          // KT:2187
                      loading: false,                                             // KT:2188
                      contentPadding: irideListItemStart - 4,                     // KT:2189
                      usePager: true,                                             // KT:2190
                      minimumRows: 1,                                             // KT:2191
                      content: (discovery, itemWidth) =>                          // KT:2192-2196
                        (discovery.recommendation && discovery.recommendation.type === 'SongItem')
                          ? ytSongItem(discovery.recommendation, itemWidth) : null,
                    });
                  }));
              };

              // KT:2203-2300  communitySection: community playlists (distinct playlist.id)
              const communitySection = () => {
                const playlists = (communityPlaylists || []).filter((x, i, a) => a.findIndex((y) => y.playlist.id === x.playlist.id) === i); // KT:2205
                if (!playlists.length) return null;                               // KT:2206
                shelfOrder.added({ hasFourRows: false });                         // KT:2208
                return C.item({ key: 'community_playlists_section' }, () =>       // KT:2209
                  C.IrideFolioSection({                                           // KT:2210-2226
                    modifier: C.Modifier.animateItem({
                      fadeInSpec: null,
                      placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                      fadeOutSpec: null,
                    }),
                    sectionKey: 'community_playlists_section',
                    artwork: playlists.slice(0, 2).map((it) => C.resize(it.playlist.thumbnail, 448, 448)), // KT:2221-2223
                    placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight), // KT:2224-2226
                  }, () => {                                                      // KT:2227
                    C.IrideFolioShelfHeader({ title: C.stringResource('from_the_community'), showNavigationArrow: false }); // KT:2228-2231
                    const communityPlaylistsState = C.rememberLazyListState();    // KT:2233
                    return C.LazyRow({                                            // KT:2234-2248
                      state: communityPlaylistsState,
                      contentPadding: { horizontal: irideGridItemStart },         // KT:2236-2237
                      horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing), // KT:2238-2241
                      overscrollEffect: null,                                     // KT:2242
                      modifier: C.Modifier.fillMaxWidth().rubberBandOverscroll('Horizontal', communityPlaylistsState), // KT:2243-2248
                      items: playlists.map((cpItem) => C.item({ key: `community_playlist_${cpItem.playlist.id}` }, () => // KT:2250-2295
                        C.PlaylistGridItem({                                      // KT:2256-2294
                          playlist: C.Playlist({                                  // KT:2258-2275
                            playlist: C.PlaylistEntity({
                              id: cpItem.playlist.id.replace(/^VL/, ''),          // KT:2261-2263
                              name: cpItem.playlist.title,                        // KT:2264-2265
                              browseId: cpItem.playlist.id.replace(/^VL/, ''),    // KT:2266-2268
                              thumbnailUrl: cpItem.playlist.thumbnail,            // KT:2269-2271
                            }),
                            songCount: 0,                                         // KT:2273
                            songThumbnails: [],                                   // KT:2274
                          }),
                          autoPlaylist: false,                                    // KT:2276
                          modifier: C.irideFolioCombinedClickable({               // KT:2277-2293
                            onClick: () => navController.navigate(`online_playlist/${cpItem.playlist.id.replace(/^VL/, '')}`), // KT:2279-2282
                            onLongClick: () => menuState.show({ anchored: true }, () => // KT:2284-2292
                              C.YouTubePlaylistMenu({ playlist: cpItem.playlist, coroutineScope: scope, onDismiss: menuState.dismiss })),
                          }),
                        }))),
                    });
                  }));
              };

              // KT:2302-2382  similarSection: rec.items (no video song), sort ArtistItem last, title.id key
              const similarSection = () => (similarRecommendations || []).map((rec, index) => {  // KT:2303
                const similarItems = rec.items                                    // KT:2304-2313
                  .filter((x, i, a) => a.findIndex((y) => y.id === x.id) === i)
                  .filter((item) => (item.type === 'SongItem' && !item.isVideoSong) || item.type === 'AlbumItem' || item.type === 'ArtistItem' || item.type === 'PlaylistItem')
                  .sort((a, b) => (a.type === 'ArtistItem' ? 1 : 0) - (b.type === 'ArtistItem' ? 1 : 0)); // sortedBy stabile
                if (similarItems.length < 2 || (rec.title.type !== 'Artist' && (rec.thumbnailUrl == null || String(rec.thumbnailUrl).trim() === ''))) return null; // KT:2314-2318
                shelfOrder.added({ hasFourRows: false });                         // KT:2319
                return C.item({ key: `similar_to_${rec.title.id}` }, () =>        // KT:2320
                  C.IrideFolioSection({                                           // KT:2321-2343
                    modifier: C.Modifier.animateItem({
                      fadeInSpec: null,
                      placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                      fadeOutSpec: null,
                    }),
                    sectionKey: `similar_to_${rec.title.id}`,
                    artwork: similarItems.slice(0, 4).map((it) => C.resize(it.thumbnail, 448, 448)), // KT:2332-2334
                    placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight, { // KT:2335-2343
                      songs: similarItems.some((it) => it.type === 'SongItem' && !it.isVideoSong),
                    }),
                  }, () => {                                                      // KT:2344
                    HomeSimilarityTitle({                                       // KT:2345-2372
                      name: rec.title.title,                                      // KT:2346
                      thumbnailUrl: rec.thumbnailUrl,                             // KT:2347
                      artistId: rec.title.type === 'Artist' ? rec.title.id : null, // KT:2348
                      isArtist: rec.title.type === 'Artist',                      // KT:2349
                      onClick: () => {                                            // KT:2350-2371
                        switch (rec.title.type) {
                          case 'Song': navController.navigate(`album/${rec.title.album.id}`); break; // KT:2352-2355
                          case 'Album': navController.navigate(`album/${rec.title.id}`); break; // KT:2356-2359
                          case 'Artist': navController.navigate(`artist/${rec.title.id}`); break; // KT:2360-2363
                          case 'Playlist': if (rec.title.playlist.browseId) navController.navigate(`online_playlist/${rec.title.playlist.browseId.replace(/^VL/, '')}`); break; // KT:2364-2369
                        }
                      },
                    });
                    return C.IrideFolioMediaShelf({                               // KT:2374-2378
                      items: similarItems,
                      songContent: ytSongItem,
                      tileContent: ytGridItem,
                    });
                  }));
              }).filter((x) => x !== null);

              // KT:2384-2430  accountSection: playlist YouTube dell'account (shuffled, distinct id)
              const accountSection = () => {
                const playlists = accountPlaylists || [];                         // KT:2385-2386
                if (!playlists.length) return null;
                shelfOrder.added({ hasFourRows: false });                         // KT:2388
                return C.item({ key: 'account_playlists_section' }, () =>         // KT:2389
                  C.IrideFolioSection({                                           // KT:2390-2406
                    modifier: C.Modifier.animateItem({
                      fadeInSpec: null,
                      placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                      fadeOutSpec: null,
                    }),
                    sectionKey: 'account_playlists_section',
                    artwork: playlists.slice(0, 2).map((it) => C.resize(it.thumbnail, 448, 448)), // KT:2401-2403
                    placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight), // KT:2404-2406
                  }, () => {                                                      // KT:2407
                    C.SharedHomeSectionTitle({                                    // KT:2408-2414
                      title: C.stringResource('home_youtube_music_playlists'),
                      onClick: () => navController.navigate('account'),
                    });
                    const shuffledPlaylists = C.remember([playlists], () =>       // KT:2416-2419
                      C.shuffled(playlists.filter((x, i, a) => a.findIndex((y) => y.id === x.id) === i)));
                    return C.SharedYouTubePlaylistRow({                           // KT:2420-2426
                      items: shuffledPlaylists,
                      key: (it) => `home_account_playlist_${it.id}`,
                      contentPadding: irideGridItemStart,
                      modifier: C.Modifier,
                      itemContent: (playlist) => ytGridItem(playlist),
                    });
                  }));
              };

              // KT:2432-2453  addFollowedReleases: HomeFollowedReleases visibile quando la lista e' nel viewport
              const addFollowedReleases = () => {
                shelfOrder.added({ hasFourRows: true });                          // KT:2433
                return C.item({ key: 'home_followed_releases' }, () => {          // KT:2434
                  const isVisible = C.derivedStateOf(() => C.listItemVisible(lazyListState, 'home_followed_releases')); // KT:2435-2446
                  return C.HomeFollowedReleases({                                 // KT:2447-2451
                    navController, snackbarHostState, isVisible: isVisible && phase1Complete,
                  });
                });
              };
              let followedReleasesAdded = false;                                  // KT:2454

              // KT:2455-2464  sectionReadiness
              const sectionReadiness = [
                true,                                                             // KT:2457
                dailyDiscoverComplete || !(dailyDiscover && dailyDiscover.length),// KT:2458
                communityComplete || !(communityPlaylists && communityPlaylists.length), // KT:2459
                similarComplete || !(similarRecommendations && similarRecommendations.length), // KT:2460
                !isLoggedIn || !isAccountPlaylistsLoading || !(accountPlaylists && accountPlaylists.length), // KT:2461-2463
              ].map((v) => !!v);

              // KT:2465-2497  for ((index, section) in [feed, daily, community, similar, account].withIndex())
              const sectionList = [feedSection, dailySection, communitySection, similarSection, accountSection];
              sectionList.forEach((section, index) => {
                const out = section();                                            // KT:2474
                if (Array.isArray(out)) body.push(...out.filter(Boolean)); else if (out) body.push(out);
                if (!sectionReadiness[index]) {                                   // KT:2475
                  body.push(C.item({ key: `home_pending_section_${index}` }, () => // KT:2476
                    C.IrideFolioSection({                                         // KT:2477-2486
                      sectionKey: `home_pending_section_${index}`,
                      ready: false,
                      placeholder: () => C.IrideFolioSectionSkeleton(currentGridHeight, { songs: index === 1 || index === 3 }), // KT:2480-2485
                    }, () => null)));                                             // KT:2486
                }
                if (!followedReleasesAdded && index + 1 >= followedReleasesPosition && shelfOrder.canAdd({ hasFourRows: true })) { // KT:2489-2492
                  body.push(addFollowedReleases());                               // KT:2494
                  followedReleasesAdded = true;                                   // KT:2495
                }
              });

              // KT:2499-2502  if (!followedReleasesAdded) { shelfOrder.added(false); addFollowedReleases() }
              if (!followedReleasesAdded) {
                shelfOrder.added({ hasFourRows: false });
                body.push(addFollowedReleases());
              }

              // KT:2503-2505  home_charts: NewReleaseChartsShelf(navController, chartViewModel, preload = false)
              body.push(C.item({ key: 'home_charts' }, () => C.NewReleaseChartsShelf(navController, chartViewModel, { preload: false })));

              // KT:2506-2560  reverie_section: archivio Reverie (mesi) o card di disponibilita'
              body.push(C.item({ key: 'reverie_section' }, () =>
                C.IrideFolioSection({                                             // KT:2507-2516
                  sectionKey: 'reverie_section',
                  modifier: C.Modifier.animateItem({
                    fadeInSpec: null,
                    placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                    fadeOutSpec: null,
                  }),
                }, () => {                                                        // KT:2517
                  C.SharedHomeSectionTitle({ title: C.stringResource('reverie_archive_title') }); // KT:2518-2520
                  if (reverieMonths.length > 0) {                                 // KT:2522
                    const archiveState = C.rememberLazyListState();               // KT:2523
                    return C.LazyRow({                                            // KT:2524-2539
                      state: archiveState,
                      overscrollEffect: null,                                     // KT:2526
                      modifier: C.Modifier.fillMaxWidth().padding({ bottom: 24 }).rubberBandOverscroll('Horizontal', archiveState), // KT:2528-2533
                      contentPadding: { horizontal: C.IrideFolio.HorizontalPadding }, // KT:2534-2537
                      horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing), // KT:2538-2539
                      items: reverieMonths.map((month) => C.item({ key: String(month) }, () =>  // KT:2541-2549
                        C.ReverieHomeCard({
                          month,
                          onClick: () => navController.navigate(`reverie/${month}`), // KT:2545
                          modifier: C.Modifier.width(300),                        // KT:2547
                        }))),
                    });
                  }
                  // KT:2551-2558  else: ReverieAvailabilityCard(YearMonth.now(), firstFullReverieMonth, ...)
                  return C.ReverieAvailabilityCard(C.YearMonth.now(), firstFullReverieMonth,
                    C.Modifier.padding({ horizontal: C.IrideFolio.HorizontalPadding }).padding({ bottom: 24 }));
                })));

              // KT:2561-2630  iride_features_section: 2 card (stats, spotify/import)
              body.push(C.item({ key: 'iride_features_section' }, () =>
                C.IrideFolioSection({                                             // KT:2562-2572
                  sectionKey: 'iride_features_section',
                  modifier: C.Modifier.animateItem({
                    fadeInSpec: null,
                    placementSpec: homeReducedMotion ? null : C.IrideMotion.PlacementSpec,
                    fadeOutSpec: null,
                  }),
                }, () => {
                  C.SharedHomeSectionTitle({ title: C.stringResource('home_iride_features_title') }); // KT:2573-2575
                  const featuresState = C.rememberLazyListState();                // KT:2577
                  return C.LazyRow({                                              // KT:2578-2592
                    state: featuresState,
                    overscrollEffect: null,                                       // KT:2580
                    modifier: C.Modifier.fillMaxWidth().padding({ bottom: 24 }).rubberBandOverscroll('Horizontal', featuresState), // KT:2582-2587
                    contentPadding: { horizontal: C.IrideFolio.HorizontalPadding }, // KT:2588-2589
                    horizontalArrangement: C.spacedBy(C.IrideFolio.ShelfItemSpacing), // KT:2590-2591
                    items: [0, 1].map((index) => index === 0                      // KT:2593-2626
                      ? IrideFeatureCard({                                        // KT:2595-2608
                          title: C.stringResource('home_iride_feature_stats_title'),
                          subtitle: C.stringResource('home_iride_feature_stats_description'),
                          iconRes: 'stats',                                       // KT:2605
                          onClick: () => navController.navigate('stats'),         // KT:2606
                          modifier: C.Modifier.width(300),                        // KT:2607
                        })
                      : IrideFeatureCard({                                        // KT:2610-2625
                          title: C.stringResource('home_iride_feature_spotify_title'),
                          subtitle: C.stringResource('home_iride_feature_spotify_description'),
                          iconRes: 'playlist_add',                                // KT:2620
                          onClick: () => navController.navigate('music_import'),  // KT:2622
                          modifier: C.Modifier.width(300),                        // KT:2624
                        })),
                  });
                })));

              // KT:2631-2636  fine IridePageLayout body
              return body;
            });
          }))));
  }

  global.IrideHomeScreen = { HomeScreen, HomeScreenContent, SHOW_HOME_TEST_PLAYLIST, IrideMoodChipsRow, ForYouShelfSkeleton, HomeSimilarityTitle, HomeFeedLoadingShelf, IrideFeatureCard, SyncBanner };
})(typeof window !== 'undefined' ? window : globalThis);
