/* Squircle come SquircleShape di Iride Android (io.github.stoyan-vuchev:squircle-shape-android 3.0.1):
   stessa geometria di squircleShapePath(), cornerSmoothing clampato a [0.55, 1] come createSquircleShapeOutline(). */
(function () {
  const SMOOTHING = 0.48;
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

  function applySquircle(el, radius) {
    const w = el.offsetWidth, h = el.offsetHeight;
    if (!w || !h) return;
    el.style.clipPath = `path('${squircleShapePath(w, h, radius, SMOOTHING)}')`;
  }

  // raggi come in Android: panel 24dp (FloatingPill), tile 14dp (SquircleShape radius 14.dp)
  const TARGETS = [
    { sel: '.sidebar', radius: 24 },
    { sel: '.sidebar .nav-item', radius: 14 },
  ];

  function refresh() {
    for (const t of TARGETS) document.querySelectorAll(t.sel).forEach((el) => applySquircle(el, t.radius));
  }

  const ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(refresh) : null;
  function observe() {
    if (!ro) return;
    for (const t of TARGETS) document.querySelectorAll(t.sel).forEach((el) => ro.observe(el));
  }

  function init() { refresh(); observe(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
  window.addEventListener('load', refresh);
  window.addEventListener('resize', refresh);
})();
