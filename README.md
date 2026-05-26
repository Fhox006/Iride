<div align="center">

# Iride

A visually-focused YouTube Music client for Android

<br/>

<img src="https://github.com/user-attachments/assets/f44e5ef0-af13-4268-8353-c335e4a1980d" width="320" alt="Screenshot_20260526-230201"/>

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

Right now, the project is in **Phase 1**.

---

## Screenshots

<table>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/f34dcc82-4f86-44ff-be6b-c8b6e3fba7bc" width="200"/></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/07b8b0fa-ce03-4175-88d1-e69e5eb07da0" width="200"/></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/a192df0c-7ffe-4924-a0c2-7efae43e5529" width="200"/></td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/93e8f6df-a78b-4f6c-ae88-099dd975683c" width="200"/></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/0391f531-d605-4b12-9ef1-069a95bbbd04" width="200"/></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/21c3a87e-d5bd-4401-839d-2c2aba62fdac" width="200"/></td>
  </tr>
</table>

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
