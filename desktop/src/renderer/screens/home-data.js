/*
 * Layer dati della HOME tradotta (home-screen.js).
 * Sostituisce il ViewModel Kotlin HomeViewModel: fornisce gli stessi flussi usati da HomeScreen.
 * Fonte dati: server locale del desktop, route /api/home-feed (Innertube FEmusic_home, gia' in server.js).
 *
 * Stato: PRIMO PASSO. Copre il flusso homePage (sezioni del feed) e i chip dell'umore.
 * Gli altri flussi (quickPicks, forYou, mood, dailyDiscover, community, similar, account, hero, mixes)
 * restano vuoti finche' non vengono aggiunte le route dedicate nel server.
 */
(function (global) {
  'use strict';

  // Porta del server locale: stesso default di app.js (38472).
  function serverPort() { return global.__riffServerPort || 38472; }

  // Flusso minimo compatibile con collectDeferredHomeState (espone .value e le funzioni usate dalla UI).
  function flow(v) {
    const g = function () { return v; };
    g.value = v;
    g.collectLatest = async function () {};
    return g;
  }

  // Mappa un item del parser del server (kind/id/title/sub/thumbnail) nei tipi Kotlin usati da HomeScreen.
  function mapItem(it) {
    const artists = it.sub ? [{ name: it.sub, id: null }] : [];
    if (it.kind === 'track') return { type: 'SongItem', id: it.id, title: it.title, thumbnail: it.thumbnail || '', artists, isVideoSong: false, endpoint: null };
    if (it.kind === 'album') return { type: 'AlbumItem', id: it.id, title: it.title, thumbnail: it.thumbnail || '', artists, albumType: null };
    if (it.kind === 'artist') return { type: 'ArtistItem', id: it.id, title: it.title, thumbnail: it.thumbnail || '', artist: { name: it.title } };
    return { type: 'PlaylistItem', id: it.id, title: it.title, thumbnail: it.thumbnail || '', artists };
  }

  // Mappa una risposta /api/home-feed in HomePage { sections, chips } (forma usata da HomeScreen).
  function mapHomePage(data) {
    const sections = (data.sections || []).map((s) => ({
      title: s.title || '',
      label: '',
      thumbnail: null,
      endpoint: null,
      items: (s.items || []).map(mapItem).filter(Boolean),
    }));
    // KT: HomeViewModel.transformChips -- senza chip "home" e "podcast" (ordine del feed, senza shuffle)
    const chips = (data.chips || []).filter((c) => {
      const t = String(c.title || '').toLowerCase();
      return !t.includes('home') && !t.includes('podcast');
    }).map((c) => ({ title: c.title, endpoint: { browseId: 'FEmusic_home', params: c.params || null }, deselectEndPoint: null }));
    return { sections, chips };
  }

  // Chiama il server: ritorna HomePage oppure null se non disponibile.
  async function fetchHomePage(params) {
    try {
      const q = params ? `?params=${encodeURIComponent(params)}` : '';
      const res = await fetch(`http://127.0.0.1:${serverPort()}/api/home-feed${q}`);
      const data = await res.json();
      if (!data || !data.success) return null;
      return mapHomePage(data);
    } catch (_) {
      return null;
    }
  }


  // Storico locale del desktop (app.js: localStorage 'devsize_history', max 100, piu' recente prima).
  // KT: HomeViewModel.quickPicks -> database.quickPicks() (Room). Qui: stesso ordine, senza conteggi.
  function readLocalHistory() {
    try {
      const raw = global.localStorage ? global.localStorage.getItem('devsize_history') : null;
      const list = raw ? JSON.parse(raw) : [];
      return Array.isArray(list) ? list.filter((t) => t && t.id) : [];
    } catch (_) { return []; }
  }
  // Mappa una traccia dello storico nel Song usato da quickPicks (campi letti da HomeScreen: id, title, song.*).
  function mapHistoryToSong(t) {
    const artist = t.artist || t.artistName || '';
    return {
      id: String(t.id),
      title: t.title || '',
      artists: artist ? [{ name: artist, id: null }] : [],
      thumbnail: t.thumbnail || t.cover || '',
      song: { id: String(t.id), title: t.title || '', thumbnailUrl: t.thumbnail || t.cover || '', isVideo: false, isEpisode: false },
    };
  }
  // KT: quickPicks = database.quickPicks() -- PARZIALE: ultime tracce uniche dello storico locale.
  function quickPicksFromHistory() {
    return readLocalHistory().map(mapHistoryToSong);
  }



  // KT: HeroCarouselGenerator -- PARZIALE: artisti dello storico locale (primi per ordine di ascolto), copertina = traccia.
  function buildHeroCarousel() {
    const seen = new Map();
    readLocalList('devsize_history').forEach((t) => {
      const name = String(t.artist || t.artistName || '').trim();
      if (!name || seen.has(name)) return;
      seen.set(name, { type: 'ArtistRadio', artistId: 'artist:' + name, artistName: name, coverUrl: t.thumbnail || t.cover || '' });
    });
    return Array.from(seen.values()).slice(0, 8);
  }

  // Lista locale del desktop (localStorage), vuota se assente o non valida.
  function readLocalList(key) {
    try {
      const raw = global.localStorage ? global.localStorage.getItem(key) : null;
      const list = raw ? JSON.parse(raw) : [];
      return Array.isArray(list) ? list.filter((t) => t && t.id) : [];
    } catch (_) { return []; }
  }
  // KT: ForYouShelfTile.Local(item) -- la tessera e' una traccia (Song), perche' il desktop non ha album locali.
  function mapTrackToCell(t) {
    const artist = t.artist || t.artistName || '';
    return {
      type: 'Song',
      id: String(t.id),
      title: t.title || '',
      thumbnailUrl: t.thumbnail || t.cover || '',
      artists: artist ? [{ name: artist, id: null }] : [],
      song: { id: String(t.id), title: t.title || '', thumbnailUrl: t.thumbnail || t.cover || '', isVideo: false, isEpisode: false },
      endpoint: null,
    };
  }
  // KT: Artist (cella artista dello shelf). Chiave sintetica: il desktop non ha id artista locali.
  function mapArtistCell(name, t) {
    return {
      type: 'Artist',
      id: 'artist:' + name,
      title: name,
      thumbnailUrl: (t && (t.thumbnail || t.cover)) || '',
      artist: { name, channelId: null, isLocal: true, isPodcastChannel: false, isPrivatelyOwnedArtist: false },
    };
  }
  // KT: getForYouShelves / buildForYouShelf -- PARZIALE: pool = artisti dello storico e dei preferiti,
  // tessere = tracce distinte dell'artista (servono 3 per fare uno shelf, come nel Kotlin).
  function buildForYouShelves() {
    const all = readLocalList('devsize_history').concat(readLocalList('devsize_favorites'));
    const order = [];
    const byArtist = new Map();
    all.forEach((t) => {
      const name = String(t.artist || t.artistName || '').trim();
      if (!name) return;
      if (!byArtist.has(name)) { byArtist.set(name, []); order.push(name); }
      byArtist.get(name).push(t);
    });
    const shelves = [];
    for (const name of order) {
      const seen = new Set();
      const tiles = [];
      for (const t of byArtist.get(name)) {
        if (seen.has(t.id)) continue;
        seen.add(t.id);
        tiles.push({ type: 'Local', item: mapTrackToCell(t) });
        if (tiles.length === 3) break;
      }
      if (tiles.length < 3) continue;
      shelves.push({ artist: mapArtistCell(name, byArtist.get(name)[0]), tiles });
      if (shelves.length >= 20) break;
    }
    return shelves;
  }

  // ViewModel minimo: stessi nomi dei flussi di HomeViewModel.kt usati da HomeScreen.
  // I flussi non ancora alimentati restano null / vuoti (nessun dato inventato).
  async function createHomeViewModel() {
    const homePage = await fetchHomePage();
    const loaded = homePage !== null;
    const firstChip = homePage && homePage.chips && homePage.chips[0];
    const initialMood = (firstChip && firstChip.endpoint && firstChip.endpoint.params
      ? await fetchMoodItems(firstChip.endpoint.params) : null) || { items: [], posters: {}, params: null };
    const vm = {
      homePage: flow(homePage),
      isHomeFeedLoading: flow(false),
      isForYouLoading: flow(false),
      isAccountPlaylistsLoading: flow(false),
      phase1Complete: flow(true),
      phase2DailyDiscoverDone: flow(true),
      phase2CommunityDone: flow(true),
      phase2SimilarDone: flow(true),
      communityPlaylists: flow([]),
      dailyDiscover: flow([]),
      accountPlaylists: flow([]),
      homePersonalMixes: flow([]),
      starredPlaylistThumbnails: flow([]),
      similarRecommendations: flow([]),
      quickPicks: flow(quickPicksFromHistory()),
      forYouShelves: flow(buildForYouShelves()),
      isHeroCarouselEnabled: flow(true),
      isHeroCarouselLoading: flow(false),
      heroCarouselItems: flow(buildHeroCarousel()),
      accountName: flow('Guest'),
      syncState: flow({ overallStatus: 'Idle', currentOperation: '', likedSongs: 'Idle', librarySongs: 'Idle', uploadedSongs: 'Idle', likedAlbums: 'Idle', uploadedAlbums: 'Idle', artists: 'Idle', playlists: 'Idle' }),
      syncBannerLaunchCount: flow(0),
      homeMoodContent: flow(initialMood),
      isMoodLoading: flow(false),
      isMoodError: flow(!loaded),
      preferredMoodParams: flow(null),
      isMoodPreferenceReady: flow(true),
      cachedMoodSnapshot: flow(null),
      testPlaylistTrackThumbnails: flow([]),
      contentSnapshot: { setVisible() {} },
      archiveMonths: flow([]),
      firstFullMonth: flow(null),
      // azioni (no-op finche' i flussi non sono collegati)
      // loadMoodPage: carica la pagina del chip e ridisegna solo se i parametri cambiano
      loadMoodPage(params) {
        if (!params || (vm.homeMoodContent.value && vm.homeMoodContent.value.params === params)) return;
        fetchMoodItems(params).then((content) => {
          if (!content) return;
          vm.homeMoodContent = flow(content);
          if (vm.__rerender) vm.__rerender();
        });
      },
      loadHomeData() {}, loadMoreForYouShelves() {}, onSectionBecameVisible() {},
      repairHeroRadioCover() {}, loadTestPlaylistArtwork() {},
      moodPosterImageLoader: null,
    };
    return vm;
  }



  // KT: homeMoodContent -- pagina di un chip umore: elementi playlist (mix) della pagina, in ordine.
  async function fetchMoodItems(params) {
    const page = await fetchHomePage(params);
    if (!page) return null;
    const items = [];
    page.sections.forEach((sec) => sec.items.forEach((it) => { if (it.type === 'PlaylistItem' || it.type === 'AlbumItem') items.push(it); }));
    return { items, posters: {}, params };
  }

  // Monta la HOME tradotta dentro un contenitore DOM. Ritorna il nodo montato o null.
  async function mountHomeScreen(container, options) {
    const vm = await createHomeViewModel();
    const navController = (options && options.navController) || { navigate() {} };
    let current = null;
    // Ridisegna la schermata intera (la traduzione non ha un sistema di stato reattivo).
    const render = () => {
      const node = global.IrideHomeScreen.HomeScreen({ navController, snackbarHostState: null, viewModel: vm });
      if (!node || node.nodeType !== 1) return null;
      if (current && current.parentNode === container) container.replaceChild(node, current);
      else container.appendChild(node);
      current = node;
      return node;
    };
    vm.__rerender = render;
    return render();
  }

  global.IrideHomeData = { createHomeViewModel, mapHomePage, mapItem, fetchHomePage, mountHomeScreen, quickPicksFromHistory, buildForYouShelves, fetchMoodItems, buildHeroCarousel };
})(typeof window !== 'undefined' ? window : globalThis);
