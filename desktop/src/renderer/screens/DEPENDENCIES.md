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
