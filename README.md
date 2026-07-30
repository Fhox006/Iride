<div align="center">

# Iride

A visually-focused YouTube Music client for Android

<br/>

<img width="300" alt="Iride player — retro monospace UI" src="https://github.com/user-attachments/assets/8d1ff52e-de45-4c59-94bc-2cd987e0e1f6" />

<br/><br/>

**The first pre-release is out — v0.1.0-alpha05**

Alpha build. Rough edges, missing pieces, breaking changes ahead.  
Worth trying if you're curious.

<br/>

<a href="https://github.com/Fhox006/Iride/releases/download/v0.1.0-alpha05/Iride.apk">
  <img src="https://img.shields.io/badge/↓_Download_APK-iride.apk_·_v0.1.0--alpha05-4361ee?style=for-the-badge" alt="Download iride.apk"/>
</a>

<br/><br/>

[![License](https://img.shields.io/github/license/Fhox006/Iride?style=flat-square&labelColor=0d1117&color=555)](https://github.com/Fhox006/Iride/blob/main/LICENSE)

> 🚧 Personal fork, actively under development.  
> Built by a student at [Politecnico di Torino – Design Dept.](https://www.polito.it/en)

</div>

---

## What is Iride?

Iride is a fork of [Metrolist](https://github.com/mostafaalagamy/metrolist), a YouTube Music Android client. This fork is driven by a design-first vision: the goal is to deliver a music experience that feels as good as it sounds — prioritising the **visual and aesthetic dimension** of the app.

The project follows a deliberate two-phase approach:

1. **UX first** — fix, refine, and align all functional and structural aspects inherited from Metrolist before touching the visual layer.
2. **UI second** — once the UX foundation is solid, redesign the visual identity: typography, colour, motion, and layout.

**Phase 1 (UX) is done.** The app now works the way it should. Development has moved into **Phase 2**: the visual identity is being rebuilt around a **retro monospace look** — Space Mono typography, flat monochrome surfaces, a click-wheel player, and consistent motion across every screen.

---

## Screenshots

<table>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/53bad714-6025-44ad-afc6-8e4379e8ef8b" width="180"/><br/><sub><b>Home</b></sub></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/9d894b7f-e946-458d-a9df-17c2e1c5319c" width="180"/><br/><sub><b>Library</b></sub></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/92927863-14ee-47ca-9ce8-c07c78b2d9d2" width="180"/><br/><sub><b>Search</b></sub></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/3aaf1a26-f850-440b-a2ed-d013692c518b" width="180"/><br/><sub><b>Search (detail)</b></sub></td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/8d1ff52e-de45-4c59-94bc-2cd987e0e1f6" width="180"/><br/><sub><b>Player</b></sub></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/482bf08d-85e1-47a4-823f-08c43b1e661f" width="180"/><br/><sub><b>Lyrics</b></sub></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/a3c1129d-c178-4510-8117-e561c7dc0161" width="180"/><br/><sub><b>Artist</b></sub></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/1fba1367-ceaf-4900-bf3a-d0efefb928aa" width="180"/><br/><sub><b>Album</b></sub></td>
  </tr>
</table>

---

## Current Focus — Phase 2: UI

With the UX foundation solid, work has shifted fully to graphics and style. The app is being refined into a cohesive **retro monospace** identity:

- Space Mono typography across the whole app
- Flat, monochrome surfaces — no gradients, no noise
- Click-wheel player and vinyl-peek mini player
- Consistent enter/exit motion on every screen (Home, Library, Search, Artist, Album, Player)

This phase is actively in progress — screens are being redesigned and polished one by one.

---

## Roadmap

| Phase | Focus | Status |
|-------|-------|--------|
| Phase 1 | UX — Fix & refine all Metrolist-inherited issues | ✅ Done |
| Phase 2 | UI — Retro monospace visual redesign (typography, colour, motion, layout) | 🔄 In progress |

---

## Features (inherited from Metrolist)

- Stream songs and videos from YouTube Music
- Background playback, offline download & caching
- Live synced lyrics and AI-powered translation
- Audio normalization, equalizer, tempo & pitch control
- Full library management and YouTube Music account sync
- Listen together with friends in real-time
- Light / Dark / Black / Dynamic themes, Material 3

---

## Building

```bash
git clone https://github.com/Fhox006/Iride.git
cd Iride
./gradlew assembleRelease
```

Requires Android Studio (latest stable) and JDK 17+.

---

## Credits

| Project | Authors |
|---------|---------|
| [Metrolist](https://github.com/mostafaalagamy/metrolist) | [Mo Agamy](https://github.com/mostafaalagamy) |
| [InnerTune](https://github.com/z-huang/InnerTune) | Zion Huang · Malopieds |
| [OuterTune](https://github.com/DD3Boh/OuterTune) | Davide Garberi · Michael Zh |

---

<div align="center">

**Disclaimer:** This project is not affiliated with, funded, authorized, endorsed by, or in any way associated with YouTube, Google LLC, or any of their affiliates and subsidiaries. All trademarks belong to their respective owners.

*Personal fork maintained by a Design student at Politecnico di Torino.*

</div>
