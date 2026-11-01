/*
 * Traduzione riga per riga di LibraryAlbumsScreen
 * Sorgente: app/src/main/kotlin/com/metrolist/music/ui/screens/library/LibraryScreen.kt
 *           (tag v0.1.0-alpha33, righe 359-745)
 *
 * Ogni blocco porta il numero di riga Kotlin di origine in un commento "KT:".
 * Le dipendenze chiamate (composable Kotlin) sono nel file library-components.js
 * (stub con riferimento KT). Questo file NON e' ancora caricato da index.html:
 * il collegamento alla vista #view-albums e' un passo successivo.
 *
 * Convenzioni:
 *  - Stato Compose (remember / rememberSaveable / preference) -> oggetto `state`.
 *  - LaunchedEffect -> chiamata diretta alla funzione di avvio (una volta).
 *  - Composable -> funzione JS che restituisce un nodo DOM.
 */
(function (global) {
  'use strict';

  const C = global.IrideLibraryComponents; // library-components.js

  // KT:359-364  fun LibraryAlbumsScreen(navController, isOffline, onSelectCategory, onReturnToLibrary, viewModel)
  function LibraryAlbumsScreen(props) {
    const navController = props.navController;
    const isOffline = props.isOffline === true;
    const onSelectCategory = props.onSelectCategory || null;
    const onReturnToLibrary = props.onReturnToLibrary || null;
    const viewModel = props.viewModel; // LibraryAlbumsViewModel (vedi stub in library-components.js)

    // KT:366-369  menuState, keyboardController, coroutineScope, playerConnection, database
    const menuState = props.menuState || C.LocalMenuState();
    const playerConnection = props.playerConnection || null;
    if (!playerConnection) return null; // KT:369  ?: return
    const database = props.database || null;

    // KT:370-372  isPlaying, mediaMetadata (collectAsState)
    const isPlaying = playerConnection.isEffectivelyPlaying();
    const mediaMetadata = playerConnection.mediaMetadata();

    // KT:374-378  viewType (AlbumViewTypeKey, default GRID); gridViewType; LaunchedEffect(viewType)
    const state = C.rememberPreferenceStore('AlbumViewTypeKey', 'GRID');
    let viewType = state.viewType;
    const gridViewType = viewType === 'LIST' ? 'GRID' : viewType;
    if (viewType === 'LIST') { viewType = 'GRID'; state.viewType = 'GRID'; }

    // KT:379-380  sortType (AlbumSortTypeKey, default CREATE_DATE)
    const sortType = C.rememberEnumPreference('AlbumSortTypeKey', 'CREATE_DATE');

    // KT:382-385  ytmSync, hideExplicit, pureBlack, betterLibraryBeta (preferenze)
    const ytmSync = C.rememberPreference('YtmSyncKey', true);
    const hideExplicit = C.rememberPreference('HideExplicitKey', false);
    const pureBlack = C.rememberPreference('PureBlackKey', false);
    const betterLibraryBeta = C.rememberPreference('BetterLibraryBetaKey', false);

    // KT:387-391  LaunchedEffect(Unit) { if (ytmSync) withContext(IO) { viewModel.sync() } }
    if (ytmSync) C.launchIO(() => viewModel.sync());

    // KT:393  albums (downloadedAlbums se offline, altrimenti allAlbums)
    const albums = isOffline ? viewModel.downloadedAlbums() : viewModel.allAlbums();

    // KT:394-396  continueListeningAlbums, continueListeningHidden, continueListeningAvailable
    const continueListeningAlbums = viewModel.continueListeningAlbums();
    const continueListeningHidden = viewModel.isContinueListeningHidden();
    const continueListeningAvailable = continueListeningAlbums.length > 0;

    // KT:397-398  continueListeningAlbumSelected (mutableStateOf(false))
    const continueListeningAlbumSelected = { value: false };

    // KT:399-423  DisposableEffect(lifecycleOwner): ON_RESUME / ON_PAUSE -> recordContinueListeningVisit
    C.disposableLifecycleEffect({
      onResume() { continueListeningAlbumSelected.value = false; },
      onPause() {
        if (continueListeningAvailable) viewModel.recordContinueListeningVisit(continueListeningAlbumSelected.value);
      },
    });

    // KT:425-430  LaunchedEffect(mediaMetadata?.album?.id, continueListeningAlbums) -> hideContinueListening
    const activeAlbumId = mediaMetadata && mediaMetadata.album ? mediaMetadata.album.id : null;
    if (activeAlbumId && continueListeningAlbums.some((a) => a.id === activeAlbumId)) viewModel.hideContinueListening();

    // KT:431-438  searchQuery, searchFieldValue (rememberSaveable), LaunchedEffect(searchQuery)
    const searchQuery = viewModel.searchQuery();
    const searchFieldValue = C.rememberTextFieldValue(searchQuery);
    const searchFocusRequester = C.rememberFocusRequester();
    const normalizedQuery = C.normalizeForSearch(searchQuery); // KT:440 remember(searchQuery) { normalizeForSearch() }

    // KT:442-449  filteredAlbums: hideExplicit, matchesNormalizedQuery(title, artists), distinctBy id
    const filteredAlbums = (() => {
      const visible = hideExplicit ? albums.filter((a) => !a.album.explicit) : albums;
      const matched = visible.filter((album) => {
        const artistNames = album.artists.map((x) => x.name);
        return C.matchesNormalizedQuery(normalizedQuery, album.album.title, ...artistNames);
      });
      return C.distinctBy(matched, (a) => a.id);
    })();

    // KT:451-454  downloadUtil, likedAlbumsSongs, isProcessingDownloads, downloadState, showRemoveDownloadDialog
    const downloadUtil = props.downloadUtil || null;
    const likedAlbumsSongs = viewModel.likedAlbumsSongs();
    const isProcessingDownloads = viewModel.isProcessingDownloads();
    const downloadState = C.downloadStateFor(likedAlbumsSongs, isProcessingDownloads, downloadUtil);
    const showRemoveDownloadDialog = { value: false };

    // KT:457-485  LaunchedEffect(likedAlbumsSongs, isProcessingDownloads) -> downloadState (vedi downloadStateFor)

    // KT:487-493  onDownloadAllClick: when(downloadState) COMPLETED -> dialog; DOWNLOADING|QUEUED -> remove; else -> download all
    const onDownloadAllClick = () => {
      if (downloadState === C.STATE_COMPLETED) showRemoveDownloadDialog.value = true;
      else if (downloadState === C.STATE_DOWNLOADING || downloadState === C.STATE_QUEUED) viewModel.removeAllFavoriteAlbumsDownloads();
      else viewModel.downloadAllFavoriteAlbums();
    };

    // KT:495-519  if (showRemoveDownloadDialog) DefaultDialog { text remove_all_favorite_albums_confirm; buttons cancel / remove_download }
    if (showRemoveDownloadDialog.value) {
      C.DefaultDialog({
        onDismiss: () => { showRemoveDownloadDialog.value = false; },
        text: C.str('remove_all_favorite_albums_confirm'),
        buttons: [
          { kind: 'secondary', label: C.str('cancel'), onClick: () => { showRemoveDownloadDialog.value = false; } },
          { kind: 'primary', label: C.str('remove_download'), onClick: () => {
            showRemoveDownloadDialog.value = false;
            viewModel.removeAllFavoriteAlbumsDownloads();
          } },
        ],
      });
    }

    // KT:521-528  sortOptions (CREATE_DATE, YEAR, NAME, SONG_COUNT, LENGTH, PLAY_TIME)
    const sortOptions = [
      ['CREATE_DATE', C.str('sort_by_create_date')],
      ['YEAR', C.str('sort_by_year')],
      ['NAME', C.str('sort_by_name')],
      ['SONG_COUNT', C.str('sort_by_song_count')],
      ['LENGTH', C.str('sort_by_length')],
      ['PLAY_TIME', C.str('sort_by_play_time')],
    ];

    // KT:530-531  itemCountText = pluralStringResource(n_album, size); albumsQueueTitle = albums
    const itemCountText = C.pluralStr('n_album', filteredAlbums.length);
    const albumsQueueTitle = C.str('albums');

    // KT:533-534  lazyGridState
    const lazyGridState = C.rememberLazyGridState();

    // KT:535-541  scrollToTop (savedStateHandle "scrollToTop") -> lazyGridState.animateScrollToItem(0)
    if (props.scrollToTop) { lazyGridState.animateScrollToItem(0); props.onScrollToTopHandled && props.onScrollToTopHandled(); }

    // KT:543-550  continueListeningTitle: LibrarySectionLabel(continue_listening, verticalPadding 4dp)
    const continueListeningTitle = () => C.LibrarySectionLabel({ text: C.str('continue_listening'), verticalPadding: 4 });

    // KT:551-572  continueListeningRow: BoxWithConstraints; albumTileSize = (maxWidth - 24) / 3;
    //             LazyRow(spacedBy 12dp) items -> LibraryContinueListeningAlbumItem(size, onAlbumSelected)
    const continueListeningRow = () => C.ContinueListeningRow({
      albums: continueListeningAlbums,
      gapDp: 12,
      tileSizeFor: (maxWidthDp) => (maxWidthDp - 24) / 3,
      render: (album, size) => C.LibraryContinueListeningAlbumItem({
        navController, menuState, album,
        isActive: album.id === (mediaMetadata && mediaMetadata.album ? mediaMetadata.album.id : null),
        isPlaying, size,
        onAlbumSelected: () => { continueListeningAlbumSelected.value = true; },
      }),
    });

    // KT:574  screenProgress = rememberEnterProgress(play=true, durationMillis=Short, easing=EaseOutQuart)
    const screenProgress = C.rememberEnterProgress({ play: true, durationMillis: C.IrideMotion.Short, easing: C.IrideMotion.EaseOutQuart });

    // KT:576-584  Box(fillMaxSize) { Box(fillMaxSize, background Transparent, graphicsLayer alpha = screenProgress) {
    const root = C.Box({ alpha: screenProgress, fill: true });

    // KT:585-589  CompositionLocalProvider(LocalItemHorizontalPadding=false, LocalContentColor=White, LocalLibraryTextColor=White)
    const ctx = C.provideLocals({ itemHorizontalPadding: false, contentColor: '#fff', libraryTextColor: '#fff' });

    // KT:590-591  LibraryGridPage(titleRes = albums, onNavigateUp = onReturnToLibrary ?: navigateUp)
    const page = C.LibraryGridPage({
      titleRes: 'albums',
      onNavigateUp: () => (onReturnToLibrary ? onReturnToLibrary() : navController.navigateUp()),

      // KT:592-598  columns: GRID_WIDE -> Fixed(3), else Fixed(2)
      columns: gridViewType === 'GRID_WIDE' ? 3 : 2,

      // KT:599-600  state = lazyGridState
      state: lazyGridState,

      // KT:601-603  bottomPadding (LocalPlayerAwareWindowInsets), horizontalArrangement / verticalArrangement
      bottomPadding: C.playerAwareBottomPadding(),
      horizontalGapDp: 12,
      verticalGapDp: 8,

      // KT:604-611  menuSections: libraryGridMenuActions(gridViewType), librarySortMenuActions(sortOptions, sortType)
      menuSections: [
        C.libraryGridMenuActions(gridViewType, (v) => { state.viewType = v; }),
        C.librarySortMenuActions(sortOptions, sortType.value, (v) => sortType.set(v)),
      ],

      // KT:612-621  menuActions: if filteredAlbums nonempty -> "download_all_albums" / "remove_download", icon download
      menuActions: filteredAlbums.length > 0 ? [
        {
          label: C.str(downloadState === C.STATE_STOPPED ? 'download_all_albums' : 'remove_download'),
          icon: 'download',
          onClick: onDownloadAllClick,
        },
      ] : [],
    }, (grid) => {
      // KT:622-660  item("library_search_actions", span full) { if (filtered nonempty || searchQuery nonblank) LibrarySearchAndActions(...) }
      grid.item({ key: 'library_search_actions', fullSpan: true }, () => {
        if (filteredAlbums.length > 0 || searchQuery !== '') {
          C.LibrarySearchAndActions({
            query: searchFieldValue,
            onQueryChange: (value) => { searchFieldValue.set(value); viewModel.updateSearchQuery(value); },
            placeholderRes: 'search_albums',
            focusRequester: searchFocusRequester,
            onSearch: () => C.hideKeyboard(),
            onClear: () => { searchFieldValue.set(''); viewModel.updateSearchQuery(''); },
            showActions: filteredAlbums.length > 0,
            // KT:636-661  LibraryPlaybackActions(onShuffle, onRotation)
            actions: C.LibraryPlaybackActions({
              onShuffle: () => C.playRandomAlbumWeighted({ albums: filteredAlbums, database, playerConnection, weightByListen: true, probability: 0.85 }),
              onRotation: () => C.playRotationQueue({ albums: filteredAlbums, database, playerConnection, title: albumsQueueTitle, sourceRoute: 'library_albums' }),
            }),
          });
        }
      });

      // KT:662-668  if (continueListeningAlbums nonempty) item title + row (fullSpan)
      if (!continueListeningHidden && continueListeningAlbums.length > 0) {
        grid.item({ key: 'continue_listening_title', fullSpan: true }, () => continueListeningTitle());
        grid.item({ key: 'continue_listening_row', fullSpan: true }, () => continueListeningRow());
      }

      // KT:670-707  filteredAlbums.let { albums -> empty placeholder; items(albums, key id) { LibraryAlbumGridItem } }
      if (filteredAlbums.length === 0) {
        grid.item({ key: 'empty', fullSpan: true }, () => {
          if (searchQuery !== '') return C.LibrarySearchEmptyPlaceholder({});
          return C.LibraryEmptyState({ text: C.str('library_empty_albums'), minHeightDp: 480 });
        });
      }
      filteredAlbums.forEach((album) => {
        grid.item({ key: album.id, contentType: 'album' }, () => C.LibraryAlbumGridItem({
          navController, menuState, album,
          isActive: album.id === (mediaMetadata && mediaMetadata.album ? mediaMetadata.album.id : null),
          isPlaying,
        }));
      });

      // KT:708-713  footer: if (filteredAlbums nonempty) LibraryFooterCount(itemCountText), fullSpan
      if (filteredAlbums.length > 0) {
        grid.item({ key: 'footer', fullSpan: true }, () => C.LibraryFooterCount({ text: itemCountText }));
      }
    });

    // KT:576-584 (cont.) root contiene la pagina
    root.append(ctx.wrap(page));
    return root;
  }

  global.IrideScreens = global.IrideScreens || {};
  global.IrideScreens.LibraryAlbumsScreen = LibraryAlbumsScreen;
})(typeof window !== 'undefined' ? window : globalThis);
