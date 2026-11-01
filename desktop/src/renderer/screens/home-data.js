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
    const chips = (data.chips || []).map((c) => ({ title: c.title, endpoint: { browseId: 'FEmusic_home', params: c.params || null }, deselectEndPoint: null }));
    return { sections, chips };
  }

  // Chiama il server: ritorna HomePage oppure null se non disponibile.
  async function fetchHomePage() {
    try {
      const res = await fetch(`http://127.0.0.1:${serverPort()}/api/home-feed`);
      const data = await res.json();
      if (!data || !data.success) return null;
      return mapHomePage(data);
    } catch (_) {
      return null;
    }
  }

  // ViewModel minimo: stessi nomi dei flussi di HomeViewModel.kt usati da HomeScreen.
  // I flussi non ancora alimentati restano null / vuoti (nessun dato inventato).
  async function createHomeViewModel() {
    const homePage = await fetchHomePage();
    const loaded = homePage !== null;
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
      quickPicks: flow([]),
      forYouShelves: flow([]),
      isHeroCarouselEnabled: flow(false),
      isHeroCarouselLoading: flow(false),
      heroCarouselItems: flow([]),
      accountName: flow('Guest'),
      syncState: flow({ overallStatus: 'Idle', currentOperation: '', likedSongs: 'Idle', librarySongs: 'Idle', uploadedSongs: 'Idle', likedAlbums: 'Idle', uploadedAlbums: 'Idle', artists: 'Idle', playlists: 'Idle' }),
      syncBannerLaunchCount: flow(0),
      homeMoodContent: flow({ items: [], posters: {}, params: null }),
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
      loadMoodPage() {}, loadHomeData() {}, loadMoreForYouShelves() {}, onSectionBecameVisible() {},
      repairHeroRadioCover() {}, loadTestPlaylistArtwork() {},
      moodPosterImageLoader: null,
    };
    return vm;
  }

  global.IrideHomeData = { createHomeViewModel, mapHomePage, mapItem, fetchHomePage };
})(typeof window !== 'undefined' ? window : globalThis);
