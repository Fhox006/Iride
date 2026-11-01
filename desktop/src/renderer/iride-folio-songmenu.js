/* SongMenu.kt 1:1: header, NewActionGrid, Material3MenuGroup (icone drawable Android, testi strings.xml) */
(function () {
  const P = {
    edit: ['0 0 960 960', 'M200,760L257,760L648,369L591,312L200,703L200,760ZM120,840L120,670L648,143Q660,132 674.5,126Q689,120 705,120Q721,120 736,126Q751,132 762,144L817,200Q829,211 834.5,226Q840,241 840,256Q840,272 834.5,286.5Q829,301 817,313L290,840L120,840ZM760,256L760,256L704,200L704,200L760,256ZM619,341L591,312L591,312L648,369L648,369L619,341Z'],
    playlist_add: ['0 0 960 960', 'M120,640L120,560L400,560L400,640L120,640ZM120,480L120,400L560,400L560,480L120,480ZM120,320L120,240L560,240L560,320L120,320ZM640,800L640,640L480,640L480,560L640,560L640,400L720,400L720,560L880,560L880,640L720,640L720,800L640,800Z'],
    share: ['0 0 24 24', 'M15,5.63L20.66,12L15,18.37V15v-1h-1c-3.96,0 -7.14,1 -9.75,3.09c1.84,-4.07 5.11,-6.4 9.89,-7.1L15,9.86V9V5.63M14,3v6C6.22,10.13 3.11,15.33 2,21c2.78,-3.97 6.44,-6 12,-6v6l8,-9L14,3L14,3z'],
    playlist_play: ['0 0 960 960', 'M100,640L100,560L420,560L420,640L100,640ZM100,480L100,400L580,400L580,480L100,480ZM100,320L100,240L580,240L580,320L100,320ZM620,840L620,520L860,680L620,840Z'],
    queue_music: ['0 0 960 960', 'M640,800Q590,800 555,765Q520,730 520,680Q520,630 555,595Q590,560 640,560Q651,560 661,561.5Q671,563 680,568L680,240L880,240L880,320L760,320L760,680Q760,730 725,765Q690,800 640,800ZM120,640L120,560L440,560L440,640L120,640ZM120,480L120,400L600,400L600,480L120,480ZM120,320L120,240L600,240L600,320L120,320Z'],
    add: ['0 0 960 960', 'M440,760L440,520L200,520L200,440L440,440L440,200L520,200L520,440L760,440L760,520L520,520L520,760L440,760Z'],
    remove: ['0 0 960 960', 'M200,520L200,440L760,440L760,520L200,520Z'],
    download: ['0 0 960 960', 'M240,800Q207,800 183.5,776.5Q160,753 160,720L160,600L240,600L240,720Q240,720 240,720Q240,720 240,720L720,720Q720,720 720,720Q720,720 720,720L720,600L800,600L800,720Q800,753 776.5,776.5Q753,800 720,800L240,800ZM480,640L280,440L336,382L440,486L440,160L520,160L520,486L624,382L680,440L480,640Z'],
    artist: ['0 0 960 960', 'M740,400h140v80h-80v220q0,42 -29,71t-71,29q-42,0 -71,-29t-29,-71q0,-42 29,-71t71,-29q8,0 18,1.5t22,6.5v-208ZM120,800v-112q0,-35 17.5,-63t46.5,-43q62,-31 126,-46.5T440,520q42,0 83.5,6.5T607,546q-20,12 -36,29t-28,37q-26,-6 -51.5,-9t-51.5,-3q-57,0 -112,14t-108,40q-9,5 -14.5,14t-5.5,20v32h321q2,20 9.5,40t20.5,40L120,800ZM440,480q-66,0 -113,-47t-47,-113q0,-66 47,-113t113,-47q66,0 113,47t47,113q0,66 -47,113t-113,47ZM440,400q33,0 56.5,-23.5T520,320q0,-33 -23.5,-56.5T440,240q-33,0 -56.5,23.5T360,320q0,33 23.5,56.5T440,400ZM440,320ZM440,720Z'],
    delete: ['0 0 960 960', 'M280,840q-33,0 -56.5,-23.5T200,760v-520h-40v-80h200v-40h240v40h200v80h-40v520q0,33 -23.5,56.5T680,840L280,840ZM680,240L280,240v520h400v-520ZM360,680h80v-360h-80v360ZM520,680h80v-360h-80v360ZM280,240v520,-520Z'],
    star: ['0 0 24 24', 'M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z'],
    star_border: ['0 0 24 24', 'M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z'],
    close: ['0 0 960 960', 'm256,760 -56,-56 224,-224 -224,-224 56,-56 224,224 224,-224 56,56 -224,224 224,224 -56,56 -224,-224 -224,224Z'],
    album: ['0 0 24 24', 'M12,3c4.96,0 9,4.04 9,9s-4.04,9 -9,9s-9,-4.04 -9,-9S7.04,3 12,3M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2L12,2z M12,7c-2.77,0 -5,2.23 -5,5s2.23,5 5,5s5,-2.23 5,-5S14.77,7 12,7zM12,13.11c-0.61,0 -1.11,-0.5 -1.11,-1.11s0.5,-1.11 1.11,-1.11s1.11,0.5 1.11,1.11S12.61,13.11 12,13.11z'],
    radio: ['0 0 24 24', 'M7.76,16.24C6.67,15.16 6,13.66 6,12s0.67,-3.16 1.76,-4.24l1.42,1.42C8.45,9.9 8,10.9 8,12c0,1.1 0.45,2.1 1.17,2.83L7.76,16.24zM16.24,16.24C17.33,15.16 18,13.66 18,12s-0.67,-3.16 -1.76,-4.24l-1.42,1.42C15.55,9.9 16,10.9 16,12c0,1.1 -0.45,2.1 -1.17,2.83L16.24,16.24zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2s2,-0.9 2,-2S13.1,10 12,10zM20,12c0,2.21 -0.9,4.21 -2.35,5.65l1.42,1.42C20.88,17.26 22,14.76 22,12s-1.12,-5.26 -2.93,-7.07l-1.42,1.42C19.1,7.79 20,9.79 20,12zM6.35,6.35L4.93,4.93C3.12,6.74 2,9.24 2,12s1.12,5.26 2.93,7.07l1.42,-1.42C4.9,16.21 4,14.21 4,12S4.9,7.79 6.35,6.35z'],
    block: ['0 0 960 960', 'M480,880q-83,0 -156,-31.5T197,763q-54,-54 -85.5,-127T80,480q0,-83 31.5,-156T197,197q54,-54 127,-85.5T480,80q83,0 156,31.5T763,197q54,54 85.5,127T880,480q0,83 -31.5,156T763,763q-54,54 -127,85.5T480,880ZM480,800q54,0 104,-17.5t92,-50.5L228,284q-33,42 -50.5,92T160,480q0,134 93,227t227,93ZM732,676q33,-42 50.5,-92T800,480q0,-134 -93,-227t-227,-93q-54,0 -104,17.5T284,228l448,448Z'],
    image: ['0 -960 960 960', 'm460-380 280-180-280-180v360ZM320-240q-33 0-56.5-23.5T240-320v-480q0-33 23.5-56.5T320-880h480q33 0 56.5 23.5T880-800v480q0 33-23.5 56.5T800-240H320Zm0-80h480v-480H320v480ZM160-80q-33 0-56.5-23.5T80-160v-560h80v560h560v80H160Zm160-720v480-480Z'],
  };
  const ic = (n) => '<svg class="sm-ic" viewBox="' + P[n][0] + '" width="24" height="24" fill="currentColor" aria-hidden="true"><path d="' + P[n][1] + '"/></svg>';
  const esc = (x) => String(x == null ? '' : x).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

  // IrideFolioContextMotionSpec
  const SPEC = { initialScale: 0.94, blur: 14, travel: 8, stiffness: 460, damping: 0.84, closeMs: 300 };
  const reduced = () => window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
  const smooth = (x) => x * x * (3 - 2 * x);
  const bez = (x1, y1, x2, y2) => (t) => { // CubicBezierEasing(0.2,0,0.2,1)
    let lo = 0, hi = 1, u = t;
    for (let i = 0; i < 24; i++) { u = (lo + hi) / 2; const x = 3 * (1 - u) * (1 - u) * u * x1 + 3 * (1 - u) * u * u * x2 + u * u * u; if (x < t) lo = u; else hi = u; }
    return 3 * (1 - u) * (1 - u) * u * y1 + 3 * (1 - u) * u * u * y2 + u * u * u;
  };
  const closeEase = bez(0.2, 0, 0.2, 1);
  const spring = (t) => { // spring(dampingRatio .84, stiffness 460), 1 = rest
    const w = Math.sqrt(SPEC.stiffness), z = SPEC.damping, wd = w * Math.sqrt(1 - z * z);
    return 1 - Math.exp(-z * w * t) * (Math.cos(wd * t) + (z * w / wd) * Math.sin(wd * t));
  };
  function apply(menu, reveal, origin) {
    const r = menu.getBoundingClientRect();
    const focus = Math.min(1, Math.max(0, reveal));
    const scale = SPEC.initialScale + (1 - SPEC.initialScale) * reveal;
    const ox = Math.min(r.width, Math.max(0, origin.x - menu.offsetLeft)), oy = Math.min(r.height, Math.max(0, origin.y - menu.offsetTop));
    const tx = Math.max(-SPEC.travel, Math.min(SPEC.travel, origin.x - menu.offsetLeft - ox)) * (1 - reveal);
    const ty = Math.max(-SPEC.travel, Math.min(SPEC.travel, origin.y - menu.offsetTop - oy)) * (1 - reveal);
    const disp = 1 - focus, fade = Math.min(1, Math.max(0, (disp - 0.4) / 0.6));
    menu.style.transformOrigin = ox + 'px ' + oy + 'px';
    menu.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + scale + ')';
    menu.style.filter = 'blur(' + (SPEC.blur * (1 - focus * focus)) + 'px)';
    menu.style.opacity = String(1 - smooth(fade));
  }
  let raf = 0;
  function run(menu, opening, origin) {
    cancelAnimationFrame(raf);
    if (reduced()) { menu.style.transform = menu.style.filter = ''; menu.style.opacity = opening ? '1' : '0'; return; }
    const from = parseFloat(menu.dataset.reveal || (opening ? '0' : '1'));
    const t0 = performance.now();
    const step = (now) => {
      const t = (now - t0) / 1000;
      let v, done;
      if (opening) { v = from + (1 - from) * spring(t); done = t > 0.9; }
      else { const k = Math.min(1, t * 1000 / SPEC.closeMs); v = from * (1 - closeEase(k)); done = k >= 1; }
      menu.dataset.reveal = String(v);
      apply(menu, opening ? v : Math.max(0, v), origin);
      if (!done) raf = requestAnimationFrame(step);
      else if (opening) { menu.dataset.reveal = '1'; menu.style.transform = menu.style.filter = ''; menu.style.opacity = '1'; }
      else { menu.dataset.reveal = '0'; menu.style.opacity = '0'; }
    };
    raf = requestAnimationFrame(step);
  }

  // songMenuPlacement: prima sopra se ci sta, poi sotto, altrimenti il lato più grande con scroll
  function place(menu, anchor) {
    const W = 224, gap = 8, margin = 8, hm = 20;
    const vp = { l: hm, r: innerWidth - hm, t: 56 + margin, b: innerHeight - ((document.querySelector('#playbar')?.getBoundingClientRect().height) || 0) - margin - 8 };
    menu.style.maxHeight = 'none'; menu.style.width = W + 'px';
    const h0 = menu.scrollHeight;
    const aboveB = Math.min(vp.b, Math.max(vp.t, anchor.top - gap)), belowT = Math.min(vp.b, Math.max(vp.t, anchor.bottom + gap));
    const aboveS = aboveB - vp.t, belowS = vp.b - belowT;
    const above = h0 <= aboveS ? true : h0 <= belowS ? false : aboveS >= belowS;
    const avail = above ? aboveS : belowS;
    const h = Math.min(h0, avail > 0 ? avail : vp.b - vp.t);
    const x = Math.min(Math.max(anchor.right - W, vp.l), Math.max(vp.l, vp.r - W));
    const y = Math.min(Math.max(above ? aboveB - h : belowT, vp.t), Math.max(vp.t, vp.b - h));
    menu.style.maxHeight = h + 'px'; menu.style.left = x + 'px'; menu.style.top = y + 'px';
  }

  window.irideRenderSongMenu = function (menu, track, o) {
    const tr = (x) => (typeof window.t === 'function' ? window.t(x) : x);
    menu.classList.add('iride-songmenu');
    menu.innerHTML = '';
    const B = o.byId;
    const sections = [
      [o.playNext && { icon: 'playlist_play', title: 'Next', action: o.playNext },
       B.queue && { icon: 'queue_music', title: 'Queue', action: B.queue.action }],
      [B.fav && { icon: o.isFav ? 'star' : 'star_border', title: o.isFav ? 'Remove star' : 'Star', action: B.fav.action },
       B.playlist && { icon: 'playlist_add', title: 'Add to playlist', action: B.playlist.action },
       B.download && { icon: 'download', title: 'Download', action: B.download.action }],
      [track.artist && o.viewArtist && { icon: 'artist', title: 'View artist', action: o.viewArtist },
       o.viewAlbum && { icon: 'album', title: 'View album', action: o.viewAlbum }],
      [B.share && { icon: 'share', title: 'Share', action: B.share.action }],
      [B.rename && { icon: 'edit', title: 'Edit', action: B.rename.action },
       B.banner && { icon: 'image', title: 'Change banner', action: B.banner.action },
       B['pin-sidebar'] && { icon: B['pin-sidebar'].active ? 'remove' : 'add', title: B['pin-sidebar'].active ? 'Unpin from sidebar' : 'Pin to sidebar', action: B['pin-sidebar'].action },
       B.pin && { icon: B.pin.active ? 'remove' : 'add', title: B.pin.active ? 'Unpin from Speed dial' : 'Pin to Speed dial', action: B.pin.action }],
      [B['remove-pl'] && { icon: 'delete', title: 'Remove from playlist', action: B['remove-pl'].action },
       B['remove-q'] && { icon: 'delete', title: 'Remove from queue', action: B['remove-q'].action },
       B['remove-local'] && { icon: 'delete', title: 'Remove from library', action: B['remove-local'].action },
       B['block-track'] && { icon: 'block', title: B['block-track'].label, action: B['block-track'].action },
       B['block-artist'] && { icon: 'block', title: B['block-artist'].label, action: B['block-artist'].action }],
    ].map((g) => g.filter(Boolean)).filter((g) => g.length);
    sections.forEach((sec, i) => {
      if (i) { const d = document.createElement('div'); d.className = 'riff-ctx-divider'; menu.appendChild(d); }
      sec.forEach((r) => {
        const b = document.createElement('button');
        b.type = 'button'; b.className = 'sm-row';
        b.innerHTML = ic(r.icon) + '<span>' + esc(tr(r.title)) + '</span>';
        b.addEventListener('click', (ev) => { ev.stopPropagation(); o.close(); r.action(); });
        menu.appendChild(b);
      });
    });
    if (!menu._smObs) {
      menu._smObs = new MutationObserver(() => {
        if (!menu.classList.contains('iride-songmenu')) return;
        const open = menu.classList.contains('open');
        if (open === menu._smOpen) return;
        menu._smOpen = open;
        document.querySelectorAll('.btn-row-more.folio-open').forEach((x) => { if (!open) x.classList.remove('folio-open'); });
        run(menu, open, menu._smOrigin || { x: 0, y: 0 });
      });
      menu._smObs.observe(menu, { attributes: true, attributeFilter: ['class'] });
    }
  };

  // chiamata da app.js dopo il render: posizione + origine dell'animazione
  window.irideSongMenuPlace = function (menu, ev) {
    const a = window.__irideMenuAnchor; window.__irideMenuAnchor = null;
    const anchor = a || { left: ev.clientX - 24, right: ev.clientX + 24, top: ev.clientY - 24, bottom: ev.clientY + 24 };
    menu.classList.remove('open'); menu._smOpen = false;
    menu.style.transition = 'none'; menu.style.opacity = '0'; menu.style.transform = menu.style.filter = ''; menu.dataset.reveal = '0';
    place(menu, anchor);
    menu._smOrigin = { x: ev.clientX, y: ev.clientY };
    if (a) menu._smOrigin = { x: (a.left + a.right) / 2, y: (a.top + a.bottom) / 2 };
  };
})();
