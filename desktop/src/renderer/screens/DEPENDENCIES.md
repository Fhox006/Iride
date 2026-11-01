# Registro dipendenze da tradurre

Regola: quando una schermata chiama una funzione Kotlin che non è ancora tradotta, si scrive
UNO STUB con la stessa firma (stessi parametri, stesso ordine), il riferimento `KT: file:riga`
e lo stato in questa tabella. La schermata chiamante resta tradotta riga per riga. Lo stub si
riempie dopo, senza toccare il chiamante.

Stati: `STUB` = solo firma, `PARZIALE` = manca qualcosa (nota), `OK` = tradotto 1:1 e verificato.

| Funzione (JS) | Kotlin (file:riga) | Usata da | Stato | Nota |
|---|---|---|---|---|
| `str` | res/values/strings.xml | LibraryAlbumsScreen | STUB | da collegare a `t()` di app.js |
| `pluralStr` | pluralStringResource(n_album) | LibraryAlbumsScreen | STUB | |
| `LocalMenuState` | ui/component/Menu... | LibraryAlbumsScreen | STUB | |
| `rememberPreference` / `rememberEnumPreference` / `rememberPreferenceStore` | utils (preferenze), constants/PreferenceKeys.kt | LibraryAlbumsScreen | STUB | su localStorage |
| `launchIO` | LaunchedEffect + Dispatchers.IO | LibraryAlbumsScreen | STUB | |
| `disposableLifecycleEffect` | DisposableEffect(lifecycleOwner) | LibraryAlbumsScreen | STUB | |
| `rememberTextFieldValue` / `rememberFocusRequester` | rememberSaveable, FocusRequester | LibraryAlbumsScreen | STUB | |
| `normalizeForSearch` | extensions/StringUtils | LibraryAlbumsScreen | PARZIALE | minuscolo e senza accenti, da verificare con la versione Kotlin |
| `matchesNormalizedQuery` | extensions | LibraryAlbumsScreen | PARZIALE | |
| `distinctBy` | Iterable.distinctBy | LibraryAlbumsScreen | OK | |
| `downloadStateFor` | LibraryScreen.kt:457-485 | LibraryAlbumsScreen | STUB | ritorna sempre STOPPED |
| `DefaultDialog` | ui/component/Dialog.kt:63 | LibraryAlbumsScreen | STUB | |
| `rememberLazyGridState` | compose lazy grid | LibraryAlbumsScreen | STUB | |
| `LibraryContinueListeningAlbumItem` | ui/component/Library.kt:190 | LibraryAlbumsScreen | STUB | |
| `ContinueListeningRow` | LibraryScreen.kt:551-572 | LibraryAlbumsScreen | STUB | |
| `LibrarySectionLabel` | ui/component/LibraryPageTemplate.kt:~20 | LibraryAlbumsScreen | STUB | |
| `rememberEnterProgress` | ui/utils/IrideMotion.kt:190 | LibraryAlbumsScreen | OK | |
| `rubberBandOverscroll` | ui/component/RubberBandOverscroll.kt:74 | LibraryAlbumsScreen (futuro) | PARZIALE | manca RubberBandNavGate |
| `Box` | layout Box(fillMaxSize, graphicsLayer) | LibraryAlbumsScreen | STUB | |
| `provideLocals` | CompositionLocalProvider | LibraryAlbumsScreen | STUB | |
| `LibraryGridPage` | ui/component/LibraryGridPage.kt:26 | LibraryAlbumsScreen | STUB | |
| `libraryGridMenuActions` | ui/component/LibraryGridPage.kt | LibraryAlbumsScreen | STUB | |
| `librarySortMenuActions` | ui/component/LibraryGridPage.kt | LibraryAlbumsScreen | STUB | |
| `playerAwareBottomPadding` | LocalPlayerAwareWindowInsets | LibraryAlbumsScreen | STUB | |
| `LibrarySearchAndActions` | LibraryScreen.kt:257 | LibraryAlbumsScreen | STUB | |
| `LibraryPlaybackActions` | ui/component/LibraryPageTemplate.kt:76 | LibraryAlbumsScreen | STUB | |
| `playRandomAlbumWeighted` | LibraryScreen.kt:636-661 | LibraryAlbumsScreen | STUB | |
| `playRotationQueue` | LibraryScreen.kt:662-687 | LibraryAlbumsScreen | STUB | |
| `LibraryEmptyState` | ui/component/EmptyPlaceholder.kt:63 | LibraryAlbumsScreen | STUB | |
| `LibrarySearchEmptyPlaceholder` | ui/component/LibrarySearchHeader.kt:96 | LibraryAlbumsScreen | STUB | |
| `LibraryAlbumGridItem` | ui/component/Library.kt:155 → Items.kt:1222 | LibraryAlbumsScreen | STUB | |
| `LibraryFooterCount` | ui/component/LibraryPageTemplate.kt | LibraryAlbumsScreen | STUB | |

## HomeScreen (home-screen.js, parte 1: KT 219-664)

| Funzione (JS) | Kotlin (file:riga) | Usata da | Stato | Nota |
|---|---|---|---|---|
| `rememberEnterProgress` | ui/utils/IrideMotion.kt:190 | HomeScreen | STUB | ritorna 1 subito |
| `IrideTabEntrance` | IrideTabEntrance (oggetto) | HomeScreen | STUB | mai rivelato |
| `LocalLifecycleOwner` / `disposableLifecycleEffect` / `lifecycleIsAtLeastStarted` | compose lifecycle | HomeScreen | STUB | |
| `provideLocals` | CompositionLocalProvider | HomeScreen | STUB | esegue solo il body |
| `LocalMenuState` / `hiltViewModel` / `PreloadHomeCharts` | ui/component, hilt | HomeScreen | STUB | |
| `rememberSaveable` | compose saveable | HomeScreen | STUB | in memoria, non persiste |
| `randomInt` | kotlin.random.Random.nextInt | HomeScreen | OK | |
| `LocalPlayerConnection` / `LocalDatabase` / `LocalContext` / `LocalListenTogetherManager` | Local* | HomeScreen | STUB | ritornano null |
| `MutableStateFlow` | kotlinx.coroutines.flow | HomeScreen | PARZIALE | solo value |
| `collectDeferredHomeState` | ui/component (collectDeferredHomeState) | HomeScreen | STUB | passa il valore del flow |
| `rememberPreference` | utils | HomeScreen | PARZIALE | localStorage, solo boolean/stringa |
| `parseCookieString` | innertube/utils | HomeScreen | PARZIALE | solo chiavi |
| `appendLoadedHomeSections` | utils | HomeScreen | STUB | |
| `isInternetAvailable` | utils | HomeScreen | STUB | true |
| `delay` | kotlinx.coroutines | HomeScreen | OK | |
| `currentGridThumbnailHeight` | ui/component | HomeScreen | STUB | 160 fisso |
| `rememberLazyListState` / `rememberReducedMotion` | compose lazy / ui/utils | HomeScreen | STUB | |
| `toPx` | Dp -> Px | HomeScreen | STUB | 1 dp = 1 px |
| `remember` / `useState` / `launchEffect` / `launchRepeatOnLifecycle` / `rememberCoroutineScope` / `LocalViewModelStoreOwner` | compose | HomeScreen | STUB | effetti eseguiti una volta |
| `MoodMixes.chips` | discovery/MoodMixes | HomeScreen | STUB | passa i chip |
| `firstPlaylistItem` | HomeScreen.kt:523-532 | HomeScreen | STUB | null |

### HomeScreen parte 2 (home-screen.js: KT 668-1231 tradotte)

Tutte le altre funzioni `C.*` usate in parte 2 (Box, Column, Row, Text, Image, AsyncImage, LazyRow, item, Scaffold, BoxWithConstraints, YouTubeGridItem, YouTubeListItem, menu Youtube*/Song/Album/Artist/Playlist, IrideFolio*, rememberArtistThumbnail, rememberIrideArtistArtwork, rememberCollectionExpansionTile, resize, Modifier, FolioShape, ecc.) sono STUB generici in `home-components.js`: il Proxy le registra come `DA TRADURRE` e ritorna null. Non sono ancora elencate una per una in questa tabella.

### HomeScreen parte 3 (home-screen.js: KT 1233-2829 tradotte)

Tradotte: IridePageLayout (corpo pagina), hero carousel, picked for you, feed filtrato (addFeedSection), your mood, personal mixes, feed status, daily discover, community, similar, account, followed releases, sezione pending, charts, reverie, features. Helper top-level: IrideMoodChipsRow, HomeSimilarityTitle, HomeFeedLoadingShelf, IrideFeatureCard, SyncBanner, homeTestPlaylistSection (test, disattivato).

Stato: tutte le funzioni Compose (item, Column, Row, LazyRow, IrideFolio*, YouTube*Menu, ecc.) sono STUB generici (`DA TRADURRE`, ritornano null). HomeShelfOrder e' implementato (conteggio). Nessuna verifica a runtime con dati reali: la schermata NON e' collegata a index.html.

### Batch 1 implementazioni DOM (home-dom.js)

| Funzione | Kotlin | Stato | Nota |
|---|---|---|---|
| `Modifier` (catena: fillMaxWidth, width, height, size, padding, background, border, clip, align, weight, alpha, offset, clickable, combinedClickable, then, onGloballyPositioned) | androidx.compose.ui.Modifier | PARZIALE | stili CSS; offset/alpha parziali; effetti iride* = no-op |
| `MaterialTheme` (colorScheme, typography) | ui/theme | PARZIALE | palette scura fissa, da collegare ai colori Iride reali |
| `Box`, `Column`, `Row`, `Spacer` | Compose layout | OK | |
| `Text`, `Image`, `AsyncImage`, `Icon` | Compose / coil3 | PARZIALE | Icon senza glifo Material |
| `Surface`, `Card`, `Scaffold`, `BoxWithConstraints`, `CompositionLocalProvider` | Compose material3 | PARZIALE | |
| `item`, `LazyRow` | LazyListScope / LazyRow | PARZIALE | eager, nessuna virtualizzazione |
| `IridePageLayout` | ui/component | PARZIALE | titolo + overlay + body |
| `IrideFolioSection`, `IrideFolioShelfHeader`, `IrideFolioMediaShelf` | ui/component | PARZIALE | placeholder = skeleton |

Restano STUB (ancora 'DA TRADURRE'): menu (YouTube*Menu, SongMenu, AlbumMenu, ArtistMenu, PlaylistMenu), YouTubeGridItem, YouTubeListItem, SongListItem, SharedQuickPicksCarousel, SharedHomeSectionTitle, SharedYouTubePlaylistRow, HeroCarouselSection, PlaylistGridItem, ReverieHomeCard, ReverieAvailabilityCard, NewReleaseChartsShelf, HomeFollowedReleases, ItemThumbnail, rememberArtistThumbnail, rememberIrideArtistArtwork, rememberCollectionExpansionTile, resize, collectAsStateWithLifecycle, dati del ViewModel e tutte le preferenze/Local*.

### Batch 2 (10 dipendenze, home-dom.js)

| Funzione | Kotlin | Stato | Nota |
|---|---|---|---|
| `resize` | String.resize (Coil) | PARZIALE | identita'; ridimensionamento URL Kotlin da portare |
| `ItemThumbnail` | ui/component | PARZIALE | img, senza hairline/isActive |
| `YouTubeGridItem` | ui/component/Items.kt | PARZIALE | card copertina+titolo+artisti; niente menu |
| `YouTubeListItem` / `SongListItem` | ui/component | PARZIALE | riga con trailing; niente swipe |
| `SharedQuickPicksCarousel` | ui/component | PARZIALE | colonne da minimumRows, niente pager |
| `SharedHomeSectionTitle` | ui/component | OK | |
| `SharedYouTubePlaylistRow` | ui/component | PARZIALE | LazyRow |
| `PlaylistGridItem` | ui/component | PARZIALE | niente autoPlaylist |
| `ReverieHomeCard` | ui/component | PARZIALE | solo mese, nessun grafico |

### Batch 3 (10 dipendenze, home-dom.js)

| Funzione | Kotlin | Stato | Nota |
|---|---|---|---|
| `HeroCarouselSection` | ui/component | PARZIALE | card artista con copertina |
| `ReverieAvailabilityCard` | ui/component | PARZIALE | testo, senza logica date reale |
| `NewReleaseChartsShelf` | screens/playlist | STUB | nessuna sorgente classifiche nel desktop |
| `HomeFollowedReleases` | ui/component | STUB | nessuna sorgente dati nel desktop |
| `rememberArtistThumbnail` | ui/component | PARZIALE | restituisce l'URL, nessuna cache |
| `rememberIrideArtistArtwork` | ui/component | STUB | ritorna null |
| `rememberCollectionExpansionTile` | ui/component | STUB | modificatori vuoti |
| `rememberArtistPortraitTile` | ui/component | STUB | modificatori vuoti |
| `SongMenu` | ui/menu | PARZIALE | titolo + chiudi, niente azioni |
| `YouTubeSongMenu` | ui/menu | PARZIALE | titolo + chiudi, niente azioni |

### Layer dati HomeScreen (home-data.js)

| Flusso | Fonte | Stato | Nota |
|---|---|---|---|
| `homePage` (sezioni + chip umore) | `/api/home-feed` (server.js, Innertube FEmusic_home) | PARZIALE | mappa item kind→tipo Kotlin; niente endpoint di sezione |
| `quickPicks`, `forYouShelves`, `homeMoodContent`, `dailyDiscover`, `communityPlaylists`, `similarRecommendations`, `accountPlaylists`, `homePersonalMixes`, `heroCarouselItems` | — | STUB | liste vuote: serve una route dedicata nel server |
| `syncState`, `accountName`, flag di caricamento | — | STUB | valori fissi |
