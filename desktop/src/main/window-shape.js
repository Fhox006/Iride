'use strict';
// Bordi della finestra Windows con squircle, stessa geometria di SquircleShape (Iride Android):
// cornerSmoothing 0.48 clampato a [0.55, 1]. La regione è costruita come unione di rettangoli
// (setShape), così i pixel fuori dalla forma non ricevono input.
const RADIUS = 16;
const SMOOTHING = 0.55;

function cubicPoints(p0, p1, p2, p3, steps) {
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

function squirclePolygon(w, h, r, s) {
  const k = 1 - s;
  const P = [{ x: r, y: 0 }, { x: w - r, y: 0 }];
  P.push(...cubicPoints({ x: w - r, y: 0 }, { x: w - r * k, y: 0 }, { x: w, y: r * k }, { x: w, y: r }, 12));
  P.push({ x: w, y: h - r });
  P.push(...cubicPoints({ x: w, y: h - r }, { x: w, y: h - r * k }, { x: w - r * k, y: h }, { x: w - r, y: h }, 12));
  P.push({ x: r, y: h });
  P.push(...cubicPoints({ x: r, y: h }, { x: r * k, y: h }, { x: 0, y: h - r * k }, { x: 0, y: h - r }, 12));
  P.push({ x: 0, y: r });
  P.push(...cubicPoints({ x: 0, y: r }, { x: 0, y: r * k }, { x: r * k, y: 0 }, { x: r, y: 0 }, 12));
  return P;
}

// Scanline: spans per riga, poi unione di righe consecutive uguali in rettangoli.
function squircleRects(w, h, r = RADIUS, s = SMOOTHING) {
  const P = squirclePolygon(w, h, r, s);
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

function attach(win) {
  if (process.platform !== 'win32' || !win || win.isDestroyed()) return;
  const apply = () => {
    if (win.isDestroyed()) return;
    const { width, height } = win.getBounds();
    if (win.isMaximized() || win.isFullScreen()) {
      win.setShape([{ x: 0, y: 0, width, height }]);
      return;
    }
    win.setShape(squircleRects(width, height));
  };
  for (const ev of ['ready-to-show', 'resize', 'maximize', 'unmaximize', 'enter-full-screen', 'leave-full-screen']) win.on(ev, apply);
  apply();
}

module.exports = { attach, squircleRects, squirclePolygon };
