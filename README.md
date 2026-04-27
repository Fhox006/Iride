<div align="center">

<img src="https://github.com/IrideGroup/Iride/blob/main/fastlane/metadata/android/en-US/images/icon.png" alt="Iride app icon" width="200" />

# Iride

### A visually-focused YouTube Music client for Android

<br/>

[![Latest release](https://img.shields.io/github/v/release/Fhox006/Iride?style=for-the-badge&labelColor=0d1117)](https://github.com/Fhox006/Iride/releases)
[![License](https://img.shields.io/github/license/Fhox006/Iride?style=for-the-badge&labelColor=0d1117)](https://github.com/Fhox006/Iride/blob/main/LICENSE)

<br/>

> 🚧 **This is a personal fork actively under development.**  
> Built by a student at [Politecnico di Torino – Design Department](https://www.polito.it/en).

</div>

---

## What is Iride?

Iride is a fork of [Metrolist](https://github.com/mostafaalagamy/metrolist), a YouTube Music Android client. This fork is driven by a design-first vision: the goal is to deliver a music experience that feels as good as it sounds — prioritising the **visual and aesthetic dimension** of the app.

The project follows a deliberate two-phase approach:

1. **UX first** — fix, refine, and align all functional and structural aspects inherited from Metrolist before touching the visual layer.
2. **UI second** — once the UX foundation is solid, redesign the visual identity: typography, colour, motion, and layout.

Right now, the project is in **Phase 1**.

---

## Current Focus — Phase 1: UX

The current development effort is focused on addressing all the gaps, inconsistencies, and rough edges inherited from the Metrolist codebase. This includes:

- Fixing navigation flows and screen transitions
- Resolving layout and interaction inconsistencies
- Improving state management and edge-case handling
- Aligning the overall structure with a coherent user experience

No cosmetic changes are being introduced at this stage. The goal is to make the app work exactly as it should before making it look exactly as it should.

---

## Roadmap

| Phase | Focus | Status |
|-------|-------|--------|
| Phase 1 | UX — Fix & refine all Metrolist-inherited issues | 🔄 In progress |
| Phase 2 | UI — Visual redesign (typography, colour, motion, layout) | ⏳ Planned |

---

## Features (inherited from Metrolist)

These features are available from the upstream project and are being maintained and improved:

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

Iride is built on top of the excellent work done by the open-source community:

| Project | Authors |
|---------|---------|
| [Metrolist](https://github.com/mostafaalagamy/metrolist) | [Mo Agamy](https://github.com/mostafaalagamy) |
| [InnerTune](https://github.com/z-huang/InnerTune) | Zion Huang · Malopieds |
| [OuterTune](https://github.com/DD3Boh/OuterTune) | Davide Garberi · Michael Zh |

---

<div align="center">

**Disclaimer:** This project is not affiliated with, funded, authorized, endorsed by, or in any way associated with YouTube, Google LLC, or any of their affiliates and subsidiaries. All trademarks belong to their respective owners.

<br/>

*Personal fork maintained by a Design student at Politecnico di Torino.*

</div>
