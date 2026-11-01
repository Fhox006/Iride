const { dialog, ipcMain } = require('electron');
const fs = require('fs');
const path = require('path');
const { DATA_DIR } = require('../shared/platform');

const THEMES_DIR = path.join(DATA_DIR, 'themes');
const INDEX_PATH = path.join(THEMES_DIR, 'index.json');
const MAX_BYTES = 2 * 1024 * 1024;
const safeId = (id) => /^t[a-z0-9]{4,24}$/.test(String(id));

function readIndex() {
  try {
    const j = JSON.parse(fs.readFileSync(INDEX_PATH, 'utf8'));
    return Array.isArray(j.themes) ? j.themes : [];
  } catch (e) { return []; }
}

function writeIndex(themes) {
  fs.mkdirSync(THEMES_DIR, { recursive: true });
  fs.writeFileSync(INDEX_PATH, JSON.stringify({ themes }));
}

function parseMeta(css, fallbackName) {
  const block = css.match(/\/\*[\s\S]*?\*\//);
  const text = block ? block[0] : '';
  const pick = (key) => {
    const m = text.match(new RegExp('@' + key + '[ \\t]+([^\\r\\n*]+)'));
    return m ? m[1].trim() : '';
  };
  return { name: pick('name') || fallbackName, author: pick('author'), description: pick('description') };
}

function loadAll() {
  return readIndex().map((t) => {
    try { return { ...t, css: fs.readFileSync(path.join(THEMES_DIR, `${t.id}.css`), 'utf8') }; } catch (e) { return null; }
  }).filter(Boolean);
}

function register(getWindow) {
  ipcMain.handle('themes-list', () => loadAll());
  ipcMain.handle('themes-import', async () => {
    const result = await dialog.showOpenDialog(getWindow(), { title: 'Add theme', properties: ['openFile', 'multiSelections'], filters: [{ name: 'CSS theme', extensions: ['css'] }] });
    if (result.canceled) return [];
    const themes = readIndex();
    const added = [];
    for (const file of result.filePaths) {
      try {
        if (fs.statSync(file).size > MAX_BYTES) continue;
        const css = fs.readFileSync(file, 'utf8');
        const id = `t${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
        fs.mkdirSync(THEMES_DIR, { recursive: true });
        fs.writeFileSync(path.join(THEMES_DIR, `${id}.css`), css);
        const meta = parseMeta(css, path.basename(file, path.extname(file)));
        const entry = { id, name: meta.name, author: meta.author, description: meta.description, enabled: false };
        themes.push(entry);
        added.push({ ...entry, css });
      } catch (e) {}
    }
    writeIndex(themes);
    return added;
  });
  ipcMain.on('themes-set-enabled', (e, id, enabled) => {
    if (!safeId(id)) return;
    writeIndex(readIndex().map((t) => (t.id === id ? { ...t, enabled: !!enabled } : t)));
  });
  ipcMain.on('themes-disable-all', () => {
    writeIndex(readIndex().map((t) => ({ ...t, enabled: false })));
  });
  ipcMain.on('themes-remove', (e, id) => {
    if (!safeId(id)) return;
    try { fs.unlinkSync(path.join(THEMES_DIR, `${id}.css`)); } catch (err) {}
    writeIndex(readIndex().filter((t) => t.id !== id));
  });
  ipcMain.handle('read-app-css', () => {
    try { return fs.readFileSync(path.join(__dirname, '../renderer/styles.css'), 'utf8'); } catch (e) { return ''; }
  });
}

module.exports = { register };
