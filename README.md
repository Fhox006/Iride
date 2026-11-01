<p align="center">
  <img src="banner.png?v=2" alt="Riff - desktop music player and audio suite for YouTube Music, SoundCloud, and local files" width="100%">
</p>

<p align="center">
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/windows8/windows8-original.svg" width="22" height="22" alt="Windows">&nbsp;&nbsp;
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/apple/apple-original.svg" width="22" height="22" alt="macOS">&nbsp;&nbsp;
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/linux/linux-original.svg" width="22" height="22" alt="Linux">&nbsp;&nbsp;
  <img src="https://cdn.simpleicons.org/ios/9ca3af" width="22" height="22" alt="iOS">&nbsp;&nbsp;
  <img src="https://cdn.jsdelivr.net/gh/devicons/devicon@latest/icons/android/android-original.svg" width="22" height="22" alt="Android">
</p>

<h1 align="center">
  <img src="github/readme/emoj2i.gif" width="48" height="48" align="absmiddle" alt="emoji">&nbsp;Riff
</h1>

<p align="center">
  <b>A free, open-source desktop music player for YouTube Music, SoundCloud, and local files.</b><br>
  Built with Material Design 3, no tracking, and no account needed.
</p>

<p align="center">
  <a href="https://github.com/rootscripts/riff/releases">
    <img src="https://img.shields.io/badge/Download-6750A4?style=for-the-badge&logo=download&logoColor=white" height="40" alt="Download">
  </a>
  &nbsp;
  <a href="https://riff.hs.vc">
    <img src="https://img.shields.io/badge/Website-1F2328?style=for-the-badge&logo=open-webui&logoColor=white" height="40" alt="Visit Website">
  </a>
</p>

<p align="center">
  <a href="#core-features">Features</a> &nbsp;·&nbsp;
  <a href="#built-in-audio-suite">Audio Suite</a> &nbsp;·&nbsp;
  <a href="#material-design-3-expressive-ui">Interface</a> &nbsp;·&nbsp;
  <a href="#download-and-install">Install</a> &nbsp;·&nbsp;
  <a href="#faq">FAQ</a> &nbsp;·&nbsp;
  <a href="#build-from-source">Build</a>
</p>

<p align="center">
  <a href="https://github.com/rootscripts/riff/stargazers"><img src="https://img.shields.io/github/stars/rootscripts/riff?style=flat-square&labelColor=16191C&color=CAD3F5" alt="Stars"></a>
  <a href="https://github.com/rootscripts/riff/commits/main"><img src="https://img.shields.io/github/last-commit/rootscripts/riff?style=flat-square&labelColor=16191C&color=94C9FF" alt="Last Commit"></a>
  <a href="https://github.com/rootscripts/riff/blob/main/LICENSE"><img src="https://img.shields.io/github/license/rootscripts/riff?style=flat-square&labelColor=16191C&color=8EEDD8" alt="License"></a>
  <a href="https://ko-fi.com/rootlesszen"><img src="https://img.shields.io/badge/Ko--fi-support-FF5E5B?style=flat-square&logo=kofi&logoColor=white&labelColor=16191C" alt="Support on Ko-fi"></a>
</p>

> [!IMPORTANT]
> **Contributions & Pull Requests Policy**  
> This project is actively developed and maintained exclusively by its core team. **Public Pull Requests are disabled and will not be accepted.**
> 
> * **Bugs:** Please report them via the Issues tab using the provided bug report template.
> * **Custom features & experiments:** You are welcome to **fork the repository** and maintain your own builds.
> * Please do not open issues requesting to enable PRs.

## What is Riff?

Riff is an open-source, telemetry-free desktop music app and player for **Windows, Linux, and macOS**. It unifies playback across **YouTube Music**, **YouTube**, **SoundCloud**, and offline local media into a single high-performance interface without ads, subscriptions, or login constraints. Provides very fancy UI and layout.

Built with **Electron**, **Vanilla JavaScript**, and **WebGL**, Riff provides responsive UI interactions and hardware-accelerated audio visualization without runtime framework overhead. The project is distributed under the **MIT license** and enforces strict privacy: all configuration, cached streams, and playback history remain strictly on the local machine.

## Features

- High quality streaming with **YouTube Music, YouTube, and SoundCloud** without third-party advertisements or account requirements
- **Offline lossless playback with full local library support** for high resolution formats including FLAC, WAV, MP3, OGG, and M4A.
- **Easy built-in audio editor**, downloading with effects or trimming 10-band & 3-band equalizer, and real-time DSP effects (speed, pitch, reverb, distortion).
- **1-Click multi-platform playlist migration**: Direct import and resolution of public playlists from Spotify, Apple Music, Deezer, YouTube Music, and SoundCloud
- **Material Design 3 UI**: Dynamic color extraction from album artwork, deep UI theming, responsive layout presets, and an integrated CSS editor
- **Hardware-Accelerated Visuals**: Canvas and WebGL visualizers including real-time frequency analyzers, an interactive squiggly progress scrubber, and synchronized karaoke lyrics.
- **Discord Rich Presence**: Real-time playback status reporting track metadata, artist info, and elapsed time to Discord.
- **Zero Telemetry**: No tracking identifiers, analytics pings, or background data collection.
- **Fullscreen mode** with fancy lyrics animation.
- **Albums** - see all of your favorite artist albums.
- **Customization** - built in theme editor, configure everything, and a CSS editor, with animated backgrounds and interface opacity sliders.
- **Plugins** - built-in plugin feature where you can add a plugin to edit how Riff works with something or add a new function.
## Built-in Audio Suite

Riff includes an integrated audio engineering toolkit accessible directly within the player interface:

### Visual Audio Editor
- **Lossless Audio Trimming**: Define start and end boundaries to trim tracks without transcoding loss.
- **Region Extraction**: Select and isolate specific audio segments to save as standalone files.
- **Non-Destructive Processing**: Edit operations produce independent files while preserving original source media untouched.

### 10-Band Parametric Equalizer
- Dedicated frequency sliders covering 32 Hz through 16 kHz with pre-configured acoustic profiles (Bass Boost, Vocal Clarity, Electronic, Flat, Rock).
- Per-channel gain adjustments with real-time Web Audio API signal processing.

### Real-Time DSP Effects Chain
- **Tempo Control**: Dynamically speed up or slow down playback rate without altering pitch.
- **Pitch Shifting**: Independent semitone pitch adjustment for rehearsal and tuning.
- **Reverb & Spatial Effects**: Algorithmic convolution and room size simulation for spatial enhancement.
- **Distortion / Overdrive**: Adjustable wave-shaping saturation and drive harmonics.

### Offline Library & Download Pipeline
- Download streams from YouTube Music, YouTube, and SoundCloud directly into local disk storage (MP3, FLAC, Opus, WAV).
- Automatic ID3 and metadata tagging, including high-resolution cover art embedding.
- Complete offline availability with full playlist compilation and search indexing.

## Material Design 3 Expressive UI

Riff implements the Material Design 3 (M3 Expressive) specification:

- **Dynamic Color Extraction**: Generates responsive UI tonal palettes on the fly based on the dominant and secondary hues of active album art.
- **Deep UI Customization**: Configurable banner backdrops, mesh gradients, blur filters, and typography controls.
- **Integrated CSS Editor**: Built-in developer console allowing direct CSS overrides and custom style injection to personalize every aspect of the interface.
- **Optimized Layout Modes**: Compact mini-player layout, expanded desktop view, and fullscreen TV / Karaoke mode with synchronized lyrics rendering.

## Multi-Platform Playlist Migration

Migrate existing music libraries between platforms in a single operation:

- Import public playlist URLs from **Spotify** (including playlists with 100+ tracks via automated pagination).
- Import public playlists from **Deezer**, and public playlists and albums from **Apple Music**, **SoundCloud**, and **YouTube / YouTube Music**.
- Platform-aware search resolution: matches imported track metadata against the selected audio provider to construct immediate local playlists.

## Synchronized Lyrics Engine

- Automatic multi-provider lyrics lookup matching artist and title metadata.
- Native support for synchronized LRC files, plain text lyrics, and dynamic online timestamp alignment.
- Inline time-offset adjustments to manually synchronize lyrics with live streams.
- Multi-language translation support directly inside the lyrics panel.

## Download and install

Pre-built binaries are available on the [Releases page](https://github.com/rootscripts/riff/releases).

Standard releases include all necessary dependencies (bundled yt-dlp and ffmpeg binaries). No secondary runtime configuration is required on Windows or Linux.

On Windows, the installer lets you choose the installation directory. Uninstalling Riff does not delete your app data.

### macOS

macOS support is currently experimental and compiled via continuous integration.

1. Download the appropriate image:
   - `Riff-<version>-mac-arm64.dmg` for Apple Silicon (M1 / M2 / M3 / M4)
   - `Riff-<version>-mac-x64.dmg` for Intel-based hardware
2. Open the disk image and drag Riff to your `Applications` directory.
3. Riff is ad-hoc signed but not notarized, so the first launch is blocked. Right-click `Riff.app` in `Applications`, select **Open**, and confirm the prompt. On macOS 15 and later, open it once, then go to **System Settings → Privacy & Security** and click **Open Anyway**.
4. If macOS still flags the bundle as damaged (older releases), execute the following command in Terminal:
   ```bash
   xattr -cr /Applications/Riff.app
   ```
   If a quarantine warning occurs when mounting the `.dmg` itself, run:
   ```bash
   xattr -d -r com.apple.quarantine /path/to/Riff.dmg
   ```
5. Riff downloads yt-dlp on first launch. Installing the tools with Homebrew is recommended, especially on Apple Silicon, where the ffmpeg build Riff downloads needs Rosetta (Riff finds Homebrew paths even when opened from Finder or the Dock):
   ```bash
   brew install yt-dlp ffmpeg
   ```
   Streaming works without ffmpeg; it is only needed for downloads and the audio editor.

macOS application data and local indexes are stored in `~/Library/Application Support/Riff`.

## Build from source

Requires Node.js 18 or newer and npm.

```bash
git clone https://github.com/rootscripts/riff.git
cd riff
npm install
npm start        # run the dev version
npm run dist     # make a production build
```

The exact build output depends on the current project configuration.

## Memory usage

Riff (single Electron renderer and server process) typically utilizes approximately **180 MB of RAM** during steady-state playback. Actual footprint varies based on active visualizer modes, library size, and OS window compositing.

### Memory usage is only classified as a leak when sustained footprint exceeds 600 MB.

## Mobile versions

There is no official Riff client for Android or iOS. Third-party community forks are documented below:
### https://rootless.shit.vc/fork
Additional mobile implementations may be reviewed in future releases.

## FAQ

**Is Riff free?**
Yes. Riff is free software published under the permissive MIT license. There are no subscriptions, paywalls, or premium tiers.

**Does Riff display advertisements?**
No. Streams from YouTube Music, YouTube, and SoundCloud are resolved directly and play without advertisements.

**Is an account or sign-in required?**
No account is required for playback or public playlist imports. Optional Google sign-in lets you import your own YouTube likes and playlists; Riff uses Google's OAuth flow and YouTube Data API for that feature. Telemetry and analytic tracking are strictly omitted.

**Which operating systems are supported?**
Windows 10/11, modern Linux distributions, and macOS (experimental).

**Can Riff function as an offline local music player?**
Yes. Riff natively indexes local audio files (including lossless FLAC, WAV, MP3, and OGG), reads standard metadata tags and embedded artwork, and manages playlists without altering underlying disk files.

**Can I use Riff entirely offline?**
Yes. Downloaded tracks, saved audio segments, and indexed local libraries function without an internet connection.

**How does playlist migration work?**
Paste any public Spotify, Apple Music, Deezer, YouTube Music, or SoundCloud playlist link. Riff extracts track titles and artists, batches requests to resolve streams on your active platform, and saves the result as an editable local playlist.

**Does Riff support synchronized lyrics?**
Yes. Riff supports online synchronized lyrics, local LRC files, manual timing adjustments, and real-time synchronized karaoke display.

**Does Riff integrate with Discord?**
Yes. Native Discord Rich Presence displays the currently playing track title, artist, elapsed time, and platform indicator.

**Are there official mobile builds?**
No official mobile releases exist. See [Mobile versions](#mobile-versions) for community-maintained forks.

## Contributing

Pull requests are welcome. Please verify all changes locally, ensure code passes syntax validation (`node --check`), and confirm stability before submitting contributions. Untested pull requests or changes introducing regressions will not be merged.

## Support

You can support ongoing development via [Ko-fi](https://ko-fi.com/rootlesszen). Repository stars on GitHub are also appreciated.

## Disclaimer

Riff is developed for educational purposes and personal media management. Users are responsible for adhering to the terms of service of third-party platforms and applicable local copyright regulations.

## License

[MIT](LICENSE)

---

<sub>Keywords: free music player, YouTube Music desktop app, SoundCloud desktop player, ad-free music player, Material Design 3, M3 Expressive, visual audio editor, lossless audio trimmer, 10-band equalizer, DSP effects, tempo changer, pitch shift, FLAC player, WAV playback, Spotify playlist import, Apple Music playlist migration, Electron music player, Vanilla JS, WebGL audio visualizer, Discord Rich Presence, open source music player, MIT license, telemetry-free desktop player.</sub>
