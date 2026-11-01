/* Forma squircle unica per main process e renderer.
   Geometria di squircleShapePath() della libreria io.github.stoyan-vuchev:squircle-shape-android 3.0.1
   usata da Iride Android (cornerSmoothing clampato a [0.55, 1] come createSquircleShapeOutline()).
   Cambiare TOKENS cambia pannello, tile e bordi finestra in modo concentrico. */
(function (root) {
  const TOKENS = {
    smoothing: 0.48,      // cornerSmoothing Android
    navRadius: 24,        // FloatingPill: SquircleShape(radius = 24.dp)
    navPadding: 8,        // padding interno del pannello di navigazione
    tileSize: 56,         // altezza/larghezza tile (NavRowHeight 56)
    windowInset: 6,       // distanza del pannello dal bordo della finestra
  };
  TOKENS.tileRadius = TOKENS.navRadius - TOKENS.navPadding;        // tile concentrico al pannello
  TOKENS.railWidth = TOKENS.navPadding * 2 + TOKENS.tileSize;      // larghezza pannello
  TOKENS.windowRadius = TOKENS.navRadius + TOKENS.windowInset;     // finestra concentrica al pannello

  const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));

  function cubic(p0, p1, p2, p3, steps) {
    const pts = [];
    for (let i = 1; i <= steps; i++) {
      const t = i / steps, u = 1 - t;
      pts.push({
        x: u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x,
        y: u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y,
      });
    }
    return pts;
  }

  // poligono del contorno squircle (stessi 4 angoli cubici di squircleShapePath)
  function squirclePolygon(w, h, radius, smoothing) {
    const r = clamp(radius, 0, Math.min(w, h) / 2);
    const s = clamp(smoothing == null ? TOKENS.smoothing : smoothing, 0.55, 1);
    const k = 1 - s;
    const P = [{ x: r, y: 0 }, { x: w - r, y: 0 }];
    P.push(...cubic({ x: w - r, y: 0 }, { x: w - r * k, y: 0 }, { x: w, y: r * k }, { x: w, y: r }, 12));
    P.push({ x: w, y: h - r });
    P.push(...cubic({ x: w, y: h - r }, { x: w, y: h - r * k }, { x: w - r * k, y: h }, { x: w - r, y: h }, 12));
    P.push({ x: r, y: h });
    P.push(...cubic({ x: r, y: h }, { x: r * k, y: h }, { x: 0, y: h - r * k }, { x: 0, y: h - r }, 12));
    P.push({ x: 0, y: r });
    P.push(...cubic({ x: 0, y: r }, { x: 0, y: r * k }, { x: r * k, y: 0 }, { x: r, y: 0 }, 12));
    return P;
  }

  // valore CSS clip-path: polygon(...) equivalente al contorno squircle
  function clipPolygonCss(w, h, radius, smoothing) {
    return 'polygon(' + squirclePolygon(w, h, radius, smoothing)
      .map((p) => `${p.x.toFixed(2)}px ${p.y.toFixed(2)}px`).join(',') + ')';
  }

  // scanline: unione di rettangoli per setShape (regione finestra)
  function squircleRects(w, h, radius, smoothing) {
    const P = squirclePolygon(w, h, radius, smoothing);
    const n = P.length;
    const out = [];
    let active = [];
    for (let y = 0; y < h; y++) {
      const yc = y + 0.5;
      const xs = [];
      for (let i = 0; i < n; i++) {
        const a = P[i], b = P[(i + 1) % n];
        if ((a.y <= yc && b.y > yc) || (b.y <= yc && a.y > yc)) xs.push(a.x + (yc - a.y) * (b.x - a.x) / (b.y - a.y));
      }
      xs.sort((p, q) => p - q);
      const next = [];
      for (let i = 0; i + 1 < xs.length; i += 2) {
        const x = Math.round(xs[i]), width = Math.round(xs[i + 1]) - x;
        if (width <= 0) continue;
        const match = active.find((a) => a.x === x && a.width === width && a.y + a.height === y);
        if (match) { match.height += 1; next.push(match); active = active.filter((a) => a !== match); }
        else next.push({ x, y, width, height: 1 });
      }
      out.push(...active);
      active = next;
    }
    out.push(...active);
    return out;
  }

  const api = { TOKENS, squirclePolygon, clipPolygonCss, squircleRects };
  if (typeof module === 'object' && module.exports) module.exports = api;
  else root.IRIDE_SQUIRCLE = api;
})(typeof globalThis !== 'undefined' ? globalThis : window);
