/* Squircle come SquircleShape di Iride Android (io.github.stoyan-vuchev:squircle-shape-android 3.0.1):
   geometria di squircleShapePath(), cornerSmoothing clampato a [0.55, 1] come createSquircleShapeOutline().
   I raggi dei tile sono concentrici: raggio tile = raggio pannello - distanza dal bordo del pannello. */
(function () {
  const SMOOTHING = 0.48;
  const PANEL_RADIUS = 24;   // FloatingPill: SquircleShape(radius = 24.dp)
  const clamp = (v, lo, hi) => Math.min(hi, Math.max(lo, v));
  const f = (n) => +n.toFixed(3);

  function squircleShapePath(w, h, radius, cornerSmoothing) {
    const r = clamp(radius, 0, Math.min(w, h) / 2);
    const s = clamp(cornerSmoothing, 0.55, 1);
    const k = 1 - s;
    return [
      `M${f(r)} 0`,
      `L${f(w - r)} 0`,
      `C${f(w - r * k)} 0 ${f(w)} ${f(r * k)} ${f(w)} ${f(r)}`,
      `L${f(w)} ${f(h - r)}`,
      `C${f(w)} ${f(h - r * k)} ${f(w - r * k)} ${f(h)} ${f(w - r)} ${f(h)}`,
      `L${f(r)} ${f(h)}`,
      `C${f(r * k)} ${f(h)} 0 ${f(h - r * k)} 0 ${f(h - r)}`,
      `L0 ${f(r)}`,
      `C0 ${f(r * k)} ${f(r * k)} 0 ${f(r)} 0`,
      'Z',
    ].join(' ');
  }

  function applyClip(el, radius) {
    const w = el.offsetWidth, h = el.offsetHeight;
    if (!w || !h) return;
    el.style.clipPath = `path('${squircleShapePath(w, h, radius, SMOOTHING)}')`;
  }

  // distanza minima tra il bordo del tile e quello del pannello
  function concentricRadius(panel, el) {
    const p = panel.getBoundingClientRect(), b = el.getBoundingClientRect();
    const inset = Math.min(b.left - p.left, b.top - p.top, p.right - b.right, p.bottom - b.bottom);
    return Math.max(0, PANEL_RADIUS - inset);
  }

  function refresh() {
    const panel = document.querySelector('.sidebar');
    if (!panel) return;
    applyClip(panel, PANEL_RADIUS);
    // Account (#nav-settings) è fuori dalla riga animata: nessun tile
    document.querySelectorAll('.sidebar .nav-item:not(#nav-settings)').forEach((el) => {
      applyClip(el, concentricRadius(panel, el));
    });
  }

  const ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(refresh) : null;
  function init() {
    refresh();
    if (!ro) return;
    const panel = document.querySelector('.sidebar');
    if (panel) ro.observe(panel);
    document.querySelectorAll('.sidebar .nav-item').forEach((el) => ro.observe(el));
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
  window.addEventListener('load', refresh);
  window.addEventListener('resize', refresh);
})();
