/* Indicatore di selezione della navigazione: scorre tra le voci con molla (stile iOS),
   segue il dito con trascinamento verticale e si aggancia alla voce più vicina al rilascio.
   Account resta fuori dalla riga, come in Iride Android. */
(function () {
  const T = window.IRIDE_SQUIRCLE.TOKENS;
  const K = 420;                          // rigidità molla
  const DAMP = 2 * Math.sqrt(K) * 0.82;   // smorzamento (rapporto ~0.82: leggero overshoot)
  const DRAG_START = 6;                   // px prima di considerare il gesto un trascinamento

  let nav = null, slider = null;
  let y = 0, v = 0, target = 0, raf = 0, last = 0, initialized = false;
  let down = null, dragging = false, suppressClick = false, programmaticClick = false;

  const tiles = () => nav
    ? [...nav.querySelectorAll(':scope > .nav-item:not(#nav-settings)')]
      .filter((el) => el.offsetHeight > 0)
      .sort((a, b) => a.offsetTop - b.offsetTop)
    : [];
  // voce selezionata: se l'utente ha appena scelto una voce (rilascio del trascinamento) questa ha la priorità
  let pinned = null;
  const activeTile = () => {
    const act = tiles().filter((el) => el.classList.contains('active'));
    if (pinned && !act.includes(pinned)) pinned = null;
    if (pinned) return pinned;
    return act[0] || null;
  };

  function render() { slider.style.transform = `translateY(${y}px)`; }

  function frame(ts) {
    const dt = Math.min(1 / 30, ((ts - last) / 1000) || 1 / 60);
    last = ts;
    const a = -K * (y - target) - DAMP * v;
    v += a * dt;
    y += v * dt;
    render();
    if (Math.abs(y - target) < 0.05 && Math.abs(v) < 0.05) {
      y = target; v = 0; render(); raf = 0; return;
    }
    raf = requestAnimationFrame(frame);
  }

  function springTo(t) {
    target = t;
    if (!raf) { last = performance.now(); raf = requestAnimationFrame(frame); }
  }

  function setY(val) { y = target = val; v = 0; render(); }

  function ensureSlider() {
    if (slider && slider.parentNode === nav) return;
    slider = document.createElement('div');
    slider.className = 'nav-slider is-hidden';
    slider.setAttribute('aria-hidden', 'true');
    nav.prepend(slider);
    window.IRIDE_NAV_SHAPE && window.IRIDE_NAV_SHAPE.clip(slider, T.tileRadius);
  }

  function sync() {
    if (!nav) return;
    ensureSlider();
    const a = activeTile();
    if (!a) { slider.classList.add('is-hidden'); return; }
    slider.style.height = a.offsetHeight + 'px';
    if (!initialized) { setY(a.offsetTop); initialized = true; }
    else springTo(a.offsetTop);
    slider.classList.remove('is-hidden');
  }

  function nearestTile(posY) {
    let best = null, bestD = Infinity;
    for (const t of tiles()) {
      const d = Math.abs(t.offsetTop - posY);
      if (d < bestD) { bestD = d; best = t; }
    }
    return best;
  }

  function init() {
    nav = document.querySelector('.sidebar .sidebar-nav');
    if (!nav) return;
    ensureSlider();

    // ignora le modifiche di classe che facciamo noi (slider/nav), altrimenti loop
    new MutationObserver((muts) => {
      if (muts.every((m) => m.type === 'attributes' && (m.target === slider || m.target === nav))) return;
      ensureSlider();
      window.IRIDE_NAV_SHAPE && window.IRIDE_NAV_SHAPE.refresh();
      sync();
    }).observe(nav, { subtree: true, childList: true, attributes: true, attributeFilter: ['class'] });

    window.addEventListener('resize', sync);

    nav.addEventListener('pointerdown', (e) => {
      const tile = e.target.closest('.sidebar-nav > .nav-item:not(#nav-settings)');
      if (!tile || e.button !== 0) return;
      down = { x: e.clientX, y: e.clientY, startY: y, pointerId: e.pointerId };
      dragging = false;
    }, true);

    window.addEventListener('pointermove', (e) => {
      if (!down || e.pointerId !== down.pointerId) return;
      const dy = e.clientY - down.y;
      if (!dragging) {
        if (Math.abs(dy) < DRAG_START) return;
        dragging = true;
        nav.classList.add('is-sliding');
      }
      const list = tiles();
      if (!list.length) return;
      const min = list[0].offsetTop, max = list[list.length - 1].offsetTop;
      setY(Math.min(max, Math.max(min, down.startY + dy)));
    }, true);

    const finish = () => {
      if (!down) return;
      const wasDragging = dragging;
      down = null;
      dragging = false;
      nav.classList.remove('is-sliding');
      if (!wasDragging) return;
      const t = nearestTile(y);
      if (!t) { sync(); return; }
      suppressClick = true;
      setTimeout(() => { suppressClick = false; }, 300);
      pinned = t;
      springTo(t.offsetTop);
      if (!t.classList.contains('active')) {
        programmaticClick = true;
        t.click();
        programmaticClick = false;
      }
    };
    window.addEventListener('pointerup', finish, true);
    window.addEventListener('pointercancel', finish, true);

    // il clic che chiude un trascinamento non deve attivare la voce due volte
    nav.addEventListener('click', (e) => {
      if (programmaticClick || !suppressClick) return;
      suppressClick = false;
      e.stopPropagation();
      e.preventDefault();
    }, true);

    sync();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
