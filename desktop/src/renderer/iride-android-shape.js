/* Forma squircle della navigazione: tokens e geometria da src/shared/squircle.js (stessi valori dei bordi finestra). */
(function () {
  const S = window.IRIDE_SQUIRCLE;
  const T = S.TOKENS;

  // token -> variabili CSS, così iride-android-nav.css usa la stessa sorgente
  const root = document.documentElement.style;
  root.setProperty('--ir-nav-radius', T.navRadius + 'px');
  root.setProperty('--ir-nav-padding', T.navPadding + 'px');
  root.setProperty('--ir-tile-radius', T.tileRadius + 'px');
  root.setProperty('--ir-window-inset', T.windowInset + 'px');
  root.setProperty('--ir-rail-w', T.railWidth + 'px');

  function clip(el, radius) {
    const w = el.offsetWidth, h = el.offsetHeight;
    if (!w || !h) return;
    el.style.clipPath = S.clipPolygonCss(w, h, radius, T.smoothing);
  }

  function refresh() {
    const panel = document.querySelector('.sidebar');
    if (panel) clip(panel, T.navRadius);
    // Account (#nav-settings) è fuori dalla riga animata: nessun tile
    document.querySelectorAll('.sidebar .nav-item:not(#nav-settings)').forEach((el) => clip(el, T.tileRadius));
  }

  window.IRIDE_NAV_SHAPE = { refresh, clip };

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
