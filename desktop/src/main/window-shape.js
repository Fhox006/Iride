'use strict';
// Bordi della finestra Windows: stessa forma squircle della barra di navigazione (src/shared/squircle.js).
const { TOKENS, squircleRects } = require('../shared/squircle');

function attach(win) {
  if (process.platform !== 'win32' || !win || win.isDestroyed()) return;
  const apply = () => {
    if (win.isDestroyed()) return;
    const { width, height } = win.getBounds();
    if (win.isMaximized() || win.isFullScreen()) {
      win.setShape([{ x: 0, y: 0, width, height }]);
      return;
    }
    win.setShape(squircleRects(width, height, TOKENS.windowRadius, TOKENS.smoothing));
  };
  for (const ev of ['ready-to-show', 'resize', 'maximize', 'unmaximize', 'enter-full-screen', 'leave-full-screen']) win.on(ev, apply);
  apply();
}

module.exports = { attach };
