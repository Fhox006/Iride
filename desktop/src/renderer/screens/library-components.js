/*
 * Dipendenze di library-albums.js (LibraryAlbumsScreen).
 * Ogni funzione porta il riferimento Kotlin di origine (file:riga, tag v0.1.0-alpha33).
 * STATO "stub": la firma e' quella del Kotlin, il corpo e' da completare nel passo di collegamento.
 * STATO "ok": gia' implementato con lo stesso comportamento.
 */
(function (global) {
  'use strict';

  const mem = {}; const storage = (typeof localStorage !== 'undefined') ? localStorage : {
    getItem(k) { return k in mem ? mem[k] : null; }, setItem(k, v) { mem[k] = String(v); },
  };

  const IrideMotion = {
    // KT: ui/utils/IrideMotion.kt  object IrideMotion
    EaseOutExpo: [0.16, 1, 0.3, 1],   // CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    EaseOutQuart: [0.25, 1, 0.5, 1],  // CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
    Quick: 160,
    Short: 240,
    Medium: 300,
    Long: 420,
    Fullscreen: 520,
    StaggerStep: 40,
  };

  const STATE_STOPPED = 'STOPPED';       // Download.STATE_STOPPED
  const STATE_DOWNLOADING = 'DOWNLOADING';
  const STATE_QUEUED = 'QUEUED';
  const STATE_COMPLETED = 'COMPLETED';

  // Stringhe: in Kotlin sono R.string.*; qui la chiave, da mappare sul dizionario i18n di app.js.
  // KT: res/values/strings.xml
  function str(key) { return key; } // STUB: collegare a t() di app.js

  function pluralStr(key, n) { return `${n} ${key}`; } // STUB: KT pluralStringResource(R.plurals.n_album)

  // KT: LocalMenuState (ui/component/Menu...) — STUB
  function LocalMenuState() { return { show() {}, dismiss() {} }; }

  // KT: utils/rememberPreference / rememberEnumPreference — STUB collegato a localStorage (chiavi da constants/PreferenceKeys.kt)
  function rememberPreference(key, defaultValue) {
    try {
      const raw = storage.getItem('riff_' + key);
      if (raw === null) return defaultValue;
      return JSON.parse(raw);
    } catch (_) { return defaultValue; }
  }
  function rememberEnumPreference(key, defaultValue) {
    const storageKey = 'riff_' + key;
    return {
      value: (storage.getItem(storageKey) || defaultValue),
      set(v) { try { storage.setItem(storageKey, v); } catch (_) {} this.value = v; },
    };
  }
  function rememberPreferenceStore(key, defaultValue) {
    // KT: rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID) — STUB
    const storageKey = 'riff_' + key;
    const store = {
      get viewType() { return storage.getItem(storageKey) || defaultValue; },
      set viewType(v) { try { storage.setItem(storageKey, v); } catch (_) {} },
    };
    return store;
  }

  // KT: LaunchedEffect(Unit) { withContext(Dispatchers.IO) { ... } } — STUB: esecuzione immediata asincrona
  function launchIO(fn) { Promise.resolve().then(fn); }

  // KT: DisposableEffect(lifecycleOwner) { ON_RESUME / ON_PAUSE } — STUB: collegare a visibilita' della vista
  function disposableLifecycleEffect(handlers) { return handlers; }

  // KT: rememberSaveable { mutableStateOf(TextFieldValue(...)) } — STUB
  function rememberTextFieldValue(initial) {
    return { value: initial, set(v) { this.value = v; } };
  }
  // KT: FocusRequester() — STUB
  function rememberFocusRequester() { return { requestFocus() {} }; }

  // KT: extensions/StringUtils: normalizeForSearch() — STUB: minuscolo + senza accenti
  function normalizeForSearch(s) {
    return String(s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
  }
  // KT: matchesNormalizedQuery(normalizedQuery, vararg fields) — STUB
  function matchesNormalizedQuery(q, ...fields) {
    if (!q) return true;
    return fields.some((f) => normalizeForSearch(f).includes(q));
  }
  // KT: Iterable.distinctBy — ok
  function distinctBy(list, keyOf) {
    const seen = new Set(); const out = [];
    for (const x of list) { const k = keyOf(x); if (!seen.has(k)) { seen.add(k); out.push(x); } }
    return out;
  }

  // KT: LaunchedEffect(likedAlbumsSongs, isProcessingDownloads) — STUB: stato di download
  function downloadStateFor(likedSongs, isProcessing, downloadUtil) {
    if (isProcessing) return STATE_DOWNLOADING;
    if (!likedSongs || likedSongs.length === 0) return STATE_STOPPED;
    return STATE_STOPPED; // STUB: collegare a downloadUtil.downloads
  }

  // KT: ui/component/Dialog.kt:63 DefaultDialog — STUB (renderer dialog di app.js)
  function DefaultDialog(opts) { return opts; }

  // KT: ui/utils (rememberLazyGridState) — STUB
  function rememberLazyGridState() {
    return { animateScrollToItem() {} };
  }

  // KT: ui/component/Library.kt:190 LibraryContinueListeningAlbumItem — STUB
  function LibraryContinueListeningAlbumItem(p) { return p; }

  // KT: BoxWithConstraints + LazyRow (LibraryScreen.kt:551-572) — STUB
  function ContinueListeningRow(p) { return p; }

  // KT: ui/component/LibrarySectionLabel — STUB
  function LibrarySectionLabel(p) { return p; }

  // KT: ui/utils/IrideMotion.kt:190 rememberEnterProgress — STUB (0->1 con easing, dopo delayMillis)
  function rememberEnterProgress(p) { return p.durationMillis ? 1 : 1; }

  // KT: layout Box(fillMaxSize, graphicsLayer alpha) — STUB
  function Box(p) {
    return { alpha: p.alpha, fill: p.fill, children: [], append(c) { this.children.push(c); } };
  }
  // KT: CompositionLocalProvider — STUB
  function provideLocals(locals) {
    return { locals, wrap(node) { return node; } };
  }

  // KT: ui/component/LibraryGridPage.kt:26 LibraryGridPage — STUB: restituisce la pagina con le sue voci
  function LibraryGridPage(opts, builder) {
    const items = [];
    builder({ item(meta, render) { items.push({ meta, render }); } });
    return { opts, items };
  }

  // KT: ui/component/LibraryGridPage.kt:~90 libraryGridMenuActions — STUB
  function libraryGridMenuActions(viewType, onChange) {
    return [
      { label: 'Grid', selected: viewType !== 'GRID_WIDE', onClick: () => onChange('GRID') },
      { label: 'Dense grid', selected: viewType === 'GRID_WIDE', onClick: () => onChange('GRID_WIDE') },
    ];
  }
  // KT: ui/component/LibraryGridPage.kt librarySortMenuActions — STUB
  function librarySortMenuActions(options, current, onChange) {
    return options.map(([key, label]) => ({ label, selected: key === current, onClick: () => onChange(key) }));
  }

  // KT: LocalPlayerAwareWindowInsets bottom padding — STUB
  function playerAwareBottomPadding() { return 0; }

  // KT: LibraryScreen.kt:257 LibrarySearchAndActions — STUB
  function LibrarySearchAndActions(p) { return p; }
  function hideKeyboard() {}

  // KT: ui/component/LibraryPageTemplate.kt:76 LibraryPlaybackActions — STUB (pill Shuffle / In rotation)
  function LibraryPlaybackActions(p) { return p; }

  // KT: LibraryScreen.kt:636-661 onShuffle (monthly stats 85% weighted random) — STUB
  function playRandomAlbumWeighted(p) { return p; }
  // KT: LibraryScreen.kt:662-687 onRotation (rotation queue) — STUB
  function playRotationQueue(p) { return p; }

  // KT: ui/component/EmptyPlaceholder.kt:63 LibraryEmptyState — STUB
  function LibraryEmptyState(p) { return p; }
  // KT: ui/component/LibrarySearchHeader.kt:96 LibrarySearchEmptyPlaceholder — STUB
  function LibrarySearchEmptyPlaceholder(p) { return p; }

  // KT: ui/component/Library.kt:155 LibraryAlbumGridItem -> Items.kt:1222 AlbumGridItem — STUB
  function LibraryAlbumGridItem(p) { return p; }

  // KT: ui/component/LibraryFooterCount (LibraryPageTemplate.kt) — STUB
  function LibraryFooterCount(p) { return p; }

  global.IrideLibraryComponents = {
    IrideMotion, STATE_STOPPED, STATE_DOWNLOADING, STATE_QUEUED, STATE_COMPLETED,
    str, pluralStr, LocalMenuState,
    rememberPreference, rememberEnumPreference, rememberPreferenceStore,
    launchIO, disposableLifecycleEffect, rememberTextFieldValue, rememberFocusRequester,
    normalizeForSearch, matchesNormalizedQuery, distinctBy, downloadStateFor,
    DefaultDialog, rememberLazyGridState, LibraryContinueListeningAlbumItem,
    ContinueListeningRow, LibrarySectionLabel, rememberEnterProgress, Box, provideLocals,
    LibraryGridPage, libraryGridMenuActions, librarySortMenuActions, playerAwareBottomPadding,
    LibrarySearchAndActions, hideKeyboard, LibraryPlaybackActions,
    playRandomAlbumWeighted, playRotationQueue, LibraryEmptyState,
    LibrarySearchEmptyPlaceholder, LibraryAlbumGridItem, LibraryFooterCount,
  };
})(typeof window !== 'undefined' ? window : globalThis);
