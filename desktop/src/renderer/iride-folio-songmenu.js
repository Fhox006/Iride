/* Iride Folio -> desktop: IrideAnchoredSongMenu + IrideFolioContextMenu + SongMenu.kt (alpha34).
   Icone: drawable XML Android convertiti 1:1 (viewport, fillType, stroke). Testi: strings.xml. */
(function () {
  const I = {"playlist_play":{"vb":"0 0 960.0 960.0","p":[{"d":"M100,640L100,560L420,560L420,640L100,640ZM100,480L100,400L580,400L580,480L100,480ZM100,320L100,240L580,240L580,320L100,320ZM620,840L620,520L860,680L620,840Z"}]},"queue_music":{"vb":"0 0 960.0 960.0","p":[{"d":"M640,800Q590,800 555,765Q520,730 520,680Q520,630 555,595Q590,560 640,560Q651,560 661,561.5Q671,563 680,568L680,240L880,240L880,320L760,320L760,680Q760,730 725,765Q690,800 640,800ZM120,640L120,560L440,560L440,640L120,640ZM120,480L120,400L600,400L600,480L120,480ZM120,320L120,240L600,240L600,320L120,320Z"}]},"radio":{"vb":"0 0 24.0 24.0","p":[{"d":"M7.76,16.24C6.67,15.16 6,13.66 6,12s0.67,-3.16 1.76,-4.24l1.42,1.42C8.45,9.9 8,10.9 8,12c0,1.1 0.45,2.1 1.17,2.83L7.76,16.24zM16.24,16.24C17.33,15.16 18,13.66 18,12s-0.67,-3.16 -1.76,-4.24l-1.42,1.42C15.55,9.9 16,10.9 16,12c0,1.1 -0.45,2.1 -1.17,2.83L16.24,16.24zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2s2,-0.9 2,-2S13.1,10 12,10zM20,12c0,2.21 -0.9,4.21 -2.35,5.65l1.42,1.42C20.88,17.26 22,14.76 22,12s-1.12,-5.26 -2.93,-7.07l-1.42,1.42C19.1,7.79 20,9.79 20,12zM6.35,6.35L4.93,4.93C3.12,6.74 2,9.24 2,12s1.12,5.26 2.93,7.07l1.42,-1.42C4.9,16.21 4,14.21 4,12S4.9,7.79 6.35,6.35z"}]},"download":{"vb":"0 0 960.0 960.0","p":[{"d":"M240,800Q207,800 183.5,776.5Q160,753 160,720L160,600L240,600L240,720Q240,720 240,720Q240,720 240,720L720,720Q720,720 720,720Q720,720 720,720L720,600L800,600L800,720Q800,753 776.5,776.5Q753,800 720,800L240,800ZM480,640L280,440L336,382L440,486L440,160L520,160L520,486L624,382L680,440L480,640Z"}]},"playlist_add":{"vb":"0 0 960.0 960.0","p":[{"d":"M120,640L120,560L400,560L400,640L120,640ZM120,480L120,400L560,400L560,480L120,480ZM120,320L120,240L560,240L560,320L120,320ZM640,800L640,640L480,640L480,560L640,560L640,400L720,400L720,560L880,560L880,640L720,640L720,800L640,800Z"}]},"artist":{"vb":"0 0 960.0 960.0","p":[{"d":"M740,400h140v80h-80v220q0,42 -29,71t-71,29q-42,0 -71,-29t-29,-71q0,-42 29,-71t71,-29q8,0 18,1.5t22,6.5v-208ZM120,800v-112q0,-35 17.5,-63t46.5,-43q62,-31 126,-46.5T440,520q42,0 83.5,6.5T607,546q-20,12 -36,29t-28,37q-26,-6 -51.5,-9t-51.5,-3q-57,0 -112,14t-108,40q-9,5 -14.5,14t-5.5,20v32h321q2,20 9.5,40t20.5,40L120,800ZM440,480q-66,0 -113,-47t-47,-113q0,-66 47,-113t113,-47q66,0 113,47t47,113q0,66 -47,113t-113,47ZM440,400q33,0 56.5,-23.5T520,320q0,-33 -23.5,-56.5T440,240q-33,0 -56.5,23.5T360,320q0,33 23.5,56.5T440,400ZM440,320ZM440,720Z"}]},"album":{"vb":"0 0 24.0 24.0","p":[{"d":"M12,3c4.96,0 9,4.04 9,9s-4.04,9 -9,9s-9,-4.04 -9,-9S7.04,3 12,3M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2L12,2z","eo":1,"sw":1.0},{"d":"M12,7c-2.77,0 -5,2.23 -5,5s2.23,5 5,5s5,-2.23 5,-5S14.77,7 12,7zM12,13.11c-0.61,0 -1.11,-0.5 -1.11,-1.11s0.5,-1.11 1.11,-1.11s1.11,0.5 1.11,1.11S12.61,13.11 12,13.11z"}]},"share":{"vb":"0 0 24.0 24.0","p":[{"d":"M15,5.63L20.66,12L15,18.37V15v-1h-1c-3.96,0 -7.14,1 -9.75,3.09c1.84,-4.07 5.11,-6.4 9.89,-7.1L15,9.86V9V5.63M14,3v6C6.22,10.13 3.11,15.33 2,21c2.78,-3.97 6.44,-6 12,-6v6l8,-9L14,3L14,3z","sw":0.8,"cap":"butt","join":"miter"}]},"favorite":{"vb":"0 0 24.0 24.0","p":[{"d":"M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"}]},"favorite_border":{"vb":"0 0 24.0 24.0","p":[{"d":"M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z"}]},"edit":{"vb":"0 0 960 960","p":[{"d":"M200,760L257,760L648,369L591,312L200,703L200,760ZM120,840L120,670L648,143Q660,132 674.5,126Q689,120 705,120Q721,120 736,126Q751,132 762,144L817,200Q829,211 834.5,226Q840,241 840,256Q840,272 834.5,286.5Q829,301 817,313L290,840L120,840ZM760,256L760,256L704,200L704,200L760,256ZM619,341L591,312L591,312L648,369L648,369L619,341Z"}]},"delete":{"vb":"0 0 960.0 960.0","p":[{"d":"M280,840q-33,0 -56.5,-23.5T200,760v-520h-40v-80h200v-40h240v40h200v80h-40v520q0,33 -23.5,56.5T680,840L280,840ZM680,240L280,240v520h400v-520ZM360,680h80v-360h-80v360ZM520,680h80v-360h-80v360ZM280,240v520,-520Z"}]},"add":{"vb":"0 0 960.0 960.0","p":[{"d":"M440,760L440,520L200,520L200,440L440,440L440,200L520,200L520,440L760,440L760,520L520,520L520,760L440,760Z"}]},"remove":{"vb":"0 0 960.0 960.0","p":[{"d":"M200,520L200,440L760,440L760,520L200,520Z"}]},"close":{"vb":"0 0 24.0 24.0","p":[{"d":"M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z"}]},"more_vert":{"vb":"0 0 960.0 960.0","p":[{"d":"M480,800Q447,800 423.5,776.5Q400,753 400,720Q400,687 423.5,663.5Q447,640 480,640Q513,640 536.5,663.5Q560,687 560,720Q560,753 536.5,776.5Q513,800 480,800ZM480,560Q447,560 423.5,536.5Q400,513 400,480Q400,447 423.5,423.5Q447,400 480,400Q513,400 536.5,423.5Q560,447 560,480Q560,513 536.5,536.5Q513,560 480,560ZM480,320Q447,320 423.5,296.5Q400,273 400,240Q400,207 423.5,183.5Q447,160 480,160Q513,160 536.5,183.5Q560,207 560,240Q560,273 536.5,296.5Q513,320 480,320Z"}]},"check":{"vb":"0 0 24.0 24.0","p":[{"d":"M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z"}]},"library_add":{"vb":"0 0 24.0 24.0","p":[{"d":"M4,20h14v1H3V6h1V20zM21,3v15H6V3H21zM20,4H7v13h13V4z","sw":1.0,"cap":"butt"},{"d":"M18,10h-4V6h-1v4H9v1h4v4h1v-4h4V10z","sw":0.5,"cap":"butt"}]},"library_add_check":{"vb":"0 0 24.0 24.0","p":[{"d":"M17.58,8.41l-1.41,-1.41l-4.18,4.18l-1.58,-1.58L9,11.01L11.99,14L17.58,8.41z"},{"d":"M4,20h14v1H3V6h1V20zM21,3v15H6V3H21zM20,4H7v13h13V4z","sw":1.0,"cap":"butt"}]},"sync":{"vb":"0 0 960.0 960.0","p":[{"d":"M160,800L160,720L270,720L254,706Q205,657 182.5,599.5Q160,542 160,482Q160,371 226.5,284.5Q293,198 400,170L400,254Q328,280 284,342.5Q240,405 240,482Q240,527 257,569.5Q274,612 310,648L320,658L320,560L400,560L400,800L160,800ZM560,790L560,706Q632,680 676,617.5Q720,555 720,478Q720,433 703,390.5Q686,348 650,312L640,302L640,400L560,400L560,160L800,160L800,240L690,240L706,254Q755,303 777.5,360.5Q800,418 800,478Q800,589 733.5,675.5Q667,762 560,790Z"}]},"mic":{"vb":"0 0 24 24","p":[{"d":"M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z"}]}};
  const X = {};
  const ic = (n) => {
    if (X[n]) return '<svg class="sm-ic" viewBox="' + X[n][0] + '" width="24" height="24" fill="currentColor" aria-hidden="true"><path d="' + X[n][1] + '"/></svg>';
    const d = I[n];
    return '<svg class="sm-ic" viewBox="' + d.vb + '" width="24" height="24" fill="currentColor" aria-hidden="true">' + d.p.map((p) =>
      '<path d="' + p.d + '"' + (p.eo ? ' fill-rule="evenodd"' : '') +
      (p.sw ? ' stroke="currentColor" stroke-width="' + p.sw + '"' + (p.cap ? ' stroke-linecap="' + p.cap + '"' : '') + (p.join ? ' stroke-linejoin="' + p.join + '"' : '') : '') + '/>').join('') + '</svg>';
  };
  window.irideIcon = ic;
  const esc = (x) => String(x == null ? '' : x).replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

  // ---------- IrideFolioContextMotionSpec ----------
  const SPEC = { initialScale: 0.94, blur: 14, travel: 8, stiffness: 460, damping: 0.84, closeMs: 300, compress: 0.06, longPressMs: 400, slop: 8 };
  const RB = { resistance: 0.55, stiffness: 200 };           // RubberBandResistance, Spring.StiffnessLow (damping 1)
  const reduced = () => window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
  const clamp = (x, lo, hi) => Math.min(hi, Math.max(lo, x));
  const smooth = (x) => x * x * (3 - 2 * x);
  const bez = (x1, y1, x2, y2) => (t) => {
    let lo = 0, hi = 1, u = t;
    for (let i = 0; i < 28; i++) { u = (lo + hi) / 2; const x = 3 * (1 - u) * (1 - u) * u * x1 + 3 * (1 - u) * u * u * x2 + u * u * u; if (x < t) lo = u; else hi = u; }
    return 3 * (1 - u) * (1 - u) * u * y1 + 3 * (1 - u) * u * u * y2 + u * u * u;
  };
  const closeEase = bez(0.2, 0, 0.2, 1);          // CloseTween
  const anticipation = bez(0.35, 0, 0.65, 1);     // AnticipationEasing
  const spring = (t) => {                          // OpenSpring: 0 -> 1
    const w = Math.sqrt(SPEC.stiffness), z = SPEC.damping, wd = w * Math.sqrt(1 - z * z);
    return 1 - Math.exp(-z * w * t) * (Math.cos(wd * t) + (z * w / wd) * Math.sin(wd * t));
  };
  const critical = (x0, v0, k, t) => { const w = Math.sqrt(k); return (x0 + (v0 + w * x0) * t) * Math.exp(-w * t); }; // damping ratio 1

  // ---------- FolioShape: SquircleShape(radius, cornerSmoothing = 0.6) ----------
  function squircle(w, h, r, sm) {
    sm = sm == null ? 0.6 : sm;
    r = Math.min(r, w / 2, h / 2);
    const maxP = Math.min(w, h) / 2;
    let p = (1 + sm) * r;
    if (p > maxP) { sm = maxP / r - 1; p = maxP; }
    const R = Math.PI / 180;
    const arcM = 90 * (1 - sm), arc = Math.sin(arcM / 2 * R) * r * Math.SQRT2;
    const alpha = (90 - arcM) / 2, p34 = r * Math.tan(alpha / 2 * R), beta = 45 * sm;
    const c = p34 * Math.cos(beta * R), d = c * Math.tan(beta * R);
    const b = (p - arc - c - d) / 3, a = 2 * b;
    const seg = [['c', a, 0, a + b, 0, a + b + c, d], ['a', r, r, 0, 0, 1, arc, arc], ['c', d, c, d, b + c, d, a + b + c]];
    const rot = (k) => (x, y) => k === 0 ? [x, y] : k === 1 ? [-y, x] : k === 2 ? [-x, -y] : [y, -x];
    const f = (n) => +n.toFixed(3);
    const corner = (k) => {
      const m = rot(k);
      return seg.map((s) => {
        if (s[0] === 'a') { const e = m(s[6], s[7]); return 'a' + s[1] + ' ' + s[2] + ' 0 0 1 ' + f(e[0]) + ' ' + f(e[1]); }
        const o = []; for (let i = 1; i < 7; i += 2) { const e = m(s[i], s[i + 1]); o.push(f(e[0]), f(e[1])); }
        return 'c' + o.join(' ');
      }).join('');
    };
    return 'M' + f(w - p) + ' 0' + corner(0) + 'L' + f(w) + ' ' + f(h - p) + corner(1) + 'L' + f(p) + ' ' + f(h) + corner(2) + 'L0 ' + f(p) + corner(3) + 'Z';
  }
  const sqClip = (w, h, r) => "path('" + squircle(w, h, r) + "')";

  // ---------- animazione del pannello (IrideAnchoredSongMenu.graphicsLayer) ----------
  function apply(menu, reveal, origin) {
    const w = menu.offsetWidth, h = menu.offsetHeight;
    const focus = clamp(reveal, 0, 1);
    const scale = SPEC.initialScale + (1 - SPEC.initialScale) * reveal;
    const left = menu.offsetLeft, top = menu.offsetTop;
    const pivotX = clamp((origin.x - left) / Math.max(1, w), 0, 1), pivotY = clamp((origin.y - top) / Math.max(1, h), 0, 1);
    const tx = clamp(origin.x - left - w * pivotX, -SPEC.travel, SPEC.travel) * (1 - reveal);
    const ty = clamp(origin.y - top - h * pivotY, -SPEC.travel, SPEC.travel) * (1 - reveal);
    const fade = clamp((1 - focus - 0.4) / 0.6, 0, 1);
    menu.style.transformOrigin = (pivotX * 100) + '% ' + (pivotY * 100) + '%';
    menu.style.transform = 'translate(' + tx + 'px,' + ty + 'px) scale(' + scale + ')';
    menu.style.filter = 'blur(' + (SPEC.blur * (1 - focus * focus)) + 'px)';
    menu.style.opacity = String(1 - smooth(fade));
  }
  const srcScale = (el, p) => { if (!el) return; el.style.transformOrigin = '50% 50%'; el.style.scale = p ? String(clamp(1 - SPEC.compress * p, 0.9, 1.015)) : ''; };
  let raf = 0;
  function run(menu, opening, origin) {
    cancelAnimationFrame(raf);
    const src = menu._smSource; const p0 = menu._smP0 || 0;
    if (reduced()) { menu.style.transform = menu.style.filter = ''; menu.style.opacity = opening ? '1' : '0'; srcScale(src, 0); return; }
    const from = parseFloat(menu.dataset.reveal || (opening ? '0' : '1'));
    const t0 = performance.now();
    const step = (now) => {
      const t = (now - t0) / 1000;
      let v, done;
      if (opening) { const sp = spring(t); v = from + (1 - from) * sp; done = t > 0.9; srcScale(src, p0 * (1 - sp)); }
      else { const k = Math.min(1, t * 1000 / SPEC.closeMs); v = from * (1 - closeEase(k)); done = k >= 1; }
      menu.dataset.reveal = String(v);
      apply(menu, Math.max(0, v), origin);
      if (!done) raf = requestAnimationFrame(step);
      else if (opening) { menu.dataset.reveal = '1'; menu.style.transform = menu.style.filter = ''; menu.style.opacity = '1'; srcScale(src, 0); }
      else { menu.dataset.reveal = '0'; menu.style.opacity = '0'; }
    };
    raf = requestAnimationFrame(step);
  }

  // ---------- songMenuViewport + songMenuPlacement ----------
  function viewport() {
    const margin = 8, hm = 20;
    const pb = document.querySelector('#playbar');
    const footerTop = pb ? pb.getBoundingClientRect().top : innerHeight;
    return { l: hm, r: innerWidth - hm, t: 56 + margin, b: Math.min(innerHeight, footerTop) - margin };
  }
  function placement(anchor, vp, W, H0, gap) {
    const aboveB = clamp(anchor.top - gap, vp.t, vp.b), belowT = clamp(anchor.bottom + gap, vp.t, vp.b);
    const aboveS = aboveB - vp.t, belowS = vp.b - belowT;
    const above = H0 <= aboveS ? true : H0 <= belowS ? false : aboveS >= belowS;
    const avail = above ? aboveS : belowS;
    const h = Math.min(H0, avail > 0 ? avail : vp.b - vp.t);
    const maxX = Math.max(vp.l, vp.r - W), maxY = Math.max(vp.t, vp.b - h);
    return { x: clamp(anchor.right - W, vp.l, maxX), y: clamp(above ? aboveB - h : belowT, vp.t, maxY), h, constrained: h < H0 };
  }

  // ---------- IrideFolioScrollbar + rubberBandOverscroll ----------
  function attachScroll(menu) {
    const vp = menu._smVp, bar = menu._smBar, track = bar.firstChild, thumb = track.firstChild;
    const st = { raw: 0, raf: 0, idle: 0 };
    const dim = () => vp.clientHeight;
    const offset = () => { const r = st.raw, d = dim(); if (!r || d <= 0) return 0; const a = Math.abs(r); return Math.sign(r) * (a * d * RB.resistance) / (d + RB.resistance * a); };
    const metrics = () => {
      const trackH = track.clientHeight, content = vp.scrollHeight, view = vp.clientHeight;
      if (trackH <= 0 || content <= view) return null;
      const min = Math.min(20, trackH), range = content - view;
      const height = clamp(trackH * view / content, min, trackH), travel = trackH - height;
      return { trackH, height, travel, range, offset: travel * clamp(vp.scrollTop / range, 0, 1) };
    };
    const paint = () => {
      const off = offset();
      vp.style.transform = off ? 'translateY(' + off + 'px)' : '';
      const m = metrics();
      if (!m) { thumb.style.display = 'none'; return; }
      thumb.style.display = '';
      const comp = clamp(Math.abs(off) / Math.max(1, m.trackH * 0.55), 0, 0.65);
      const th = Math.max(m.height * (1 - comp), Math.min(20, m.trackH)), travel = m.trackH - th;
      const to = off > 0 ? 0 : off < 0 ? travel : m.offset;
      thumb.style.height = th + 'px'; thumb.style.transform = 'translateY(' + to + 'px)';
    };
    const settle = (v0) => {
      cancelAnimationFrame(st.raf);
      if (!st.raw) return;
      if (reduced()) { st.raw = 0; paint(); return; }
      const x0 = st.raw, t0 = performance.now();
      const f = (now) => {
        const t = (now - t0) / 1000; st.raw = critical(x0, v0 || 0, RB.stiffness, t); // spring(dampingRatio 1, StiffnessLow)
        if (Math.abs(st.raw) < 0.01) { st.raw = 0; paint(); return; }
        paint(); st.raf = requestAnimationFrame(f);
      };
      st.raf = requestAnimationFrame(f);
    };
    // onPreScroll (reduce an existing pull first) + onPostScroll (unconsumed distance feeds the pull)
    const pull = (delta, atTop, atBottom) => {
      const cur = st.raw;
      if (cur !== 0 && delta !== 0 && Math.sign(delta) !== Math.sign(cur)) {
        const next = cur > 0 ? Math.max(0, cur + delta) : Math.min(0, cur + delta); st.raw = next; paint(); return true;
      }
      if ((delta > 0 && atTop) || (delta < 0 && atBottom)) { st.raw = cur + delta; paint(); return true; }
      return false;
    };
    const edges = () => ({ top: vp.scrollTop <= 0, bottom: vp.scrollTop + vp.clientHeight >= vp.scrollHeight - 1 });
    vp.addEventListener('wheel', (e) => {
      const e2 = edges(), d = -e.deltaY * (e.deltaMode === 1 ? 16 : 1) * 0.64;
      if (pull(d, e2.top, e2.bottom)) { e.preventDefault(); e.stopPropagation(); cancelAnimationFrame(st.raf); clearTimeout(st.idle); st.idle = setTimeout(() => settle(0), 90); }
    }, { passive: false });
    let ty = null;
    vp.addEventListener('pointerdown', (e) => { if (e.pointerType === 'mouse') return; ty = e.clientY; cancelAnimationFrame(st.raf); });
    vp.addEventListener('pointermove', (e) => {
      if (ty == null || e.pointerType === 'mouse') return;
      const d = e.clientY - ty; ty = e.clientY;
      const before = vp.scrollTop; const e2 = edges();
      if (pull(d, e2.top, e2.bottom)) return;
      vp.scrollTop = before - d;
      const consumed = before - vp.scrollTop, left = d - consumed;
      if (left) { st.raw += left; paint(); }
    });
    const end = () => { if (ty == null) return; ty = null; settle(0); };
    vp.addEventListener('pointerup', end); vp.addEventListener('pointercancel', end);
    vp.addEventListener('scroll', paint, { passive: true });
    // thumb drag (draggable target)
    let dy = null;
    bar.addEventListener('pointerdown', (e) => { dy = e.clientY; bar.setPointerCapture(e.pointerId); e.preventDefault(); });
    bar.addEventListener('pointermove', (e) => { if (dy == null) return; const m = metrics(); const d = e.clientY - dy; dy = e.clientY; if (m && m.travel > 0) vp.scrollTop += d * m.range / m.travel; });
    bar.addEventListener('pointerup', () => { dy = null; }); bar.addEventListener('pointercancel', () => { dy = null; });
    menu._smPaint = paint;
  }

  // ---------- layout: IrideFolioMenuSurface (superficie, bordo, clip) ----------
  const SVGNS = 'http://www.w3.org/2000/svg';
  function layout(menu, W, H, constrained) {
    menu.style.width = W + 'px'; menu.style.minWidth = W + 'px'; menu.style.height = H + 'px';
    menu.style.clipPath = sqClip(W, H, 18);                           // ControlsShape
    menu._smClip.style.clipPath = sqClip(W - 2, H - 2, 17);           // MenuContentShape
    const sv = menu._smStroke; sv.setAttribute('width', W); sv.setAttribute('height', H);
    sv.firstChild.setAttribute('d', squircle(W - 1, H - 1, 17.5));    // border 1dp, drawn inside the outline
    menu._smVp.style.paddingRight = constrained ? '22px' : '2px';     // ScrollbarTouchTargetWidth
    menu._smBar.style.display = constrained ? '' : 'none';
    if (menu._smPaint) menu._smPaint();
  }

  // ---------- IrideFolioPressProtocol: compressione verso il dito, poi molla ----------
  const press = { row: null, p: 0, raf: 0, timer: 0, t0: 0, x: 0, y: 0, committed: false };
  function animPress(row, from, to, ms, ease) {
    cancelAnimationFrame(press.raf);
    const t0 = performance.now();
    const f = (now) => {
      const k = Math.min(1, (now - t0) / ms); press.p = from + (to - from) * ease(k); srcScale(row, press.p);
      if (k < 1) press.raf = requestAnimationFrame(f); else if (!to) srcScale(row, 0);
    };
    press.raf = requestAnimationFrame(f);
  }
  function releasePress(row) {
    cancelAnimationFrame(press.raf); clearTimeout(press.timer);
    if (!row) return;
    const p0 = press.p, t0 = performance.now();
    if (reduced() || !p0) { srcScale(row, 0); press.p = 0; return; }
    const f = (now) => {                                               // ReturnSpring: damping 1, stiffness 900
      press.p = critical(p0, 0, 900, (now - t0) / 1000); srcScale(row, press.p);
      if (Math.abs(press.p) > 0.001) press.raf = requestAnimationFrame(f); else { press.p = 0; srcScale(row, 0); }
    };
    press.raf = requestAnimationFrame(f);
  }
  document.addEventListener('pointerdown', (e) => {
    if (e.button !== 0 || e.target.closest('button, a, input, textarea, select, .riff-ctx-menu')) return;
    const row = e.target.closest('.track-row');
    if (!row || document.querySelector('.riff-ctx-menu.open')) return;
    press.row = row; press.x = e.clientX; press.y = e.clientY; press.committed = false; press.p = 0;
    if (!reduced()) animPress(row, 0, 1, SPEC.longPressMs, anticipation);
    clearTimeout(press.timer);
    press.timer = setTimeout(() => {
      press.committed = true; cancelAnimationFrame(press.raf);
      window.__irideTouch = { x: press.x, y: press.y, row, p: reduced() ? 0 : press.p };
      const swallow = (ev) => { ev.stopPropagation(); ev.preventDefault(); };
      document.addEventListener('click', swallow, { capture: true, once: true });
      setTimeout(() => document.removeEventListener('click', swallow, true), 1200);
      row.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, cancelable: true, clientX: press.x, clientY: press.y, view: window }));
      if (window.__irideTouch) { window.__irideTouch = null; releasePress(row); }   // nothing opened
    }, SPEC.longPressMs);
  }, true);
  const cancelPress = () => { if (!press.row || press.committed) { press.row = null; return; } const r = press.row; press.row = null; releasePress(r); };
  document.addEventListener('pointerup', cancelPress, true);
  document.addEventListener('pointercancel', cancelPress, true);
  document.addEventListener('pointermove', (e) => { if (press.row && !press.committed && Math.hypot(e.clientX - press.x, e.clientY - press.y) > SPEC.slop) cancelPress(); }, true);
  document.addEventListener('dragstart', cancelPress, true);

  // ---------- contenuto: SongMenu.kt ----------
  window.irideRenderSongMenu = function (menu, track, o) {
    const tr = (x) => (typeof window.t === 'function' ? window.t(x) : x);
    menu.classList.add('iride-songmenu');
    menu.innerHTML = '';
    const clip = document.createElement('div'); clip.className = 'sm-clip';
    const vp = document.createElement('div'); vp.className = 'sm-vp';
    const bar = document.createElement('div'); bar.className = 'sm-bar';
    bar.innerHTML = '<div class="sm-track"><div class="sm-thumb"></div></div>';
    const sv = document.createElementNS(SVGNS, 'svg'); sv.setAttribute('class', 'sm-stroke');
    const sp = document.createElementNS(SVGNS, 'path'); sp.setAttribute('transform', 'translate(.5 .5)'); sp.setAttribute('fill', 'none'); sp.setAttribute('stroke', 'rgba(255,255,255,.22)'); sp.setAttribute('stroke-width', '1'); sv.appendChild(sp);
    clip.appendChild(vp); menu.append(clip, bar, sv);
    menu._smClip = clip; menu._smVp = vp; menu._smBar = bar; menu._smStroke = sv;
    attachScroll(menu);

    const B = o.byId;
    const multi = String(track.artist || '').split(/,|&| feat\.? | ft\.? | x /i).filter((x) => x.trim()).length > 1;
    const sections = [
      [o.playNext && { icon: 'playlist_play', title: 'Next', action: o.playNext },
       B.queue && { icon: 'queue_music', title: 'Queue', action: B.queue.action },
       o.radio && { icon: 'radio', title: 'Radio', action: o.radio }],
      [B.fav && { icon: o.isFav ? 'favorite' : 'favorite_border', title: o.isFav ? 'Remove star' : 'Star', action: B.fav.action },
       B.playlist && { icon: 'playlist_add', title: 'Add to playlist', action: B.playlist.action },
       B.download && { icon: 'download', title: 'Download', action: B.download.action }],
      [track.artist && o.viewArtist && { icon: 'artist', avatar: o.artistThumb, round: true, title: multi ? 'View artists' : 'View artist', action: o.viewArtist },
       o.viewAlbum && { icon: 'album', avatar: o.thumb, title: 'View album', action: o.viewAlbum }],
      [B.share && { icon: 'share', title: 'Share', action: B.share.action }],
      [B['remove-pl'] && { icon: 'delete', title: 'Remove from playlist', action: B['remove-pl'].action },
       B['remove-q'] && { icon: 'delete', title: 'Remove from queue', action: B['remove-q'].action },
       B['remove-local'] && { icon: 'delete', title: 'Remove from library', action: B['remove-local'].action },
      ],
    ].map((g) => g.filter(Boolean)).filter((g) => g.length);
    let rows = 0;
    sections.forEach((sec, i) => {
      if (i) { const d = document.createElement('div'); d.className = 'riff-ctx-divider'; vp.appendChild(d); }
      sec.forEach((r) => {
        rows++;
        const b = document.createElement('button');
        b.type = 'button'; b.className = 'sm-row';
        const lead = r.avatar
          ? '<img class="sm-av' + (r.round ? ' sm-av-round' : '') + '" src="' + esc(r.avatar) + '" alt="" draggable="false">'
          : ic(r.icon);
        b.innerHTML = '<span class="sm-lead">' + lead + '</span><span class="sm-t">' + esc(tr(r.title)) + '</span>';
        b.addEventListener('click', (ev) => { ev.stopPropagation(); o.close(); r.action(); });
        vp.appendChild(b);
      });
    });
    menu._smRows = rows; menu._smDividers = Math.max(0, sections.length - 1);
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
    const touch = window.__irideTouch; window.__irideTouch = null;
    const px = touch ? touch.x : ev.clientX, py = touch ? touch.y : ev.clientY;
    const R = 24;                                                     // observeSongMenuAnchor radius
    const anchor = a || { left: px - R, right: px + R, top: py - R, bottom: py + R };
    menu.classList.remove('open'); menu._smOpen = false;
    menu.style.transition = 'none'; menu.style.opacity = '0'; menu.style.transform = menu.style.filter = ''; menu.dataset.reveal = '0';
    const vp = viewport();
    const W = Math.min(224, vp.r - vp.l);
    const H0 = menu._smRows * 42 + menu._smDividers * 5 + 18;        // MenuItemHeight, section padding + divider, (padding + border) * 2
    const pl = placement(anchor, vp, W, H0, 8);
    menu.style.left = pl.x + 'px'; menu.style.top = pl.y + 'px';
    layout(menu, W, pl.h, pl.constrained);
    menu._smVp.scrollTop = 0;
    menu._smOrigin = a ? { x: (a.left + a.right) / 2, y: (a.top + a.bottom) / 2 } : { x: px, y: py };
    menu._smSource = touch ? touch.row : null; menu._smP0 = touch ? touch.p : 0;
    if (touch) { press.committed = true; press.row = null; }
  };
})();
