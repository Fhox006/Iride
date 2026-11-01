document.addEventListener('DOMContentLoaded', () => {
  const body = document.body;
  const byId = (id) => document.getElementById(id);

  // ricerca a comparsa dalla barra laterale
  const searchBtn = byId('nav-search-rail');
  const input = byId('search-input');
  const topNav = document.querySelector('.top-nav');
  const setSearch = (open) => {
    body.classList.toggle('ir-search-open', open);
    searchBtn.classList.toggle('active', open);
    if (open) { input.focus(); input.select(); }
  };
  searchBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    setSearch(!body.classList.contains('ir-search-open'));
  });
  document.addEventListener('mousedown', (e) => {
    if (body.classList.contains('ir-search-open') && !topNav.contains(e.target) && !searchBtn.contains(e.target)) setSearch(false);
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') setSearch(false); });
  input.addEventListener('keydown', (e) => { if (e.key === 'Enter') setTimeout(() => setSearch(false), 0); });

  // libreria: raggruppa le viste che prima stavano nella barra laterale
  const libViews = { 'view-favorites': 'favorites', 'view-albums': 'albums', 'view-local': 'local', 'view-history': 'history', 'view-queue': 'queue' };
  byId('ir-lib-bar').addEventListener('click', (e) => {
    const b = e.target.closest('[data-target]');
    if (!b) return;
    // il pannello playlist si chiude ai click esterni: questo non deve arrivarci
    e.stopPropagation();
    byId(b.dataset.target).click();
  });

  // pulsanti album
  const albumList = byId('album-tracks-list');
  const albumSearch = byId('ir-album-search');
  const firstRow = () => albumList.querySelector('.track-row');
  byId('ir-album-play').addEventListener('click', () => firstRow()?.click());
  byId('ir-album-shuffle').addEventListener('click', () => {
    if (!byId('btn-shuffle').classList.contains('active')) byId('btn-shuffle').click();
    firstRow()?.click();
  });
  byId('ir-album-like').addEventListener('click', () => byId('btn-save-album').click());
  byId('ir-album-download').addEventListener('click', () => {
    albumList.querySelectorAll('.btn-download, [title*="Download"]').forEach((b) => b.click());
  });
  albumSearch.addEventListener('input', () => {
    const q = albumSearch.value.trim().toLowerCase();
    albumList.querySelectorAll('.track-row').forEach((r) => { r.style.display = r.dataset.trackTitle.toLowerCase().includes(q) ? '' : 'none'; });
  });
  new MutationObserver(() => { albumSearch.value = ''; }).observe(albumList, { childList: true });

  // disco dell'album (VinylPeekDisc): esce e gira solo se suona questo album
  const coverWrap = byId('album-detail-cover');
  const art = document.createElement('div');
  art.className = 'folio-album-art';
  art.innerHTML = '<div class="folio-disc"><div class="folio-disc-spin"><img alt="" draggable="false"></div></div>';
  coverWrap.parentElement.insertBefore(art, coverWrap);
  art.appendChild(coverWrap);
  const discLabel = art.querySelector('.folio-disc-spin img');
  const audio = byId('native-audio');

  // alone: solo nelle pagine con copertina, come ArtworkGradient sul mobile
  const artworkViews = { 'view-album': '#album-detail-cover img', 'view-playlist-detail': 'img', 'view-artist': 'img' };
  const ambient = document.createElement('div');
  ambient.id = 'ir-ambient';
  body.prepend(ambient);
  let lastCover = null;

  // pigmenti della copertina: albumArtworkPigments (bucket 4 bit, accenti prima dei neutri, primi 4)
  // e sfondo ArtworkHeaderGradientBackground (stile predefinito: velo + due aloni dall'alto)
  const luminance = ([r, g, b]) => [r, g, b].map((v) => { v /= 255; return v <= .03928 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4; })
    .reduce((s, v, i) => s + v * [.2126, .7152, .0722][i], 0);
  const limitLuminance = (rgb, max) => {
    if (luminance(rgb) <= max) return rgb;
    let lo = 0, hi = 1;
    for (let i = 0; i < 12; i++) {
      const mid = (lo + hi) / 2;
      if (luminance(rgb.map((v) => v * mid)) <= max) lo = mid; else hi = mid;
    }
    return rgb.map((v) => v * lo);
  };
  const pigmentsOf = (pixels, w, h) => {
    const pop = new Int32Array(4096), hx = new Float64Array(4096), vy = new Float64Array(4096), dist = new Int32Array(4096).fill(1 << 30), col = new Array(4096);
    let visible = 0;
    for (let i = 0; i < w * h; i++) {
      const r = pixels[i * 4], g = pixels[i * 4 + 1], b = pixels[i * 4 + 2];
      if (pixels[i * 4 + 3] < 128) continue;
      const k = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);
      pop[k]++; hx[k] += i % w; vy[k] += Math.floor(i / w); visible++;
      const d = (r - ((r & 240) + 8)) ** 2 + (g - ((g & 240) + 8)) ** 2 + (b - ((b & 240) + 8)) ** 2;
      if (d < dist[k]) { dist[k] = d; col[k] = [r, g, b]; }
    }
    if (!visible) return [];
    const min = Math.max(1, Math.ceil(visible * .008));
    const cands = [];
    for (let k = 0; k < 4096; k++) if (pop[k] >= min) cands.push({ rgb: col[k], n: pop[k], x: hx[k] / pop[k] / Math.max(1, w - 1), y: vy[k] / pop[k] / Math.max(1, h - 1) });
    const hi = (c) => Math.max(...c.rgb), lo = (c) => Math.min(...c.rgb);
    const accents = cands.filter((c) => hi(c) >= 32 && hi(c) - lo(c) >= hi(c) * .18);
    const pool = accents.length ? accents : (cands.filter((c) => hi(c) >= 32).length ? cands.filter((c) => hi(c) >= 32) : cands);
    const score = (c) => Math.sqrt(c.n) * (accents.length ? .65 + ((hi(c) - lo(c)) / (hi(c) || 1)) * .35 : Math.max(.05, hi(c) / 255));
    return pool.sort((a, b) => score(b) - score(a)).slice(0, 4);
  };
  const pigments = new Map();
  const pigmentOf = (url) => pigments.get(url) || new Promise((resolve) => {
    const fallback = [{ rgb: [48, 50, 59], x: .5, y: .5 }];
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        const scale = Math.min(1, 128 / Math.max(img.naturalWidth, img.naturalHeight));
        const w = Math.max(1, Math.round(img.naturalWidth * scale)), h = Math.max(1, Math.round(img.naturalHeight * scale));
        const c = document.createElement('canvas');
        c.width = w; c.height = h;
        const x = c.getContext('2d', { willReadFrequently: true });
        x.drawImage(img, 0, 0, w, h);
        const list = pigmentsOf(x.getImageData(0, 0, w, h).data, w, h);
        resolve(list.length ? list : fallback);
      } catch (_) { resolve(fallback); }
    };
    img.onerror = () => resolve(fallback);
    img.src = url;
  }).then((list) => { pigments.set(url, list); return list; });
  const css = (rgb, a) => `rgba(${rgb.map(Math.round).join(',')},${a})`;
  // drawArtworkAmbientHalos: due ellissi che entrano da sopra il bordo, con dissolvenza smootherstep
  const halo = (p, i, width) => {
    const H = 380;
    const cx = width * ((i ? .76 : .22) + (p.x - .5) * .10), cy = -H * (.02 + p.y * .04);
    const rx = width * ((i ? .60 : .68) + p.x * .06), ry = Math.min(rx, H * (.78 + p.y * .06));
    const rgb = limitLuminance(p.rgb, .10), alpha = i ? .65 : .40;
    const stops = Array.from({ length: 33 }, (_, s) => { const t = s / 32, f = t * t * t * (t * (t * 6 - 15) + 10); return `${css(rgb, alpha * (1 - f))} ${(t * 100).toFixed(1)}%`; });
    return `radial-gradient(${rx.toFixed(0)}px ${ry.toFixed(0)}px at ${cx.toFixed(0)}px ${cy.toFixed(0)}px, ${stops.join(',')})`;
  };
  const tintFrom = (url) => pigmentOf(url).then((list) => {
    if (url !== lastCover) return;
    const width = ambient.clientWidth || window.innerWidth;
    body.style.setProperty('--ir-tint', css(limitLuminance(list[0].rgb, .10), 1));
    const halos = [0, 1].map((i) => halo(list[i] || list[0], i, width));
    ambient.style.setProperty('--ir-halos', halos.join(','));
  });

  // intestazione album: avatar tondo da 18 accanto all'artista, riga meta "anno • durata"
  const artistBtn = byId('album-detail-artist');
  const artistRow = document.createElement('div');
  artistRow.className = 'ir-album-artist-row';
  artistRow.innerHTML = '<img class="ir-album-avatar" alt="">';
  artistBtn.parentElement.insertBefore(artistRow, artistBtn);
  artistRow.appendChild(artistBtn);
  const avatar = artistRow.firstChild;
  const metaEl = byId('album-detail-count');
  const polishAlbum = () => {
    const src = (coverWrap.querySelector('img') || {}).src || '';
    avatar.style.display = src ? '' : 'none';
    if (src && avatar.src !== src) avatar.src = src;
    const rows = [...albumList.querySelectorAll('.track-row')];
    if (!rows.length) return;
    const albumArtists = new Set(artistBtn.textContent.split(/\s*(?:,|&|;)\s*/).map((n) => n.trim().toLowerCase()).filter(Boolean));
    let total = 0;
    rows.forEach((row) => {
      const secs = seconds(row.querySelector('.track-row-duration')?.textContent || '0:00');
      total += secs;
      const feat = (row.querySelector('.track-row-artist-wrap')?.textContent || '').split(/\s*(?:,|&|;)\s*/)
        .map((n) => n.trim()).filter((n) => n && !albumArtists.has(n.toLowerCase()));
      const sub = [feat.length ? `feat. ${feat.join(', ')}` : '', secs >= 60 && secs <= 300 ? '' : row.querySelector('.track-row-duration')?.textContent || ''].filter(Boolean).join(' • ');
      let el = row.querySelector('.ir-sub');
      if (!el) { el = document.createElement('span'); el.className = 'ir-sub'; row.querySelector('.track-row-info').appendChild(el); }
      if (el.textContent !== sub) el.textContent = sub;
    });
    const year = (metaEl.textContent.match(/\b(19|20)\d{2}\b/) || [])[0];
    const mins = Math.round(total / 60);
    const meta = [year, mins ? `${mins} min` : ''].filter(Boolean).join(' • ');
    if (meta && metaEl.textContent !== meta) metaEl.textContent = meta;
  };
  new MutationObserver(() => polishAlbum()).observe(albumList, { childList: true });
  new MutationObserver(() => polishAlbum()).observe(coverWrap, { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] });

  // il disco gira a 100 gradi al secondo e si puo' afferrare: trascinando, la velocita' della riproduzione segue la mano
  const disc = art.querySelector('.folio-disc');
  const spin = art.querySelector('.folio-disc-spin');
  let rot = 0, last = performance.now(), scratching = false, coast = 0, lastAngle = 0, lastT = 0, vel = 1, holdTimer = 0;
  const setRate = (v) => { audio.playbackRate = Math.min(16, Math.max(.0625, Math.abs(v))); };
  const paint = () => spin.style.setProperty('--disc-rot', `${rot}deg`);
  const tick = (now) => {
    const dt = (now - last) / 1000;
    last = now;
    if (!scratching && !coast && body.classList.contains('folio-album-playing')) { rot = (rot + dt * 100) % 360; paint(); }
    requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
  const angleOf = (e) => { const r = disc.getBoundingClientRect(); return Math.atan2(e.clientY - (r.y + r.height / 2), e.clientX - (r.x + r.width / 2)) * 180 / Math.PI; };
  const settle = () => {
    clearTimeout(holdTimer);
    let v = vel, prev = performance.now();
    const step = (now) => {
      const dt = (now - prev) / 1000; prev = now;
      v = 1 + (v - 1) * Math.exp(-3.2 * dt);
      rot = (rot + v * dt * 360 / 3.6 + 360) % 360; paint(); setRate(v);
      if (Math.abs(v - 1) > .02) { coast = requestAnimationFrame(step); return; }
      audio.playbackRate = 1; coast = 0; scratching = false; disc.classList.remove('scratching');
    };
    coast = requestAnimationFrame(step);
  };
  disc.addEventListener('pointerdown', (e) => {
    if (getComputedStyle(disc).opacity < .4) return;
    cancelAnimationFrame(coast); coast = 0;
    disc.setPointerCapture(e.pointerId);
    scratching = true; disc.classList.add('scratching');
    lastAngle = angleOf(e); lastT = e.timeStamp; vel = 1;
    setRate(0);
    holdTimer = setTimeout(() => { vel = 0; setRate(0); }, 70);
  });
  disc.addEventListener('pointermove', (e) => {
    if (!scratching || coast) return;
    const a = angleOf(e);
    let delta = a - lastAngle;
    if (delta > 180) delta -= 360; if (delta < -180) delta += 360;
    lastAngle = a; rot = (rot + delta + 360) % 360; paint();
    const dt = e.timeStamp - lastT;
    if (dt > 0) {
      const inst = Math.max(-12, Math.min(12, (delta / 360 * 3600) / dt));
      setRate(inst); vel += (inst - vel) * .35;
    }
    lastT = e.timeStamp;
    clearTimeout(holdTimer);
    holdTimer = setTimeout(() => { vel = 0; setRate(0); }, 70);
  });
  const release = () => { if (scratching && !coast) settle(); };
  disc.addEventListener('pointerup', release);
  disc.addEventListener('pointercancel', release);

  const sync = () => {
    const view = document.querySelector('.content-view.active');
    const viewName = view && libViews[view.id];
    body.classList.toggle('ir-lib', !!viewName);
    byId('nav-library').classList.toggle('active', !!viewName || (view && view.id.startsWith('view-library')));
    document.querySelectorAll('.ir-lib-chip').forEach((c) => c.classList.toggle('active', !!viewName && c.dataset.view === viewName));

    const selector = view && artworkViews[view.id];
    const img = selector && view.querySelector(selector);
    const cover = img && img.getAttribute('src') ? img.src : '';
    body.classList.toggle('folio-artwork-page', !!cover);
    if (cover && cover !== lastCover) {
      lastCover = cover;
      tintFrom(cover);
    }
    const albumCover = coverWrap.querySelector('img');
    if (albumCover && discLabel.getAttribute('src') !== albumCover.getAttribute('src')) discLabel.src = albumCover.src;
    const albumPlaying = view && view.id === 'view-album' && !audio.paused && !!albumList.querySelector('.track-row.playing');
    body.classList.toggle('folio-album-playing', !!albumPlaying);
  };
  audio.addEventListener('play', () => sync());
  audio.addEventListener('pause', () => sync());
  let pending = 0;
  new MutationObserver(() => {
    clearTimeout(pending);
    pending = setTimeout(sync, 150);
  }).observe(byId('main-content'), { subtree: true, childList: true, attributes: true, attributeFilter: ['class', 'src'] });
  new MutationObserver(sync).observe(byId('playbar-artwork'), { attributes: true, attributeFilter: ['src'] });
  sync();

  // avanzamento: riempimento colorato della barra del player
  const seconds = (t) => t.split(':').reduce((sum, n) => sum * 60 + Number(n), 0);
  const current = byId('time-current');
  const duration = byId('time-duration');
  const progress = () => {
    const total = seconds(duration.textContent);
    return total ? Math.min(100, seconds(current.textContent) / total * 100) : 0;
  };

  // anello di avanzamento del mini player
  const artwork = byId('playbar-artwork-wrapper');
  artwork.insertAdjacentHTML('beforeend', '<svg class="folio-ring" viewBox="0 0 44 44"><rect class="track" x="1" y="1" width="42" height="42" rx="10" pathLength="100"/><rect class="value" x="1" y="1" width="42" height="42" rx="10" pathLength="100"/></svg>');
  const syncProgress = () => {
    const p = progress();
    artwork.style.setProperty('--ir-ring', p);
    mp3.times[0].textContent = current.textContent;
    mp3.times[1].textContent = duration.textContent;
    mp3.bar.style.setProperty('--p', `${p}%`);
  };

  // copertine delle righe: un secondo tentativo dopo 1 s, come RadioCover
  document.addEventListener('error', (e) => {
    const img = e.target;
    if (!img.classList || !img.classList.contains('track-row-thumb') || img.dataset.retried) return;
    img.dataset.retried = '1';
    const src = img.src;
    setTimeout(() => {
      img.style.display = '';
      img.src = `${src}${src.includes('?') ? '&' : '?'}r=1`;
    }, 1000);
  }, true);

  // preferiti con la stella del mobile: l'app cambia solo fill, il tracciato resta questo
  const starPath = 'm12 3.500 2.600 5.400 5.900.8-4.300 4.100 1 5.900-5.200-2.800-5.200 2.800 1-5.900-4.300-4.100 5.900-.8z';
  byId('ir-album-like').querySelector('svg').innerHTML = '<path/>';
  ['btn-toggle-fav', 'ir-album-like', 'btn-save-album'].forEach((id) => {
    const path = byId(id)?.querySelector('svg path');
    if (path) path.setAttribute('d', starPath);
  });
  const albumSave = byId('btn-save-album').querySelector('svg');
  const syncAlbumStar = () => byId('ir-album-like').querySelector('svg').setAttribute('fill', albumSave.getAttribute('fill') || 'none');
  new MutationObserver(syncAlbumStar).observe(albumSave, { attributes: true, attributeFilter: ['fill'] });

  // pager: scorrimento a pagine e indicatore (CarouselPageIndicator)
  const pagers = [[byId('ir-hero'), byId('ir-hero-dots')], [byId('ir-now'), byId('ir-now-dots')]];
  const paintDots = (scroller, dots) => {
    if (!scroller.clientWidth) return;
    const count = Math.ceil((scroller.scrollWidth - 1) / scroller.clientWidth);
    const page = Math.min(count - 1, Math.round(scroller.scrollLeft / scroller.clientWidth));
    const slots = Math.min(count, 5);
    const first = count <= 1 ? 0 : Math.max(0, Math.min(page - 2, count - slots));
    if (count <= 1) { dots.textContent = ''; return; }
    while (dots.childElementCount < slots) dots.appendChild(document.createElement('i'));
    while (dots.childElementCount > slots) dots.lastChild.remove();
    [...dots.children].forEach((dot, i) => {
      dot.classList.toggle('on', first + i === page);
      dot.classList.toggle('edge', first + i !== page && ((i === 0 && first > 0) || (i === slots - 1 && first + slots < count)));
    });
  };
  pagers.forEach(([scroller, dots]) => scroller.addEventListener('scroll', () => paintDots(scroller, dots), { passive: true }));
  const paintAllDots = () => pagers.forEach(([s, d]) => paintDots(s, d));
  window.addEventListener('folio-pager-changed', () => requestAnimationFrame(paintAllDots));
  window.addEventListener('resize', paintAllDots);

  // player MP3 (IrideMp3Player): schermo a sinistra, ghiera a destra
  const ffIcon = '<svg viewBox="0 0 24 24"><path d="M3 5.500v13l9-6.500zm9.500 0v13l9-6.500z"/></svg>';
  const playIcon = '<svg viewBox="0 0 24 24"><path d="M7 4.500v15l12.500-7.500z"/></svg>';
  const pauseIcon = '<svg viewBox="0 0 24 24"><path d="M6 4.500h4v15H6zm8 0h4v15h-4z"/></svg>';
  const radioIcon = '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="2.500"/><path fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" d="M7.800 16.200a6 6 0 0 1 0-8.400M16.200 7.800a6 6 0 0 1 0 8.400M5 19a10 10 0 0 1 0-14M19 5a10 10 0 0 1 0 14"/></svg>';
  const upIcon = '<svg viewBox="0 0 24 24"><path fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="m6 15 6-6 6 6"/></svg>';
  const downIcon = '<svg viewBox="0 0 24 24"><path fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6"/></svg>';
  const root = document.createElement('div');
  root.id = 'folio-mp3';
  root.innerHTML = `
    <button type="button" class="folio-mp3-close" title="Close" aria-label="Close"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 6 6 6-6 6"/></svg></button>
    <div class="folio-mp3-frame">
      <div class="folio-mp3-screen"><img class="folio-mp3-art" alt="" draggable="false"><div class="folio-mp3-menu"></div></div>
      <div class="folio-mp3-times"><span>0:00</span><span>0:00</span></div>
      <div class="folio-mp3-bar"><i></i></div>
      <div class="folio-mp3-info"><div class="folio-mp3-text"><div class="folio-mp3-title"></div><div class="folio-mp3-artist"></div></div>
      <div class="folio-mp3-actions"><button type="button" class="folio-mp3-star" title="Favorite"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.700" stroke-linejoin="round"><path d="m12 3.500 2.600 5.400 5.900.8-4.300 4.100 1 5.900-5.200-2.800-5.200 2.800 1-5.900-4.300-4.100 5.900-.8z"/></svg></button><button type="button" class="folio-mp3-more" title="More"><svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><circle cx="12" cy="5" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="12" cy="19" r="2"/></svg></button></div></div>
    </div>
    <div class="folio-wheel">
      <button type="button" class="z up" aria-label="Menu"></button>
      <button type="button" class="z prev" aria-label="Previous">${ffIcon}</button>
      <button type="button" class="z next" aria-label="Next">${ffIcon}</button>
      <button type="button" class="z down" aria-label="Shuffle"></button>
      <button type="button" class="center" aria-label="Play or pause"></button>
    </div>`;
  body.appendChild(root);
  const q = (s) => root.querySelector(s);
  const mp3 = { times: root.querySelectorAll('.folio-mp3-times span'), bar: q('.folio-mp3-bar') };
  const menuRows = [
    ['Lyrics', 'btn-toggle-lyrics-panel'],
    ['Queue', 'btn-toggle-queue-panel'],
    ['Shuffle', 'btn-shuffle'],
    ['Repeat', 'btn-repeat'],
    ['Favorite', 'btn-toggle-fav'],
    ['Audio', 'btn-open-settings'],
  ];
  let menuIndex = -1;
  const paintMenu = () => {
    const open = menuIndex >= 0;
    root.classList.toggle('menu', open);
    q('.up').innerHTML = open ? upIcon : '<span>MENU</span>';
    q('.down').innerHTML = open ? downIcon : radioIcon;
    q('.down').classList.toggle('on', !open && byId('btn-shuffle').classList.contains('active'));
    root.querySelectorAll('.folio-mp3-menu button').forEach((b, i) => b.classList.toggle('on', i === menuIndex));
    q('.center').innerHTML = audio.paused ? playIcon : pauseIcon;
  };
  menuRows.forEach(([label, target], i) => {
    const row = document.createElement('button');
    row.type = 'button';
    row.textContent = label;
    row.addEventListener('click', () => { menuIndex = i; selectMenu(); });
    q('.folio-mp3-menu').appendChild(row);
  });
  const setMp3Open = (open) => {
    root.classList.toggle('open', open);
    body.classList.toggle('folio-mp3-open', open);
    window.dispatchEvent(new Event('resize'));
  };
  const closeMp3 = () => { setMp3Open(false); menuIndex = -1; paintMenu(); };
  const selectMenu = () => {
    const [, target] = menuRows[menuIndex];
    // i pannelli si aprono sopra l'app, le altre voci restano nel player
    if (target === 'btn-toggle-lyrics-panel' || target === 'btn-toggle-queue-panel' || target === 'btn-open-settings') closeMp3();
    else menuIndex = -1;
    byId(target).click();
    syncMp3();
  };
  const syncMp3 = () => {
    q('.folio-mp3-art').src = byId('playbar-artwork').src || '';
    q('.folio-mp3-title').textContent = byId('playbar-title').textContent;
    q('.folio-mp3-artist').textContent = byId('playbar-artist').textContent;
    const favSvg = byId('btn-toggle-fav').querySelector('svg');
    q('.folio-mp3-star').classList.toggle('on', !!favSvg && favSvg.getAttribute('fill') === 'currentColor');
    paintMenu();
  };
  q('.folio-mp3-close').addEventListener('click', closeMp3);
  q('.folio-mp3-star').addEventListener('click', () => { byId('btn-toggle-fav').click(); syncMp3(); });
  q('.folio-mp3-more').addEventListener('click', (e) => { e.stopPropagation(); byId('btn-playbar-more').click(); });
  q('.prev').addEventListener('click', () => byId('btn-prev').click());
  q('.next').addEventListener('click', () => byId('btn-next').click());
  q('.up').addEventListener('click', () => {
    menuIndex = menuIndex < 0 ? 0 : Math.max(0, menuIndex - 1);
    paintMenu();
  });
  q('.down').addEventListener('click', () => {
    if (menuIndex >= 0) menuIndex = Math.min(menuRows.length - 1, menuIndex + 1);
    else byId('btn-shuffle').click();
    paintMenu();
  });
  q('.center').addEventListener('click', () => {
    if (menuIndex >= 0) selectMenu();
    else { byId('btn-play-pause').click(); setTimeout(paintMenu, 80); }
  });
  mp3.bar.addEventListener('click', (e) => {
    const r = mp3.bar.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (e.clientX - r.left) / r.width));
    if (Number.isFinite(audio.duration)) audio.currentTime = audio.duration * ratio;
  });
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape' && root.classList.contains('open')) closeMp3(); });

  // tocco sul mini player: apre il player, come sul telefono
  byId('playbar').addEventListener('click', (e) => {
    if (e.target.closest('button, input, .volume-control-group, .artist-link-part')) return;
    if (!e.target.closest('.playbar-left')) return;
    e.stopPropagation();
    if (root.classList.contains('open')) { closeMp3(); return; }
    syncMp3();
    setMp3Open(true);
  }, true);
  audio.addEventListener('play', paintMenu);
  audio.addEventListener('pause', paintMenu);
  new MutationObserver(syncMp3).observe(byId('playbar-artwork'), { attributes: true, attributeFilter: ['src'] });
  new MutationObserver(syncMp3).observe(byId('playbar-title'), { childList: true, characterData: true, subtree: true });
  new MutationObserver(syncProgress).observe(current, { childList: true, characterData: true, subtree: true });
  syncMp3();
});
