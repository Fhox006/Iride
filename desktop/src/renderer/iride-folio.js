/* Iride Folio: grana nera, come Folio (GrainSize 0,55dp; SurfaceGrainOpacity 0,05; ButtonGrainOpacity 0,065).
   Solo nero: ~1/5 dei pixel al picco di opacita', quindi la media e' un quinto del picco. */
(function () {
  function tile(peak, n) {
    const c = document.createElement('canvas');
    c.width = c.height = n;
    const g = c.getContext('2d');
    const img = g.createImageData(n, n);
    for (let i = 0; i < img.data.length; i += 4) {
      img.data[i + 3] = Math.random() < 0.2 ? Math.round(peak * 255) : 0;
    }
    g.putImageData(img, 0, 0);
    return 'url(' + c.toDataURL('image/png') + ')';
  }
  function apply() {
    const dpr = window.devicePixelRatio || 1;
    const n = 256;
    const root = document.documentElement.style;
    root.setProperty('--folio-grain-surface', tile(0.05, n));
    root.setProperty('--folio-grain-button', tile(0.065, n));
    root.setProperty('--folio-grain-size', (n / dpr) + 'px');
  }
  apply();
  window.matchMedia && window.matchMedia('(resolution: ' + (window.devicePixelRatio || 1) + 'dppx)')
    .addEventListener && window.matchMedia('(resolution: ' + (window.devicePixelRatio || 1) + 'dppx)').addEventListener('change', apply);
})();
