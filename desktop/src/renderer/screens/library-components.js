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

  // KT: ui/utils/IrideMotion.kt:190 rememberEnterProgress — ok
  // Curva di Bezier cubica come CubicBezierEasing(x1,y1,x2,y2) di Compose (bisezione sulla x).
  function cubicBezier(x1, y1, x2, y2) {
    return (t) => {
      let lo = 0, hi = 1, u = t;
      for (let i = 0; i < 28; i++) {
        u = (lo + hi) / 2;
        const x = 3 * (1 - u) * (1 - u) * u * x1 + 3 * (1 - u) * u * u * x2 + u * u * u;
        if (x < t) lo = u; else hi = u;
      }
      return 3 * (1 - u) * (1 - u) * u * y1 + 3 * (1 - u) * u * u * y2 + u * u * u;
    };
  }
  const reducedMotion = () => typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
  // Restituisce un handle con .value (0..1). play=false -> 0 finche' non diventa true.
  // Reduced motion -> 1 subito (come KT: return if (play) 1f else 0f).
  function rememberEnterProgress(p) {
    const easing = Array.isArray(p.easing) ? cubicBezier(...p.easing) : (p.easing || cubicBezier(...IrideMotion.EaseOutExpo));
    const dur = p.durationMillis || IrideMotion.Medium;
    const delay = p.delayMillis || 0;
    const handle = { value: 0, onChange: null };
    const set = (v) => { handle.value = v; if (handle.onChange) handle.onChange(v); };
    if (!p.play) return handle;
    if (reducedMotion()) { set(1); return handle; }
    let t0 = null;
    const frame = (now) => {
      if (t0 === null) t0 = now;
      const elapsed = now - t0 - delay;
      if (elapsed <= 0) { requestAnimationFrame(frame); return; }
      const k = Math.min(1, elapsed / dur);
      set(easing(k));
      if (k < 1) requestAnimationFrame(frame);
    };
    requestAnimationFrame(frame);
    return handle;
  }

  // KT: ui/component/RubberBandOverscroll.kt:74 Modifier.rubberBandOverscroll — ok (senza NavGate)
  // Costanti: IrideFolio.RubberBandResistance = 0.55, RubberBandDamping = 1, RubberBandStiffness = StiffnessLow (200).
  const RB_RESISTANCE = 0.55;
  const RB_STIFFNESS = 200;
  // Offset visibile dal tiro: sign(r) * (|r| * d * R) / (d + R * |r|)   [RubberBandPull.offset]
  function rubberBandOffset(raw, dimension) {
    if (raw === 0 || dimension <= 0) return 0;
    const ar = Math.abs(raw);
    return Math.sign(raw) * (ar * dimension * RB_RESISTANCE) / (dimension + RB_RESISTANCE * ar);
  }
  // Molla a smorzamento critico (dampingRatio 1, stiffness StiffnessLow): x(t) = (x0 + (v0 + w*x0) t) e^{-w t}
  function criticalSpring(x0, v0, k, t) {
    const w = Math.sqrt(k);
    return (x0 + (v0 + w * x0) * t) * Math.exp(-w * t);
  }
  // Applica il rubber band a un contenitore scrollabile verticale (o orizzontale).
  // target = elemento da traslare (translateY/translateX). Ritorna { detach(), pull }.
  function rubberBandOverscroll(scrollEl, target, orientation) {
    const vertical = orientation !== 'Horizontal';
    const pull = { raw: 0, dimension: 0 };
    let settleRaf = 0;
    const apply = () => {
      const off = rubberBandOffset(pull.raw, pull.dimension);
      target.style.transform = off ? (vertical ? `translateY(${off}px)` : `translateX(${off}px)`) : '';
    };
    // onPreScroll / onPostScroll: il tiro riduce prima un tiro opposto, poi si accumula ai bordi
    const onWheel = (e) => {
      pull.dimension = vertical ? scrollEl.clientHeight : scrollEl.clientWidth;
      const delta = -(vertical ? e.deltaY : e.deltaX);
      const atTop = vertical ? scrollEl.scrollTop <= 0 : scrollEl.scrollLeft <= 0;
      const atEnd = vertical
        ? scrollEl.scrollTop + scrollEl.clientHeight >= scrollEl.scrollHeight - 1
        : scrollEl.scrollLeft + scrollEl.clientWidth >= scrollEl.scrollWidth - 1;
      const cur = pull.raw;
      if (cur !== 0 && delta !== 0 && Math.sign(delta) !== Math.sign(cur)) {
        pull.raw = cur > 0 ? Math.max(0, cur + delta) : Math.min(0, cur + delta);
      } else if ((delta > 0 && atTop) || (delta < 0 && atEnd)) {
        pull.raw = cur + delta;
      } else {
        return;
      }
      e.preventDefault();
      cancelAnimationFrame(settleRaf);
      apply();
      clearTimeout(onWheel._idle);
      onWheel._idle = setTimeout(() => settle(0), 90); // onPreFling/onPostFling senza velocita' nel wheel
    };
    // onPreFling / onPostFling: ritorno a zero con molla
    const settle = (v0) => {
      cancelAnimationFrame(settleRaf);
      if (pull.raw === 0) return;
      if (reducedMotion()) { pull.raw = 0; apply(); return; }
      const x0 = pull.raw;
      const t0 = performance.now();
      const frame = (now) => {
        pull.raw = criticalSpring(x0, v0, RB_STIFFNESS, (now - t0) / 1000);
        if (Math.abs(pull.raw) < 0.01) { pull.raw = 0; apply(); return; }
        apply();
        settleRaf = requestAnimationFrame(frame);
      };
      settleRaf = requestAnimationFrame(frame);
    };
    scrollEl.addEventListener('wheel', onWheel, { passive: false });
    return {
      pull,
      settle,
      detach() { scrollEl.removeEventListener('wheel', onWheel); cancelAnimationFrame(settleRaf); },
    };
  }

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
    ContinueListeningRow, LibrarySectionLabel, rememberEnterProgress, rubberBandOverscroll,
    rubberBandOffset, criticalSpring, cubicBezier, Box, provideLocals,
    LibraryGridPage, libraryGridMenuActions, librarySortMenuActions, playerAwareBottomPadding,
    LibrarySearchAndActions, hideKeyboard, LibraryPlaybackActions,
    playRandomAlbumWeighted, playRotationQueue, LibraryEmptyState,
    LibrarySearchEmptyPlaceholder, LibraryAlbumGridItem, LibraryFooterCount,
  };
})(typeof window !== 'undefined' ? window : globalThis);
