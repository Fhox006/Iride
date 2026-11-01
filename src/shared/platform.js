const fs = require('fs');
const os = require('os');
const path = require('path');

const IS_WIN = process.platform === 'win32';

if (process.platform === 'darwin') {
  const parts = (process.env.PATH || '').split(':').filter(Boolean);
  for (const dir of ['/opt/homebrew/bin', '/opt/homebrew/sbin', '/usr/local/bin', path.join(os.homedir(), '.local', 'bin')]) {
    if (!parts.includes(dir)) parts.push(dir);
  }
  process.env.PATH = parts.join(':');
}

const DEFAULT_DATA_DIR = IS_WIN
  ? path.join(process.env.LOCALAPPDATA || path.join(os.homedir(), 'AppData', 'Local'), 'Riff')
  : process.platform === 'darwin'
  ? path.join(os.homedir(), 'Library', 'Application Support', 'Riff')
  : path.join(os.homedir(), '.cache', 'devsize');

const CONFIG_DIR = IS_WIN
  ? path.join(process.env.LOCALAPPDATA || path.join(os.homedir(), 'AppData', 'Local'), 'Riff')
  : process.platform === 'darwin'
  ? path.join(os.homedir(), 'Library', 'Application Support', 'Riff')
  : path.join(process.env.XDG_CONFIG_HOME || path.join(os.homedir(), '.config'), 'devsize');
const CONFIG_PATH = path.join(CONFIG_DIR, 'config.json');

function readConfig() {
  try {
    const parsed = JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf-8'));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch (e) {
    return {};
  }
}

function resolveDataDir() {
  const override = process.env.RIFF_DATA_DIR;
  if (override && override.trim()) {
    try { return path.resolve(override.trim()); } catch (e) {}
  }
  const configured = readConfig().dataDir;
  if (configured && typeof configured === 'string' && configured.trim()) {
    try { return path.resolve(configured.trim()); } catch (e) {}
  }
  return DEFAULT_DATA_DIR;
}

function setDataDir(dir) {
  const cfg = readConfig();
  if (dir && String(dir).trim()) cfg.dataDir = path.resolve(String(dir).trim());
  else delete cfg.dataDir;
  fs.mkdirSync(CONFIG_DIR, { recursive: true });
  fs.writeFileSync(CONFIG_PATH, JSON.stringify(cfg, null, 2), 'utf-8');
  return cfg.dataDir || DEFAULT_DATA_DIR;
}

function resolveDownloadDir() {
  const configured = readConfig().downloadDir;
  if (configured && typeof configured === 'string' && configured.trim()) {
    try { return path.resolve(configured.trim()); } catch (e) {}
  }
  return null;
}

function defaultDownloadDir() {
  if (IS_WIN) {
    const drive = 'D:\\';
    try {
      fs.accessSync(drive, fs.constants.W_OK);
      return path.join(drive, 'Downloads');
    } catch (e) {}
  }
  try { return require('electron').app.getPath('downloads'); } catch (e) { return path.join(os.homedir(), 'Downloads'); }
}

function setDownloadDir(dir) {
  const cfg = readConfig();
  if (dir && String(dir).trim()) cfg.downloadDir = path.resolve(String(dir).trim());
  else delete cfg.downloadDir;
  fs.mkdirSync(CONFIG_DIR, { recursive: true });
  fs.writeFileSync(CONFIG_PATH, JSON.stringify(cfg, null, 2), 'utf-8');
  return cfg.downloadDir || null;
}

const DATA_DIR = resolveDataDir();

const CACHE_DIR = path.join(DATA_DIR, 'tracks');
const CUSTOM_LYRICS_DIR = path.join(DATA_DIR, 'custom-lyrics');
const LYRICS_DIR = path.join(DATA_DIR, 'lyrics');
const THUMBNAILS_DIR = path.join(DATA_DIR, 'thumbnails');
const METADATA_PATH = path.join(DATA_DIR, 'metadata.json');
const LIBRARY_DIR = path.join(DATA_DIR, 'library');
const LIBRARY_PATH = path.join(DATA_DIR, 'library.json');
const BIN_DIR = path.join(DATA_DIR, 'bin');
const TUTORIAL_MARKER = IS_WIN ? 'C:\\Riff\\.rifftutorial' : path.join(os.homedir(), '.rifftutorial');
const TUTORIAL_MARKER_FALLBACK = path.join(DATA_DIR, '.rifftutorial');
const STARTUP_PATH = path.join(DATA_DIR, 'startup.json');
function readStartupConfig() { try { return JSON.parse(fs.readFileSync(STARTUP_PATH, 'utf8')) || {}; } catch (e) { return {}; } }
function writeStartupConfig(patch) {
  try {
    fs.mkdirSync(DATA_DIR, { recursive: true });
    fs.writeFileSync(STARTUP_PATH, JSON.stringify({ ...readStartupConfig(), ...patch }));
    return true;
  } catch (e) { return false; }
}
const APP_VERSION = (() => { try { return require('../../package.json').version || '0'; } catch (e) { return '0'; } })();
function tutorialDone() {
  for (const file of [TUTORIAL_MARKER, TUTORIAL_MARKER_FALLBACK]) {
    try {
      if (!fs.existsSync(file)) continue;
      if (fs.readFileSync(file, 'utf8').trim() === APP_VERSION) return true;
    } catch (e) {}
  }
  return false;
}
function markTutorialDone() {
  for (const file of [TUTORIAL_MARKER, TUTORIAL_MARKER_FALLBACK]) {
    try { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, APP_VERSION); return true; } catch (e) {}
  }
  return false;
}

async function copyDirContents(fromDir, toDir, skipNames = new Set(['config.json'])) {
  await fs.promises.mkdir(toDir, { recursive: true });
  const entries = await fs.promises.readdir(fromDir, { withFileTypes: true });
  for (const entry of entries) {
    if (skipNames.has(entry.name)) continue;
    const src = path.join(fromDir, entry.name);
    const dest = path.join(toDir, entry.name);
    if (entry.isDirectory()) await copyDirContents(src, dest, skipNames);
    else if (entry.isFile()) await fs.promises.copyFile(src, dest);
  }
}

async function removeDirContents(dir, skipNames = new Set(['config.json'])) {
  const entries = await fs.promises.readdir(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (skipNames.has(entry.name)) continue;
    const target = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      await removeDirContents(target, skipNames);
      try { await fs.promises.rmdir(target); } catch (e) {}
    } else if (entry.isFile()) {
      try { await fs.promises.unlink(target); } catch (e) {}
    }
  }
}

const RIFF_DATA_ENTRIES = [
  'tracks', 'custom-lyrics', 'lyrics', 'thumbnails', 'library', 'bin', 'plugins', 'themes', 'logs', 'i18n',
  'metadata.json', 'library.json', 'startup.json', 'yt-cookies.txt', 'google-oauth.enc', 'spotify-auth.enc',
  '.yt-logged-out', '.rifftutorial'
];

async function copyDataEntries(fromDir, toDir) {
  await fs.promises.mkdir(toDir, { recursive: true });
  for (const name of RIFF_DATA_ENTRIES) {
    const src = path.join(fromDir, name);
    let st;
    try { st = await fs.promises.stat(src); } catch (e) { continue; }
    const dest = path.join(toDir, name);
    if (st.isDirectory()) await copyDirContents(src, dest);
    else if (st.isFile()) await fs.promises.copyFile(src, dest);
  }
}

async function removeDataEntries(dir) {
  for (const name of RIFF_DATA_ENTRIES) {
    try { await fs.promises.rm(path.join(dir, name), { recursive: true, force: true }); } catch (e) {}
  }
}

function isExecutableFile(p) {
  try {
    fs.accessSync(p, fs.constants.X_OK);
    return fs.statSync(p).isFile();
  } catch (e) {
    return false;
  }
}

function findInPath(name, env = process.env, platform = process.platform) {
  const win = platform === 'win32';
  const dirs = (env.PATH || env.Path || '').split(win ? ';' : ':').filter(Boolean);
  const extra = platform === 'darwin' ? ['/opt/homebrew/bin', '/usr/local/bin'] : [];
  const exts = win ? ['.exe', '.cmd', '.bat', ''] : [''];
  for (const dir of dirs.concat(extra)) {
    for (const ext of exts) {
      const candidate = path.join(dir, name + ext);
      if (isExecutableFile(candidate)) return candidate;
    }
  }
  return null;
}

const binaryCache = {};

function resolveBinary(name) {
  if (name in binaryCache) {
    const cached = binaryCache[name];
    if (cached === null || isExecutableFile(cached)) return cached;
    delete binaryCache[name];
  }
  const exe = IS_WIN ? `${name}.exe` : name;
  const envKey = `RIFF_${name.replace(/[^a-z0-9]/gi, '').toUpperCase()}`;
  const candidates = [
    process.env[envKey],
    path.join(BIN_DIR, exe),
    process.resourcesPath && path.join(process.resourcesPath, 'bin', exe),
    path.join(__dirname, '../../bin', exe)
  ].filter(Boolean);
  let found = candidates.find(isExecutableFile) || findInPath(name);
  if (!found && !IS_WIN) {
    const local = path.join(os.homedir(), '.local', 'bin', exe);
    if (isExecutableFile(local)) found = local;
  }
  binaryCache[name] = found || null;
  return binaryCache[name];
}

function ytDlpCommand(args) {
  const bin = resolveBinary('yt-dlp');
  if (!bin) {
    throw new Error('yt-dlp not found. Put yt-dlp and ffmpeg into the bin folder next to the app, or add them to PATH.');
  }
  const ffmpeg = resolveBinary('ffmpeg');
  const extra = ffmpeg && !args.includes('--ffmpeg-location') ? ['--ffmpeg-location', path.dirname(ffmpeg)] : [];
  return { bin, args: extra.concat(args) };
}

function jsRuntime() {
  if (findInPath('node') || !process.versions.electron) return { spec: 'node', env: {} };
  return { spec: `node:${process.execPath}`, env: { ELECTRON_RUN_AS_NODE: '1' } };
}

function clearBinaryCache() {
  Object.keys(binaryCache).forEach((k) => delete binaryCache[k]);
}

function cookieJarHasYtLogin(text) {
  try {
    let hasLoginInfo = false;
    let hasSapisid = false;
    let hasSid = false;
    for (const line of String(text || '').split(/\r?\n/)) {
      if (!line || line.startsWith('#')) continue;
      const parts = line.split('\t');
      if (parts.length < 7) continue;
      const domain = String(parts[0] || '').toLowerCase().replace(/^\./, '');
      if (domain !== 'youtube.com' && !domain.endsWith('.youtube.com')) continue;
      const name = parts[5];
      if (name === 'LOGIN_INFO') hasLoginInfo = true;
      else if (name === 'SAPISID') hasSapisid = true;
      else if (name === 'SID') hasSid = true;
    }
    return hasLoginInfo || (hasSapisid && hasSid);
  } catch (_) {
    return false;
  }
}

module.exports = {
  clearBinaryCache,
  IS_WIN, DATA_DIR, DEFAULT_DATA_DIR, CONFIG_PATH, BIN_DIR, CACHE_DIR, CUSTOM_LYRICS_DIR, LYRICS_DIR, THUMBNAILS_DIR, METADATA_PATH,
  LIBRARY_DIR, LIBRARY_PATH, STARTUP_PATH,
  findInPath, resolveBinary, ytDlpCommand, jsRuntime, cookieJarHasYtLogin,
  tutorialDone, markTutorialDone,
  readStartupConfig, writeStartupConfig,
  readConfig, resolveDataDir, setDataDir, resolveDownloadDir, defaultDownloadDir, setDownloadDir, copyDirContents, removeDirContents,
  RIFF_DATA_ENTRIES, copyDataEntries, removeDataEntries
};
