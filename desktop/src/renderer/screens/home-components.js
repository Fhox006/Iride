/*
 * Dipendenze di home-screen.js (HomeScreen).
 * Ogni funzione porta il riferimento Kotlin di origine (file:riga, tag v0.1.0-alpha33).
 * Stato "STUB": firma Kotlin, corpo provvisorio da completare nel passo di collegamento.
 * Il registro e' in DEPENDENCIES.md.
 */
(function (global) {
  'use strict';

  const warned = {};
  function stub(name, ref, fallback) {
    return function () {
      if (!warned[name]) { warned[name] = true; console.warn('[STUB] ' + name + ' (' + ref + ')'); }
      return typeof fallback === 'function' ? fallback.apply(null, arguments) : fallback;
    };
  }

  const mem = {};
  const storage = (typeof localStorage !== 'undefined') ? localStorage : {
    getItem(k) { return k in mem ? mem[k] : null; }, setItem(k, v) { mem[k] = String(v); },
  };

  // KT: ui/utils/IrideMotion.kt  object IrideMotion (valori copiati dal Kotlin)
  const IrideMotion = {
    EaseOutExpo: [0.16, 1, 0.3, 1],
    EaseOutQuart: [0.25, 1, 0.5, 1],
    Quick: 160, Short: 240, Medium: 300, Long: 420, Fullscreen: 520, StaggerStep: 40,
  };

  // KT: ui/utils/IrideMotion.kt:190  rememberEnterProgress -- STUB (ritorna 1 subito)
  const rememberEnterProgress = stub('rememberEnterProgress', 'IrideMotion.kt:190', 1);

  // KT: IrideTabEntrance (object) -- STUB: mai rivelato
  const IrideTabEntrance = {
    wasRevealed: stub('IrideTabEntrance.wasRevealed', 'IrideTabEntrance', false),
    markRevealed: stub('IrideTabEntrance.markRevealed', 'IrideTabEntrance', undefined),
  };

  // KT: HomeScreen.kt:393-391 composizione: marca "home" come rivelato quando progress >= 1
  function markRevealedWhenDone(progress, key) {
    if (progress >= 1) IrideTabEntrance.markRevealed(key); // KT:648
    return progress;
  }

  // KT: LocalLifecycleOwner -- STUB
  const LocalLifecycleOwner = stub('LocalLifecycleOwner', 'compose lifecycle', () => ({
    lifecycle: { currentState: 'RESUMED', addObserver() {}, removeObserver() {} },
  }));
  // KT: DisposableEffect(lifecycle, viewModel) -- STUB (registra solo onDispose)
  function disposableLifecycleEffect(lifecycle, key, onEvent, onDispose) {
    return stub('disposableLifecycleEffect', 'DisposableEffect', () => onDispose)();
  }
  // KT: lifecycle.currentState.isAtLeast(STARTED) -- STUB
  const lifecycleIsAtLeastStarted = stub('lifecycleIsAtLeastStarted', 'Lifecycle.State', true);

  // KT: CompositionLocalProvider -- STUB (esegue solo il body)
  const provideLocals = stub('provideLocals', 'CompositionLocalProvider', (locals, body) => body());

  // KT: ui/component/LocalMenuState -- STUB
  const LocalMenuState = stub('LocalMenuState', 'ui/component/Menu', () => ({ show() {} }));
  // KT: hiltViewModel -- STUB (ritorna oggetto vuoto)
  const hiltViewModel = stub('hiltViewModel', 'hilt', (name) => ({ name, archiveMonths: () => null, firstFullMonth: () => null }));
  // KT: PreloadHomeCharts(chartViewModel) -- STUB
  const PreloadHomeCharts = stub('PreloadHomeCharts', 'ui/screens/playlist', () => {});
  // KT: rememberSaveable { init } -- STUB (in memoria, non persiste)
  const rememberSaveable = stub('rememberSaveable', 'compose saveable', (key, init) => init());
  // KT: kotlin.random.Random.nextInt(from, until) -- funzionante
  function randomInt(from, until) { return from + Math.floor(Math.random() * (until - from)); }

  // KT: LocalPlayerConnection / LocalDatabase / LocalContext / LocalListenTogetherManager -- STUB (null)
  const LocalPlayerConnection = stub('LocalPlayerConnection', 'LocalPlayerConnection', null);
  const LocalDatabase = stub('LocalDatabase', 'LocalDatabase', null);
  const LocalContext = stub('LocalContext', 'compose platform', null);
  const LocalListenTogetherManager = stub('LocalListenTogetherManager', 'LocalListenTogetherManager', null);

  // KT: kotlinx.coroutines.flow.MutableStateFlow(v) -- STUB minimale (value + collect)
  function MutableStateFlow(v) { return { value: v, collect() {} }; }

  // KT: collectDeferredHomeState(key, hasContent, identity, mergeWhileVisible) -- STUB (passa il valore)
  function collectDeferredHomeState(flow, key, opts) {
    return stub('collectDeferredHomeState', 'ui/component/collectDeferredHomeState', (f) => (f && f.value !== undefined ? f.value : f))(flow);
  }

  // KT: utils/rememberPreference(key, default) -- STUB (localStorage, come da registro)
  function rememberPreference(key, defaultValue) {
    try {
      const raw = storage.getItem(key);
      if (raw === null) return defaultValue;
      return typeof defaultValue === 'boolean' ? raw === 'true' : raw;
    } catch (e) { return defaultValue; }
  }
  // KT: innertube/utils parseCookieString -- STUB (solo chiavi)
  function parseCookieString(cookie) {
    const set = new Set();
    String(cookie || '').split(';').forEach((p) => { const k = p.split('=')[0].trim(); if (k) set.add(k); });
    return set;
  }

  // KT: utils/appendLoadedHomeSections -- STUB (concatena senza duplicati)
  const appendLoadedHomeSections = stub('appendLoadedHomeSections', 'utils', (cur, inc, key) => {
    const seen = new Set(cur.map(key));
    return cur.concat(inc.filter((x) => !seen.has(key(x))));
  });
  // KT: utils/isInternetAvailable -- STUB (true)
  const isInternetAvailable = stub('isInternetAvailable', 'utils', true);
  // KT: kotlinx.coroutines.delay -- funzionante
  function delay(ms) { return new Promise((r) => setTimeout(r, ms)); }

  // KT: ui/component/currentGridThumbnailHeight -- STUB (valore fisso)
  const currentGridThumbnailHeight = stub('currentGridThumbnailHeight', 'ui/component', 160);
  // KT: rememberLazyListState -- STUB (oggetto con scrollToItem)
  const rememberLazyListState = stub('rememberLazyListState', 'compose lazy', () => ({
    scrollToItem: async () => {}, animateScrollToItem: async () => {},
  }));
  // KT: rememberReducedMotion -- STUB (false)
  const rememberReducedMotion = stub('rememberReducedMotion', 'ui/utils', false);

  // KT: Dp -> px (1:1 con density). STUB: 1 dp = 1 px
  function toPx(dp) { return dp; }
  function clamp(v, lo, hi) { return Math.min(Math.max(v, lo), hi); }
  // KT: Iterable.associateBy -- funzionante
  function associateBy(list, keyFn) { const m = {}; list.forEach((x) => { m[keyFn(x)] = x; }); return m; }
  // KT: data class copy(...) -- funzionante
  function copy(obj, patch) { return Object.assign({}, obj, patch); }
  // KT: compose remember(keys) { init } -- STUB (calcolo diretto, nessuna memoria)
  const remember = stub('remember', 'compose', (keys, init) => init());
  // KT: mutableStateOf(init) -- STUB (box con .value)
  function useState(name, init) { return { value: init }; }
  // KT: LaunchedEffect(keys) { block } -- STUB (esegue una volta)
  const launchEffect = stub('launchEffect', 'compose', (keys, block) => block());
  // KT: repeatOnLifecycle(STARTED) { block } -- STUB (esegue una volta)
  const launchRepeatOnLifecycle = stub('launchRepeatOnLifecycle', 'lifecycle', (lc, state, block) => block());
  // KT: rememberCoroutineScope -- STUB
  const rememberCoroutineScope = stub('rememberCoroutineScope', 'compose', () => ({ launch(fn) { return fn(); } }));
  // KT: LocalViewModelStoreOwner as NavBackStackEntry -- STUB (null)
  const LocalViewModelStoreOwner = stub('LocalViewModelStoreOwner', 'compose', null);
  // KT: collectLatest -- STUB (non funzionante)
  // (non usato direttamente: chiamato come viewModel.isMoodError.collectLatest)

  // KT: ui/component/MoodMixes.chips -- STUB (passa attraverso i chip)
  const MoodMixes = {
    chips: stub('MoodMixes.chips', 'discovery/MoodMixes', (chips) => chips || []),
  };
  // KT: HomeScreen.kt:523-532 primo PlaylistItem nella feed -- STUB (null)
  const firstPlaylistItem = stub('firstPlaylistItem', 'HomeScreen.kt:523', null);

  // KT: HomeScreen.kt:553 -- nessun composable qui
  global.IrideHomeComponents = {
    IrideMotion, rememberEnterProgress, IrideTabEntrance, markRevealedWhenDone,
    LocalLifecycleOwner, disposableLifecycleEffect, lifecycleIsAtLeastStarted, provideLocals,
    LocalMenuState, hiltViewModel, PreloadHomeCharts, rememberSaveable, randomInt,
    LocalPlayerConnection, LocalDatabase, LocalContext, LocalListenTogetherManager,
    MutableStateFlow, collectDeferredHomeState, rememberPreference, parseCookieString,
    appendLoadedHomeSections, isInternetAvailable, delay, currentGridThumbnailHeight,
    rememberLazyListState, rememberReducedMotion, toPx, clamp, associateBy, copy,
    remember, useState, launchEffect, launchRepeatOnLifecycle, rememberCoroutineScope,
    LocalViewModelStoreOwner, MoodMixes, firstPlaylistItem,
  };
})(typeof window !== 'undefined' ? window : globalThis);
