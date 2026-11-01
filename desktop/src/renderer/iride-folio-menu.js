/* Iride Folio: i tre puntini di un brano aprono il pannello Folio (come Android), non piu' il dialogo download.
   Il pannello ha gia' "Download". Si ancora sotto il bottone, bordo destro allineato, offset verticale MenuOffset (8). */
document.addEventListener('click', (e) => {
  const btn = e.target.closest && e.target.closest('.btn-row-more');
  if (!btn) return;
  const row = btn.closest('.track-row');
  if (!row) return;
  e.preventDefault();
  e.stopImmediatePropagation();
  const r = btn.getBoundingClientRect();
  row.dispatchEvent(new MouseEvent('contextmenu', {
    bubbles: true, cancelable: true,
    clientX: Math.max(8, r.right - 224), clientY: r.bottom + 8,
  }));
}, true);
