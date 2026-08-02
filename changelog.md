## 0.1.0-alpha22

### New features

- **Notes on songs and albums**: attach a title, star rating (half-star precision) and free-text thoughts to any song or album, from the redesigned song/album menu
- **Playlist categories**: replaced the old genre filter pills with real, persisted, user-created categories you can assign playlists to
- **Turntable needle-drop sound**: pressing PLAY on an album now plays a small procedural "needle drop" sound effect (thunk + crackle-pops), matching the New Iride UI vinyl look
- **Discover Weekly-style mix**: new generated discovery playlist pulling from your library and listening history
- **In-app video playback**: watch the official YouTube video for a track directly in the app instead of leaving to YouTube
- **Ko-fi / donations**: donations entry in Settings now has a small heart bounce animation to draw the eye — support the project if you'd like

### Improvements

- Player Appearance settings decluttered: removed dead/duplicate mini-player and slider style options, keeping one clear background style control
- Consistent entrance motion (IrideMotion) extended to Library Artists, Playlists, Songs and the Cached Playlist screen, matching Artist/Album/Home
- Cached Playlist screen visual rework to match the rest of the New Iride UI
- Auto-playlist custom manual-order feature removed after sync issues; manual playlist reordering is unaffected
- Various search, stats and library screen refinements

## 0.1.0-alpha21

### Improvements

- Update popup now matches the New Iride UI monospace look (Space Mono, flat monochrome, when the toggle is on)
- Cold start: fixed a black/white flash before the app's real theme takes over, and a re-layout jump caused by the nav bar insets landing after the first frame
- Fixed a crash on some playlist screens when the frosted top bar's glass effect turned on
- Feed thumbnails now request only the resolution they actually draw, cutting network and memory use
- Network requests time out faster after a lost connection so browse/library/lyrics don't look stuck for minutes
- Player: click-wheel center button simplified to match the surrounding wheel; lyrics background animation now only runs while lyrics are visible, cutting player battery/CPU use

## 0.1.0-alpha20

### New features

- **Playlist reorder**: drag to reorder songs in playlists, with a dedicated reorder mode (menu entry + morphing button) and instant response (fixed a per-row blocking read that caused stutter); auto playlists (favorites, most played, etc.) now support a custom manual order too
- **Artist discography screen**: browse an artist's full catalog beyond the top shelves
- **New release notifications**: Library Artists shows "+N" badges for new releases (title total + per-artist), auto-clears when you open the artist's profile, subtracts songs you've already played, and suggests artists you listen to but haven't followed

### Improvements

- Consistent entrance motion (fade + reveal) now applied across Artist, Album, Home and Library screens for the New Iride UI
- Playlist screen (New Iride UI) unified with the Album/Artist top bar style, with a new play/pause control panel and gradient background
- Player: rebalanced cover and control wheel sizing for better fit and spacing
- Fixed a rare cold-start glitch where the mini player briefly showed placeholder text before the real song info loaded
- Fixed the app getting stuck showing "offline" until force-closed even after the network came back; added a manual refresh button to Library
- Fixed taps being swallowed by the player sheet in some cases, and the guess-the-song game round switch occasionally showing the wrong round

## 0.1.0-alpha19

### New look

- **New graphic style**: a fresh monospace typographic identity (Space Mono) now runs across the New Iride UI — song, album, artist and playlist titles and subtitles all adopt the new typeface for a cleaner, more distinctive feel
- Player redesigned: larger, better-spaced album title, refined circular controls with subtle borders, reworked background gradient blend, and corrected touch zones so every control reacts where you tap

### Improvements

- **Guess the song**: rounds now start instantly — the next song's audio is preloaded the moment you answer, so there is no buffering between rounds; songs are loaded in full upfront (a short one-time wait, explained on the loading screen)
- Guess the song scoreboard shows a clearer "correct / total" result, and the songs-left counter now reads naturally in singular and plural
- Continued monochrome/flat styling pass across menus, bottom sheets, and lyrics for the New Iride UI

## 0.1.0-alpha18

### New features

- **Guess the song**: new artist-page quiz game (New Iride UI) — guess songs from a random full-catalog pool of that artist, wrong guesses cost a 3s penalty, best score tracked per artist
- **Library Albums sections**: new Favorites and Continue Listening rows on the Library Albums screen

### Improvements

- New "Top gradient on albums" setting, mirroring the existing home top gradient, subtly blended behind Album/Single/EP screens
- New glass-style playlist cover component
- Further monochrome/flat styling pass across menus, dialogs, settings screens, search, and the player for New Iride UI

## 0.1.0-alpha17

### New features

- **Comments**: view and post YouTube comments for the current song from the player (Settings → Player Appearance, opt-in and off by default) — sort by top/newest, reply counts, sign-in prompt when logged out
- **News tab**: new opt-in bottom navigation tab (Settings → Interface, between Home and Library) surfacing new albums and songs from your followed/frequently-played artists plus YouTube's curated new-release feed, with each release's year verified before it's shown
- **Genre filter pills**: playlist screens (auto, cached, local, online, top) now show genre filter chips resolved via Last.fm's tag data with an iTunes Search fallback, cached on disk
- **Smart Search**: search results can show full per-category carousels ordered by inferred query intent, each backed by its own dedicated search instead of YouTube's truncated summary shelves

### Improvements

- Consolidated the three separate player-logic toggles (Fast Song Loader BETA, Metrolist Player Logic, Faster Loader) into a single "Muzza Player Logic" setting — a faithful, fully isolated port of Muzza's own stream-resolution path
- More compact song/album list rows with subtle dividers between plain rows and softer squircle thumbnail corners
- Redesigned the playback-error UI: a small transient banner over the album art with a copy-debug-info button, instead of replacing the whole screen
- Refined word-synced lyrics animation with an Apple-Music-style micro letter lift and glow
- Artist page header now anchors the name/controls to the bottom of the artwork regardless of content, and recent releases are picked by newest year across all Albums/Singles/EP shelves instead of the first shelf match
- Artist song/album list screens now support sorting by name or artist

## 0.1.0-alpha16

### New features

- **Hero Carousel**: new opt-in home screen carousel (Settings → Interface) surfacing new releases, mood picks, personalized mixes, and artist radio — replaces the old Discovery Carousel
- **BetterLyrics Unison & Sillaba providers**: two new word-synced lyrics sources, each individually toggleable in Settings → Content, with tier-based tie-break priority against existing providers
- **Share to platform**: sharing a song now lets you pick YouTube Music, Spotify, Apple Music, or SoundCloud, resolving a matching link instead of only sharing the YouTube Music URL
- **Faster Loader / Metrolist Player Logic**: two new player settings for faster stream resolution when changing tracks

### Bug fixes

- Fixed "Start radio" silently doing nothing in some cases by always forcing a full queue replace instead of an incremental insert that could swallow exceptions
- Fixed network status reporting a stale "offline" state on signal degradation (walking, cellular handover, in-call) by dropping the strict `NET_CAPABILITY_VALIDATED` requirement
- Fixed search results always following YouTube's fixed Songs/Videos/Albums/Artists order regardless of query intent — results are now reordered based on the inferred top-result category (artist, album, playlist, podcast)

### Improvements

- Search result grid now uses per-type thumbnail sizing and aspect ratio (16:9 for videos, larger tiles for albums)
- Queue drag-to-reorder auto-scroll near screen edges slowed to ~25% of its previous speed
- Search results are enriched in the background with additional items per category beyond the initial truncated summary
- Small hidden bias added to lyrics word-timing offset to correct for perceptibly-early animation

## 0.1.0-alpha15

### New features

- **Queue reorder with drag handle**: tracks in the queue can now be reordered by dragging the handle icon — long-press is no longer required
- **Persistent equalizer settings**: equalizer presets and custom band values are now saved across sessions and restored on app restart
- **Download progress indicator**: per-track download progress is now shown inline in the song row and in the player menu download sheet

### Bug fixes

- Fixed crash when rapidly skipping tracks while the sleep timer countdown reaches zero
- Fixed offline mode toggle state not persisting after app restart
- Fixed album art not loading for local files with embedded cover larger than 1 MB
- Fixed search history not clearing when the user taps "Clear all"

### Improvements

- Player bottom sheet drag handle now has a larger touch target for easier dismissal
- Improved lyrics sync accuracy for LrcLib tracks with irregular line spacing
- Reduced background memory usage when the player is in background-only mode
- Updated Kotlin and Compose dependencies to latest stable versions

## 0.1.0-alpha14

### New features

- **Fast Song Loader (BETA)**: new option in Settings → Player that skips URL validation for faster stream resolution when changing tracks manually — reduces perceived load time; may occasionally cause a retry on the first attempt
- **Sleep Timer in player menu**: sleep timer dialog now accessible directly from the player action grid — no need to open Settings
- **Liked Songs custom cover**: Liked Songs playlist header now shows a star icon instead of the first song's thumbnail

### Bug fixes

- Fixed updater comparing versions lexicographically instead of numerically — alpha9 no longer sorts above alpha10
- Fixed updater using `versionName` instead of `tagName` when checking for available updates
- Fixed Library Songs screen opening on the "Liked" filter instead of "Library"
- Fixed Library Albums screen showing all albums instead of liked-only
- Fixed Local Album Radio including duplicate album tracks in the radio queue
- Fixed Player Menu download icons having inconsistent background containers

### Improvements

- Library Artists screen: filter chips replaced by unified sort+view toggle row (LibrarySortRow) — cleaner layout
- LrcLib lyrics matching: artist similarity filter now applied in relaxed mode, reducing wrong-artist matches
- Play icon path adjusted for better visual centering

## 0.1.0-alpha13

### New features

- **New player UI**: fully redesigned bottom player with thumbnail carousel — swipe left/right to navigate the queue directly from the player
- **Lyrics animations**: reworked lyrics line rendering with smoother highlight transitions and better scroll behavior
- **Animated gradient background**: improved dynamic background that reacts to album art colors with fluid animations
- **Player appearance settings**: new options to customize player look in Settings → Player
- **Satoshi font**: added Satoshi variable font for improved typography

### Bug fixes

- Fixed home screen auto-chip selection resetting unexpectedly
- Fixed library Artists and Songs screens layout and filter behavior
- Fixed gradient background flickering on player open/close
- Multiple stability and rendering fixes across player, queue, and lyrics components
- Updater reliability improvements

## 0.1.0-alpha10

### Changes

- Artists screen: collapsing header with smooth scroll behavior
- Library songs screen: renamed "Songs" to "All Tracks"

## 0.1.0-alpha09

### Changes

- Added Genres screen with full browsing support
- Reworked page title handling across all screens: titles now adapt dynamically and collapse correctly on scroll
- New filter chips on search and listing screens for faster content navigation
- Playback optimizations: reduced latency on track start and queue transitions
- Typography adjustments: text sizes tuned for better readability across screen sizes
- Fixed: APK correctly published as **Iride** (com.iride.music), no conflict with local installs

## 0.1.0-alpha08

### Changes

- Reworked page title handling across all screens: titles now adapt dynamically and collapse correctly on scroll
- New filter chips on search and listing screens for faster content navigation
- Playback optimizations: reduced latency on track start and queue transitions
- Typography adjustments: text sizes tuned for better readability across screen sizes
- Added Genres screen
- Fixed: APK now correctly published as **Iride** (com.iride.music), no conflict with local installs

## 0.1.0-alpha07

### Changes

- Reworked page title handling across all screens: titles now adapt dynamically and collapse correctly on scroll
- New filter chips on search and listing screens for faster content navigation
- Playback optimizations: reduced latency on track start and queue transitions
- Typography adjustments: text sizes tuned for better readability across screen sizes
- Added Genres screen

## 0.1.0-alpha06

### Major changes

**In-app update system**
Added AppUpdateDialog: a native popup that appears on launch when a new version is available on GitHub. Uses Updater.kt which calls the GitHub Releases API, compares semver against the current build, detects the correct APK variant (foss/gms), and triggers download + install. The check runs every 2 hours with in-memory caching. The dialog is shown at most once per session.

**Animated collapsing app bar**
Screens now use a two-state title bar: a large title visible at the top of the list that collapses into a compact sticky header when the user scrolls down. The transition is animated. This affects the Library screen and all sub-screens (albums, artists, playlists, songs).

**Favorites button rework**
The favorite/like button across song rows, player, and detail screens has been redesigned. The toggle logic and visual state have been unified to behave consistently in every context.

**Library and Offline Library redesign**
The Library screen has a new layout: a spring-animated pill toggle to switch between Library mode and Offline mode, followed by a category row (Albums, Artists, Songs, All Tracks, Playlists) with chevron navigation. The offline mode filters all content to locally available tracks only.

### Minor improvements
- OnlinePlaylistScreen: sticky header with thumbnail, song count, play and shuffle buttons, chip filter row
- LocalPlaylistScreen, AutoPlaylistScreen, TopPlaylistScreen: consistent header and action bar
- AlbumScreen: chip filter row, cleaner action layout
- ArtistScreen: simplified rendering, reduced code
- SearchScreen: chip filter bar, consistent result layout
- LibrarySongsScreen, LibraryAlbumsScreen, LibraryArtistsScreen: sort header and alignment fixes
- HomeScreen: internal cleanup

## 0.1.0-alpha01
First public alpha release of Iride. Experimental — expect bugs and breaking changes.
