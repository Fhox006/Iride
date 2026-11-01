# Iride Desktop: istruzioni per Claude

App Electron (Windows/Linux/macOS) con UI in `src/renderer/`. Lavora solo in `desktop/` salvo richiesta esplicita.

## Stile di risposta
- Italiano semplice, frasi brevi, niente gergo tecnico. Se serve un termine tecnico, spiegalo in una frase.

## Git
- Branch di lavoro: `desktop`. Niente PR se non richiesta esplicitamente.
- Prima di modificare: `git fetch origin && git rebase origin/desktop`.
- Commit e push solo su quel branch: `git push -u origin desktop`.
- Mai `force push`, mai `reset --hard` su commit non tuoi.

## Struttura
- `src/main/main.js`: processo principale, finestra, IPC. Flag debug: `RIFF_DEBUG=1`, flag Chromium extra: `RIFF_FLAGS="a;b=c"`.
- `src/renderer/index.html`: ordine di caricamento CSS/JS. I CSS sovrascrivono in quest'ordine: `styles.css` → `iride-liner.css` → `iride-folio.css` → `iride-folio-surface.css`.
- `src/renderer/screens/`: traduzioni Kotlin → JS (`library-albums.js`, `library-components.js`). Non ancora caricate da `index.html`.

## Comandi
- Dipendenze: `npm install` (in `desktop/`).
- Avvio: `npm start`.
- Test: `npm test` (runner `node --test`, file in `test/`).
- Verifica UI in Linux senza display: `xvfb-run -a node_modules/.bin/electron --no-sandbox .` con `RIFF_FLAGS="remote-debugging-port=9222"`, poi screenshot via Playwright `connectOverCDP`. Imposta `HOME` su una cartella temporanea con file `.rifftutorial` (contenuto = versione in `package.json`) per saltare il welcome.

## Regole di modifica
- Il codice Android (`app/`) si legge soltanto, con `git show v0.1.0-alpha33:<file>`. Non modificarlo.
- Dopo modifiche al CSS/JS, verifica con uno screenshot prima di committare.
- Non introdurre dipendenze nuove senza richiesta.

## Checklist 1:1 schermate (da v0.1.0-alpha33)

Regola: una schermata alla volta. Segna `[x]` solo dopo conversione 1:1 e screenshot verificato. Se parziale scrivi `[~]` con il motivo. Fonte: `git show v0.1.0-alpha33:<file>`.

Tipo di lavoro: TRADUZIONE RIGA PER RIGA, NON interpretazione.
- Ogni riga Kotlin corrisponde a una riga JS, nello stesso ordine. Un blocco Kotlin (`if`, `when`, `LaunchedEffect`, `item {}`) diventa un blocco JS equivalente con lo stesso comportamento.
- Non semplificare, non ottimizzare, non cambiare la struttura, non sostituire con soluzioni proprie. Se una riga non è traducibile così com'è, scrivi uno stub con il riferimento Kotlin e segnalalo.
- Ogni blocco porta il numero di riga Kotlin in un commento `KT:`.
- Non "ispirarsi" al design: i valori (dp, colori, durate, curve) si copiano dal Kotlin così come sono.
- Le dipendenze non pronte restano stub marcati con file e riga Kotlin, da collegare in un passo successivo.

- [ ] `UpdateInterstitialScreen` — `UpdateInterstitial.kt:77`
- [ ] `LoadingScreen` — `LoadingScreen.kt:39`
- [ ] `AccountScreen` — `AccountScreen.kt:110`
- [ ] `AlbumScreen` — `AlbumScreen.kt:220`
- [ ] `BrowseScreen` — `BrowseScreen.kt:51`
- [ ] `CrashScreen` — `CrashActivity.kt:123`
- [ ] `ExploreScreen` — `ExploreScreen.kt:70`
- [ ] `GenresScreen` — `GenresScreen.kt:256`
- [ ] `HistoryScreen` — `HistoryScreen.kt:86`
- [ ] `HomeScreen` — `HomeScreen.kt:365`
- [ ] `IrideScreen` — `IrideScreen.kt:169`
- [ ] `ListenTogetherScreen` — `ListenTogetherScreen.kt:106`
- [ ] `LoginScreen` — `LoginScreen.kt:63`
- [ ] `MoodAndGenresScreen` — `MoodAndGenresScreen.kt:54`
- [ ] `MusicImportScreen` — `MusicImportScreen.kt:45`
- [ ] `NewReleasesScreen` — `NewReleasesScreen.kt:79`
- [ ] `OnboardingScreen` — `OnboardingScreen.kt:67`
- [ ] `ReverieScreen` — `ReverieScreen.kt:102`
- [ ] `SpotifyImportScreen` — `SpotifyImportScreen.kt:61`
- [ ] `AppleMusicImportScreen` — `SpotifyImportScreen.kt:64`
- [ ] `StatsScreen` — `StatsScreen.kt:58`
- [ ] `VideoPlayerScreen` — `VideoPlayerScreen.kt:15`
- [ ] `YouTubeBrowseScreen` — `YouTubeBrowseScreen.kt:60`
- [ ] `ArtistAlbumsScreen` — `ArtistAlbumsScreen.kt:71`
- [ ] `ArtistDiscographyScreen` — `ArtistDiscographyScreen.kt:81`
- [ ] `ArtistGameScreen` — `ArtistGameScreen.kt:71`
- [ ] `ArtistItemsScreen` — `ArtistItemsScreen.kt:81`
- [ ] `ArtistScreen` — `ArtistScreen.kt:189`
- [ ] `ArtistSongsScreen` — `ArtistSongsScreen.kt:66`
- [ ] `EqEditorScreen` — `EqEditorScreen.kt:125`
- [ ] `LibraryPodcastsScreen` — `LibraryPodcastsScreen.kt:116`
- [ ] `LibraryScreen` — `LibraryScreen.kt:285`
- [~] `LibraryAlbumsScreen` — `LibraryScreen.kt:359` — traduzione riga per riga in `src/renderer/screens/library-albums.js` + dipendenze stub in `library-components.js`; NON ancora collegata a `#view-albums`; mancano implementazioni reali delle dipendenze
- [ ] `LibraryArtistsScreen` — `LibraryScreen.kt:755`
- [ ] `LibraryPlaylistsScreen` — `LibraryScreen.kt:993`
- [ ] `LibrarySongsScreen` — `LibraryScreen.kt:1314`
- [ ] `LibraryMixScreen` — `LibraryScreen.kt:1671`
- [ ] `AutoPlaylistScreen` — `AutoPlaylistScreen.kt:136`
- [ ] `DailyChartScreen` — `DailyChartScreen.kt:100`
- [ ] `DailyChartsWorldScreen` — `DailyChartsWorldScreen.kt:9`
- [ ] `LocalPlaylistScreen` — `LocalPlaylistScreen.kt:190`
- [ ] `AllChartsScreen` — `NewReleaseCharts.kt:126`
- [ ] `OnlinePlaylistScreen` — `OnlinePlaylistScreen.kt:161`
- [ ] `TopPlaylistScreen` — `TopPlaylistScreen.kt:107`
- [ ] `OnlinePodcastScreen` — `OnlinePodcastScreen.kt:97`
- [ ] `RecognitionHistoryScreen` — `RecognitionHistoryScreen.kt:75`
- [ ] `RecognitionScreen` — `RecognitionScreen.kt:92`
- [ ] `LocalSearchScreen` — `LocalSearchScreen.kt:80`
- [ ] `OnlineSearchScreen` — `OnlineSearchScreen.kt:141`
- [ ] `RockModuleScreen` — `RockModuleScreen.kt:37`
- [ ] `SearchScreen` — `SearchScreen.kt:89`
- [ ] `AboutScreen` — `AboutScreen.kt:68`
- [ ] `DesignTestScreen` — `DesignTestScreen.kt:42`
- [ ] `DiscordLoginScreen` — `DiscordLoginScreen.kt:62`
- [ ] `MyAccountScreen` — `MyAccountScreen.kt:92`
- [ ] `NotesScreen` — `NotesScreen.kt:73`
- [ ] `SettingsScreen` — `SettingsScreen.kt:105`
- [ ] `UpdaterScreen` — `UpdaterSettings.kt:71`
- [ ] `IntegrationScreen` — `IntegrationScreen.kt:43`
- [ ] `WrappedScreen` — `WrappedScreen.kt:86`
- [ ] `WrappedTotalAlbumsScreen` — `AlbumPages.kt:59`
- [ ] `WrappedTopAlbumScreen` — `AlbumPages.kt:150`
- [ ] `WrappedMinutesScreen` — `WrappedMinutesScreen.kt:45`
- [ ] `WrappedTopArtistScreen` — `WrappedTopArtistScreen.kt:46`
- [ ] `WrappedTopSongScreen` — `WrappedTopSongScreen.kt:47`
- [ ] `WrappedTotalArtistsScreen` — `WrappedTotalArtistsScreen.kt:43`
- [ ] `WrappedTotalSongsScreen` — `WrappedTotalSongsScreen.kt:42`
