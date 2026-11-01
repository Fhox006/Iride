const http = require('http');
const https = require('https');
const keepAliveHttpsAgent = new https.Agent({
  keepAlive: true,
  keepAliveMsecs: 30000,
  maxSockets: 32,
  maxFreeSockets: 10,
  family: 4
});
const url = require('url');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { spawn } = require("child_process");
const { StringDecoder } = require("string_decoder");
const dns = require('dns');
const net = require('net');

const os = require('os');
const PORT = 38472;
let ACTIVE_PORT = PORT;
const { DATA_DIR, CACHE_DIR, CUSTOM_LYRICS_DIR, LYRICS_DIR, THUMBNAILS_DIR, METADATA_PATH, LIBRARY_DIR, LIBRARY_PATH, ytDlpCommand, resolveDownloadDir, defaultDownloadDir, resolveBinary, clearBinaryCache, cookieJarHasYtLogin } = require('../shared/platform');
const YT_COOKIES_FILE = path.join(DATA_DIR, 'yt-cookies.txt');

let ytCookieCache = { mtime: -1, valid: false };
function hasYtLoginCookies() {
  try {
    if (!fs.existsSync(YT_COOKIES_FILE)) {
      ytCookieCache = { mtime: -1, valid: false };
      return false;
    }
    const stat = fs.statSync(YT_COOKIES_FILE);
    if (stat.mtimeMs === ytCookieCache.mtime) return ytCookieCache.valid;
    const txt = fs.readFileSync(YT_COOKIES_FILE, 'utf8');
    const valid = cookieJarHasYtLogin(txt);
    ytCookieCache = { mtime: stat.mtimeMs, valid };
    return valid;
  } catch (_) {
    return false;
  }
}

function getYtAuthHeaders(origin = 'https://www.youtube.com') {
  try {
    if (!fs.existsSync(YT_COOKIES_FILE)) return null;
    const raw = fs.readFileSync(YT_COOKIES_FILE, 'utf8');
    const cookiePairs = [];
    let sapisid = '';
    for (const line of raw.split('\n')) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) continue;
      const parts = trimmed.split('\t');
      if (parts.length >= 7) {
        const domain = parts[0];
        const name = parts[5];
        const val = parts[6];
        if (domain.includes('youtube.com')) {
          cookiePairs.push(`${name}=${val}`);
          if (name === 'SAPISID' || (!sapisid && name === '__Secure-3PAPISID')) {
            sapisid = val;
          }
        }
      }
    }
    if (!sapisid || !cookiePairs.length) return null;
    const ts = Math.floor(Date.now() / 1000);
    const hash = crypto.createHash('sha1').update(`${ts} ${sapisid} ${origin}`).digest('hex');
    return {
      'Cookie': cookiePairs.join('; '),
      'Authorization': `SAPISIDHASH ${ts}_${hash}`,
      'Origin': origin,
      'X-Origin': origin,
      'X-Goog-AuthUser': '0'
    };
  } catch (_) {
    return null;
  }
}

const I18N_DIR = path.join(DATA_DIR, 'i18n');
try { fs.mkdirSync(I18N_DIR, { recursive: true }); } catch (_) {}
try {
  const files = fs.readdirSync(I18N_DIR);
  for (const f of files) {
    const p = path.join(I18N_DIR, f);
    const content = fs.readFileSync(p, 'utf8');
    if (content.includes('linux.do')) fs.unlinkSync(p);
  }
} catch (_) {}

const I18N_FIX = {
  ru: {
    'About': 'О приложении',
    'About the artist': 'Об исполнителе',
    'Follow': 'Подписаться',
    'Following': 'Вы подписаны',
    'Word': 'Слово',
    'Line': 'Строка',
    'Default output': 'Устройство по умолчанию',
    'No local tracks in library yet.': 'В библиотеке пока нет локальных треков.',
    'Add files': 'Добавить файлы'
  }
};

function i18nValueBroken(key, value) {
  if (typeof value !== 'string') return true;
  const v = value.trim();
  if (!v) return true;
  if (v.indexOf(String.fromCharCode(0xFFFD)) !== -1) return true;
  if (/https?:\/\/|linux\.do/i.test(v)) return true;
  return false;
}
const logger = require('../shared/logger');
const { getSystemAudioOutputInfo } = require('../shared/audio-output');

let serverActiveProxy = '';

function checkProxyAlive(proxyUrl, timeoutMs = 1200) {
  return new Promise((resolve) => {
    if (!proxyUrl) return resolve(false);
    try {
      const u = new URL(proxyUrl.includes('://') ? proxyUrl : ('socks5://' + proxyUrl));
      const port = parseInt(u.port, 10);
      const host = u.hostname || '127.0.0.1';
      if (!port) return resolve(false);
      const sock = net.connect({ host, port, timeout: timeoutMs });
      let done = false;
      const finish = (ok) => { if (done) return; done = true; try { sock.destroy(); } catch (_) {} resolve(ok); };
      sock.on('connect', () => finish(true));
      sock.on('timeout', () => finish(false));
      sock.on('error', () => finish(false));
    } catch (_) {
      resolve(false);
    }
  });
}

const _nativeFetch = typeof globalThis.fetch === 'function' ? globalThis.fetch.bind(globalThis) : null;
function riffFetch(...args) {
  if (serverActiveProxy && typeof global.riffSessionFetch === 'function') {
    return global.riffSessionFetch(...args);
  }
  return _nativeFetch ? _nativeFetch(...args) : Promise.reject(new Error('fetch not available'));
}
globalThis.fetch = riffFetch;
const fetch = riffFetch;

function stripYoutubeTopic(artist) {
  if (!artist || typeof artist !== 'string') return artist || '';
  return artist.endsWith(' - Topic') ? artist.slice(0, -8).trim() : artist;
}

function boundMap(map, maxSize = 35) {
  if (!map) return;
  if (map instanceof Map) {
    while (map.size > maxSize) {
      const firstKey = map.keys().next().value;
      map.delete(firstKey);
    }
  } else if (typeof map === 'object') {
    const keys = Object.keys(map);
    if (keys.length > maxSize) {
      for (let i = 0; i < keys.length - maxSize; i++) {
        delete map[keys[i]];
      }
    }
  }
}

const MAX_BODY_BYTES = 2 * 1024 * 1024;
const SAFE_MEDIA_URL = /^https?:\/\//i;

function isSafeMediaUrl(value) {
  return typeof value === 'string' && SAFE_MEDIA_URL.test(value);
}

function ipv4IsPrivate(a, b) {
  if (a === 0 || a === 10 || a === 127) return true;
  if (a === 169 && b === 254) return true;
  if (a === 172 && b >= 16 && b <= 31) return true;
  if (a === 192 && b === 168) return true;
  return false;
}

function isPrivateIp(address) {
  const kind = net.isIP(address);
  if (kind === 4) {
    const p = address.split('.').map(Number);
    return ipv4IsPrivate(p[0], p[1]);
  }
  if (kind === 6) {
    const h = address.toLowerCase();
    if (h === '::' || h === '::1') return true;
    if (h.startsWith('fe8') || h.startsWith('fe9') || h.startsWith('fea') || h.startsWith('feb')) return true;
    if (h.startsWith('fc') || h.startsWith('fd')) return true;
    const mapped = h.match(/^::ffff:(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
    if (mapped) return ipv4IsPrivate(Number(mapped[1]), Number(mapped[2]));
    return false;
  }
  return true;
}

function normalizeNumericHost(h) {
  if (/^\d+$/.test(h) && h.length < 11) {
    const n = Number(h);
    if (n >= 0 && n <= 0xffffffff) {
      return [(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, n & 255].join('.');
    }
  }
  const hex = h.match(/^0x([0-9a-f]+)$/);
  if (hex) {
    const n = parseInt(hex[1], 16);
    if (n >= 0 && n <= 0xffffffff) {
      return [(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, n & 255].join('.');
    }
  }
  return null;
}

async function isPrivateHost(hostname) {
  let host = (hostname || '').toLowerCase().replace(/^\[|\]$/g, '');
  if (!host) return true;
  if (host === 'localhost' || host.endsWith('.localhost') || host.endsWith('.local') || host.endsWith('.internal')) return true;
  const numeric = normalizeNumericHost(host);
  if (numeric) host = numeric;
  if (net.isIP(host)) return isPrivateIp(host);
  try {
    const addrs = await dns.promises.lookup(host, { all: true });
    if (!addrs.length) return true;
    return addrs.some((a) => isPrivateIp(a.address));
  } catch (e) {
    return true;
  }
}

function streamFileToResponse(stream, res) {
  stream.on('error', () => {
    if (!res.headersSent) {
      try { res.writeHead(500); } catch (e) {}
    }
    try { res.end('Error reading file'); } catch (e) {}
    try { stream.destroy(); } catch (e) {}
  });
  res.on('close', () => { try { stream.destroy(); } catch (e) {} });
  stream.pipe(res);
}

const DOWNLOAD_TIMEOUT_MS = 10 * 60 * 1000;

function parseByteRange(rangeHeader, fileSize) {
  const match = /^bytes=(\d*)-(\d*)$/.exec(String(rangeHeader).trim());
  if (!match) return null;
  const startRaw = match[1];
  const endRaw = match[2];
  if (startRaw === '' && endRaw === '') return null;
  let start;
  let end;
  if (startRaw === '') {
    const suffix = parseInt(endRaw, 10);
    if (!Number.isFinite(suffix) || suffix <= 0) return null;
    start = Math.max(0, fileSize - suffix);
    end = fileSize - 1;
  } else {
    start = parseInt(startRaw, 10);
    end = endRaw === '' ? fileSize - 1 : parseInt(endRaw, 10);
  }
  if (!Number.isFinite(start) || !Number.isFinite(end) || start > end || start >= fileSize) return null;
  if (end >= fileSize) end = fileSize - 1;
  return { start, end };
}

if (!fs.existsSync(CACHE_DIR)) fs.mkdirSync(CACHE_DIR, { recursive: true });
if (!fs.existsSync(CUSTOM_LYRICS_DIR)) fs.mkdirSync(CUSTOM_LYRICS_DIR, { recursive: true });
if (!fs.existsSync(LYRICS_DIR)) fs.mkdirSync(LYRICS_DIR, { recursive: true });
if (!fs.existsSync(THUMBNAILS_DIR)) fs.mkdirSync(THUMBNAILS_DIR, { recursive: true });
if (!fs.existsSync(LIBRARY_DIR)) fs.mkdirSync(LIBRARY_DIR, { recursive: true });

let trackMetadataIndex = {};
try {
  if (fs.existsSync(METADATA_PATH)) {
    trackMetadataIndex = JSON.parse(fs.readFileSync(METADATA_PATH, 'utf-8'));
  }
} catch (e) {
  trackMetadataIndex = {};
}

function saveTrackMetadata(cleanId, meta) {
  if (!cleanId) return;
  trackMetadataIndex[cleanId] = {
    ...(trackMetadataIndex[cleanId] || {}),
    ...meta,
    lastPlayed: Date.now()
  };
  try {
    fs.writeFileSync(METADATA_PATH, JSON.stringify(trackMetadataIndex, null, 2), 'utf-8');
  } catch (e) {}
}

let libraryIndex = {};
try {
  if (fs.existsSync(LIBRARY_PATH)) {
    libraryIndex = JSON.parse(fs.readFileSync(LIBRARY_PATH, 'utf-8'));
  }
} catch (e) {
  libraryIndex = {};
}

function saveLibraryIndex() {
  try {
    fs.writeFileSync(LIBRARY_PATH, JSON.stringify(libraryIndex, null, 2), 'utf-8');
  } catch (e) {}
}

const searchCache = new Map();
const streamUrlCache = new Map();
const streamCache = streamUrlCache;
const urlCache = streamUrlCache;
const streamInfoInflight = new Map();
const lyricsCache = new Map();
const artistCache = new Map();
const vibeCache = new Map();
const ytArtistCache = new Map();
const scClientCache = new Map();
const downloadingSet = new Set();
const prefetchingSet = new Set();
const downloadInFlight = new Map();

[lyricsCache, searchCache, streamUrlCache, artistCache, scClientCache].forEach(m => {
  const origSet = m.set.bind(m);
  m.set = function(k, v) {
    const res = origSet(k, v);
    boundMap(m, 35);
    return res;
  };
});

const cacheMaintenanceTimer = setInterval(() => {
  boundMap(searchCache, 20);
  boundMap(lyricsCache, 30);
  if (global.gc) { try { global.gc(); } catch (_) {} }
}, 300000);
cacheMaintenanceTimer.unref();

const MAX_CACHE_ENTRIES = 500;
function cacheSet(map, key, value, max = MAX_CACHE_ENTRIES) {
  if (!map.has(key) && map.size >= max) {
    map.delete(map.keys().next().value);
  }
  map.set(key, value);
  boundMap(map, 35);
}

let currentAppState = {};
const sseClients = new Set();
const YT_PLAYER_CLIENTS = ['default', 'android_vr'];
const YT_EXTRACTOR_ARGS = 'youtube:player_client=' + YT_PLAYER_CLIENTS.join(',');
const ytContentLengthByUrl = new Map();
function rememberYtContentLength(u, len) {
  const n = parseInt(len, 10);
  if (!u || !n) return;
  ytContentLengthByUrl.set(u, n);
  if (ytContentLengthByUrl.size > 300) ytContentLengthByUrl.delete(ytContentLengthByUrl.keys().next().value);
}
let lastGoodYtClient = null;

let _bgYtDlpActive = 0;
const _bgYtDlpQueue = [];
function bgYtDlp(fn) {
  return new Promise((resolve, reject) => {
    const run = () => {
      _bgYtDlpActive++;
      Promise.resolve().then(fn).then(resolve, reject).finally(() => {
        _bgYtDlpActive--;
        if (_bgYtDlpQueue.length > 0) _bgYtDlpQueue.shift()();
      });
    };
    if (_bgYtDlpActive < 1) { run(); } else { _bgYtDlpQueue.push(run); }
  });
}

function broadcastSSE(data) {
  const msg = `data: ${JSON.stringify(data)}\n\n`;
  for (const client of sseClients) {
    try {
      client.write(msg);
    } catch (e) {
      sseClients.delete(client);
    }
  }
}

function cleanTrackId(id) {
  return (id || '').replace(/[^a-zA-Z0-9_-]/g, '_');
}

async function findCachedFile(trackId) {
  const cleanId = cleanTrackId(trackId);
  const extensions = ['.opus', '.webm', '.m4a', '.mp3'];
  for (const ext of extensions) {
    const filePath = path.join(CACHE_DIR, `${cleanId}${ext}`);
    try {
      const stat = await fs.promises.stat(filePath);
      if (stat.size > 60000) {
        const now = new Date();
        try {
          await fs.promises.utimes(filePath, now, now);
        } catch (e) {}
        return filePath;
      }
    } catch (e) {}
  }
  return null;
}

function findCachedFileByMeta(title, artist) {
  if (!title) return null;
  const targetTitle = title.toLowerCase().trim();
  const targetArtist = (artist || '').toLowerCase().trim();
  for (const [cid, meta] of Object.entries(trackMetadataIndex)) {
    if (meta && meta.title && meta.title.toLowerCase().trim() === targetTitle) {
      const metaArtist = (meta.artist || '').toLowerCase().trim();
      if (!artist || !meta.artist || metaArtist === targetArtist || metaArtist.includes(targetArtist)) {
        const extensions = ['.opus', '.webm', '.m4a', '.mp3'];
        for (const ext of extensions) {
          const fp = path.join(CACHE_DIR, `${cid}${ext}`);
          if (fs.existsSync(fp)) return fp;
        }
      }
    }
  }
  return null;
}

async function pruneDiskCache() {
  try {
    const files = await fs.promises.readdir(CACHE_DIR);
    const now = Date.now();
    for (const file of files) {
      const fullPath = path.join(CACHE_DIR, file);
      try {
        const stat = await fs.promises.stat(fullPath);
        if (file.includes('.temp.')) {
          if (now - stat.mtimeMs > 10 * 60 * 1000) {
            await fs.promises.unlink(fullPath);
          }
          continue;
        }
        if (stat.size < 1000) {
          await fs.promises.unlink(fullPath);
          continue;
        }
      } catch (e) {}
    }
  } catch (e) {
    console.warn('Prune cache error:', e);
  }
}

pruneDiskCache();

function parseDurationSeconds(str) {
  if (!str) return 0;
  const parts = str.split(':').map(Number);
  if (parts.length === 2) return parts[0] * 60 + parts[1];
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
  return 0;
}

function isSpamOrUnofficial(title, duration, channelName = '') {
  if (duration && (duration < 90 || duration > 420)) return true;

  const t = (title || '').toLowerCase();
  const c = (channelName || '').toLowerCase();
  const combined = `${t} ${c}`;

  const nonMusicWordPatterns = [
    /\blecture\b/i,
    /\btalk\b/i,
    /\bconversation\b/i,
    /\bin conversation\b/i,
    /\bacademy\b/i,
    /\bmasterclass\b/i,
    /\bdocumentary\b/i,
    /\bbehind the scenes\b/i,
    /\bmaking of\b/i,
    /\bexplained\b/i,
    /\bhistory of\b/i,
    /\bhow to\b/i,
    /\bnews\b/i,
    /\blive stream\b/i,
    /\blivestream\b/i,
    /\bfull concert\b/i,
    /\bfull set\b/i,
    /\blive at\b/i,
    /\bofficial video essay\b/i
  ];
  if (nonMusicWordPatterns.some(pat => pat.test(combined))) return true;

  const nonMusicPatterns = [
    /\bepisode\s*\d+/i,
    /\bseason\s*\d+/i,
    /\bs\d+e\d+\b/i,
    /\bep\s*\.?\s*\d+\b/i,
    /\bseries\b/i,
    /\btrailer\b/i,
    /\bteaser\b/i,
    /\bfull movie\b/i,
    /\bshort film\b/i,
    /\bpodcast\b/i,
    /\bgameplay\b/i,
    /\bwalkthrough\b/i,
    /\bplaythrough\b/i,
    /\breview\b/i,
    /\breaction\b/i,
    /\binterview\b/i,
    /\baudiobook\b/i,
    /\bvlog\b/i,
    /\bunboxing\b/i,
    /\btutorial\b/i,
    /\bparody\b/i
  ];
  if (nonMusicPatterns.some(pat => pat.test(combined))) return true;

  const badKeywords = [
    '1 hour', '2 hours', '10 hours', 'hour loop', 'hours loop',
    'full album', 'discography', 'ost compilation', 'soundtrack compilation',
    'playlist mix', 'dj set', 'type beat', 'slowed + reverb', 'slowed down',
    'sped up', 'bass boosted', 'nightcore', 'reaction to', 'reacting to'
  ];
  if (badKeywords.some(bad => combined.includes(bad))) return true;

  return false;
}

function safeJsonParse(value, fallback) {
  try {
    const parsed = JSON.parse(value);
    return parsed ?? fallback;
  } catch (error) {
    return fallback;
  }
}

function getTrackNormalizedKey(artist, title) {
  let a = String(artist || '').toLowerCase();
  let t = String(title || '').toLowerCase();
  t = t.replace(/\s*\([^)]*official[^)]*\)/gi, '')
       .replace(/\s*\[[^\]]*\]/gi, '')
       .replace(/\s*\([^)]*(video|audio|remaster(ed)?|lyrics?|visualizer)[^)]*\)/gi, '');
  t = t.replace(/\s*(feat\.?|ft\.?)\s+[^(\[-]+/gi, '')
       .replace(/\s*\((feat\.?|ft\.?)[^)]*\)/gi, '');
  a = a.replace(/\s*(feat\.?|ft\.?)\s+.*$/gi, '');
  a = a.replace(/[^\p{L}\p{N}\s]/gu, ' ').replace(/\s+/g, ' ').trim();
  t = t.replace(/[^\p{L}\p{N}\s]/gu, ' ').replace(/\s+/g, ' ').trim();
  return `${a}::${t}`;
}

function normalizeTrack(track) {
  if (!track || typeof track !== 'object') return null;
  const title = String(track.title || '').trim();
  const artist = String(track.artist || '').trim();
  const id = String(track.id || track.url || `${artist.toLowerCase()}:${title.toLowerCase()}`).trim();
  if (!id && !title) return null;
  return {
    id,
    title: title || 'untitled',
    artist: artist || 'unknown artist',
    duration: Number(track.duration) || 0,
    thumbnail: track.thumbnail || '',
    url: track.url || '',
    platform: track.platform || 'soundcloud',
    searchQuery: track.searchQuery || ''
  };
}

function normalizeArtistName(name) {
  return String(name || '')
    .replace(/\b(official|vevo|records|topic)\b/gi, '')
    .replace(/[^\w\s&-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

function trackFingerprint(track) {
  const clean = normalizeTrack(track);
  if (!clean) return '';
  return getTrackNormalizedKey(clean.artist, clean.title);
}

function cleanSearchResults(tracks, query) {
  const seenIds = new Set();
  const seenTracks = new Set();
  return (Array.isArray(tracks) ? tracks : [])
    .filter((track) => {
      const clean = normalizeTrack(track);
      if (!clean) return false;
      if (/\b\d+\s*(?:hours?|hrs?)\b/i.test(clean.title) ||
          /\b(?:playlist|compilation)\s+of\b/i.test(clean.title)) return false;
      const idKey = clean.id ? `${clean.platform}:${clean.id}` : '';
      const fingerprint = trackFingerprint(track);
      if (idKey && seenIds.has(idKey)) return false;
      if (fingerprint && seenTracks.has(fingerprint)) return false;
      if (idKey) seenIds.add(idKey);
      if (fingerprint) seenTracks.add(fingerprint);
      return true;
    });
}

const recentlyRecommendedTrackIds = new Set();
const recentlyRecommendedKeys = new Set();

function normalizeSoundCloudUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') return rawUrl;
  try {
    const dec = decodeURIComponent(rawUrl);
    const m = dec.match(/soundcloud:tracks:(\d+)/i);
    if (m && m[1]) return 'https://api.soundcloud.com/tracks/' + m[1];
  } catch (_) {}
  return rawUrl;
}

function extractYtIdFromAny(urlOrId) {
  const s = String(urlOrId || '').trim();
  if (!s || /^(cand_|dz_|it_|sp_|ym_|alb_|local_|track_|dz-|it-)/i.test(s)) return '';
  if (/^[a-zA-Z0-9_-]{11}$/.test(s)) return s;
  const m = s.match(/(?:v=|youtu\.be\/|\/embed\/|\/v\/)([a-zA-Z0-9_-]{11})/);
  return (m && !/^(cand_|dz_|it_|sp_|ym_)/i.test(m[1])) ? m[1] : '';
}

function evictStreamCache(rawUrl) {
  if (!rawUrl) return;
  for (const [k, v] of streamUrlCache.entries()) {
    if (v && v.data && (v.data.rawStreamUrl === rawUrl || v.data.streamUrl === rawUrl || (typeof v.data.streamUrl === 'string' && v.data.streamUrl.includes(encodeURIComponent(rawUrl))))) {
      streamUrlCache.delete(k);
    }
  }
}

function verifyStreamPlayable(streamUrl, userAgent, redirectsLeft = 3) {
  return new Promise((resolve) => {
    if (!streamUrl || typeof streamUrl !== 'string') return resolve(false);
    if (streamUrl.includes('.m3u8')) return resolve(true);
    let settled = false;
    const done = (ok) => {
      if (settled) return;
      settled = true;
      resolve(ok);
    };
    try {
      const parsed = new URL(streamUrl);
      const cParam = (parsed.searchParams.get('c') || '').toUpperCase();
      let ua = userAgent || 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
      if (!userAgent) {
        if (cParam === 'IOS') ua = 'com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)';
        else if (cParam === 'ANDROID_VR') ua = 'com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip';
      }
      const isGooglevideo = parsed.hostname.endsWith('googlevideo.com');
      const req = https.request(streamUrl, {
        method: 'GET',
        agent: keepAliveHttpsAgent,
        family: 4,
        timeout: 5000,
        headers: Object.assign({
          'Host': parsed.host,
          'Range': 'bytes=0-',
          'User-Agent': ua,
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          'Accept-Language': 'en-us,en;q=0.5',
          'Sec-Fetch-Mode': 'navigate'
        }, isGooglevideo ? {
          'Origin': 'https://www.youtube.com',
          'Referer': 'https://www.youtube.com/'
        } : {})
      }, (res) => {
        if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location && redirectsLeft > 0) {
          res.resume();
          const nextUrl = new URL(res.headers.location, streamUrl).toString();
          return verifyStreamPlayable(nextUrl, ua, redirectsLeft - 1).then(done);
        }
        if (res.statusCode !== 200 && res.statusCode !== 206) {
          res.destroy();
          return done(false);
        }
        let received = 0;
        res.on('data', (chunk) => {
          received += chunk.length;
          if (received >= 16384) {
            res.destroy();
            done(true);
          }
        });
        res.on('end', () => done(received >= 16384 || received > 0));
        res.on('error', () => done(false));
      });
      req.on('timeout', () => { req.destroy(); done(false); });
      req.on('error', () => done(false));
      req.end();
    } catch (_) {
      done(false);
    }
  });
}
const verifyGoogleVideoStream = verifyStreamPlayable;

let cachedYtVisitorData = '';
let cachedYtVisitorTs = 0;
async function getYtVisitorData() {
  if (cachedYtVisitorData && (Date.now() - cachedYtVisitorTs < 30 * 60 * 1000)) return cachedYtVisitorData;
  try {
    const r = await fetch('https://www.youtube.com/sw.js_data', {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36' },
      signal: AbortSignal.timeout(2500)
    });
    if (r.ok) {
      const txt = await r.text();
      const m = txt.match(/["']([Cgt][a-zA-Z0-9_%-]{10,60})["']/);
      if (m && m[1]) {
        cachedYtVisitorData = m[1];
        cachedYtVisitorTs = Date.now();
        return cachedYtVisitorData;
      }
    }
  } catch (_) {}
  return cachedYtVisitorData;
}

async function fetchDirectYoutubeStreamServer(videoId, quality = 'medium') {
  const vid = extractYtIdFromAny(videoId);
  if (!vid) return null;
  const visitorData = '';
  const clients = [
    {
      clientName: 'IOS',
      clientVersion: '20.10.4',
      deviceMake: 'Apple',
      deviceModel: 'iPhone16,2',
      userAgent: 'com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)',
      osName: 'iOS',
      osVersion: '18.3.2.22D60'
    },
    {
      clientName: 'ANDROID_MUSIC',
      clientVersion: '7.27.52',
      androidSdkVersion: 34,
      userAgent: 'com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14; en_US; Quest 3) gzip',
      host: 'music.youtube.com'
    }
  ];

  const authHeaders = getYtAuthHeaders('https://www.youtube.com');
  if (authHeaders) {
    clients.unshift({
      clientName: 'TVHTML5',
      clientVersion: '7.20250312.16.00',
      userAgent: 'Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) 85.0.4183.93/6.5 TV Safari/537.36',
      extraHeaders: authHeaders
    });
  }

  const queryClient = (c) => new Promise((resolve, reject) => {
    const clientPayload = {
      clientName: c.clientName,
      clientVersion: c.clientVersion,
      hl: 'en',
      gl: 'US'
    };
    if (visitorData && !c.extraHeaders) clientPayload.visitorData = visitorData;
    if (c.androidSdkVersion) clientPayload.androidSdkVersion = c.androidSdkVersion;
    if (c.deviceMake) clientPayload.deviceMake = c.deviceMake;
    if (c.deviceModel) clientPayload.deviceModel = c.deviceModel;
    if (c.osName) clientPayload.osName = c.osName;
    if (c.osVersion) clientPayload.osVersion = c.osVersion;

    const ctx = { client: clientPayload };
    if (c.embedded) ctx.thirdParty = { embedUrl: 'https://www.youtube.com/' };

    const bodyPayload = JSON.stringify({
      videoId: vid,
      context: ctx,
      contentCheckOk: true,
      racyCheckOk: true
    });

    const reqHeaders = Object.assign({
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(bodyPayload),
      'User-Agent': c.userAgent,
      'X-Goog-Api-Format-Version': '2'
    }, c.extraHeaders || (visitorData ? { 'X-Goog-Visitor-Id': visitorData } : {}));

    const req = https.request({
      hostname: c.host || 'www.youtube.com',
      path: '/youtubei/v1/player?prettyPrint=false',
      method: 'POST',
      agent: keepAliveHttpsAgent,
      family: 4,
      timeout: 2200,
      headers: reqHeaders
    }, (res) => {
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', chunk => { raw += chunk; });
      res.on('end', () => {
        if (res.statusCode !== 200) return reject(new Error('status ' + res.statusCode));
        try {
          const data = JSON.parse(raw);
          if (data.playabilityStatus && data.playabilityStatus.status !== 'OK') {
            return reject(new Error('playability: ' + (data.playabilityStatus.status || 'UNPLAYABLE')));
          }
          const adaptive = (data.streamingData && data.streamingData.adaptiveFormats) || [];
          const muxed = (data.streamingData && data.streamingData.formats) || [];
          const allFormats = [...adaptive, ...muxed];
          const audioFormats = allFormats.filter(f => f && f.url && !f.signatureCipher && !f.cipher && typeof f.mimeType === 'string' && f.mimeType.startsWith('audio/'));
          if (audioFormats.length > 0) {
            let pool = audioFormats;
            if (quality === 'low') {
              pool.sort((a, b) => (a.bitrate || 0) - (b.bitrate || 0));
            } else if (quality === 'high' || quality === 'max') {
              pool.sort((a, b) => (b.bitrate || 0) - (a.bitrate || 0));
            } else {
              const m4a = audioFormats.filter(f => f.mimeType.includes('mp4') || f.mimeType.includes('m4a'));
              const webm = audioFormats.filter(f => f.mimeType.includes('webm') || f.mimeType.includes('opus'));
              pool = m4a.length > 0 ? m4a : webm.length > 0 ? webm : audioFormats;
              pool.sort((a, b) => (b.bitrate || 0) - (a.bitrate || 0));
            }
            return resolve(pool[0].url);
          }
          const itag18 = muxed.find(f => f && f.itag === 18 && f.url && !f.signatureCipher && !f.cipher);
          if (itag18) return resolve(itag18.url);
          reject(new Error('no audio formats'));
        } catch (e) {
          reject(e);
        }
      });
    });
    req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
    req.on('error', reject);
    req.write(bodyPayload);
    req.end();
  });

  try {
    return await Promise.any(clients.map(queryClient));
  } catch (_) {
    return null;
  }
}

async function resolveScTranscodingServer(trackObj, cid) {
  if (!trackObj || !trackObj.media || !Array.isArray(trackObj.media.transcodings)) return null;
  const list = trackObj.media.transcodings.filter(t => !t.snipped);
  const prog = list.find(t => t.format && t.format.protocol === 'progressive');
  const hls = list.find(t => t.format && t.format.protocol === 'hls' && !/encrypted/i.test(t.format.mime_type || ''));
  const chosen = prog || hls || list[0];
  if (!chosen || !chosen.url || chosen.snipped === true) return null;
  const sep = chosen.url.includes('?') ? '&' : '?';
  const auth = trackObj.track_authorization ? `&track_authorization=${encodeURIComponent(trackObj.track_authorization)}` : '';
  const res = await fetch(`${chosen.url}${sep}client_id=${cid}${auth}`, { headers: { 'User-Agent': 'Mozilla/5.0' } });
  if (!res.ok) return null;
  const j = await res.json();
  const u = (j && j.url) ? j.url : null;
  if (!u || u.includes('/preview/') || u.includes('cf-preview-media')) return null;
  return u;
}

let serverScClientId = 'tHVbSbDP4TaoMbPppngv6Tdry83vNbMQ';
let serverScClientIdTs = 0;
scClientCache.set('clientId', serverScClientId);
boundMap(scClientCache, 35);

async function refreshSoundCloudClientId(force = false) {
  if (!force && serverScClientId && (Date.now() - serverScClientIdTs < 30 * 60 * 1000)) {
    return serverScClientId;
  }
  try {
    const html = await fetch('https://soundcloud.com', {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/131.0.0.0 Safari/537.36' }
    }).then(r => r.text());
    const scripts = [...html.matchAll(/src="(https:\/\/a-v2\.sndcdn\.com\/assets\/[^"]+\.js)"/g)].map(m => m[1]);
    for (let i = scripts.length - 1; i >= Math.max(0, scripts.length - 5); i--) {
      try {
        const js = await fetch(scripts[i]).then(r => r.text());
        const m = js.match(/client_id\s*[:=]\s*["']([a-zA-Z0-9]{32})["']/);
        if (m && m[1]) {
          serverScClientId = m[1];
          serverScClientIdTs = Date.now();
          scClientCache.set('clientId', serverScClientId);
          boundMap(scClientCache, 35);
          return serverScClientId;
        }
      } catch (_) {}
    }
  } catch (_) {}
  return serverScClientId;
}

const refreshServerScClientId = () => refreshSoundCloudClientId(true);

async function fetchDirectSoundCloudStreamServer(scUrlOrId, queryFallback) {
  let cid = await refreshSoundCloudClientId(false);
  const norm = normalizeSoundCloudUrl(scUrlOrId || '');
  const idMatch = String(norm).match(/(?:tracks\/|^)(\d{6,})/i);
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      if (idMatch && idMatch[1]) {
        const r = await fetch(`https://api-v2.soundcloud.com/tracks/${idMatch[1]}?client_id=${cid}`, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        if (r.status === 401 || r.status === 403) { cid = await refreshServerScClientId(); continue; }
        if (r.ok) {
          const tObj = await r.json();
          const u = await resolveScTranscodingServer(tObj, cid);
          if (u && !u.includes('/preview/') && !u.includes('cf-preview-media')) return u;
        }
      }
      if (queryFallback) {
        const sr = await fetch(`https://api-v2.soundcloud.com/search/tracks?q=${encodeURIComponent(queryFallback)}&limit=5&client_id=${cid}`, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        if (sr.status === 401 || sr.status === 403) { cid = await refreshServerScClientId(); continue; }
        if (sr.ok) {
          const sj = await sr.json();
          const col = (sj && Array.isArray(sj.collection)) ? sj.collection : [];
          const pick = col.find(t => t.policy !== 'SNIP' && t.duration > 30000 && !t.snipped) || col[0];
          if (pick) {
            const u = await resolveScTranscodingServer(pick, cid);
            if (u && !u.includes('/preview/') && !u.includes('cf-preview-media')) return u;
          }
        }
      }
      break;
    } catch (_) {}
  }
  return null;
}

async function searchSoundcloud(query, limit = 14) {
  const cacheKey = `sc:${limit}:${String(query || '').trim().toLowerCase()}`;
  if (searchCache.has(cacheKey)) {
    return searchCache.get(cacheKey);
  }

  const searchPromise = (async () => {
    const args = [
      `scsearch${limit}:${query}`,
      '--dump-single-json',
      '--flat-playlist',
      '--skip-download',
      '--no-warnings',
      '--no-check-certificates'
    ];
    try {
      const raw = await executeYtDlp(args, 18000);
      const data = JSON.parse(raw);
      const entries = (data.entries || []).map((item) => {
        let thumbnail = '';
        if (item.thumbnails && item.thumbnails.length > 0) {
          thumbnail = item.thumbnails[item.thumbnails.length - 1].url;
        } else if (item.thumbnail) {
          thumbnail = item.thumbnail;
        }

        const cleanTitle = (item.title || 'untitled');
        const cleanArtist = (item.uploader || item.channel || item.artist || 'unknown artist');
        const trackUrl = normalizeSoundCloudUrl(item.webpage_url || item.url || '');

        return {
          id: String(item.id || Math.random().toString(36).substring(2)),
          title: cleanTitle,
          artist: cleanArtist,
          duration: item.duration || 0,
          thumbnail: thumbnail,
          url: trackUrl,
          platform: 'soundcloud'
        };
      });

      if (entries.length > 0) {
        searchCache.set(cacheKey, entries);
        setTimeout(() => { searchCache.delete(cacheKey); }, 300000);
      } else if (searchCache.get(cacheKey) === searchPromise) {
        searchCache.delete(cacheKey);
      }
      return entries;
    } catch (err) {
      if (searchCache.get(cacheKey) === searchPromise) {
        searchCache.delete(cacheKey);
      }
      return [];
    }
  })();
  searchCache.set(cacheKey, searchPromise);
  return searchPromise;
}

function buildDiscoveryQueries(profile) {
  const artists = (profile.topArtists || []).map(a => typeof a === 'string' ? a : a.name).filter(Boolean);
  const shuffled = [...artists].sort(() => Math.random() - 0.5);
  const queries = [];

  if (shuffled.length > 0) {
    queries.push(shuffled[0]);
    if (shuffled.length > 1) {
      queries.push(shuffled[1]);
    }
  }

  if (profile.dominantScript === 'Cyrillic' && shuffled.length > 0) {
    queries.push(`${shuffled[0]} похожие песни`);
  }

  if (queries.length === 0) {
    queries.push('synthwave chill', 'indie electronic');
  }

  return queries;
}

function scoreDiscoveryCandidate(track, profile, heardKeys, heardIds) {
  const normalized = normalizeTrack(track);
  if (!normalized) return null;
  const key = getTrackNormalizedKey(normalized.artist, normalized.title);
  if (!normalized.url || !normalized.id) return null;

  if (heardIds.has(normalized.id) || (key && heardKeys.has(key))) return null;
  if (recentlyRecommendedTrackIds.has(normalized.id) || (key && recentlyRecommendedKeys.has(key))) return null;
  if (isSpamOrUnofficial(normalized.title, normalized.duration, normalized.artist)) return null;

  let score = 1.0;

  if (/-\s*topic$/i.test(normalized.artist || '')) {
    score += 1.5;
  }

  if (/^.+?\s+-\s+.+$/.test(normalized.title || '')) {
    score += 1.0;
  }

  const candArtist = normalizeArtistName(normalized.artist);
  const artistNames = (profile.topArtists || []).map(a => typeof a === 'string' ? a : a.name);
  if (artistNames.some(a => candArtist.includes(a) || a.includes(candArtist))) {
    score += 3.0;
  }

  const isCyrillic = /[\u0400-\u04FF]/.test(normalized.title);
  if (profile.dominantScript === 'Cyrillic' && isCyrillic) {
    score += 2.0;
  } else if (profile.dominantScript !== 'Cyrillic' && !isCyrillic) {
    score += 1.0;
  }

  if (normalized.duration > 0 && profile.typicalDuration > 0) {
    const delta = Math.abs(normalized.duration - profile.typicalDuration);
    score += Math.max(0, 2.0 - (delta / 60));
  }

  score += Math.random() * 1.5;

  return {
    ...normalized,
    score
  };
}

async function httpGetJson(url, timeoutMs = 6000) {
  return new Promise((resolve) => {
    try {
      const parsed = new URL(url);
      const mod = parsed.protocol === 'http:' ? http : https;
      const req = mod.get(url, { family: 4, headers: { 'User-Agent': 'Riff' }, timeout: timeoutMs }, (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          res.resume();
          return resolve(null);
        }
        let data = '';
        let bytesRead = 0;
        res.on('data', chunk => {
          bytesRead += chunk.length;
          if (bytesRead > 1024 * 1024) {
            res.destroy();
            return resolve(null);
          }
          data += chunk;
        });
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch (_) {
            resolve(null);
          }
        });
      });
      req.on('timeout', () => {
        req.destroy();
        resolve(null);
      });
      req.on('error', () => {
        resolve(null);
      });
    } catch (_) {
      resolve(null);
    }
  });
}

const dzArtistIdCache = new Map();

async function dzArtistId(name) {
  if (!name || typeof name !== 'string') return null;
  const clean = normalizeArtistName(name);
  if (!clean) return null;
  const cached = dzArtistIdCache.get(clean);
  if (cached && Date.now() < cached.expires) {
    return cached.id;
  }
  if (cached?.promise) {
    return cached.promise;
  }

  const lookupPromise = (async () => {
    try {
      const url = `https://api.deezer.com/search/artist?q=${encodeURIComponent(name.trim())}&limit=1`;
      const data = await httpGetJson(url, 6000);
      const id = data?.data?.[0]?.id ? String(data.data[0].id) : null;
      dzArtistIdCache.set(clean, { id, expires: Date.now() + 24 * 3600 * 1000 });
      return id;
    } catch (_) {
      if (dzArtistIdCache.get(clean)?.promise === lookupPromise) {
        dzArtistIdCache.delete(clean);
      }
      return null;
    }
  })();
  dzArtistIdCache.set(clean, { promise: lookupPromise, expires: 0 });
  return lookupPromise;
}

async function dzRelatedTracks(artist) {
  try {
    const id = await dzArtistId(artist);
    if (!id) return [];
    const relUrl = `https://api.deezer.com/artist/${encodeURIComponent(id)}/related?limit=25`;
    const relData = await httpGetJson(relUrl, 6000);
    const relatedList = (relData?.data || []).slice().sort(() => Math.random() - 0.5).slice(0, 6);
    if (relatedList.length === 0) return [];

    const tracksNested = await Promise.all(relatedList.map(async (relArtist, relatedIndex) => {
      try {
        if (!relArtist?.id) return [];
        const topUrl = `https://api.deezer.com/artist/${encodeURIComponent(relArtist.id)}/top?limit=15`;
        const topData = await httpGetJson(topUrl, 6000);
        const topTracks = (topData?.data || []).slice().sort(() => Math.random() - 0.5).slice(0, 4);
        const match = 1 - (relatedIndex / 12) * 0.6;
        return topTracks.map(item => ({
          artist: item.artist?.name || relArtist.name || 'unknown artist',
          title: item.title || 'untitled',
          match,
          source: 'deezer'
        }));
      } catch (_) {
        return [];
      }
    }));
    return tracksNested.flat();
  } catch (_) {
    return [];
  }
}

async function dzRadioTracks(artist) {
  try {
    const id = await dzArtistId(artist);
    if (!id) return [];
    const radUrl = `https://api.deezer.com/artist/${encodeURIComponent(id)}/radio?limit=40`;
    const radData = await httpGetJson(radUrl, 6000);
    const items = (radData?.data || []).slice().sort(() => Math.random() - 0.5).slice(0, 25);
    return items.map(item => ({
      artist: item.artist?.name || 'unknown artist',
      title: item.title || 'untitled',
      match: 0.7,
      source: 'deezer'
    }));
  } catch (_) {
    return [];
  }
}

const ytMixCache = new Map();

async function ytMixTracks(seed) {
  if (!seed || (!seed.artist && !seed.title)) return [];
  const cleanArtist = stripYoutubeTopic(seed.artist || '').trim();
  const cleanTitle = String(seed.title || '').trim();
  const cacheKey = `${cleanArtist}::${cleanTitle}`.toLowerCase();
  if (!cacheKey || cacheKey === '::') return [];

  const cached = ytMixCache.get(cacheKey);
  if (cached && Date.now() < cached.expires) {
    return cached.tracks;
  }
  if (cached?.promise) {
    return cached.promise;
  }

  const lookupPromise = (async () => {
    try {
      const q = cleanArtist ? `${cleanArtist} ${cleanTitle} mix` : cleanTitle;
      const songs = await searchYtmTracks(q, 14).catch(() => []);
      const results = [];
      (songs || []).forEach((entry, index) => {
        if (!entry || !entry.id || entry.id === seed.id) return;
        const duration = Number(entry.duration) || 0;
        if (duration > 480) return;
        const artist = stripYoutubeTopic(entry.artist || 'unknown artist');
        const title = entry.title || 'untitled';
        if (isSpamOrUnofficial(title, duration, artist)) return;
        results.push({
          id: entry.id,
          title,
          artist,
          duration,
          thumbnail: entry.thumbnail || '',
          url: `https://www.youtube.com/watch?v=${entry.id}`,
          platform: 'youtube',
          isCached: false,
          match: 1 - (index / 20) * 0.5,
          source: 'ytmix',
          playable: true
        });
      });
      ytMixCache.set(cacheKey, { expires: Date.now() + 30 * 60 * 1000, tracks: results });
      return results;
    } catch (_) {
      ytMixCache.delete(cacheKey);
      return [];
    }
  })();
  ytMixCache.set(cacheKey, { expires: 0, promise: lookupPromise });
  return lookupPromise;
}

async function gatherCandidates(seeds) {
  const seedList = (Array.isArray(seeds) ? seeds : []).slice(0, 4);
  const perSeed = await Promise.all(seedList.map(async (seed) => {
    const weight = Number(seed?.weight) || 1;
    const [ytmix, dzRel, dzRad] = await Promise.all([
      ytMixTracks(seed).catch(() => []),
      dzRelatedTracks(seed?.artist).catch(() => []),
      dzRadioTracks(seed?.artist).catch(() => [])
    ]);
    const list = [];
    (ytmix || []).forEach(t => {
      list.push({ ...t, match: (Number(t.match) || 0.7) * 1.3, seedWeight: weight });
    });
    (dzRel || []).forEach(t => {
      list.push({ ...t, seedWeight: weight });
    });
    (dzRad || []).forEach(t => {
      list.push({ ...t, seedWeight: weight });
    });
    return list;
  }));
  return perSeed.flat();
}

function rankCandidates(cands, profile, heardKeys, avoid, limit = 10) {
  const avoidKeys = avoid?.keys instanceof Set ? avoid.keys : new Set();
  const avoidArtists = avoid?.artists instanceof Set ? avoid.artists : new Set();
  const heardKeySet = heardKeys instanceof Set ? heardKeys : new Set();

  const grouped = new Map();
  for (const c of (cands || [])) {
    if (c && (c.platform === 'youtube' || c.source === 'ytmix' || !c.platform)) {
      c.artist = stripYoutubeTopic(c.artist);
    }
    const artist = c.artist || '';
    const title = c.title || '';
    const key = getTrackNormalizedKey(artist, title);
    if (!key) continue;
    if (heardKeySet.has(key)) continue;
    if (recentlyRecommendedKeys && recentlyRecommendedKeys.has(key)) continue;
    if (avoidKeys.has(key)) continue;

    const normArtist = normalizeArtistName(artist);
    if (avoidArtists.has(normArtist)) continue;

    const weight = Number(c.seedWeight) || 1;
    const match = Number(c.match) || 0.5;
    const scoreVal = match * weight;

    if (!grouped.has(key)) {
      grouped.set(key, {
        key,
        title,
        artist,
        normArtist,
        scoreSum: scoreVal,
        sources: new Set([c.source]),
        bestCand: c
      });
    } else {
      const g = grouped.get(key);
      g.scoreSum += scoreVal;
      g.sources.add(c.source);
      if (c.playable && !g.bestCand.playable) {
        g.bestCand = c;
      }
    }
  }

  const topArtistNames = new Set(
    (profile?.topArtists || [])
      .map(a => typeof a === 'string' ? a : a?.name)
      .filter(Boolean)
      .map(normalizeArtistName)
  );
  const isCyrillicProfile = profile?.dominantScript === 'Cyrillic';

  const scoredList = [];
  for (const entry of grouped.values()) {
    let score = entry.scoreSum;
    const isTop = topArtistNames.has(entry.normArtist) || Array.from(topArtistNames).some(a => entry.normArtist.includes(a) || a.includes(entry.normArtist));
    score += isTop ? -0.4 : 0.6;
    if (isCyrillicProfile && /[\u0400-\u04FF]/.test(entry.title)) {
      score += 0.3;
    }
    score += Math.random() * 0.15;

    scoredList.push({
      ...entry.bestCand,
      key: entry.key,
      normArtist: entry.normArtist,
      isTop,
      rankScore: score
    });
  }

  scoredList.sort((a, b) => b.rankScore - a.rankScore);

  const targetCount = (limit || 10) * 2;
  const maxTopCount = Math.max(1, Math.floor(targetCount * 0.3));
  const artistCounts = {};
  let topCount = 0;
  const result = [];

  for (const item of scoredList) {
    const aCount = artistCounts[item.normArtist] || 0;
    if (aCount >= 2) continue;
    if (item.isTop && topCount >= maxTopCount) continue;

    artistCounts[item.normArtist] = aCount + 1;
    if (item.isTop) topCount++;
    result.push(item);
    if (result.length >= targetCount) break;
  }

  return result;
}

const resolvedCache = new Map();

async function resolveCandidate(c) {
  if (!c) return null;
  const { match, source, seedWeight, playable, key, normArtist, isTop, rankScore, ...clean } = c;
  return {
    ...clean,
    id: clean.id || ('cand_' + Math.random().toString(36).slice(2, 10)),
    platform: clean.platform || 'youtube',
    searchQuery: `${clean.artist} - ${clean.title}`.trim()
  };
}

function pickWeightedSeeds(seeds, count = 4) {
  const pool = [...seeds];
  const picked = [];
  while (picked.length < count && pool.length > 0) {
    const totalWeight = pool.reduce((acc, s) => acc + (Number(s.weight) || 1), 0);
    let r = Math.random() * totalWeight;
    let selectedIdx = 0;
    for (let i = 0; i < pool.length; i++) {
      r -= (Number(pool[i].weight) || 1);
      if (r <= 0) {
        selectedIdx = i;
        break;
      }
    }
    picked.push(pool[selectedIdx]);
    pool.splice(selectedIdx, 1);
  }
  return picked;
}

async function recommendVibeTracks(historyTracks, favoriteTracks, platform = 'youtube', limit = 10, clientProfile = null, extra = {}) {
  const normalizedHistory = (historyTracks || []).map(normalizeTrack).filter(Boolean);
  const normalizedFavorites = (favoriteTracks || []).map(normalizeTrack).filter(Boolean);
  const totalUserTracks = normalizedFavorites.length + normalizedHistory.length;
  const hasExtraSeeds = Array.isArray(extra?.seeds) && extra.seeds.length > 0;

  const heardIds = new Set();
  const heardKeys = new Set();
  [...normalizedHistory, ...normalizedFavorites].forEach(track => {
    if (track.id) heardIds.add(track.id);
    const key = getTrackNormalizedKey(track.artist, track.title);
    if (key) heardKeys.add(key);
  });

  const artistCounts = {};
  let cyrillicCount = 0;
  const durations = [];

  [...normalizedFavorites, ...normalizedHistory].forEach(t => {
    if (t.artist && t.artist.trim()) {
      const a = normalizeArtistName(t.artist);
      artistCounts[a] = (artistCounts[a] || 0) + 1;
    }
    if (t.title && /[\u0400-\u04FF]/.test(t.title)) cyrillicCount++;
    if (t.duration > 0) durations.push(t.duration);
  });

  const sortedArtists = Object.entries(artistCounts)
    .sort((a, b) => b[1] - a[1])
    .map(e => e[0]);

  const profile = {
    topArtists: (clientProfile?.topArtists && clientProfile.topArtists.length > 0) ? clientProfile.topArtists : sortedArtists.slice(0, 5),
    dominantScript: clientProfile?.dominantScript || (cyrillicCount > (normalizedFavorites.length + normalizedHistory.length) * 0.35 ? 'Cyrillic' : 'Latin'),
    typicalDuration: clientProfile?.avgDuration || (durations.length > 0 ? Math.round(durations.reduce((a, b) => a + b, 0) / durations.length) : 210),
    trackCount: totalUserTracks
  };

  if (totalUserTracks < 5 && !hasExtraSeeds) {
    return {
      profile,
      tracks: []
    };
  }

  let candidateSeeds = [];
  if (hasExtraSeeds) {
    candidateSeeds = extra.seeds.map(s => ({
      artist: s.artist || '',
      title: s.title || '',
      id: s.id || '',
      platform: s.platform || 'youtube',
      weight: Number(s.weight) || 1
    }));
  } else {
    const seedMap = new Map();
    normalizedFavorites.forEach(t => {
      const k = getTrackNormalizedKey(t.artist, t.title);
      if (k && !seedMap.has(k)) {
        seedMap.set(k, { artist: t.artist, title: t.title, id: t.id, platform: t.platform || 'youtube', weight: 3 });
      }
    });
    normalizedHistory.forEach(t => {
      const k = getTrackNormalizedKey(t.artist, t.title);
      if (k && !seedMap.has(k)) {
        seedMap.set(k, { artist: t.artist, title: t.title, id: t.id, platform: t.platform || 'youtube', weight: 1 });
      }
    });
    candidateSeeds = Array.from(seedMap.values()).slice(0, 8);
  }

  const chosenSeeds = pickWeightedSeeds(candidateSeeds, 4);

  const avoidKeys = new Set(Array.isArray(extra?.avoid?.keys) ? extra.avoid.keys : []);
  const avoidArtists = new Set(
    (Array.isArray(extra?.avoid?.artists) ? extra.avoid.artists : [])
      .map(normalizeArtistName)
      .filter(Boolean)
  );
  const avoid = { keys: avoidKeys, artists: avoidArtists };
  avoid.keys.length = (extra?.avoid?.keys && typeof extra.avoid.keys.length === 'number') ? extra.avoid.keys.length : avoidKeys.size;
  avoid.artists.length = (extra?.avoid?.artists && typeof extra.avoid.artists.length === 'number') ? extra.avoid.artists.length : avoidArtists.size;
  const avoidHash = `${avoid.keys.length}:${avoid.artists.length}`;

  const sortedSeedKeys = chosenSeeds.map(s => getTrackNormalizedKey(s.artist, s.title) || s.id).sort().join('|');
  const cacheKey = sortedSeedKeys ? `${sortedSeedKeys}#avoid:${avoidHash}` : '';
  if (!extra?.refresh && cacheKey && vibeCache.has(cacheKey)) {
    const cached = vibeCache.get(cacheKey);
    if (cached && Date.now() < cached.expires) {
      const filtered = (cached.tracks || []).filter(t => {
        const k = getTrackNormalizedKey(t.artist, t.title);
        return !recentlyRecommendedTrackIds.has(t.id) && (!k || !recentlyRecommendedKeys.has(k));
      });
      if (filtered.length >= limit) {
        return { profile, tracks: filtered.slice(0, limit) };
      }
    }
  }

  let finalTracks = [];
  try {
    const cands = await gatherCandidates(chosenSeeds);
    (cands || []).forEach(c => {
      if (c && (c.platform === 'youtube' || c.source === 'ytmix' || !c.platform)) {
        c.artist = stripYoutubeTopic(c.artist);
      }
    });
    const ranked = rankCandidates(cands, profile, heardKeys, avoid, limit);
    let rIdx = 0;
    const resolveWorker = async () => {
      while (rIdx < ranked.length && finalTracks.length < limit) {
        const c = ranked[rIdx++];
        try {
          const track = await resolveCandidate(c);
          if (track && !finalTracks.some(t => t.id === track.id)) {
            const k = getTrackNormalizedKey(track.artist, track.title);
            if (!heardIds.has(track.id) && (!k || !heardKeys.has(k)) &&
                !recentlyRecommendedTrackIds.has(track.id) && (!k || !recentlyRecommendedKeys.has(k))) {
              if (finalTracks.length < limit) {
                finalTracks.push(track);
              }
            }
          }
        } catch (_) {}
      }
    };
    const concurrency = Math.min(4, ranked.length);
    if (concurrency > 0) {
      await Promise.all(Array.from({ length: concurrency }, () => resolveWorker()));
    }
  } catch (_) {}

  if (finalTracks.length > 0) {
    finalTracks.forEach(track => {
      recentlyRecommendedTrackIds.add(track.id);
      const nKey = getTrackNormalizedKey(track.artist, track.title);
      if (nKey) recentlyRecommendedKeys.add(nKey);
      if (recentlyRecommendedTrackIds.size > 100) {
        const first = recentlyRecommendedTrackIds.values().next().value;
        recentlyRecommendedTrackIds.delete(first);
      }
      if (recentlyRecommendedKeys.size > 100) {
        const firstK = recentlyRecommendedKeys.values().next().value;
        recentlyRecommendedKeys.delete(firstK);
      }
    });

    if (cacheKey) {
      vibeCache.set(cacheKey, {
        expires: Date.now() + 20 * 60 * 1000,
        tracks: finalTracks
      });
    }

    return {
      profile,
      tracks: finalTracks
    };
  }

  const queries = [];
  if (profile.topArtists && profile.topArtists.length > 0) {
    queries.push(...buildDiscoveryQueries(profile));
  }
  if (queries.length === 0) {
    queries.push('Top Hits 2025', 'Trending Music', 'Pop Hits');
  }

  const resultsArr = await Promise.all(queries.slice(0, 3).map(q => searchYtmTracks(q, 16)));
  const candidateMap = new Map();
  const allFound = [];

  for (const results of resultsArr) {
    if (Array.isArray(results)) {
      for (const track of results) {
        allFound.push(track);
        const scored = scoreDiscoveryCandidate(track, profile, heardKeys, heardIds);
        if (scored) {
          if (!candidateMap.has(scored.id) || candidateMap.get(scored.id).score < scored.score) {
            candidateMap.set(scored.id, scored);
          }
        }
      }
    }
  }

  if (candidateMap.size < limit) {
    for (const track of allFound) {
      if (!candidateMap.has(track.id)) {
        candidateMap.set(track.id, { ...track, score: 50 });
      }
      if (candidateMap.size >= limit * 2) break;
    }
  }

  let ranked = Array.from(candidateMap.values()).sort((a, b) => b.score - a.score);

  const unshown = ranked.filter(t => !recentlyRecommendedTrackIds.has(t.id));
  if (unshown.length >= limit) {
    ranked = unshown;
  }

  const artistTrackCount = {};
  const fallbackTracks = [];
  for (const track of ranked) {
    const a = normalizeArtistName(track.artist);
    if ((artistTrackCount[a] || 0) < 3) {
      artistTrackCount[a] = (artistTrackCount[a] || 0) + 1;
      fallbackTracks.push(track);
      recentlyRecommendedTrackIds.add(track.id);
      const nKey = getTrackNormalizedKey(track.artist, track.title);
      if (nKey) recentlyRecommendedKeys.add(nKey);
      if (recentlyRecommendedTrackIds.size > 100) {
        const first = recentlyRecommendedTrackIds.values().next().value;
        recentlyRecommendedTrackIds.delete(first);
      }
      if (recentlyRecommendedKeys.size > 100) {
        const firstK = recentlyRecommendedKeys.values().next().value;
        recentlyRecommendedKeys.delete(firstK);
      }
      if (fallbackTracks.length >= limit) break;
    }
  }

  if (fallbackTracks.length < limit) {
    for (const track of allFound) {
      if (!fallbackTracks.some(t => t.id === track.id)) {
        fallbackTracks.push(track);
        if (fallbackTracks.length >= limit) break;
      }
    }
  }

  return {
    profile,
    tracks: fallbackTracks
  };
}

function isTextExplicit(text) {
  if (!text) return false;
  return /\bexplicit\b|\[e\]|\(e\)|\[explicit\]|\(explicit\)|\b18\+\b|\[18\+\]|\(18\+\)|\b\+18\b|\[\+18\]|\(\+18\)|\buncensored\b|\[uncensored\]|\(uncensored\)|\bdirty\b|\[dirty\]|\(dirty\)|без\s+цензуры|нецензурн|\bмат\b/i.test(text) || (typeof isLyricsExplicit === 'function' && isLyricsExplicit(text));
}

function directYoutubeSearch(query, isMusic = false, limit = 40) {
  const filterSpam = !!isMusic;
  return new Promise((resolve) => {
    const bodyPayload = JSON.stringify({
      context: { client: { clientName: 'WEB', clientVersion: '2.20250101.00.00', hl: 'en', gl: 'US' } },
      query
    });

    const req = https.request({
      hostname: 'www.youtube.com',
      path: '/youtubei/v1/search?prettyPrint=false',
      method: 'POST',
      agent: keepAliveHttpsAgent,
      family: 4,
      timeout: 8000,
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(bodyPayload),
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      }
    }, (res) => {
      let raw = '';
      const dec = new StringDecoder('utf8');
      res.on('data', chunk => { raw += dec.write(chunk); });
      res.on('end', async () => {
        try {
          const json = JSON.parse(raw);
          const contents = json.contents?.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer?.contents ||
                           json.contents?.sectionListRenderer?.contents || [];
          const tracks = [];

          searchLoop:
          for (const section of contents) {
            const items = section.itemSectionRenderer?.contents || [];
            for (const item of items) {
              if (tracks.length >= limit) break searchLoop;
              const v = item.videoRenderer;
              if (v && v.videoId) {
                const vidId = v.videoId;
                const title = (v.title?.runs?.[0]?.text || 'untitled');
                if (!item.uploader && !item.channel && !item.artist && v.ownerText?.runs?.[0]?.text) {
                  item.artist = v.ownerText.runs[0].text;
                }
                const artist = stripYoutubeTopic(item.uploader || item.channel || item.artist || 'Unknown Artist');
                const durStr = v.lengthText?.simpleText || '0:00';
                const durSec = parseDurationSeconds(durStr);
                const thumbs = v.thumbnail?.thumbnails || [];
                const thumbnail = thumbs.length > 0 ? thumbs[thumbs.length - 1].url : '';

                if (filterSpam && isSpamOrUnofficial(title, durSec, artist)) {
                  continue;
                }

                const isCached = await findCachedFile(vidId);
                const isExplicit = Boolean(
                  (v.badges && v.badges.some(b => {
                    const mb = b.metadataBadgeRenderer;
                    return mb && (/explicit|18\+/i.test(mb.label || '') || /explicit/i.test(mb.style || '') || /explicit/i.test(mb.accessibilityData?.label || ''));
                  })) || isTextExplicit(title)
                );

                tracks.push({
                  id: vidId,
                  title: title,
                  artist: artist,
                  duration: durSec,
                  thumbnail: thumbnail,
                  url: `https://www.youtube.com/watch?v=${vidId}`,
                  platform: 'youtube',
                  isCached: !!isCached,
                  isExplicit: isExplicit
                });
              }
            }
          }
          resolve(tracks);
        } catch (e) {
          resolve([]);
        }
      });
    });

    req.on('error', () => resolve([]));
    req.on('timeout', () => { req.destroy(); resolve([]); });
    req.write(bodyPayload);
    req.end();
  });
}

function executeYtDlp(args, timeoutMs = 15000, opts = {}) {
  return new Promise((resolve, reject) => {
    const cleanEnv = Object.assign({}, process.env);
    {
      const home = os.homedir();
      const extra = ['/usr/bin', '/usr/local/bin', path.join(home, '.local/bin'), path.join(home, '.deno/bin'), '/opt/homebrew/bin'];
      const cur = (cleanEnv.PATH || '').split(path.delimiter).filter(Boolean);
      for (const d of extra) if (!cur.includes(d)) cur.push(d);
      cleanEnv.PATH = cur.join(path.delimiter);
    }
    let finalArgs = Array.isArray(args) ? [...args] : [];
    const prependFlags = [];
    let cookieJarPath = null;
    const cleanupCookieJar = () => {
      if (!cookieJarPath) return;
      const p = cookieJarPath;
      cookieJarPath = null;
      try { fs.unlinkSync(p); } catch (_) {}
    };
    if (!finalArgs.includes('--force-ipv4')) prependFlags.push('--force-ipv4');
    if (!finalArgs.includes('--geo-bypass')) prependFlags.push('--geo-bypass');
    if (!opts.noCookies && hasYtLoginCookies() && !finalArgs.includes('--cookies')) {
      try {
        cookieJarPath = path.join(DATA_DIR, `.yt-cookies-${process.pid}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}.tmp`);
        fs.copyFileSync(YT_COOKIES_FILE, cookieJarPath);
        prependFlags.push('--cookies', cookieJarPath);
      } catch (_) {
        prependFlags.push('--cookies', YT_COOKIES_FILE);
      }
    }
    if (prependFlags.length > 0) {
      finalArgs.unshift(...prependFlags);
    }
    const hasProxy = finalArgs.some(a => typeof a === 'string' && (a === '--proxy' || a.startsWith('--proxy=')));
    if (!hasProxy) {
      if (serverActiveProxy && typeof serverActiveProxy === 'string' && serverActiveProxy.trim()) {
        finalArgs.unshift('--proxy', serverActiveProxy.trim());
      } else {
        delete cleanEnv.HTTP_PROXY;
        delete cleanEnv.HTTPS_PROXY;
        delete cleanEnv.ALL_PROXY;
        delete cleanEnv.http_proxy;
        delete cleanEnv.https_proxy;
        delete cleanEnv.all_proxy;
      }
    }
    let cmd;
    try {
      cmd = ytDlpCommand(finalArgs);
    } catch (cmdErr) {
      cleanupCookieJar();
      return reject(cmdErr);
    }
    const YTDLP_PATH = cmd.bin;
    const proc = spawn(YTDLP_PATH, cmd.args, { windowsHide: true, env: cleanEnv });
    if (opts && typeof opts.onChild === 'function') {
      try { opts.onChild(proc); } catch (_) {}
    }
    let stdout = '';
    let stderr = '';
    const outDecoder = new StringDecoder('utf8');
    const errDecoder = new StringDecoder('utf8');
    let timer = null;

    if (timeoutMs > 0) {
      timer = setTimeout(() => {
        try { proc.kill('SIGKILL'); } catch (e) {}
        cleanupCookieJar();
        reject(new Error(`yt-dlp timed out after ${timeoutMs}ms`));
      }, timeoutMs);
    }

    proc.stdout.on('data', (d) => { stdout += outDecoder.write(d); });
    proc.stderr.on('data', (d) => { stderr += errDecoder.write(d); });

    proc.on('close', (code) => {
      cleanupCookieJar();
      if (timer) clearTimeout(timer);
      if (code === 0) {
        resolve(stdout);
      } else {
        const firstArgs = (args || []).slice(0, 3).join(' ');
        const errSnippet = String(stderr || `code ${code}`).slice(0, 300);
        const isWarn = !stderr || /not available|unavailable|private video|timed out/i.test(errSnippet);
        if (!opts.quiet) {
          try { logger.log(isWarn ? 'warn' : 'error', `yt-dlp failed: ${firstArgs} - ${errSnippet}`); } catch (_) {}
        }
        reject(new Error(stderr || `yt-dlp exited with code ${code}`));
      }
    });

    proc.on('error', (err) => {
      cleanupCookieJar();
      if (timer) clearTimeout(timer);
      const firstArgs = (args || []).slice(0, 3).join(' ');
      const errSnippet = String(err && err.message ? err.message : err).slice(0, 300);
      try { logger.log('error', `yt-dlp spawn error: ${firstArgs} - ${errSnippet}`); } catch (_) {}
      if (err && err.code === 'ENOENT' && !opts.retried) {
        clearBinaryCache();
        try {
          resolveBinary('yt-dlp');
        } catch (_) {}
        return executeYtDlp(args, timeoutMs, Object.assign({}, opts, { retried: true })).then(resolve, reject);
      }
      reject(err);
    });
  });
}

const thumbHostFails = new Map();

async function lookupCoverArt(artist, title) {
  const key = crypto.createHash('sha1').update(`${artist}\u0000${title}`.toLowerCase()).digest('hex').slice(0, 16);
  const dest = path.join(THUMBNAILS_DIR, `cover-${key}.jpg`);
  if (fs.existsSync(dest)) return dest;
  try {
    const res = await fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(`${artist} ${title}`)}&entity=song&limit=5`, { signal: AbortSignal.timeout(8000) });
    if (!res.ok) return null;
    const data = await res.json();
    const item = (data.results || [])[0];
    if (!item || !item.artworkUrl100) return null;
    const art = await fetch(item.artworkUrl100.replace('100x100', '600x600'), { signal: AbortSignal.timeout(8000) });
    if (!art.ok) return null;
    const buf = Buffer.from(await art.arrayBuffer());
    if (buf.length < 500) return null;
    await fs.promises.writeFile(dest, buf);
    return dest;
  } catch (e) {
    return null;
  }
}

async function fetchTrackCoverUrl(artist, title) {
  if (!artist && !title) return '';
  const term = `${artist || ''} ${title || ''}`.trim();
  try {
    const res = await fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(term)}&entity=song&limit=1`, { signal: AbortSignal.timeout(6000) });
    if (res.ok) {
      const data = await res.json();
      const item = data?.results?.[0];
      if (item?.artworkUrl100) {
        return item.artworkUrl100.replace('100x100', '600x600');
      }
    }
  } catch (_) {}
  try {
    const dzRes = await fetch(`https://api.deezer.com/search?q=${encodeURIComponent(term)}&limit=1`, { signal: AbortSignal.timeout(6000) });
    if (dzRes.ok) {
      const dzData = await dzRes.json();
      const dzItem = dzData?.data?.[0];
      const c = dzItem?.album?.cover_xl || dzItem?.album?.cover_big || dzItem?.album?.cover_medium;
      if (c) return c;
    }
  } catch (_) {}
  return '';
}

async function imageContentType(buf) {
  if (buf.length > 3 && buf[0] === 0xff && buf[1] === 0xd8) return 'image/jpeg';
  if (buf.length > 8 && buf[0] === 0x89 && buf[1] === 0x50) return 'image/png';
  if (buf.length > 12 && buf.toString('ascii', 0, 4) === 'RIFF' && buf.toString('ascii', 8, 12) === 'WEBP') return 'image/webp';
  if (buf.length > 6 && buf.toString('ascii', 0, 3) === 'GIF') return 'image/gif';
  return 'image/jpeg';
}

async function cacheThumbnailToDisk(cleanId, thumbnailUrl) {
  if (!cleanId || !thumbnailUrl || !thumbnailUrl.startsWith('http')) return null;
  const destPath = path.join(THUMBNAILS_DIR, `${cleanId}.jpg`);
  if (fs.existsSync(destPath)) return destPath;
  try {
    const res = await fetch(thumbnailUrl);
    if (!res.ok) return null;
    const buffer = Buffer.from(await res.arrayBuffer());
    if (buffer.length > 500) {
      await fs.promises.writeFile(destPath, buffer);
      return destPath;
    }
  } catch (e) {}
  return null;
}

async function cacheResolvedStreamToDisk(rawStreamUrl, primaryCleanId, secondaryCleanId, platform, meta = {}) {
  const ids = Array.from(new Set([cleanTrackId(primaryCleanId), cleanTrackId(secondaryCleanId)].filter(Boolean)));
  if (!ids.length || !rawStreamUrl || rawStreamUrl.includes('.m3u8')) return;
  const mainId = ids[0];
  if (downloadingSet.has(mainId) || await findCachedFile(mainId)) return;
  downloadingSet.add(mainId);

  const ext = (rawStreamUrl.includes('mime=audio%2Fwebm') || rawStreamUrl.includes('mime=audio/webm')) ? '.webm'
            : (rawStreamUrl.includes('.mp3') ? '.mp3' : '.m4a');
  const tempPath = path.join(CACHE_DIR, `${mainId}.temp${ext}`);
  const finalPath = path.join(CACHE_DIR, `${mainId}${ext}`);

  if (meta.thumbnail) {
    ids.forEach(id => cacheThumbnailToDisk(id, meta.thumbnail).catch(() => {}));
  }

  try {
    let parsed;
    try { parsed = new URL(rawStreamUrl); } catch (_) { downloadingSet.delete(mainId); return; }
    const headers = { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36' };
    if (parsed.hostname.endsWith('googlevideo.com')) {
      headers['User-Agent'] = googleVideoUserAgent(parsed);
      headers['Range'] = 'bytes=0-';
    }
    const resp = await fetch(rawStreamUrl, { headers, signal: AbortSignal.timeout(45000) });
    if (!resp.ok || !resp.body) throw new Error('HTTP ' + resp.status);
    const arrayBuf = await resp.arrayBuffer();
    const buf = Buffer.from(arrayBuf);
    if (buf.length > 64000) {
      await fs.promises.writeFile(tempPath, buf);
      await fs.promises.rename(tempPath, finalPath);
      for (const id of ids) {
        if (id !== mainId) {
          const aliasPath = path.join(CACHE_DIR, `${id}${ext}`);
          if (!fs.existsSync(aliasPath)) {
            try { await fs.promises.copyFile(finalPath, aliasPath); } catch (_) {}
          }
        }
        saveTrackMetadata(id, {
          id,
          title: meta.title || '',
          artist: meta.artist || '',
          duration: meta.duration || 0,
          thumbnail: meta.thumbnail || '',
          platform: platform || 'ytmusic',
          fileSize: buf.length
        });
      }
    }
  } catch (_) {
    try { if (fs.existsSync(tempPath)) await fs.promises.unlink(tempPath); } catch (_) {}
  } finally {
    downloadingSet.delete(mainId);
  }
}

async function downloadTrackToDisk(trackUrl, trackId, platform, meta = {}) {
  const cleanId = cleanTrackId(trackId);
  if (downloadingSet.has(cleanId) || await findCachedFile(cleanId)) return;
  downloadingSet.add(cleanId);

  const tempBase = path.join(CACHE_DIR, `${cleanId}.temp`);

  const args = [
    '--no-config',
    '--no-playlist',
    '--no-check-certificates',
    '--no-warnings',
    '-x',
    '--audio-format', 'best',
    '-o', `${tempBase}.%(ext)s`
  ];

  if (platform === 'youtube') {
    args.push('--extractor-args', YT_EXTRACTOR_ARGS);
  }

  args.push(trackUrl);

  if (meta.thumbnail) {
    cacheThumbnailToDisk(cleanId, meta.thumbnail).catch(() => {});
  }

  let cmd;
  try { cmd = ytDlpCommand(args); } catch (e) { downloadingSet.delete(cleanId); try { logger.error('Download command error: ' + (meta.title || trackId), e); } catch (_) {} return; }

  bgYtDlp(() => new Promise((done) => {
    const proc = spawn(cmd.bin, cmd.args, { windowsHide: true });
    let downloadTimer = null;
    if (DOWNLOAD_TIMEOUT_MS > 0) {
      downloadTimer = setTimeout(() => {
        try { proc.kill('SIGKILL'); } catch (e) {}
      }, DOWNLOAD_TIMEOUT_MS);
    }
    let downloadStderr = '';
    if (proc.stdout) proc.stdout.resume();
    if (proc.stderr) proc.stderr.on('data', (d) => { downloadStderr += d.toString(); });
    proc.on('close', async (code) => {
      if (downloadTimer) clearTimeout(downloadTimer);
      downloadingSet.delete(cleanId);
      if (code !== 0 && downloadStderr) console.error('download failed:', downloadStderr.slice(0, 500));
      if (code === 0) {
        try {
          const files = await fs.promises.readdir(CACHE_DIR);
          const tempFile = files.map(f => path.join(CACHE_DIR, f)).find(f => f.startsWith(tempBase + '.') && !f.endsWith('.part'));
          if (tempFile) {
            const stat = await fs.promises.stat(tempFile);
            if (stat.size > 60000) {
              const ext = path.extname(tempFile);
              const outputPath = path.join(CACHE_DIR, `${cleanId}${ext}`);
              await fs.promises.rename(tempFile, outputPath);
              saveTrackMetadata(cleanId, {
                id: trackId,
                title: meta.title || '',
                artist: meta.artist || '',
                duration: meta.duration || 0,
                thumbnail: meta.thumbnail || '',
                platform: platform || 'youtube',
                fileSize: stat.size
              });
            } else {
              try { await fs.promises.unlink(tempFile); } catch (e) {}
            }
          }
        } catch (e) {}
      } else {
        try { logger.error('Download failure: ' + (meta.title || trackId) + ' (exit code ' + code + ')'); } catch (_) {}
        try {
          const files = await fs.promises.readdir(CACHE_DIR);
          for (const f of files) {
            if (f.startsWith(`${cleanId}.temp`)) {
              try { await fs.promises.unlink(path.join(CACHE_DIR, f)); } catch (e) {}
            }
          }
        } catch (e) {}
      }
      done();
    });
    proc.on('error', (err) => {
      if (downloadTimer) clearTimeout(downloadTimer);
      downloadingSet.delete(cleanId);
      try { logger.error('Download spawn error: ' + (meta.title || trackId), err); } catch (_) {}
      done();
    });
  })).catch(() => { downloadingSet.delete(cleanId); });
}

async function prefetchTrack(trackUrl, trackId, platform) {
  trackUrl = normalizeSoundCloudUrl(trackUrl);
  let cleanId = cleanTrackId(trackId);
  if (!cleanId && trackUrl && trackUrl.includes('v=')) {
    cleanId = cleanTrackId(trackUrl.split('v=')[1].split('&')[0]);
  }
  const quality = 'medium';
  const cacheKey = (cleanId || trackUrl).toLowerCase() + '_' + quality;
  if (!cacheKey || prefetchingSet.size > 2) return;
  if (streamUrlCache.has(cacheKey) || prefetchingSet.has(cacheKey) || streamInfoInflight.has(cacheKey)) return;
  prefetchingSet.add(cacheKey);
  let resolveInflight;
  const inflightPromise = new Promise(resolve => { resolveInflight = resolve; });
  streamInfoInflight.set(cacheKey, inflightPromise);
  let streamUrl = null;
  try {
    if (platform === 'soundcloud' || (trackUrl && trackUrl.includes('soundcloud.com'))) {
      streamUrl = await fetchDirectSoundCloudStreamServer(trackUrl || cleanId, '');
    } else if (cleanId && extractYtIdFromAny(cleanId)) {
      streamUrl = await fetchDirectYoutubeStreamServer(cleanId, quality);
    }
    if (streamUrl) {
      const portVal = typeof ACTIVE_PORT !== 'undefined' ? ACTIVE_PORT : PORT;
      const isHls = streamUrl.includes('.m3u8');
      const proxied = 'http://127.0.0.1:' + portVal + '/api/audio-proxy?url=' + encodeURIComponent(streamUrl);
      const data = {
        success: true,
        isLocal: false,
        streamUrl: isHls ? streamUrl : proxied,
        rawStreamUrl: streamUrl,
        proxyUrl: proxied,
        isHls: isHls
      };
      cacheSet(streamUrlCache, cacheKey, { timestamp: Date.now(), data });
      if (cleanId) cacheSet(streamUrlCache, cleanId + '_' + quality, { timestamp: Date.now(), data });
    }
  } catch (_) {
  } finally {
    resolveInflight(streamUrl);
    if (streamInfoInflight.get(cacheKey) === inflightPromise) {
      streamInfoInflight.delete(cacheKey);
    }
    prefetchingSet.delete(cacheKey);
  }
}

const YTM_SONGS_PARAMS = 'EgWKAQIIAWoMEA4QChADEAQQCRAF';
const YTM_CLIENT_VERSION = '1.20250101.01.00';
let ytmFailUntil = 0;
let ytmFailCount = 0;

async function ytmSearchRequest(query, params = YTM_SONGS_PARAMS, attempt = 0, timeoutMs = 10000, maxRetries = 2) {
  const reqBody = {
    context: { client: { clientName: 'WEB_REMIX', clientVersion: YTM_CLIENT_VERSION, hl: 'en', gl: 'US' } },
    query
  };
  if (params) reqBody.params = params;
  const body = JSON.stringify(reqBody);

  return new Promise((resolve, reject) => {
    const doSessionFallback = () => {
      if (typeof global.riffSessionFetch === 'function') {
        return global.riffSessionFetch('https://music.youtube.com/youtubei/v1/search?prettyPrint=false', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Origin': 'https://music.youtube.com',
            'Referer': 'https://music.youtube.com/',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
          },
          body,
          signal: AbortSignal.timeout(7000)
        }).then(r => r.json()).then(resolve).catch(reject);
      }
      return false;
    };

    const req = https.request({
      hostname: 'music.youtube.com',
      path: '/youtubei/v1/search?prettyPrint=false',
      method: 'POST',
      agent: attempt === 0 ? keepAliveHttpsAgent : false,
      family: 4,
      timeout: timeoutMs,
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body),
        'Origin': 'https://music.youtube.com',
        'Referer': 'https://music.youtube.com/',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36'
      }
    }, (res) => {
      if (res.statusCode < 200 || res.statusCode >= 300) {
        try { logger.log('warn', `ytmusic search status ${res.statusCode}`); } catch (_) {}
        res.resume();
        if (typeof global.riffSessionFetch === 'function') {
          return doSessionFallback();
        }
        return reject(new Error(`ytmusic status ${res.statusCode}`));
      }
      let raw = '';
      res.setEncoding('utf8');
      res.on('data', chunk => { raw += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(raw));
        } catch (err) {
          try { logger.log('warn', `ytmusic search parse error: ${err.message}`); } catch (_) {}
          if (typeof global.riffSessionFetch === 'function') {
            return doSessionFallback();
          }
          reject(err);
        }
      });
    });
    let retryStarted = false;
    const retryOrReject = (err) => {
      if (retryStarted) return;
      retryStarted = true;
      if (attempt < maxRetries) {
        return resolve(ytmSearchRequest(query, params, attempt + 1, timeoutMs, maxRetries));
      }
      if (typeof global.riffSessionFetch === 'function') {
        return doSessionFallback();
      }
      try { logger.log('warn', `ytmusic search error: ${err.message}`); } catch (_) {}
      reject(err);
    };
    req.on('timeout', () => {
      req.destroy();
      retryOrReject(new Error('The operation was aborted due to timeout'));
    });
    req.on('error', (err) => {
      if (/ECONNRESET|socket hang up|EPIPE|ETIMEDOUT/i.test(err && err.message || '')) {
        return retryOrReject(err);
      }
      if (typeof global.riffSessionFetch === 'function') {
        return doSessionFallback();
      }
      try { logger.log('warn', `ytmusic search error: ${err.message}`); } catch (_) {}
      reject(err);
    });
    req.write(body);
    req.end();
  });
}

function parseYtmDuration(text) {
  if (!text || !/^\d+(:\d{1,2}){1,2}$/.test(text.trim())) return 0;
  return text.trim().split(':').map(Number).reduce((acc, n) => acc * 60 + n, 0);
}

function parseYtmSongs(json) {
  const out = [];
  const tabs = (json && json.contents && json.contents.tabbedSearchResultsRenderer && json.contents.tabbedSearchResultsRenderer.tabs) || [];
  const sections = (tabs[0] && tabs[0].tabRenderer && tabs[0].tabRenderer.content && tabs[0].tabRenderer.content.sectionListRenderer && tabs[0].tabRenderer.content.sectionListRenderer.contents) || [];
  for (const section of sections) {
    const items = [];
    if (section && section.musicShelfRenderer && Array.isArray(section.musicShelfRenderer.contents)) {
      items.push(...section.musicShelfRenderer.contents);
    }
    if (section && section.itemSectionRenderer && Array.isArray(section.itemSectionRenderer.contents)) {
      items.push(...section.itemSectionRenderer.contents);
    }
    if (section && section.musicCardShelfRenderer && Array.isArray(section.musicCardShelfRenderer.contents)) {
      items.push(...section.musicCardShelfRenderer.contents);
    }
    for (const wrap of items) {
      const r = wrap && wrap.musicResponsiveListItemRenderer;
      if (!r) continue;
      const cols = r.flexColumns || [];
      const runsOf = (i) => (cols[i] && cols[i].musicResponsiveListItemFlexColumnRenderer && cols[i].musicResponsiveListItemFlexColumnRenderer.text && cols[i].musicResponsiveListItemFlexColumnRenderer.text.runs) || [];
      const overlay = r.overlay && r.overlay.musicItemThumbnailOverlayRenderer && r.overlay.musicItemThumbnailOverlayRenderer.content && r.overlay.musicItemThumbnailOverlayRenderer.content.musicPlayButtonRenderer;
      const videoId = (r.playlistItemData && r.playlistItemData.videoId) || (overlay && overlay.playNavigationEndpoint && overlay.playNavigationEndpoint.watchEndpoint && overlay.playNavigationEndpoint.watchEndpoint.videoId);
      if (!videoId || !/^[\w-]{11}$/.test(videoId)) continue;

      const title = runsOf(0).map((x) => x.text).join('').trim();
      const segments = [];
      let cur = [];
      for (const run of runsOf(1)) {
        if (run.text === ' • ') { segments.push(cur.join('').trim()); cur = []; } else cur.push(run.text);
      }
      segments.push(cur.join('').trim());
      const clean = segments.filter((s) => s && !/^(song|video)$/i.test(s));
      let duration = 0;
      if (clean.length && parseYtmDuration(clean[clean.length - 1])) duration = parseYtmDuration(clean.pop());
      const artist = (clean[0] || '').replace(/\s*-\s*Topic$/i, '').trim();
      const album = clean[1] || '';

      const thumbs = (r.thumbnail && r.thumbnail.musicThumbnailRenderer && r.thumbnail.musicThumbnailRenderer.thumbnail && r.thumbnail.musicThumbnailRenderer.thumbnail.thumbnails) || [];
      let thumbnail = thumbs.length ? thumbs[thumbs.length - 1].url : `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;
      thumbnail = thumbnail.replace(/=w\d+-h\d+[^&]*$/, '=w544-h544');

      const isExplicit = Boolean(
        (r.badges && r.badges.some((b) => {
          const badge = b.musicInlineBadgeRenderer;
          return badge && (
            badge.icon?.iconType === 'MUSIC_EXPLICIT_BADGE' ||
            /explicit|нецензурн|18\+/i.test(badge.accessibilityData?.accessibilityData?.label || '')
          );
        })) ||
        isTextExplicit(title) ||
        isTextExplicit(album)
      );

      if (!title || (duration && duration < 30)) continue;
      out.push({ id: videoId, title, artist: artist || 'Unknown Artist', album, duration, thumbnail, isExplicit });
    }
  }
  return out;
}

async function searchYtmTracks(query, limit = 16) {
  const cacheKey = `ytm_vibe:${limit}:${String(query || '').trim().toLowerCase()}`;
  if (searchCache.has(cacheKey)) {
    return searchCache.get(cacheKey);
  }
  try {
    const json = await ytmSearchRequest(query);
    const songs = parseYtmSongs(json).slice(0, limit);
    const tracks = songs.map(s => {
      ytArtistCache.set(s.id, s.artist);
      return {
        id: s.id,
        title: s.title,
        artist: s.artist,
        duration: s.duration || 180,
        thumbnail: s.thumbnail || `https://i.ytimg.com/vi/${s.id}/hqdefault.jpg`,
        url: `https://www.youtube.com/watch?v=${s.id}`,
        platform: 'youtube',
        isExplicit: !!s.isExplicit
      };
    });
    if (tracks.length > 0) {
      searchCache.set(cacheKey, tracks);
      setTimeout(() => { searchCache.delete(cacheKey); }, 300000);
    }
    return tracks;
  } catch (e) {
    try {
      const dy = await directYoutubeSearch(query, true);
      return (dy || []).slice(0, limit).map(t => ({
        ...t,
        platform: 'youtube'
      }));
    } catch (_) {
      return [];
    }
  }
}

function cleanTitleForMatching(t) {
  return (t || '')
    .toLowerCase()
    .replace(/\s*[\(\[][^\)\]]*(?:official|audio|video|lyrics|клип|трек|prod|feat|ft|slowed|reverb|remix)[^\)\]]*[\)\]]/gi, '')
    .replace(/[\(\)\[\]\{\}\-–—_\/\\\"\'ᐸ><,.:;!?]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function fetchDeezerExplicitTitles(query) {
  return new Promise((resolve) => {
    const cleanQ = (query || '').replace(/\s*(official|audio|video|lyrics|клип|трек|slowed|reverb)\b/gi, '').trim();
    if (!cleanQ) return resolve([]);
    const url = `https://api.deezer.com/search?q=${encodeURIComponent(cleanQ)}&limit=50`;
    https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' }, timeout: 4000 }, (res) => {
      let d = '';
      res.on('data', c => d += c);
      res.on('end', () => {
        try {
          const json = JSON.parse(d);
          const list = [];
          if (json && Array.isArray(json.data)) {
            for (const t of json.data) {
              if (t && (t.explicit_lyrics || t.explicit_content_lyrics === 1 || t.explicit_content_lyrics)) {
                list.push({
                  title: t.title || '',
                  clean: cleanTitleForMatching(t.title),
                  artist: (t.artist && t.artist.name) || ''
                });
              }
            }
          }
          resolve(list);
        } catch (_) {
          resolve([]);
        }
      });
    }).on('error', () => resolve([]));
  });
}

async function fetchSoundCloudSearch(query, limit = 20) {
  try {
    let cid = await refreshSoundCloudClientId(false);
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        const sr = await fetch(`https://api-v2.soundcloud.com/search/tracks?q=${encodeURIComponent(query)}&limit=${limit}&client_id=${cid}`, {
          headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
        });
        if (sr.status === 401 || sr.status === 403) {
          cid = await refreshServerScClientId();
          continue;
        }
        if (sr.ok) {
          const sj = await sr.json();
          const col = (sj && Array.isArray(sj.collection)) ? sj.collection : [];
          const tracks = col.filter(t => t && t.id && t.title).map(t => {
            const rawThumb = t.artwork_url || (t.user && t.user.avatar_url) || '';
            const thumbnail = rawThumb ? rawThumb.replace('-large', '-t500x500') : '';
            return {
              id: String(t.id),
              title: t.title || 'untitled',
              artist: (t.user && (t.user.username || t.user.name)) || (t.publisher_metadata && t.publisher_metadata.artist) || 'unknown artist',
              duration: Math.round((t.duration || t.full_duration || 0) / 1000),
              thumbnail: thumbnail || rawThumb,
              url: normalizeSoundCloudUrl(t.permalink_url || ''),
              platform: 'soundcloud',
              isCached: false
            };
          });
          if (tracks.length > 0) return tracks;
        }
      } catch (_) {}
    }
  } catch (_) {}
  return [];
}

async function fetchMetadataSearchFallback(query, limit = 24) {
  const q = String(query || '').trim();
  if (!q) return [];
  try {
    const dzData = await httpGetJson('https://api.deezer.com/search?q=' + encodeURIComponent(q) + '&limit=' + limit, 5000);
    if (dzData && Array.isArray(dzData.data) && dzData.data.length > 0) {
      return dzData.data.filter(item => item && item.title).map(item => {
        const art = stripYoutubeTopic(item.artist?.name || 'Unknown Artist');
        const cover = item.album?.cover_xl || item.album?.cover_big || item.album?.cover_medium || '';
        return {
          id: 'dz_' + item.id,
          title: item.title,
          artist: art,
          album: item.album?.title || '',
          duration: Number(item.duration) || 180,
          thumbnail: cover,
          cover: cover,
          url: '',
          searchQuery: (art + ' - ' + item.title).trim(),
          platform: 'ytmusic',
          isExplicit: Boolean(item.explicit_lyrics || item.explicit_content_lyrics === 1)
        };
      });
    }
  } catch (_) {}
  try {
    const itData = await httpGetJson('https://itunes.apple.com/search?term=' + encodeURIComponent(q) + '&entity=song&limit=' + limit, 5000);
    if (itData && Array.isArray(itData.results) && itData.results.length > 0) {
      return itData.results.filter(item => item && item.trackName).map(item => {
        const art = item.artistName || 'Unknown Artist';
        const cover = (item.artworkUrl100 || '').replace('100x100', '600x600');
        return {
          id: 'it_' + item.trackId,
          title: item.trackName,
          artist: art,
          album: item.collectionName || '',
          duration: Math.round((item.trackTimeMillis || 180000) / 1000),
          thumbnail: cover,
          cover: cover,
          url: '',
          searchQuery: (art + ' - ' + item.trackName).trim(),
          platform: 'ytmusic',
          isExplicit: item.trackExplicitness === 'explicit'
        };
      });
    }
  } catch (_) {}
  return [];
}

async function ytmusicSearch(query, options = {}) {
  const timeoutMs = options.timeoutMs || 10000;
  const maxRetries = Number.isInteger(options.maxRetries) ? options.maxRetries : 2;
  const useFallback = options.fallback !== false;
  let json;
  try {
    json = await ytmSearchRequest(query, YTM_SONGS_PARAMS, 0, timeoutMs, maxRetries);
  } catch (_) {
    if (!useFallback) return [];
    try { return await directYoutubeSearch(query, true).then(tracks => tracks || []); } catch (_) { return []; }
  }
  try {
    const songs = parseYtmSongs(json);
    if (songs && songs.length > 0) {
      return songs.map(s => {
        const artist = stripYoutubeTopic(s.artist);
        ytArtistCache.set(s.id, artist);
        return {
          id: s.id,
          title: s.title,
          artist: stripYoutubeTopic(artist),
          album: s.album,
          duration: s.duration || 180,
          thumbnail: s.thumbnail || `https://i.ytimg.com/vi/${s.id}/hqdefault.jpg`,
          url: `https://www.youtube.com/watch?v=${s.id}`,
          platform: 'youtube',
          isExplicit: !!s.isExplicit
        };
      });
    }
  } catch (_) {}
  if (!useFallback) return [];
  return [];
}

async function handleSearch(req, res, query, platform = 'ytmusic', limit = 12, offset = 0) {
  limit = Math.max(1, parseInt(limit, 10) || 12);
  offset = Math.max(0, parseInt(offset, 10) || 0);
  const qStr = String(query || '').trim();
  if (!qStr) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, tracks: [], hasMore: false }));
  }

  const poolCacheKey = `pool:${platform}:${qStr.toLowerCase()}`;
  let pool = searchCache.get(poolCacheKey);

  if (offset === 0 && (!pool || !Array.isArray(pool))) {
    if (platform === 'soundcloud') {
      pool = (await fetchSoundCloudSearch(qStr, 40).catch(() => [])) || [];
      pool = cleanSearchResults(pool, qStr);
      searchCache.set(poolCacheKey, pool);
      setTimeout(() => { searchCache.delete(poolCacheKey); }, 300000);
      const sliced = pool.slice(0, limit);
      await Promise.all(sliced.map(async t => {
        if (t.isCached === undefined && t.id) {
          t.isCached = !!(await findCachedFile(t.id));
        }
      }));
      if (sliced[0] && sliced[0].url) {
        prefetchTrack(sliced[0].url, sliced[0].id, sliced[0].platform || 'soundcloud').catch(() => {});
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        success: true,
        tracks: sliced,
        hasMore: limit < pool.length
      }));
    }

    if (platform === 'youtube') {
      pool = (await directYoutubeSearch(qStr, false, 40).catch(() => [])) || [];
      pool = cleanSearchResults(pool, qStr);
      if (pool.length === 0) {
        pool = await fetchMetadataSearchFallback(qStr, 30);
      }
      searchCache.set(poolCacheKey, pool);
      setTimeout(() => { searchCache.delete(poolCacheKey); }, 300000);
      const sliced = pool.slice(0, limit);
      await Promise.all(sliced.map(async t => {
        if (t.isCached === undefined && t.id) {
          t.isCached = !!(await findCachedFile(t.id));
        }
      }));
      if (sliced[0] && sliced[0].url) {
        prefetchTrack(sliced[0].url, sliced[0].id, sliced[0].platform || 'youtube').catch(() => {});
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        success: true,
        tracks: sliced,
        hasMore: limit < pool.length
      }));
    }

    if (platform === 'ytmusic') {
      const ytmTracks = (await ytmusicSearch(qStr).catch(() => [])) || [];
      if (ytmTracks.length > 0) {
        pool = [...ytmTracks];
      } else {
        pool = (await directYoutubeSearch(qStr, false, 40).catch(() => [])) || [];
      }
      pool = cleanSearchResults(pool, qStr);
      if (pool.length === 0) {
        pool = await fetchMetadataSearchFallback(qStr, 30);
      }
      searchCache.set(poolCacheKey, pool);
      setTimeout(() => { searchCache.delete(poolCacheKey); }, 300000);
      const sliced = pool.slice(0, limit);
      await Promise.all(sliced.map(async t => {
        if (t.isCached === undefined && t.id) {
          t.isCached = !!(await findCachedFile(t.id));
        }
      }));
      if (sliced[0] && sliced[0].url) {
        prefetchTrack(sliced[0].url, sliced[0].id, sliced[0].platform || 'youtube').catch(() => {});
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        success: true,
        tracks: sliced,
        hasMore: limit < pool.length
      }));
    }
  }

  if (!pool || !Array.isArray(pool)) {
    if (platform === 'soundcloud') {
      pool = (await fetchSoundCloudSearch(qStr, 40).catch(() => [])) || [];
    } else if (platform === 'youtube') {
      pool = (await directYoutubeSearch(qStr, false, 40).catch(() => [])) || [];
    } else {
      const ytmTracks = (await ytmusicSearch(qStr).catch(() => [])) || [];
      if (ytmTracks.length > 0) {
        pool = [...ytmTracks];
      } else {
        pool = (await directYoutubeSearch(qStr, false, 40).catch(() => [])) || [];
      }
    }
    pool = cleanSearchResults(pool, qStr);
    if ((platform === 'ytmusic' || platform === 'youtube') && pool.length === 0) {
      pool = await fetchMetadataSearchFallback(qStr, 30);
    }
    searchCache.set(poolCacheKey, pool);
    setTimeout(() => { searchCache.delete(poolCacheKey); }, 300000);
  }

  const sliced = pool.slice(offset, offset + limit);
  await Promise.all(sliced.map(async t => {
    if (t.isCached === undefined && t.id) {
      t.isCached = !!(await findCachedFile(t.id));
    }
  }));

  if (offset === 0 && sliced[0] && sliced[0].url) {
    prefetchTrack(sliced[0].url, sliced[0].id, sliced[0].platform || 'youtube').catch(() => {});
  }

  const result = {
    success: true,
    tracks: sliced,
    hasMore: (offset + limit) < pool.length
  };
  res.writeHead(200, { 'Content-Type': 'application/json' });
  return res.end(JSON.stringify(result));
}

async function handleVibeRecommendations(req, res, payload) {
  const historyTracks = Array.isArray(payload?.history) ? payload.history : [];
  const favoriteTracks = Array.isArray(payload?.favorites) ? payload.favorites : [];
  const platform = 'youtube';
  const limit = Math.max(4, Math.min(parseInt(payload?.limit || '10', 10) || 10, 16));
  const clientProfile = payload?.clientProfile || null;
  const extra = {
    seeds: payload?.seeds,
    avoid: payload?.avoid,
    refresh: payload?.refresh
  };

  try {
    const result = await recommendVibeTracks(historyTracks, favoriteTracks, platform, limit, clientProfile, extra);
    if (result && Array.isArray(result.tracks)) {
      result.tracks.forEach(c => {
        if (c && (c.platform === 'youtube' || !c.platform)) {
          c.artist = stripYoutubeTopic(c.artist);
        }
      });
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, ...result }));
  } catch (error) {
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: false, error: error.message }));
  }
}

let serverTrendingCache = { ts: 0, tracks: [] };

function fetchItunesTopSongs(limit = 24) {
  return new Promise((resolve) => {
    const req = https.get('https://itunes.apple.com/us/rss/topsongs/limit=' + limit + '/json', {
      family: 4,
      headers: { 'User-Agent': 'Mozilla/5.0', 'Accept': 'application/json' },
      timeout: 6000
    }, (res) => {
      let body = '';
      res.on('data', chunk => { body += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          const entries = Array.isArray(json?.feed?.entry) ? json.feed.entry : [];
          const tracks = entries.map((e, idx) => {
            const imgs = Array.isArray(e['im:image']) ? e['im:image'] : [];
            const rawImg = imgs.length ? (imgs[imgs.length - 1].label || '') : '';
            const hiImg = rawImg.replace(new RegExp('\\/\\d+x\\d+bb\\.', 'i'), '/600x600bb.');
            const idAttr = e?.id?.attributes?.['im:id'] || String(idx);
            return {
              id: 'it-' + idAttr,
              title: e?.['im:name']?.label || 'Untitled',
              artist: e?.['im:artist']?.label || 'Unknown artist',
              thumbnail: hiImg,
              cover: hiImg,
              duration: 0,
              platform: 'ytmusic',
              url: ''
            };
          }).filter(t => t.title && t.title !== 'Untitled');
          resolve(tracks);
        } catch (_) { resolve([]); }
      });
    });
    req.on('error', () => resolve([]));
    req.on('timeout', () => { req.destroy(); resolve([]); });
  });
}

async function handleTrending(req, res) {
  if (serverTrendingCache.tracks.length > 0 && Date.now() - serverTrendingCache.ts < 2 * 3600 * 1000) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, tracks: serverTrendingCache.tracks }));
    return;
  }
  let tracks = await fetchItunesTopSongs(24);
  if (!tracks || tracks.length === 0) {
    try {
      const dzChart = await httpGetJson('https://api.deezer.com/chart/0/tracks?limit=24', 5000);
      const entries = Array.isArray(dzChart?.data) ? dzChart.data : [];
      tracks = entries.map(item => {
        const cover = item.album?.cover_xl || item.album?.cover_big || item.album?.cover_medium || '';
        return {
          id: item.id ? `dz-${item.id}` : '',
          title: item.title || 'Untitled',
          artist: item.artist?.name || 'Unknown artist',
          thumbnail: cover,
          cover: cover,
          duration: Number(item.duration) || 0,
          platform: 'ytmusic',
          url: ''
        };
      }).filter(t => t.title && (t.id || t.artist));
    } catch (_) { tracks = []; }
  }
  if (!tracks || tracks.length === 0) {
    try {
      const ytmSongs = await searchYtmTracks('Top Hits 2026', 20);
      const entries = Array.isArray(ytmSongs) ? ytmSongs : [];
      tracks = entries.map(item => ({
        id: item.id || '',
        title: item.title || 'Untitled',
        artist: item.artist || 'Unknown artist',
        thumbnail: item.thumbnail || '',
        cover: item.thumbnail || '',
        duration: Number(item.duration) || 0,
        platform: 'ytmusic',
        url: item.url || (item.id ? `https://www.youtube.com/watch?v=${item.id}` : '')
      })).filter(t => t.title && (t.id || t.artist));
    } catch (_) { tracks = []; }
  }
  if (tracks && tracks.length > 0) {
    serverTrendingCache = { ts: Date.now(), tracks };
    boundMap(serverTrendingCache, 35);
  }
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ success: true, tracks: tracks || [] }));
}

function scoreImportedCandidate(targetTitle, targetArtist, targetDur, cand) {
  if (!cand || !cand.title) return -999;
  const norm = (s) => String(s || '').toLowerCase()
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  const tTitle = norm(targetTitle);
  const cTitle = norm(cand.title);
  const tArtist = norm(targetArtist.split(/[,;&\/]|feat\.?|ft\.?/i)[0] || targetArtist);
  const cArtist = norm(cand.artist);
  const cFull = `${cArtist} ${cTitle}`;

  if (!tTitle || !cTitle) return -999;

  let score = 0;
  if (cTitle === tTitle) score += 55;
  else if (cTitle.includes(tTitle) || tTitle.includes(cTitle)) score += 35;
  else {
    const tWords = tTitle.split(' ').filter(w => w.length > 1);
    if (tWords.length > 0) {
      const hits = tWords.filter(w => cTitle.includes(w)).length;
      const ratio = hits / tWords.length;
      if (ratio < 0.45) return -500;
      score += ratio * 28;
    }
  }

  if (tArtist && tArtist !== 'unknown artist') {
    if (cArtist === tArtist || cArtist.includes(tArtist) || tArtist.includes(cArtist)) {
      score += 45;
    } else if (cFull.includes(tArtist)) {
      score += 32;
    } else {
      score -= 35;
    }
  }

  const modifiers = ['slowed', 'reverb', 'sped up', 'speed up', 'nightcore', 'hardstyle', 'remix', 'bootleg', 'mashup', '8d', 'karaoke', 'instrumental', 'cover', 'live', 'tribute', 'bass boosted', 'phonk remix', 'edit'];
  for (const mod of modifiers) {
    const inTarget = tTitle.includes(mod) || norm(targetArtist).includes(mod);
    const inCand = cFull.includes(mod);
    if (inCand && !inTarget) score -= 95;
    else if (inCand && inTarget) score += 22;
  }

  const tDur = Number(targetDur) || 0;
  const cDur = Number(cand.duration) || 0;
  if (tDur > 30 && cDur > 30) {
    const diff = Math.abs(tDur - cDur);
    if (diff <= 3) score += 35;
    else if (diff <= 8) score += 20;
    else if (diff <= 15) score += 8;
    else if (diff > 45) score -= 35;
  }
  if (tDur > 45 && cDur > 0 && cDur < 35) return -500;

  return score;
}

function makeCleanSearchQuery(artist, title) {
  const cleanA = String(artist || '')
    .replace(/\s*-\s*Topic$/i, '')
    .replace(/\bVEVO$/i, '')
    .split(/[,;&\/]|feat\.?|ft\.?/i)[0]
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  const cleanT = String(title || '')
    .replace(/\s*[\(\[]\s*(?:feat\.?|ft\.?|prod\.?|featuring)[^)]*[\)\]]/gi, '')
    .replace(/\s*[\(\[]\s*(?:official\s*(?:music\s*)?(?:video|audio|lyric\s*video|visualizer)|lyrics?\s*video|lyrics?|visualizer|hd|hq|4k|mv|clip)\s*[\)\]]/gi, '')
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  return `${cleanA} ${cleanT}`.trim();
}

async function handleStreamInfo(req, res, trackUrl, trackId, platform = 'youtube', title = '', artist = '', duration = 0, thumbnail = '', timeoutSec = 20) {
  const deadlineAt = Date.now() + timeoutSec * 1000;
  let timedOut = false;
  const activeYtDlps = new Set();
  const stopActiveYtDlps = () => {
    for (const proc of activeYtDlps) {
      if (!proc.killed) proc.kill('SIGKILL');
    }
  };
  const deadlineTimer = setTimeout(() => {
    timedOut = true;
    stopActiveYtDlps();
    if (!res.writableEnded) {
      res.writeHead(504, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Stream resolution timed out' }));
    }
  }, timeoutSec * 1000);
  deadlineTimer.unref();
  res.once('close', () => {
    clearTimeout(deadlineTimer);
    if (!res.writableEnded) stopActiveYtDlps();
  });
  const runYtDlp = (args, limit, extraOpts = {}) => {
    const remaining = deadlineAt - Date.now();
    if (timedOut || remaining <= 0) return Promise.reject(new Error('Stream resolution timed out'));
    return executeYtDlp(args, Math.max(1, Math.min(limit, remaining)), {
      ...extraOpts,
      onChild: (proc) => {
        activeYtDlps.add(proc);
        proc.once('close', () => activeYtDlps.delete(proc));
        if (timedOut) proc.kill('SIGKILL');
      }
    });
  };
  artist = stripYoutubeTopic(artist);
  trackUrl = normalizeSoundCloudUrl(trackUrl);
  const parsedUrl = (req && req.url) ? require('url').parse(req.url, true) : { query: {} };
  const forShare = Boolean(parsedUrl.query && parsedUrl.query.forShare === '1');
  const forceYtDlp = Boolean(parsedUrl.query && parsedUrl.query.forceYtDlp === '1');
  const quality = (parsedUrl.query && parsedUrl.query.quality) || 'medium';
  const queryQ = (parsedUrl.query && parsedUrl.query.q) ? String(parsedUrl.query.q).trim() : '';
  let cleanId = cleanTrackId(trackId);
  if (!cleanId && trackUrl && trackUrl.includes('v=')) {
    cleanId = cleanTrackId(trackUrl.split('v=')[1].split('&')[0]);
  }
  const isRealYtId = Boolean(extractYtIdFromAny(cleanId));
  if (!isRealYtId && trackUrl && /cand_|dz_|it_|sp_|ym_/i.test(trackUrl)) {
    trackUrl = '';
  }

  const isSc = platform === "soundcloud" || (trackUrl && trackUrl.includes("soundcloud.com"));
  if (isSc && (!title && !artist) && trackUrl) {
    const slugMatch = trackUrl.match(/soundcloud\.com\/[^/]+\/([^/?#]+)/);
    if (slugMatch && slugMatch[1] && slugMatch[1] !== 'sets') {
      const trackSlug = slugMatch[1];
      title = trackSlug.replace(/[-_]+/g, ' ').trim();
    }
  }

  const meta = { title, artist, duration, thumbnail };

  let cachedFile = cleanId ? await findCachedFile(cleanId) : null;
  if (!cachedFile && (title || artist)) {
    cachedFile = findCachedFileByMeta(title, artist);
    if (cachedFile) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        success: true,
        isLocal: true,
        streamUrl: `http://127.0.0.1:${PORT}/api/local-track?id=${encodeURIComponent(cleanId || cleanTrackId(title))}`
      }));
    }
  }
  const downloadedName = (title || artist) ? await findDownloadedFile(title, artist) : null;

  if (!forShare && cachedFile) {
    const cachedId = path.basename(cachedFile, path.extname(cachedFile));
    const data = {
      success: true,
      isLocal: true,
      streamUrl: `http://127.0.0.1:${ACTIVE_PORT}/api/local-track?id=${encodeURIComponent(cachedId || cleanId)}`
    };
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(data));
  }

  if (!forShare && downloadedName) {
    const data = {
      success: true,
      isLocal: true,
      isDownload: true,
      streamUrl: `http://127.0.0.1:${ACTIVE_PORT}/api/downloaded-track?name=${encodeURIComponent(downloadedName)}`
    };
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(data));
  }

  const cleanArtist = (artist || '').toLowerCase().trim();
  const cleanTitle = (title || '').toLowerCase().trim();
  const metaStreamKey = (cleanArtist && cleanTitle) ? `${cleanArtist}::${cleanTitle}_${quality}` : null;
  const cacheKey = (cleanId || trackUrl || queryQ || `${artist}_${title}`).toLowerCase() + '_' + quality;
  if (forceYtDlp) {
    if (cacheKey) streamUrlCache.delete(cacheKey);
    if (cleanId) streamUrlCache.delete(cleanId + '_' + quality);
    if (metaStreamKey) streamUrlCache.delete(metaStreamKey);
  }
  if (!forShare && !forceYtDlp && (
    (cacheKey && streamUrlCache.has(cacheKey)) ||
    (cleanId && streamUrlCache.has(cleanId + '_' + quality)) ||
    (metaStreamKey && streamUrlCache.has(metaStreamKey))
  )) {
    const cached = (cacheKey && streamUrlCache.get(cacheKey)) ||
      (cleanId ? streamUrlCache.get(cleanId + '_' + quality) : null) ||
      (metaStreamKey ? streamUrlCache.get(metaStreamKey) : null);
    if (cached && (Date.now() - cached.timestamp < 7200000)) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify(cached.data));
    }
  }

  if (!forceYtDlp && streamInfoInflight.has(cacheKey)) {
    const inflightUrl = await streamInfoInflight.get(cacheKey).catch(() => null);
    if (inflightUrl) {
      const isHls = inflightUrl.includes('.m3u8');
      const proxied = 'http://127.0.0.1:' + ACTIVE_PORT + '/api/audio-proxy?url=' + encodeURIComponent(inflightUrl);
      const data = {
        success: true,
        isLocal: false,
        streamUrl: isHls ? inflightUrl : proxied,
        rawStreamUrl: inflightUrl,
        proxyUrl: proxied,
        isHls
      };
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify(data));
    }
  }

  if (platform === 'youtube' && (!trackUrl || !trackUrl.startsWith('http')) && cleanId && isRealYtId) {
    trackUrl = `https://www.youtube.com/watch?v=${cleanId}`;
  }

  let streamUrl = null;
  let needYtAuth = false;
  let shareWebUrl = '';
  let resolveInflight, rejectInflight;
  const inflightPromise = new Promise((res, rej) => { resolveInflight = res; rejectInflight = rej; });
  streamInfoInflight.set(cacheKey, inflightPromise);

  try {
    needYtAuth = false;
    const isYt = platform === 'youtube' || platform === 'ytmusic' || (trackUrl && (trackUrl.includes('youtube.com') || trackUrl.includes('youtu.be')));
    let ytDlpQualityFormat = 'ba[ext=m4a]/ba[ext=webm]/ba/18/b/best';
    if (quality === 'low') ytDlpQualityFormat = 'wa[ext=m4a]/wa/ba/18/b/best';
    else if (quality === 'high' || quality === 'max') ytDlpQualityFormat = 'ba/18/b/best';

    const audioFormat = (forShare || platform === 'soundcloud' || isSc)
      ? 'http_mp3_128/bestaudio[ext=mp3][protocol^=http][protocol!=http_dash_segments]/bestaudio[ext=m4a][protocol^=http]/bestaudio[protocol=https]/bestaudio/18/best'
      : ytDlpQualityFormat;

    const q = ((artist ? artist + ' - ' : '') + (title || '')).trim() || queryQ;
    const searchQ = makeCleanSearchQuery(artist, title) || q;

    const sendStreamResponse = (resolvedStreamUrl, resolvedId = null) => {
      if (res.writableEnded) return;
      if (!resolvedStreamUrl) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: false, error: 'could not extract audio stream' }));
      }
      resolveInflight(resolvedStreamUrl);
      streamInfoInflight.delete(cacheKey);
      const finalId = resolvedId || cleanId;
      if (forShare) {
        const shareWebUrl = finalId ? `https://www.youtube.com/watch?v=${finalId}` : (trackUrl || '');
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: true, streamUrl: resolvedStreamUrl, shareWebUrl }));
      }
      const portVal = typeof ACTIVE_PORT !== 'undefined' ? ACTIVE_PORT : PORT;
      const isHls = typeof resolvedStreamUrl === 'string' && resolvedStreamUrl.includes('.m3u8');
      const proxied = 'http://127.0.0.1:' + portVal + '/api/audio-proxy?url=' + encodeURIComponent(resolvedStreamUrl);
      const data = {
        success: true,
        isLocal: false,
        streamUrl: isHls ? resolvedStreamUrl : proxied,
        rawStreamUrl: resolvedStreamUrl,
        proxyUrl: proxied,
        isHls: isHls
      };
      if (resolvedId) data.resolvedId = resolvedId;
      if (cacheKey) streamUrlCache.set(cacheKey, { timestamp: Date.now(), data });
      if (finalId && finalId !== cacheKey) streamUrlCache.set(finalId + '_' + quality, { timestamp: Date.now(), data });
      if (metaStreamKey) streamUrlCache.set(metaStreamKey, { timestamp: Date.now(), data });
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify(data));
    };

    const resolveYtCandidateStream = async (candId) => {
      const vid = extractYtIdFromAny(candId);
      if (!vid) return null;
      let s = null;
      if (!forceYtDlp) {
        try {
          s = await fetchDirectYoutubeStreamServer(vid, quality);
          if (s) return s;
        } catch (_) {}
      }
      if (!s && (q || searchQ)) {
        try { s = await fetchDirectSoundCloudStreamServer('', q || searchQ); } catch (_) {}
        if (s) return s;
      }
      try {
        return await resolveYtCandidateStreamDlp(candId);
      } catch (_) {
        return null;
      }
    };

    const resolveYtCandidateStreamDlp = async (candId) => {
      const vid = extractYtIdFromAny(candId);
      if (!vid) return null;
      let s = null;
      const vUrl = `https://www.youtube.com/watch?v=${vid}`;
      try {
        const args = [
          '--no-config', '-g', '--no-warnings', '--no-playlist', '--no-check-certificates',
          '--socket-timeout', '10', '--retries', '2',
          '-f', 'bestaudio/best',
          '--', vUrl
        ];
        const raw = await runYtDlp(args, 15000, { quiet: true });
        const lines = raw.trim().split('\n').map(l => l.trim()).filter(l => l.startsWith('http'));
        if (lines.length > 0) s = lines[0];
      } catch (err) {
        const msg = String((err && err.message) || err || '');
        if (/confirm your age|not a bot|Sign in to confirm|age-restricted|LOGIN_REQUIRED/i.test(msg)) {
          needYtAuth = true;
        }
      }
      if (!s && (q || searchQ)) {
        try { s = await fetchDirectSoundCloudStreamServer('', q || searchQ); } catch (_) {}
        if (s) return s;
      }
      return s;
    };

    const resolveScCandidateStream = async (candUrl, candId) => {
      let s = null;
      try {
        s = await fetchDirectSoundCloudStreamServer(candUrl || candId, '');
      } catch (_) {}
      if (!s && candUrl && isSafeMediaUrl(candUrl)) {
        try {
          const args = [
            '--no-config', '-g', '--no-warnings', '--no-playlist', '--no-check-certificates',
            '--socket-timeout', '8', '--retries', '1',
            '-f', audioFormat,
            '--', candUrl
          ];
          const raw = await runYtDlp(args, 10000);
          const lines = raw.trim().split('\n').map(l => l.trim()).filter(l => l.startsWith('http'));
          const nonDrm = lines.find(l => !/preview|drm|cf-preview-media/i.test(l));
          if (nonDrm && !nonDrm.includes('/preview/') && !nonDrm.includes('cf-preview-media')) s = nonDrm;
        } catch (_) {}
      }
      return s;
    };

    const failedYtIds = new Set();

    if (platform === 'soundcloud' || isSc) {
      streamUrl = await resolveScCandidateStream(trackUrl, cleanId);
      if (streamUrl) return sendStreamResponse(streamUrl);
    } else if (isRealYtId && cleanId) {
      streamUrl = await resolveYtCandidateStream(cleanId);
      if (streamUrl) return sendStreamResponse(streamUrl, cleanId);
      failedYtIds.add(cleanId);
    }

    if (!cachedFile && (title || artist)) {
      cachedFile = findCachedFileByMeta(title, artist);
      if (cachedFile) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          success: true,
          isLocal: true,
          streamUrl: `http://127.0.0.1:${PORT}/api/local-track?id=${encodeURIComponent(cleanId || cleanTrackId(title))}`
        }));
      }
    }

    if (!streamUrl && searchQ) {
      const maxCand = needYtAuth ? 1 : 3;
      const needScRace = failedYtIds.size > 0 || !cleanId || !isRealYtId;
      const scFallback = needScRace
        ? fetchDirectSoundCloudStreamServer('', q || searchQ).then(u => u ? { url: u, id: null } : Promise.reject(new Error('no sc')))
        : null;

      const ytCandidateResolution = (async () => {
        const [ytmHits, ytHits] = await Promise.all([
          ytmusicSearch(searchQ, { timeoutMs: 3500, maxRetries: 0, fallback: false }).catch(() => []),
          directYoutubeSearch(searchQ, false, 12).catch(() => [])
        ]);
        const candidates = new Map();
        for (const candidate of [...(ytmHits || []), ...(ytHits || [])]) {
          if (candidate && candidate.id && !failedYtIds.has(candidate.id) && !candidates.has(candidate.id)) {
            candidates.set(candidate.id, candidate);
          }
        }
        const rankedCandidates = Array.from(candidates.values())
          .map(cand => ({ cand, score: scoreImportedCandidate(title || searchQ, artist, duration, cand) }))
          .sort((a, b) => b.score - a.score)
          .slice(0, maxCand);
        rankedCandidates.forEach(item => failedYtIds.add(item.cand.id));
        if (!rankedCandidates.length) throw new Error('no yt candidates');
        const streamResults = await Promise.all(rankedCandidates.map(async ({ cand }) => {
          try {
            const candResolvedUrl = await resolveYtCandidateStream(cand.id);
            return candResolvedUrl ? { url: candResolvedUrl, id: cand.id } : null;
          } catch (_) {
            return null;
          }
        }));
        const match = streamResults.find(result => result !== null);
        if (match) return match;
        throw new Error('no yt match');
      })();

      let bestMatch = null;
      if (scFallback) {
        try {
          bestMatch = await Promise.any([ytCandidateResolution, scFallback]);
        } catch (_) {}
      } else {
        try {
          bestMatch = await ytCandidateResolution;
        } catch (_) {}
      }
      if (bestMatch) return sendStreamResponse(bestMatch.url, bestMatch.id);

      if (!streamUrl) {
        try {
          streamUrl = await fetchDirectSoundCloudStreamServer('', q || searchQ);
          if (streamUrl) return sendStreamResponse(streamUrl);
        } catch (_) {}
      }

      if (!streamUrl && !needYtAuth) {
        try {
          const fbRaw = await runYtDlp([
            '--no-config', '-g', '--no-warnings', '--no-playlist', '--no-check-certificates',
            '--socket-timeout', '8', '--retries', '1',
            '--extractor-args', 'youtube:player_client=default,android_vr',
            '-f', 'bestaudio/18/best',
            `ytsearch1:${q || searchQ} audio`
          ], 10000);
          const fbLines = fbRaw.trim().split('\n').map(l => l.trim()).filter(l => l.startsWith('http'));
          for (const fbUrl of fbLines) {
            if (await verifyStreamPlayable(fbUrl)) {
              streamUrl = fbUrl;
              return sendStreamResponse(streamUrl);
            }
          }
        } catch (_) {}
      }
    }

    if (!streamUrl) {
      try {
        streamUrl = await fetchDirectSoundCloudStreamServer('', q || searchQ);
        if (streamUrl) return sendStreamResponse(streamUrl);
      } catch (_) {}
    }


    shareWebUrl = '';
    if (forShare && (!streamUrl || streamUrl.includes('.m3u8')) && (title || artist)) {
      try {
        const rawYt = await runYtDlp([
          '--no-config', '--no-warnings', '--no-playlist',
          '--extractor-args', YT_EXTRACTOR_ARGS,
          '--print', '%(webpage_url)s\n%(url)s',
          '-f', 'bestaudio[ext=m4a]/bestaudio',
          'ytsearch1:' + ((artist ? artist + ' - ' : '') + title).trim() + ' audio'
        ], 10000);
        const lines = rawYt.trim().split(String.fromCharCode(10)).map(l => l.trim()).filter(Boolean);
        if (lines.length >= 2) {
          shareWebUrl = lines[0];
          streamUrl = lines[1];
        } else if (lines.length === 1) {
          if (lines[0].startsWith('http') && (lines[0].includes('youtube.com') || lines[0].includes('youtu.be'))) {
            shareWebUrl = lines[0];
          } else if (lines[0].startsWith('http')) {
            streamUrl = lines[0];
          }
        }
      } catch (e) {}
    }
    if (forShare && !shareWebUrl) {
      if (trackUrl && (trackUrl.includes('youtube.com') || trackUrl.includes('youtu.be'))) {
        shareWebUrl = trackUrl;
      } else if (cleanId && isYt) {
        shareWebUrl = `https://www.youtube.com/watch?v=${cleanId}`;
      }
    }

    resolveInflight(streamUrl);
  } catch (e) {
    rejectInflight(e);
  } finally {
    streamInfoInflight.delete(cacheKey);
  }

  if (forShare) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      success: true,
      streamUrl: streamUrl || '',
      shareWebUrl: shareWebUrl || ''
    }));
  }

  if (!streamUrl && !res.writableEnded) {
    try { logger.error('Stream resolve failed: ' + (title || trackUrl)); } catch (_) {}
    res.writeHead(500, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, needAuth: needYtAuth, error: needYtAuth ? 'YouTube sign-in required for this track' : 'could not extract audio stream' }));
  }

  const portVal = typeof ACTIVE_PORT !== 'undefined' ? ACTIVE_PORT : PORT;
  const isHls = streamUrl.includes('.m3u8');
  const proxied = 'http://127.0.0.1:' + portVal + '/api/audio-proxy?url=' + encodeURIComponent(streamUrl);
  const data = {
    success: true,
    isLocal: false,
    streamUrl: isHls ? streamUrl : proxied,
    rawStreamUrl: streamUrl,
    proxyUrl: proxied,
    isHls: isHls
  };

  const finalId = cleanId;
  if (cacheKey) streamUrlCache.set(cacheKey, { timestamp: Date.now(), data });
  if (cleanId && cleanId !== cacheKey) streamUrlCache.set(cleanId, { timestamp: Date.now(), data });

  if (res.writableEnded) return;
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(data));
}

function upgradeArtworkUrl(u) {
  if (!u || typeof u !== 'string') return '';
  return u
    .replace(new RegExp('\\/(\\d{2,3})x(\\d{2,3})(bb|cc|sr)?\\.(jpg|png|webp)', 'i'), '/1200x1200bb.$4')
    .replace(new RegExp('\\/picture\\/([^/]+)\\/\\d+x\\d+-', 'i'), '/picture/$1/1000x1000-')
    .replace(new RegExp('=w\\d+-h\\d+[^&?]*', 'i'), '=w1400-h1400-l90-rj')
    .replace(new RegExp('-(?:large|t500x500|crop)\\.(jpg|png)', 'i'), '-t1080x1080.$1');
}

function fetchHighResArtistPhoto(artistName) {
  return new Promise((resolve) => {
    const q = encodeURIComponent(artistName + ' photos');
    const reqUrl = 'https://www.bing.com/images/search?q=' + q + '&qft=+filterui:imagesize-large&form=IRFLTR';
    const req = https.get(reqUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Accept-Language': 'en-US,en;q=0.9'
      },
      timeout: 4500
    }, (res) => {
      let html = '';
      res.on('data', chunk => { if (html.length < 250000) html += chunk; });
      res.on('end', () => {
        try {
          const re = new RegExp('murl(?:&quot;|")\\s*:\\s*(?:&quot;|")(https?:\\/\\/[^"&]+?\\.(?:jpg|jpeg|png|webp))', 'gi');
          let m;
          while ((m = re.exec(html)) !== null) {
            const url = m[1];
            if (url && !/logo|icon|sprite|avatar_default|placeholder/i.test(url)) return resolve(url);
          }
        } catch (_) {}
        resolve('');
      });
    });
    req.on('error', () => resolve(''));
    req.on('timeout', () => { req.destroy(); resolve(''); });
  });
}

async function fetchItunesArtistAlbums(artistName) {
  if (!artistName || !artistName.trim()) return [];
  try {
    const target = artistName.toLowerCase().trim();
    const url = `https://itunes.apple.com/search?term=${encodeURIComponent(artistName)}&entity=album&limit=35`;
    const data = await fetchItunesJson(url);
    const results = (data && Array.isArray(data.results)) ? data.results : [];
    return results
      .filter(r => r && String(r.artistName || '').toLowerCase().trim() === target && (r.trackCount || 0) >= 2)
      .map(r => ({
        id: String(r.collectionId),
        title: r.collectionName,
        artist: r.artistName,
        cover: upgradeArtworkUrl(r.artworkUrl100 || ''),
        year: r.releaseDate ? r.releaseDate.slice(0, 4) : '',
        trackCount: r.trackCount || 0,
        source: 'itunes'
      }));
  } catch (e) {
    return [];
  }
}

async function fetchMusicBio(artistName) {
  const clean = (artistName || '').trim();
  if (!clean) return '';

  try {
    const lfmUrl = `https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=${encodeURIComponent(clean)}&api_key=4cb074e4b8ec4ee9ad3eb37d6f7eb240&format=json`;
    const lfmData = await httpGetJson(lfmUrl, 3500);
    const bioSummary = lfmData?.artist?.bio?.summary || lfmData?.artist?.bio?.content || '';
    if (bioSummary) {
      const cleanBio = bioSummary.replace(/<[^>]*>/g, '').replace(/\s*Read more on Last\.fm.*/i, '').trim();
      if (cleanBio.length > 20) return cleanBio;
    }
  } catch (_) {}

  const cleanLower = clean.toLowerCase();
  const isRu = /[а-яё]/i.test(clean) || clean.toUpperCase() === 'FACE';
  const wikiHost = isRu ? 'ru.wikipedia.org' : 'en.wikipedia.org';
  const queryTerm = isRu ? `${clean} (рэпер OR музыкант OR певец OR группа)` : `${clean} (musician OR rapper OR singer OR band OR music)`;

  try {
    const searchUrl = `https://${wikiHost}/w/api.php?action=query&list=search&srsearch=${encodeURIComponent(queryTerm)}&utf8=&format=json`;
    const searchData = await httpGetJson(searchUrl, 3500);
    const searchHits = searchData?.query?.search || [];
    for (const hit of searchHits.slice(0, 3)) {
      if (!hit || !hit.title) continue;
      const titleLower = hit.title.toLowerCase();
      if (!titleLower.startsWith(cleanLower)) continue;
      if (/list of |discography|may refer to/i.test(titleLower)) continue;

      const summaryUrl = `https://${wikiHost}/api/rest_v1/page/summary/${encodeURIComponent(hit.title)}`;
      const summaryData = await httpGetJson(summaryUrl, 3500);
      if (summaryData && summaryData.extract) {
        const ext = summaryData.extract;
        const sumTitle = (summaryData.title || '').toLowerCase();
        if (!sumTitle.startsWith(cleanLower)) continue;
        if (/list of |discography|may refer to/i.test(sumTitle) || /list of |discography|may refer to/i.test(ext)) continue;
        if (!/ammunition|cartridge|human face|anatomy|organism/i.test(ext)) {
          const extLower = ext.toLowerCase();
          if (extLower.includes(cleanLower) || cleanLower.split(/\s+/).some(w => w.length >= 3 && extLower.includes(w))) {
            return ext;
          }
        }
      }
    }
  } catch (_) {}

  return '';
}

async function fetchArtistInfo(artistName, hintTrack = '') {
  const cleanName = (artistName || '').replace(/ - Topic|VEVO|Official|Records/gi, '').trim();
  if (!cleanName) {
    return { name: '', bio: '', avatar: '', banner: '', albums: [], isVerified: false, isKnownArtist: false, fans: 0, monthlyListeners: '', tracks: [] };
  }
  const norm = (s) => (s || '').toLowerCase().trim();
  const normClean = norm(cleanName);
  const cacheKey = (cleanName + '::' + (hintTrack || '')).toLowerCase();
  if (artistCache.has(cacheKey)) return artistCache.get(cacheKey);

  let bio = '';
  let avatar = '';
  let banner = '';
  let isVerified = false;
  let isKnownArtist = false;
  let fansCount = 0;
  let monthlyListeners = '';
  let tracks = [];
  let albums = [];
  let resolvedName = cleanName;

  let artistMatch = null;
  try {
    const dzSearchUrl = `https://api.deezer.com/search/artist?q=${encodeURIComponent(cleanName)}&limit=10`;
    const dzRes = await new Promise((resolve) => {
      https.get(dzSearchUrl, { headers: { 'User-Agent': 'Mozilla/5.0' }, timeout: 4500 }, (dRes) => {
        let d = '';
        dRes.on('data', c => d += c);
        dRes.on('end', () => {
          try { resolve(JSON.parse(d)); } catch (_) { resolve(null); }
        });
      }).on('error', () => resolve(null));
    });

    const candidates = (dzRes && Array.isArray(dzRes.data)) ? dzRes.data : [];
    const matchingCandidates = candidates.filter(c => c && norm(c.name) === normClean);

    if (matchingCandidates.length === 1) {
      artistMatch = matchingCandidates[0];
    } else if (matchingCandidates.length > 1) {
      if (hintTrack) {
        const cleanHint = cleanTitleForMatching(hintTrack);
        for (const cand of matchingCandidates) {
          try {
            const candTop = await new Promise((resolve) => {
              https.get(`https://api.deezer.com/artist/${cand.id}/top?limit=25`, { headers: { 'User-Agent': 'Mozilla/5.0' }, timeout: 4000 }, (tRes) => {
                let d = '';
                tRes.on('data', c => d += c);
                tRes.on('end', () => {
                  try { resolve(JSON.parse(d)); } catch (_) { resolve(null); }
                });
              }).on('error', () => resolve(null));
            });
            const topList = (candTop && Array.isArray(candTop.data)) ? candTop.data : [];
            const hasMatch = topList.some(t => {
              const cTit = cleanTitleForMatching(t.title);
              return cTit && cleanHint && (cTit.includes(cleanHint) || cleanHint.includes(cTit));
            });
            if (hasMatch) {
              artistMatch = cand;
              break;
            }
          } catch (_) {}
        }
      }
      if (!artistMatch) {
        artistMatch = matchingCandidates[0];
      }
    } else if (candidates.length > 0 && norm(candidates[0].name).includes(normClean)) {
      artistMatch = candidates[0];
    }
  } catch (_) {}

  if (artistMatch && artistMatch.id) {
    resolvedName = artistMatch.name || cleanName;
    avatar = artistMatch.picture_xl || artistMatch.picture_big || artistMatch.picture_medium || '';
    banner = avatar;
    fansCount = artistMatch.nb_fan || 0;
    isKnownArtist = true;
    isVerified = fansCount > 30000;

    try {
      const topData = await new Promise((resolve) => {
        https.get(`https://api.deezer.com/artist/${artistMatch.id}/top?limit=100`, { headers: { 'User-Agent': 'Mozilla/5.0' }, timeout: 5000 }, (res) => {
          let d = '';
          res.on('data', c => d += c);
          res.on('end', () => {
            try { resolve(JSON.parse(d)); } catch (_) { resolve(null); }
          });
        }).on('error', () => resolve(null));
      });

      if (topData && Array.isArray(topData.data)) {
        const filtered = topData.data.filter(t => t && (t.artist?.id === artistMatch.id || (t.contributors || []).some(c => c && c.id === artistMatch.id)));
        tracks = filtered.map(item => {
          const dur = item.duration || 180;
          const cover = item.album?.cover_xl || item.album?.cover_big || item.album?.cover_medium || avatar;
          return {
            id: `dz_${item.id}`,
            title: item.title || 'untitled',
            artist: item.artist?.name || resolvedName,
            duration: dur,
            durationMs: dur * 1000,
            thumbnail: cover,
            url: `https://www.youtube.com/results?search_query=${encodeURIComponent((item.artist?.name || resolvedName) + ' - ' + (item.title || '') + ' official audio')}`,
            searchQuery: `${item.artist?.name || resolvedName} - ${item.title || ''} official audio`,
            platform: 'youtube',
            isCached: false,
            isExplicit: Boolean(item.explicit_lyrics || item.explicit_content_lyrics === 1 || item.explicit_content_lyrics || isTextExplicit(item.title || ''))
          };
        });
      }

      const albData = await new Promise((resolve) => {
        https.get(`https://api.deezer.com/artist/${artistMatch.id}/albums?limit=40`, { headers: { 'User-Agent': 'Mozilla/5.0' }, timeout: 4500 }, (res) => {
          let d = '';
          res.on('data', c => d += c);
          res.on('end', () => {
            try { resolve(JSON.parse(d)); } catch (_) { resolve(null); }
          });
        }).on('error', () => resolve(null));
      });
      if (albData && Array.isArray(albData.data)) {
        const rawAlbums = albData.data.filter(a => a && a.title && (a.artist?.id === artistMatch.id || norm(a.artist?.name || artistMatch.name) === normClean));
        albums = [];
        const existingTrackTitles = new Set(tracks.map(t => cleanTitleForMatching(t.title)));
        for (const a of rawAlbums) {
          const isSingle = a.nb_tracks === 1 || (a.record_type === 'single' && (!a.nb_tracks || a.nb_tracks <= 1));
          if (isSingle) {
            const cTitle = cleanTitleForMatching(a.title);
            if (cTitle && !existingTrackTitles.has(cTitle)) {
              existingTrackTitles.add(cTitle);
              tracks.push({
                id: `dz_s_${a.id}`,
                title: a.title,
                artist: artistMatch.name || resolvedName,
                duration: 180,
                durationMs: 180000,
                thumbnail: upgradeArtworkUrl(a.cover_xl || a.cover_big || a.cover_medium || avatar),
                url: '',
                searchQuery: `${artistMatch.name || resolvedName} - ${a.title}`,
                platform: 'ytmusic',
                isCached: false,
                isExplicit: Boolean(a.explicit_lyrics || isTextExplicit(a.title))
              });
            }
          } else {
            albums.push({
              id: String(a.id),
              title: a.title,
              artist: artistMatch.name,
              cover: upgradeArtworkUrl(a.cover_xl || a.cover_big || a.cover_medium || ''),
              year: a.release_date ? a.release_date.slice(0, 4) : '',
              trackCount: a.nb_tracks || 0,
              recordType: a.record_type || 'album',
              source: 'deezer'
            });
          }
        }
      }
    } catch (_) {}
  } else {
    const ytmTracks = await searchYtmTracks(cleanName, 25).catch(() => []);
    const filteredYtm = (ytmTracks || []).filter(t => {
      const a = (t.artist || '').toLowerCase().trim();
      const target = cleanName.toLowerCase().trim();
      if (a === target) return true;
      const tokens = a.split(/[,&\/]|feat\.?|ft\.?/i).map(s => s.trim());
      return tokens.includes(target);
    });
    if (filteredYtm.length >= 4) {
      tracks = filteredYtm;
    }

    if (tracks.length < 4) {
      try {
        const itunesSearchSongsUrl = `https://itunes.apple.com/search?term=${encodeURIComponent(cleanName)}&entity=song&limit=30`;
        const itunesSongsData = await fetchItunesJson(itunesSearchSongsUrl).catch(() => null);
        if (itunesSongsData && Array.isArray(itunesSongsData.results)) {
          const itTracks = itunesSongsData.results
            .filter(item => item && item.wrapperType === 'track')
            .map(item => {
              const dur = Math.round((item.trackTimeMillis || 0) / 1000);
              const cover = item.artworkUrl100 ? item.artworkUrl100.replace('100x100', '600x600') : '';
              return {
                id: `it_${item.trackId}`,
                title: item.trackName || 'untitled',
                artist: item.artistName || resolvedName,
                duration: dur,
                durationMs: item.trackTimeMillis || dur * 1000,
                thumbnail: cover,
                url: `https://www.youtube.com/results?search_query=${encodeURIComponent((item.artistName || resolvedName) + ' - ' + (item.trackName || '') + ' official audio')}`,
                searchQuery: `${item.artistName || resolvedName} - ${item.trackName || ''} official audio`,
                platform: 'youtube',
                isCached: false,
                isExplicit: Boolean(item.trackExplicitness === 'explicit' || isTextExplicit(item.trackName || ''))
              };
            });
          if (itTracks.length > 0) {
            tracks = itTracks;
          }
        }
      } catch (_) {}
    }

    if (tracks.length < 4) {
      try {
        const directResults = await directYoutubeSearch(`${cleanName} official audio`, true).catch(() => []);
        if (directResults && directResults.length > 0) {
          tracks = directResults;
        }
      } catch (_) {}
    }
  }

  bio = await fetchMusicBio(cleanName);

  try {
    const hdPhoto = await fetchHighResArtistPhoto(resolvedName);
    if (hdPhoto) {
      if (!avatar) avatar = hdPhoto;
      banner = hdPhoto;
    }
  } catch (_) {}

  try {
    const lfmUrl = `https://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=${encodeURIComponent(cleanName)}&api_key=4cb074e4b8ec4ee9ad3eb37d6f7eb240&format=json`;
    const lfmData = await httpGetJson(lfmUrl, 4000);
    if (lfmData && lfmData.artist) {
      const lfmListeners = parseInt(lfmData.artist.stats?.listeners || '0', 10);
      if (lfmListeners > 0) {
        fansCount = Math.max(fansCount, lfmListeners);
        monthlyListeners = `${lfmListeners.toLocaleString()} monthly listeners`;
      }
    }
  } catch (_) {}

  if (fansCount > 12000 || (tracks && tracks.length >= 5)) {
    isVerified = true;
    isKnownArtist = true;
  }

  const result = {
    name: resolvedName,
    bio: (bio || '').trim(),
    avatar: avatar || (tracks[0] ? tracks[0].thumbnail : ''),
    banner: banner || avatar || (tracks[0] ? tracks[0].thumbnail : ''),
    albums: albums,
    isVerified: isVerified,
    isKnownArtist: isKnownArtist,
    fans: fansCount,
    monthlyListeners: monthlyListeners,
    tracks: tracks.slice(0, 100)
  };

  artistCache.set(cacheKey, result);
  boundMap(artistCache, 35);
  return result;
}

async function handleLocalTrack(req, res, trackId) {
  const cleanId = cleanTrackId(trackId);
  let filePath = await findCachedFile(cleanId);
  if (!filePath) {
    for (const [cid, meta] of Object.entries(trackMetadataIndex)) {
      if (cid === cleanId || (meta && meta.title && cleanTrackId(meta.title) === cleanId)) {
        const extensions = ['.opus', '.webm', '.m4a', '.mp3'];
        for (const ext of extensions) {
          const fp = path.join(CACHE_DIR, `${cid}${ext}`);
          if (fs.existsSync(fp)) { filePath = fp; break; }
        }
      }
      if (filePath) break;
    }
  }

  if (!filePath) {
    res.writeHead(404);
    return res.end('File not found in local cache');
  }

  try {
    const stat = await fs.promises.stat(filePath);
    const fileSize = stat.size;
    const range = req.headers.range;

    const ext = path.extname(filePath).toLowerCase();
    const mimeTypes = {
      '.opus': 'audio/ogg; codecs=opus',
      '.webm': 'audio/webm',
      '.m4a': 'audio/mp4',
      '.mp3': 'audio/mpeg'
    };
    const contentType = mimeTypes[ext] || 'audio/mpeg';

    if (range) {
      const parsed = parseByteRange(range, fileSize);
      if (!parsed) {
        res.writeHead(416, { 'Content-Range': `bytes */${fileSize}` });
        res.end();
        return;
      }
      const { start, end } = parsed;
      const chunksize = (end - start) + 1;
      const file = fs.createReadStream(filePath, { start, end });
      const head = {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': contentType,
      };
      res.writeHead(206, head);
      streamFileToResponse(file, res);
    } else {
      const head = {
        'Content-Length': fileSize,
        'Content-Type': contentType,
        'Accept-Ranges': 'bytes'
      };
      res.writeHead(200, head);
      streamFileToResponse(fs.createReadStream(filePath), res);
    }
  } catch (e) {
    res.writeHead(500);
    res.end('Error reading local file');
  }
}

const LIBRARY_MIME_TYPES = {
  '.mp3': 'audio/mpeg',
  '.wav': 'audio/wav',
  '.flac': 'audio/flac',
  '.opus': 'audio/ogg; codecs=opus',
  '.ogg': 'audio/ogg',
  '.oga': 'audio/ogg',
  '.m4a': 'audio/mp4',
  '.aac': 'audio/aac',
  '.webm': 'audio/webm',
  '.weba': 'audio/webm'
};

async function handleLibraryFile(req, res, trackId) {
  const item = libraryIndex[trackId];
  if (!item || !item.file) {
    res.writeHead(404);
    return res.end('File not found in library');
  }

  const filePath = path.join(LIBRARY_DIR, item.file);
  try {
    const stat = await fs.promises.stat(filePath);
    const fileSize = stat.size;
    const range = req.headers.range;

    const ext = path.extname(filePath).toLowerCase();
    const contentType = LIBRARY_MIME_TYPES[ext] || 'audio/mpeg';

    if (range) {
      const parsed = parseByteRange(range, fileSize);
      if (!parsed) {
        res.writeHead(416, { 'Content-Range': `bytes */${fileSize}` });
        res.end();
        return;
      }
      const { start, end } = parsed;
      const chunksize = (end - start) + 1;
      const file = fs.createReadStream(filePath, { start, end });
      const head = {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': contentType,
      };
      res.writeHead(206, head);
      streamFileToResponse(file, res);
    } else {
      const head = {
        'Content-Length': fileSize,
        'Content-Type': contentType,
        'Accept-Ranges': 'bytes'
      };
      res.writeHead(200, head);
      streamFileToResponse(fs.createReadStream(filePath), res);
    }
  } catch (e) {
    res.writeHead(404);
    res.end('File not found in library');
  }
}

async function handleDownloadedTrack(req, res, name) {
  const safeName = path.basename(String(name || ''));
  const ext = path.extname(safeName).toLowerCase();
  const dir = getDownloadsDir();
  const filePath = path.join(dir, safeName);

  if (!safeName || !AUDIO_EXTENSIONS.has(ext) || path.dirname(filePath) !== dir) {
    res.writeHead(404);
    return res.end('File not found in downloads');
  }

  try {
    const stat = await fs.promises.stat(filePath);
    const fileSize = stat.size;
    const range = req.headers.range;

    const contentType = LIBRARY_MIME_TYPES[ext] || 'audio/mpeg';

    if (range) {
      const parsed = parseByteRange(range, fileSize);
      if (!parsed) {
        res.writeHead(416, { 'Content-Range': `bytes */${fileSize}` });
        res.end();
        return;
      }
      const { start, end } = parsed;
      const chunksize = (end - start) + 1;
      const file = fs.createReadStream(filePath, { start, end });
      const head = {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': contentType,
      };
      res.writeHead(206, head);
      streamFileToResponse(file, res);
    } else {
      const head = {
        'Content-Length': fileSize,
        'Content-Type': contentType,
        'Accept-Ranges': 'bytes'
      };
      res.writeHead(200, head);
      streamFileToResponse(fs.createReadStream(filePath), res);
    }
  } catch (e) {
    res.writeHead(404);
    res.end('File not found in downloads');
  }
}

const GV_UA = {
  IOS: 'com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)',
  ANDROID_VR: 'com.google.android.apps.youtube.vr.oculus/1.62.27 (Linux; U; Android 14; en_US; Quest 3 Build/UP1A.231005.007.A1) gzip',
  ANDROID_MUSIC: 'com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14; en_US; sdk_gphone64_x86_64 Build/UE1A.230829.036) gzip',
  TV: 'Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) 85.0.4183.93/6.5 TV Safari/537.36',
  ANDROID_TESTSUITE: 'com.google.android.youtube/1.9 (Linux; U; Android 11; en_US) gzip',
  ANDROID: 'com.google.android.youtube/19.44.38 (Linux; U; Android 14; en_US; sdk_gphone64_x86_64 Build/UE1A.230829.036) gzip',
  WEB: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
};
function googleVideoUserAgent(parsed) {
  const c = (parsed && parsed.searchParams && parsed.searchParams.get('c') || '').toUpperCase();
  if (c === 'ANDROID_MUSIC') return 'com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14; en_US; sdk_gphone64_x86_64 Build/UE1A.230829.036) gzip';
  return 'com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)';
}







async function handleGoogleVideoChunkedProxy(req, res, targetUrl, initialUrl) {
  let parsed;
  try { parsed = new URL(targetUrl); } catch (_) { res.writeHead(400); return res.end('Invalid url'); }
  const ua = googleVideoUserAgent(parsed);
  const CHUNK = 1536 * 1024;
  const mime = (parsed.searchParams.get('mime') || 'audio/mp4').replace(/%2F/i, '/');
  let total = parseInt(parsed.searchParams.get('clen') || '0', 10) || ytContentLengthByUrl.get(targetUrl) || 0;

  const rm = /bytes=(\d*)-(\d*)/i.exec(req.headers.range || '');
  let start = rm && rm[1] !== '' ? parseInt(rm[1], 10) : 0;
  let reqEnd = rm && rm[2] !== '' ? parseInt(rm[2], 10) : null;
  const hadRange = Boolean(rm);

  let aborted = false;
  let curReq = null;
  res.on('close', () => { aborted = true; try { curReq && curReq.destroy(); } catch (_) {} });

  const fetchChunk = (pos, end, useQuery, urlStr) => new Promise((resolve, reject) => {
    let u = urlStr;
    const headers = { 'User-Agent': ua, 'Accept': '*/*', 'Accept-Language': 'en-US,en;q=0.9' };
    if (useQuery) {
      const uu = new URL(urlStr);
      uu.searchParams.set('range', `${pos}-${end}`);
      u = uu.toString();
    } else {
      headers['Range'] = `bytes=${pos}-${end}`;
    }
    const r = https.request(u, { method: 'GET', family: 4, agent: keepAliveHttpsAgent, headers, timeout: 20000 }, (pr) => {
      if ([301, 302, 303, 307, 308].includes(pr.statusCode) && pr.headers.location) {
        pr.resume();
        return resolve({ redirect: new URL(pr.headers.location, u).toString() });
      }
      resolve({ pr });
    });
    curReq = r;
    r.on('timeout', () => r.destroy(new Error('chunk timeout')));
    r.on('error', reject);
    r.end();
  });

  const getChunk = async (pos, end, state) => {
    for (let attempt = 0; attempt < 2; attempt++) {
      for (const useQuery of [false, true]) {
        if (aborted) throw new Error('aborted');
        let out;
        try { out = await fetchChunk(pos, end, useQuery, state.url); } catch (e) { continue; }
        if (out.redirect) { state.url = out.redirect; try { out = await fetchChunk(pos, end, useQuery, state.url); } catch (e) { continue; } }
        if (out.pr && (out.pr.statusCode === 206 || out.pr.statusCode === 200)) return out.pr;
        if (out.pr) out.pr.resume();
      }
    }
    throw new Error('chunk refused');
  };

  const state = { url: targetUrl };
  try {
    if (!total) {
      

      const pr0 = await getChunk(start, start + 1, state);
      const cr = /\/(\d+)\s*$/.exec(pr0.headers['content-range'] || '');
      pr0.destroy();
      if (cr) total = parseInt(cr[1], 10);
    }
    if (total && start >= total) { res.writeHead(416, { 'Content-Range': `bytes */${total}`, 'Access-Control-Allow-Origin': '*' }); return res.end(); }
    const finalEnd = total ? Math.min(reqEnd === null ? total - 1 : reqEnd, total - 1) : (reqEnd === null ? start + CHUNK * 64 : reqEnd);

    const headers = {
      'Content-Type': mime,
      'Accept-Ranges': 'bytes',
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': '*',
      'Access-Control-Expose-Headers': 'Content-Length, Content-Range, Accept-Ranges'
    };
    if (total) headers['Content-Length'] = String(finalEnd - start + 1);
    if (hadRange && total) { headers['Content-Range'] = `bytes ${start}-${finalEnd}/${total}`; res.writeHead(206, headers); }
    else res.writeHead(200, headers);
    if (req.method === 'HEAD') return res.end();

    let pos = start;
    let first = true;
    while (pos <= finalEnd && !aborted) {
      const end = Math.min(pos + CHUNK - 1, finalEnd);
      let pr;
      try { pr = await getChunk(pos, end, state); }
      catch (e) {
        if (first) { evictStreamCache(initialUrl); evictStreamCache(targetUrl); }
        try { res.destroy(); } catch (_) {}
        return;
      }
      first = false;
      let got = 0;
      await new Promise((resolve) => {
        pr.on('data', (d) => { got += d.length; if (!res.write(d)) { pr.pause(); res.once('drain', () => pr.resume()); } });
        pr.on('end', resolve);
        pr.on('error', resolve);
        pr.on('aborted', resolve);
        res.once('close', resolve);
      });
      if (got === 0) { evictStreamCache(initialUrl); evictStreamCache(targetUrl); break; }
      pos += got;
      if (!total && got < (end - (pos - got) + 1)) break;
    }
    if (!res.writableEnded) res.end();
  } catch (e) {
    evictStreamCache(initialUrl);
    if (!res.headersSent) { res.writeHead(502, { 'Access-Control-Allow-Origin': '*' }); res.end('Proxy error'); }
    else { try { res.destroy(); } catch (_) {} }
  }
}

async function handleAudioProxy(req, res, targetUrl, redirectsLeft = 5, origUrl = null) {
  const initialUrl = origUrl || targetUrl;
  if (!targetUrl) {
    res.writeHead(400);
    return res.end('Missing url');
  }
  try {
    let parsed;
    try {
      parsed = new URL(targetUrl);
    } catch (e) {
      res.writeHead(400);
      return res.end('Invalid url');
    }
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
      res.writeHead(400);
      return res.end('Unsupported protocol');
    }
    const hostLower = (parsed.hostname || '').toLowerCase();
    if (hostLower === 'localhost' || hostLower === '127.0.0.1' || hostLower === '::1') {
      res.writeHead(403);
      return res.end('Blocked target');
    }

    const client = parsed.protocol === 'https:' ? https : http;
    const forward = { host: parsed.host };
    for (const key of ['range', 'accept', 'accept-language']) {
      if (req.headers[key] !== undefined) forward[key] = req.headers[key];
    }

    if (hostLower.endsWith('googlevideo.com')) {
      if (!forward['range']) {
        forward['range'] = 'bytes=0-';
      }
      const cParam = (parsed.searchParams.get('c') || '').toUpperCase();
      forward['user-agent'] = googleVideoUserAgent(parsed);
      if ((cParam === 'WEB' || cParam === 'WEB_REMIX' || cParam === 'TVHTML5') && fs.existsSync(YT_COOKIES_FILE)) {
        const authHdrs = getYtAuthHeaders();
        if (authHdrs && authHdrs.Cookie) forward['cookie'] = authHdrs.Cookie;
      }
    } else {
      forward['user-agent'] = req.headers['user-agent'] || 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';
    }

    const isGv = hostLower.endsWith('googlevideo.com');
    const proxyReq = client.request(targetUrl, {
      method: req.method,
      family: 4,
      agent: isGv ? false : (parsed.protocol === 'https:' ? keepAliveHttpsAgent : undefined),
      headers: forward
    }, (proxyRes) => {
      if (res.writableEnded) {
        proxyRes.destroy();
        return;
      }
      if ([301, 302, 303, 307, 308].includes(proxyRes.statusCode) && proxyRes.headers.location && redirectsLeft > 0) {
        proxyRes.resume();
        const nextUrl = new URL(proxyRes.headers.location, targetUrl).toString();
        return handleAudioProxy(req, res, nextUrl, redirectsLeft - 1, initialUrl);
      }
      if (proxyRes.statusCode >= 400) {
        evictStreamCache(initialUrl);
        evictStreamCache(targetUrl);
        proxyRes.resume();
        res.writeHead(502, { 'Access-Control-Allow-Origin': '*' });
        return res.end('Upstream error');
      }
      const outHeaders = Object.assign({}, proxyRes.headers);
      delete outHeaders['access-control-allow-origin'];
      delete outHeaders['Access-Control-Allow-Origin'];
      delete outHeaders['access-control-allow-credentials'];
      delete outHeaders['x-frame-options'];
      outHeaders['Access-Control-Allow-Origin'] = '*';
      outHeaders['Access-Control-Allow-Headers'] = '*';
      outHeaders['Access-Control-Expose-Headers'] = 'Content-Length, Content-Range, Accept-Ranges';

      res.writeHead(proxyRes.statusCode || 200, outHeaders);
      proxyRes.on('aborted', () => {
        evictStreamCache(initialUrl);
        evictStreamCache(targetUrl);
      });
      proxyRes.on('error', () => {
        evictStreamCache(initialUrl);
        evictStreamCache(targetUrl);
        try { res.destroy(); } catch (e) {}
      });
      proxyRes.pipe(res);
    });

    proxyReq.setTimeout(30000, () => {
      evictStreamCache(initialUrl);
      proxyReq.destroy(new Error('Proxy timeout'));
    });

    proxyReq.on('error', () => {
      evictStreamCache(initialUrl);
      evictStreamCache(targetUrl);
      if (!res.headersSent) {
        res.writeHead(502, { 'Access-Control-Allow-Origin': '*' });
        res.end('Proxy error');
      } else {
        try { res.destroy(); } catch (err) {}
      }
    });

    req.on('error', () => { try { proxyReq.destroy(); } catch (e) {} });
    req.pipe(proxyReq);
  } catch (e) {
    evictStreamCache(initialUrl);
    if (!res.headersSent) {
      res.writeHead(500, { 'Access-Control-Allow-Origin': '*' });
      res.end('Proxy failed');
    }
  }
}

function defaultDownloadsDir() {
  return defaultDownloadDir();
}

function getDownloadsDir() {
  const dir = resolveDownloadDir() || defaultDownloadsDir();
  if (!fs.existsSync(dir)) {
    try { fs.mkdirSync(dir, { recursive: true }); } catch (e) {}
  }
  return dir;
}

function sanitizeFilename(str) {
  return (str || 'track')
  .replace(/[<>:"/\\|?*]/g, '')
  .replace(/\s+/g, ' ')
  .trim();
}

const AUDIO_EXTENSIONS = new Set(['.mp3', '.m4a', '.flac', '.opus', '.wav', '.ogg', '.oga', '.aac', '.webm', '.weba']);

let downloadsIndex = null;
let downloadsIndexBuiltAt = 0;
const DOWNLOADS_INDEX_TTL = 15000;

async function buildDownloadsIndex() {
  const dir = getDownloadsDir();
  const map = new Map();
  try {
    const entries = await fs.promises.readdir(dir, { withFileTypes: true });
    for (const entry of entries) {
      if (!entry.isFile()) continue;
      const ext = path.extname(entry.name).toLowerCase();
      if (!AUDIO_EXTENSIONS.has(ext)) continue;
      const base = entry.name.slice(0, entry.name.length - ext.length).toLowerCase();
      if (base) map.set(base, entry.name);
    }
  } catch (e) {}
  downloadsIndex = map;
  downloadsIndexBuiltAt = Date.now();
  return map;
}

async function findDownloadedFile(title, artist) {
  if (!downloadsIndex || Date.now() - downloadsIndexBuiltAt > DOWNLOADS_INDEX_TTL) {
    await buildDownloadsIndex();
  }
  return findDownloadedFileName(downloadsIndex, title, artist);
}

function findDownloadedFileName(index, title, artist) {
  if (!title && !artist) return null;
  const key = `${sanitizeFilename(artist || 'unknown artist')} - ${sanitizeFilename(title || 'untitled')}`.toLowerCase();
  return index.get(key) || null;
}

async function cleanupDownloadArtifacts(dir, beforeSet, baseName, videoId, chosenExt, keepSuccessFile) {
  try {
    const afterFiles = await fs.promises.readdir(dir);
    const trashExts = new Set(['.webp', '.webm', '.part', '.ytdl', '.temp', '.jpg', '.jpeg', '.png', '.json', '.vtt', '.m4a', '.opus', '.mp4']);
    const chosenExtWithDot = '.' + chosenExt.toLowerCase();

    for (const f of afterFiles) {
      if (beforeSet.has(f)) continue;

      const fileExt = path.extname(f).toLowerCase();
      const belongs = f.startsWith(baseName) || (videoId && f.includes(videoId));
      if (!belongs) continue;

      if (keepSuccessFile && f === `${baseName}${chosenExtWithDot}`) {
        continue;
      }

      if (trashExts.has(fileExt) || !keepSuccessFile) {
        if (fileExt === chosenExtWithDot && keepSuccessFile) {
          continue;
        }
        try {
          await fs.promises.unlink(path.join(dir, f));
        } catch (_) {}
      }
    }
  } catch (_) {}
}

async function downloadCoverTemp(thumbUrl, tmpDir) {
  if (!thumbUrl || typeof thumbUrl !== 'string') return null;
  const targetUrl = upgradeArtworkUrl(thumbUrl) || thumbUrl;
  if (!targetUrl) return null;
  const coverTmp = path.join(tmpDir, 'cover_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8) + '.jpg');
  try {
    if (/^https?:\/\//i.test(targetUrl)) {
      const resp = await fetch(targetUrl, { signal: AbortSignal.timeout(10000) });
      if (!resp.ok) return null;
      const buf = Buffer.from(await resp.arrayBuffer());
      if (!buf || buf.length < 32) return null;
      await fs.promises.writeFile(coverTmp, buf);
      return coverTmp;
    } else if (fs.existsSync(targetUrl)) {
      await fs.promises.copyFile(targetUrl, coverTmp);
      return coverTmp;
    }
  } catch (err) {
    try { if (fs.existsSync(coverTmp)) await fs.promises.unlink(coverTmp); } catch (_) {}
  }
  return null;
}

async function handleDownload(req, res, trackUrl, title, artist, format = 'mp3', quality = '0', startParam = null, endParam = null, raw = false, trackId = null, streamUrlParam = null) {
  trackUrl = normalizeSoundCloudUrl(trackUrl);
  if (!trackUrl && (!title || !artist)) {
    res.writeHead(400, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'missing track information' }));
  }

  let start = null;
  if (startParam !== null && startParam !== undefined && startParam !== '') {
    const n = typeof startParam === 'number' ? startParam : parseFloat(startParam);
    if (!isNaN(n)) start = Math.max(0, n);
  }
  let end = null;
  if (endParam !== null && endParam !== undefined && endParam !== '') {
    const n = typeof endParam === 'number' ? endParam : parseFloat(endParam);
    if (!isNaN(n)) end = Math.max(0, n);
  }

  const trimArgs = [];
  if (start !== null || end !== null) {
    const s = start !== null ? start : 0;
    trimArgs.push('--postprocessor-args', 'ffmpeg:-ss ' + s + (end ? ' -to ' + end : ''));
  }

  const isRaw = Boolean(raw);
  const parsedUrl = (req && req.url) ? url.parse(req.url, true) : { query: {} };
  let downloadsDir = getDownloadsDir();
  const customDir = parsedUrl.query && parsedUrl.query.customDir;
  if (customDir) {
    try {
      if (!fs.existsSync(customDir)) {
        fs.mkdirSync(customDir, { recursive: true });
      }
      if (fs.existsSync(customDir)) {
        downloadsDir = customDir;
      }
    } catch (_) {}
  }
  const tmpDir = path.join(downloadsDir, '.tmp');
  if (!fs.existsSync(tmpDir)) {
    try { await fs.promises.mkdir(tmpDir, { recursive: true }); } catch (_) {}
  }

  const thumbUrl = parsedUrl.query && parsedUrl.query.thumbnail;
  let coverTmp = null;
  if (thumbUrl && !isRaw) {
    try {
      coverTmp = await downloadCoverTemp(thumbUrl, tmpDir);
    } catch (_) {}
  }

  const cleanTitle = sanitizeFilename(title || 'untitled');
  const cleanArtist = sanitizeFilename(artist || 'unknown artist');
  const validFormats = ['mp3', 'm4a', 'flac', 'opus', 'wav', 'ogg'];
  const safeFormat = String(format || '').toLowerCase();
  const ext = isRaw ? 'wav' : (validFormats.includes(safeFormat) ? safeFormat : 'mp3');
  const safeQuality = /^\d+$/.test(String(quality)) ? String(quality) : '0';
  const tempName = isRaw ? ('raw_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8) + '.wav') : null;
  const outFilename = isRaw ? tempName : `${cleanArtist} - ${cleanTitle}.${ext}`;
  const outPath = isRaw ? path.join(tmpDir, tempName) : path.join(downloadsDir, outFilename);
  const tempTemplate = isRaw ? path.join(tmpDir, tempName.replace(/\.wav$/, '.%(ext)s')) : path.join(downloadsDir, `${cleanArtist} - ${cleanTitle}.%(ext)s`);

  let targetUrl = trackUrl;
  if (!isSafeMediaUrl(targetUrl) || targetUrl.includes('deezer.com')) {
    targetUrl = `ytsearch1:${cleanArtist} ${cleanTitle} official audio`;
  }

  let cleanId = cleanTrackId(trackId);
  if (!cleanId && targetUrl) {
    const vidMatch = String(targetUrl).match(/(?:v=|\/embed\/|\/watch\/|youtu\.be\/|\/v\/)([a-zA-Z0-9_-]{11})/);
    if (vidMatch) cleanId = cleanTrackId(vidMatch[1]);
  }

  const downloadKey = !isRaw ? JSON.stringify([cleanId || targetUrl, cleanArtist, cleanTitle, ext, safeQuality, start, end]) : null;
  if (downloadKey && downloadInFlight.has(downloadKey)) {
    const outcome = await downloadInFlight.get(downloadKey);
    res.writeHead(outcome.status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(outcome.body));
  }

  let resolveDownload;
  const downloadPromise = downloadKey ? new Promise(resolve => { resolveDownload = resolve; }) : null;
  if (downloadKey) downloadInFlight.set(downloadKey, downloadPromise);
  let downloadOutcome = null;
  const sendDownloadResponse = (status, body) => {
    downloadOutcome = { status, body };
    if (resolveDownload) resolveDownload(downloadOutcome);
    if (!res.writableEnded) {
      res.writeHead(status, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(body));
    }
  };

  try {
  const cachedFile = cleanId ? await findCachedFile(cleanId) : null;
  if (cachedFile) {
    const ffmpegBin = resolveBinary('ffmpeg');
    if (ffmpegBin) {
      const bitrateMap = { '0': '320k', '2': '256k', '4': '192k', '5': '128k' };
      const bitrate = bitrateMap[safeQuality] || '320k';
      let ffmpegArgs;
      if (coverTmp && ext === 'mp3') {
        ffmpegArgs = ['-y'];
        if (start !== null) ffmpegArgs.push('-ss', String(start));
        if (end !== null) ffmpegArgs.push('-to', String(end));
        ffmpegArgs.push('-i', cachedFile, '-i', coverTmp, '-map', '0:a:0', '-map', '1:v:0', '-c:v', 'mjpeg', '-id3v2_version', '3', '-metadata:s:v', 'title=Album cover', '-metadata:s:v', 'comment=Cover (front)', '-b:a', bitrate, '-metadata', 'title=' + (title || cleanTitle), '-metadata', 'artist=' + (artist || cleanArtist), outPath);
      } else {
        ffmpegArgs = ['-y'];
        if (start !== null) ffmpegArgs.push('-ss', String(start));
        if (end !== null) ffmpegArgs.push('-to', String(end));
        ffmpegArgs.push('-i', cachedFile);
        if (coverTmp && ['m4a', 'flac'].includes(ext)) {
          ffmpegArgs.push('-i', coverTmp, '-map', '0:a:0', '-map', '1:v:0', '-c:v', 'mjpeg', '-disposition:v:0', 'attached_pic');
        }
        if (['mp3', 'm4a', 'opus', 'ogg'].includes(ext)) {
          ffmpegArgs.push('-b:a', bitrate);
        }
        ffmpegArgs.push('-metadata', `title=${title || cleanTitle}`);
        ffmpegArgs.push('-metadata', `artist=${artist || cleanArtist}`);
        ffmpegArgs.push(outPath);
      }

      let ffmpegProc = null;
      res.on('close', () => {
        if (coverTmp) {
          try { fs.unlinkSync(coverTmp); } catch (_) {}
        }
        if (!res.writableEnded && ffmpegProc) {
          try { ffmpegProc.kill('SIGKILL'); } catch (_) {}
          try { fs.unlinkSync(outPath); } catch (_) {}
        }
      });

      try {
        await new Promise((resolve, reject) => {
          ffmpegProc = spawn(ffmpegBin, ffmpegArgs, { windowsHide: true });
          ffmpegProc.on('close', code => {
            if (code === 0) resolve();
            else reject(new Error(`ffmpeg exited with code ${code}`));
          });
          ffmpegProc.on('error', reject);
        });
        downloadsIndex = null;
        return sendDownloadResponse(200, isRaw ? {
          success: true,
          tempName: tempName
        } : {
          success: true,
          filename: outFilename,
          directory: downloadsDir
        });
      } catch (cachedErr) {
        try { await fs.promises.unlink(outPath); } catch (_) {}
      }
    }
  }

  const targetDir = isRaw ? tmpDir : downloadsDir;
  const beforeFiles = new Set(await fs.promises.readdir(targetDir).catch(() => []));
  let child = null;

  async function cleanupPartial() {
    try {
      const files = await fs.promises.readdir(targetDir);
      const prefix = isRaw ? tempName.replace(/\.wav$/, '') : `${cleanArtist} - ${cleanTitle}.`;
      for (const f of files) {
        if (beforeFiles.has(f)) continue;
        if (!f.startsWith(prefix)) continue;
        const lower = f.toLowerCase();
        if (
          lower.endsWith('.part') ||
          lower.endsWith('.ytdl') ||
          lower.endsWith('.temp') ||
          f === outFilename ||
          !res.writableEnded
        ) {
          try { await fs.promises.unlink(path.join(targetDir, f)); } catch (_) {}
        }
      }
    } catch (_) {}
  }

  res.on('close', () => {
    if (coverTmp) {
      try { fs.unlinkSync(coverTmp); } catch (_) {}
    }
    if (!res.writableEnded && child) {
      try { child.kill('SIGKILL'); } catch (e) {}
      cleanupPartial();
    }
  });

  const baseName = isRaw ? tempName.replace(/\.wav$/, '') : `${cleanArtist} - ${cleanTitle}`;
  const videoId = cleanId;

  const coverFormats = ['mp3', 'm4a', 'flac', 'opus', 'ogg'];
  const canCarryCover = !isRaw && coverFormats.includes(ext);

  const args = [
    '--no-playlist',
    '--no-check-certificates',
    '--no-warnings',
    '-x',
    '--audio-format', ext,
    '--audio-quality', safeQuality,
    '--embed-metadata',
    '-o', tempTemplate,
    '--extractor-args', YT_EXTRACTOR_ARGS,
  ];
  if (canCarryCover) {
    args.push('--embed-thumbnail', '--convert-thumbnails', 'jpg');
  }
  if (trimArgs.length) {
    args.push(...trimArgs);
  }
  args.push('--', targetUrl);

  async function finalizeDownloadSuccess() {
    if (!isRaw) {
      await cleanupDownloadArtifacts(downloadsDir, beforeFiles, baseName, videoId, ext, true);
      downloadsIndex = null;
      if (coverTmp && ['mp3', 'm4a', 'flac'].includes(ext) && fs.existsSync(outPath)) {
        try {
          const embedTmp = outPath + '.cover.' + ext;
          const bitrateMap = { '0': '320k', '2': '256k', '4': '192k', '5': '128k' };
          const bitrate = bitrateMap[safeQuality] || '320k';
          const ffmpegBin = resolveBinary('ffmpeg');
          if (ffmpegBin) {
            let embedArgs;
            if (ext === 'mp3') {
              embedArgs = ['-y', '-i', outPath, '-i', coverTmp, '-map', '0:a:0', '-map', '1:v:0', '-c:v', 'mjpeg', '-id3v2_version', '3', '-metadata:s:v', 'title=Album cover', '-metadata:s:v', 'comment=Cover (front)', '-b:a', bitrate, '-metadata', 'title=' + (title || cleanTitle), '-metadata', 'artist=' + (artist || cleanArtist), embedTmp];
            } else {
              embedArgs = ['-y', '-i', outPath, '-i', coverTmp, '-map', '0:a:0', '-map', '1:v:0', '-c:v', 'mjpeg', '-disposition:v:0', 'attached_pic'];
              if (['m4a', 'opus', 'ogg'].includes(ext)) embedArgs.push('-b:a', bitrate);
              embedArgs.push('-metadata', 'title=' + (title || cleanTitle), '-metadata', 'artist=' + (artist || cleanArtist), embedTmp);
            }
            await new Promise((resolve, reject) => {
              const proc = spawn(ffmpegBin, embedArgs, { windowsHide: true });
              proc.on('close', code => (code === 0 ? resolve() : reject(new Error('ffmpeg failed ' + code))));
              proc.on('error', reject);
            });
            await fs.promises.rename(embedTmp, outPath);
          }
        } catch (_) {
          try { if (fs.existsSync(outPath + '.cover.' + ext)) await fs.promises.unlink(outPath + '.cover.' + ext); } catch (_) {}
        }
      }
    }
    return sendDownloadResponse(200, isRaw ? {
      success: true,
      tempName: tempName
    } : {
      success: true,
      filename: outFilename,
      directory: downloadsDir
    });
  }

  try {
    await executeYtDlp(args, 60000, { onChild: c => { child = c; } });
    return finalizeDownloadSuccess();
  } catch (err) {
    try { logger.error('Download primary failed: ' + (title || trackUrl), err); } catch (_) {}
    if (res.writableEnded) {
      sendDownloadResponse(500, { success: false, error: err.message });
      return;
    }

    const errMsg = (err && (err.message || String(err))) || '';
    const isRetryable = /403|not a bot|sign in to confirm|drm protected/i.test(errMsg);

    if (isRetryable && (title || artist)) {
      const q = (artist && title) ? `${artist} - ${title}` : (title || artist);
      const isSc = /soundcloud\.com|scsearch/i.test(targetUrl);
      const fallbackTarget = isSc ? `ytsearch1:${q} audio` : `scsearch1:${q}`;
      try {
        const retryArgs = [
          '--no-playlist',
          '--no-check-certificates',
          '--no-warnings',
          '-x',
          '--audio-format', ext,
          '--audio-quality', safeQuality,
          '--embed-metadata',
          '-o', tempTemplate,
          '--extractor-args', YT_EXTRACTOR_ARGS,
        ];
        if (canCarryCover) {
          retryArgs.push('--embed-thumbnail', '--convert-thumbnails', 'jpg');
        }
        if (trimArgs.length) {
          retryArgs.push(...trimArgs);
        }
        retryArgs.push('--', fallbackTarget);

        await executeYtDlp(retryArgs, 60000, { onChild: c => { child = c; } });
        return finalizeDownloadSuccess();
      } catch (retryErr) {
        try { logger.error('Download fallback retry failed: ' + fallbackTarget, retryErr); } catch (_) {}
      }
    }

    if (res.writableEnded) {
      sendDownloadResponse(500, { success: false, error: err.message });
      return;
    }

    try {
      const fallbackArgs = [
        '--no-playlist',
        '--no-check-certificates',
        '--no-warnings',
        '-x',
        '--audio-format', ext,
        '--audio-quality', safeQuality,
        '--embed-metadata',
        '-o', tempTemplate,
        '--extractor-args', YT_EXTRACTOR_ARGS,
      ];
      if (canCarryCover) {
        fallbackArgs.push('--embed-thumbnail', '--convert-thumbnails', 'jpg');
      }
      if (trimArgs.length) {
        fallbackArgs.push(...trimArgs);
      }
      fallbackArgs.push('--', targetUrl);

      await executeYtDlp(fallbackArgs, 60000, { onChild: c => { child = c; } });
      return finalizeDownloadSuccess();
    } catch (e2) {
      if (!isRaw) {
        await cleanupDownloadArtifacts(downloadsDir, beforeFiles, baseName, videoId, ext, false);
      } else {
        await cleanupPartial();
      }
      try { logger.error('Download failure: ' + (title || trackUrl) + ' (' + (e2.message || err.message) + ')', e2); } catch (_) {}
      sendDownloadResponse(500, { success: false, error: e2.message || err.message });
    }
  }
  } finally {
    if (coverTmp) {
      try { await fs.promises.unlink(coverTmp); } catch (_) {}
    }
    if (downloadKey && downloadInFlight.get(downloadKey) === downloadPromise) {
      downloadInFlight.delete(downloadKey);
    }
    if (resolveDownload && !downloadOutcome) {
      resolveDownload({ status: 500, body: { success: false, error: 'Download ended without a result' } });
    }
  }
}

function handleTmpFile(req, res, parsedUrl) {
  const fileName = parsedUrl.query.name || '';
  if (!/^[\w .-]+\.wav$/.test(fileName) || fileName.includes('/') || fileName.includes('\\') || fileName.includes('..')) {
    res.writeHead(400, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'Invalid file name' }));
  }
  const downloadsDir = getDownloadsDir();
  const filePath = path.join(downloadsDir, '.tmp', fileName);
  if (!fs.existsSync(filePath)) {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'File not found' }));
  }
  const stat = fs.statSync(filePath);
  res.writeHead(200, {
    'Content-Type': 'audio/wav',
    'Content-Length': stat.size
  });
  const stream = fs.createReadStream(filePath);
  stream.pipe(res);
  req.on('close', () => {
    if (!res.writableEnded) stream.destroy();
  });
}

async function handleEncode(req, res, parsedUrl) {
  const sourceName = parsedUrl.query.name || '';
  const title = parsedUrl.query.title || 'untitled';
  const artist = parsedUrl.query.artist || 'unknown artist';
  const format = parsedUrl.query.format || 'mp3';
  const quality = parsedUrl.query.quality || '0';

  const validFormats = ['mp3', 'm4a', 'flac', 'opus', 'wav', 'ogg'];
  const safeFormat = String(format || '').toLowerCase();
  const ext = validFormats.includes(safeFormat) ? safeFormat : 'mp3';
  const bitrateMap = {
    '0': '320k',
    '2': '256k',
    '4': '192k',
    '5': '128k',
  };
  const bitrate = bitrateMap[String(quality)] || '320k';

  let downloadsDir = getDownloadsDir();
  const customDir = parsedUrl.query && parsedUrl.query.customDir;
  if (customDir) {
    try {
      if (!fs.existsSync(customDir)) {
        fs.mkdirSync(customDir, { recursive: true });
      }
      if (fs.existsSync(customDir)) {
        downloadsDir = customDir;
      }
    } catch (_) {}
  }
  const tmpDir = path.join(downloadsDir, '.tmp');
  if (!fs.existsSync(tmpDir)) {
    try { await fs.promises.mkdir(tmpDir, { recursive: true }); } catch (_) {}
  }
  const thumbUrl = parsedUrl.query && parsedUrl.query.thumbnail;
  let coverTmp = null;
  if (thumbUrl) {
    try {
      coverTmp = await downloadCoverTemp(thumbUrl, tmpDir);
    } catch (_) {}
  }
  const cleanTitle = sanitizeFilename(title);
  const cleanArtist = sanitizeFilename(artist);
  const outFilename = `${cleanArtist} - ${cleanTitle}.${ext}`;
  const outPath = path.join(downloadsDir, outFilename);

  const renderTempName = 'render_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8) + '.wav';
  const renderTempPath = path.join(tmpDir, renderTempName);

  const MAX_BODY_SIZE = 500 * 1024 * 1024;
  let receivedBytes = 0;
  let aborted = false;

  const fileWriteStream = fs.createWriteStream(renderTempPath);

  try {
    await new Promise((resolve, reject) => {
      req.on('data', chunk => {
        receivedBytes += chunk.length;
        if (receivedBytes > MAX_BODY_SIZE) {
          aborted = true;
          try { req.destroy(new Error('Payload too large')); } catch (_) {}
          try { fileWriteStream.destroy(); } catch (_) {}
          try { fs.unlinkSync(renderTempPath); } catch (_) {}
          if (!res.writableEnded) {
            res.writeHead(413, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ success: false, error: 'WAV exceeds 300MB limit' }));
          }
          reject(new Error('Payload too large'));
          return;
        }
        fileWriteStream.write(chunk);
      });
      req.on('end', () => {
        fileWriteStream.end(() => resolve());
      });
      req.on('error', err => {
        try { fs.unlinkSync(renderTempPath); } catch (_) {}
        reject(err);
      });
      fileWriteStream.on('error', err => {
        try { fs.unlinkSync(renderTempPath); } catch (_) {}
        reject(err);
      });
    });
  } catch (readErr) {
    if (aborted) return;
    try { fs.unlinkSync(renderTempPath); } catch (_) {}
    if (!res.writableEnded) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: readErr.message || 'Failed to read audio body' }));
    }
    return;
  }

  const ffmpegBin = resolveBinary('ffmpeg');
  if (!ffmpegBin) {
    try { await fs.promises.unlink(renderTempPath); } catch (_) {}
    if (!res.writableEnded) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'ffmpeg not found' }));
    }
    return;
  }

  let ffmpegProc = null;
  res.on('close', () => {
    if (coverTmp) {
      try { fs.unlinkSync(coverTmp); } catch (_) {}
    }
    if (!res.writableEnded && ffmpegProc) {
      try { ffmpegProc.kill('SIGKILL'); } catch (_) {}
      try { fs.unlinkSync(renderTempPath); } catch (_) {}
      try { fs.unlinkSync(outPath); } catch (_) {}
    }
  });

  let ffmpegArgs;
  if (coverTmp && ext === 'mp3') {
    ffmpegArgs = ['-y', '-i', renderTempPath, '-i', coverTmp, '-map', '0:a:0', '-map', '1:v:0', '-c:v', 'mjpeg', '-id3v2_version', '3', '-metadata:s:v', 'title=Album cover', '-metadata:s:v', 'comment=Cover (front)', '-b:a', bitrate, '-metadata', 'title=' + title, '-metadata', 'artist=' + artist, outPath];
  } else {
    ffmpegArgs = ['-y', '-i', renderTempPath];
    if (coverTmp && ['m4a', 'flac'].includes(ext)) {
      ffmpegArgs.push('-i', coverTmp, '-map', '0:a:0', '-map', '1:v:0', '-c:v', 'mjpeg', '-disposition:v:0', 'attached_pic');
    }
    if (['mp3', 'm4a', 'opus', 'ogg'].includes(ext)) {
      ffmpegArgs.push('-b:a', bitrate);
    }
    ffmpegArgs.push('-metadata', `title=${title}`);
    ffmpegArgs.push('-metadata', `artist=${artist}`);
    ffmpegArgs.push(outPath);
  }

  try {
    await new Promise((resolve, reject) => {
      ffmpegProc = spawn(ffmpegBin, ffmpegArgs, { windowsHide: true });
      ffmpegProc.on('close', code => {
        if (code === 0) resolve();
        else reject(new Error(`ffmpeg exited with code ${code}`));
      });
      ffmpegProc.on('error', reject);
    });

    downloadsIndex = null;
    if (!res.writableEnded) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: true,
        filename: outFilename,
        directory: downloadsDir
      }));
    }
  } catch (err) {
    try { logger.error('Encode failure: ' + (title || sourceName) + ' (' + err.message + ')', err); } catch (_) {}
    try { await fs.promises.unlink(outPath); } catch (_) {}
    if (!res.writableEnded) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: err.message || 'Encoding failed' }));
    }
  } finally {
    if (coverTmp) {
      try { await fs.promises.unlink(coverTmp); } catch (_) {}
    }
    try { await fs.promises.unlink(renderTempPath); } catch (_) {}
    if (sourceName && /^[\w .-]+\.wav$/.test(sourceName)) {
      try { await fs.promises.unlink(path.join(tmpDir, sourceName)); } catch (_) {}
    }
  }
}

function sanitizeForLyrics(title, artist) {
  let cleanTitle = (title || '')
  .replace(/\[.*?\]|\(.*?\)/g, '')
  .replace(/official\s*(music)?\s*video|official\s*audio|lyrics|hd|4k|remastered|visualizer|clip\s*officiel|video\s*clip/gi, '')
  .replace(/ft\.?|feat\.?/gi, '')
  .replace(/[^\w\s\u0400-\u04FF]/gi, ' ')
  .replace(/\s+/g, ' ')
  .trim();

  let cleanArtist = (artist || '')
  .replace(/\[.*?\]|\(.*?\)/g, '')
  .replace(/- topic|vevo|records|official/gi, '')
  .replace(/[^\w\s\u0400-\u04FF]/gi, ' ')
  .replace(/\s+/g, ' ')
  .trim();

  return { cleanTitle, cleanArtist };
}

function logNetworkFailure(targetUrl) {
  try {
    const u = new URL(targetUrl);
    logger.log('warn', `${u.host}${u.pathname}`);
  } catch (_) {}
}

function decodeUtf8Chunks(chunks) {
  return Buffer.concat(chunks).toString('utf8');
}

function hasBrokenLyricText(text) {
  return typeof text === 'string' && text.includes('\uFFFD');
}

function fetchLyricsDirect(title, artist) {
  return new Promise((resolve) => {
    const { cleanTitle, cleanArtist } = sanitizeForLyrics(title, artist);
    const apiUrl = `https://lrclib.net/api/get?track_name=${encodeURIComponent(cleanTitle)}&artist_name=${encodeURIComponent(cleanArtist)}`;

    const req1 = https.get(apiUrl, { headers: { 'User-Agent': 'devsize/2.0' }, timeout: 8000 }, (res) => {
      let data = '';
      const dec = new StringDecoder('utf8');
      res.on('data', c => data += dec.write(c));
      res.on('end', () => {
        data += dec.end();
        try {
          if (res.statusCode === 200) {
            const parsed = JSON.parse(data);
            if (parsed.instrumental) {
              return resolve({ instrumental: true, syncedLyrics: '', plainLyrics: '' });
            }
            if (parsed.syncedLyrics || parsed.plainLyrics) {
              return resolve(parsed.syncedLyrics || parsed.plainLyrics);
            }
          }
        } catch (e) {}

        const query = `${cleanArtist} ${cleanTitle}`.trim();
        const searchUrl = `https://lrclib.net/api/search?q=${encodeURIComponent(query)}`;
        const req2 = https.get(searchUrl, { headers: { 'User-Agent': 'devsize/2.0' }, timeout: 8000 }, (res2) => {
          const chunks = [];
          res2.on('data', chunk => chunks.push(chunk));
          res2.on('end', () => {
            const data2 = decodeUtf8Chunks(chunks);
            try {
              const list = JSON.parse(data2);
              if (Array.isArray(list) && list.length > 0) {
                if (list[0].instrumental) {
                  return resolve({ instrumental: true, syncedLyrics: '', plainLyrics: '' });
                }
                const withSynced = list.find(item => item.syncedLyrics);
                if (withSynced) return resolve(withSynced.syncedLyrics);
                return resolve(list[0].syncedLyrics || list[0].plainLyrics || null);
              }
            } catch (e) {}
            resolve(null);
          });
        });
        req2.on('timeout', () => {
          req2.destroy();
          logNetworkFailure(searchUrl);
          resolve(null);
        });
        req2.on('error', () => {
          logNetworkFailure(searchUrl);
          resolve(null);
        });
      });
    });
    req1.on('timeout', () => {
      req1.destroy();
      logNetworkFailure(apiUrl);
      resolve(null);
    });
    req1.on('error', () => {
      logNetworkFailure(apiUrl);
      resolve(null);
    });
  });
}

function extractGeniusLyrics(html) {
  let lyrics = '';
  const marker = 'data-lyrics-container="true"';
  let pos = 0;
  while (true) {
    const idx = html.indexOf(marker, pos);
    if (idx === -1) break;
    const contentStart = html.indexOf('>', idx) + 1;
    let depth = 1;
    let cur = contentStart;
    while (depth > 0 && cur < html.length) {
      const nextOpen = html.indexOf('<div', cur);
      const nextClose = html.indexOf('</div>', cur);
      if (nextClose === -1) break;
      if (nextOpen !== -1 && nextOpen < nextClose) {
        depth++;
        cur = nextOpen + 4;
      } else {
        depth--;
        if (depth === 0) {
          const block = html.substring(contentStart, nextClose);
          lyrics += (lyrics ? '\n' : '') + block;
          pos = nextClose + 6;
          break;
        }
        cur = nextClose + 6;
      }
    }
    if (depth > 0) break;
  }
  let text = lyrics
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#039;/g, "'")
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&nbsp;/g, ' ')
    .replace(/&#(\d+);/g, (m, dec) => String.fromCharCode(parseInt(dec, 10)))
    .replace(/&#x([0-9a-f]+);/gi, (m, hex) => String.fromCharCode(parseInt(hex, 16)));

  text = text.replace(/\[(?!(?:instrumental|music|intro|outro|solo)\b)[^\]]*\]/gi, '').replace(/\n{3,}/g, '\n\n');

  const lines = text.split('\n')
    .map(l => l.trim())
    .filter(l => {
      if (/^\d*\s*Contributors/i.test(l)) return false;
      if (/Lyrics$/i.test(l) && !l.includes(' ') && l.length < 30) return false;
      if (/^\[?.*Lyrics\]?$/i.test(l) && l.toLowerCase().includes('lyrics') && !l.includes('[')) return false;
      return true;
    });
  return lines.join('\n').trim();
}

function fetchGeniusLyrics(title, artist) {
  return new Promise((resolve) => {
    const query = (title + ' ' + artist)
      .replace(/\s*\(feat\.[^)]*\)/gi, '')
      .replace(/\s*\[feat\.[^\]]*\]/gi, '')
      .replace(/\s*\(featuring[^)]*\)/gi, '')
      .replace(/\s*feat\.[^\s]+/gi, '')
      .replace(/\s*-\s*remaster(ed)?(\s*\d+)?/gi, '')
      .replace(/\s*\(live[^)]*\)/gi, '')
      .replace(/\s*\[live[^\]]*\]/gi, '')
      .replace(/\s+/g, ' ')
      .trim();

    const searchUrl = `https://genius.com/api/search/song?q=${encodeURIComponent(query)}`;
    const req = https.get(searchUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' },
      timeout: 8000
    }, (res) => {
      const chunks = [];
      res.on('data', chunk => chunks.push(chunk));
      res.on('end', () => {
        const data = decodeUtf8Chunks(chunks);
        if (res.statusCode !== 200) {
          logNetworkFailure(searchUrl);
          return resolve(null);
        }
        let hits = [];
        try {
          const json = JSON.parse(data);
          hits = (json.response && json.response.sections && json.response.sections[0] && json.response.sections[0].hits) || [];
        } catch (_) {}
        if (hits.length === 0) return resolve(null);

        const normSimple = s => (s || '').toLowerCase().replace(/[^a-z0-9]/gi, '');
        const nTitle = normSimple(title);
        const nArtist = normSimple(artist);
        let bestHit = hits[0];
        let bestScore = -1;
        for (const h of hits) {
          if (!h.result || !h.result.url) continue;
          const hTitle = normSimple(h.result.title);
          const hArtist = normSimple(h.result.artist_names);
          let score = 0;
          if (hTitle && nTitle && (hTitle === nTitle || hTitle.includes(nTitle) || nTitle.includes(hTitle))) score += 2;
          if (hArtist && nArtist && (hArtist === nArtist || hArtist.includes(nArtist) || nArtist.includes(hArtist))) score += 1;
          if (score > bestScore) {
            bestScore = score;
            bestHit = h;
          }
        }

        const songUrl = bestHit && bestHit.result && bestHit.result.url;
        if (!songUrl) return resolve(null);

        const pageReq = https.get(songUrl, {
          headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' },
          timeout: 8000
        }, (pageRes) => {
          const chunks = [];
          pageRes.on('data', chunk => chunks.push(chunk));
          pageRes.on('end', () => {
            const html = decodeUtf8Chunks(chunks);
            if (pageRes.statusCode !== 200) {
              logNetworkFailure(songUrl);
              return resolve(null);
            }
            try {
              const lyrics = extractGeniusLyrics(html);
              if (lyrics && lyrics.length > 0) return resolve(lyrics);
            } catch (_) {}
            resolve(null);
          });
        });
        pageReq.on('timeout', () => {
          pageReq.destroy();
          logNetworkFailure(songUrl);
          resolve(null);
        });
        pageReq.on('error', () => {
          logNetworkFailure(songUrl);
          resolve(null);
        });
      });
    });
    req.on('timeout', () => {
      req.destroy();
      logNetworkFailure(searchUrl);
      resolve(null);
    });
    req.on('error', () => {
      logNetworkFailure(searchUrl);
      resolve(null);
    });
  });
}

function parseYrcWords(rest) {
  const wordRegex = /\((\d+),(\d+)(?:,\d+)?\)([^\(]*)/g;
  const words = [];
  let curWord = null;
  let wMatch;
  while ((wMatch = wordRegex.exec(rest)) !== null) {
    const wStartMs = parseInt(wMatch[1], 10);
    const wDurMs = parseInt(wMatch[2], 10);
    const rawText = wMatch[3];
    const startSec = wStartMs / 1000;
    const endSec = (wStartMs + wDurMs) / 1000;
    const isCjk = /[\u4e00-\u9fa5\u3040-\u30ff]/.test(rawText);

    if (curWord && (isCjk || /[\u4e00-\u9fa5\u3040-\u30ff]/.test(curWord.text))) {
      curWord.text = curWord.text.trim();
      if (curWord.text) words.push(curWord);
      curWord = null;
    }

    if (!curWord) {
      curWord = { text: rawText, start: startSec, end: endSec };
    } else {
      curWord.text += rawText;
      curWord.end = Math.max(curWord.end, endSec);
    }

    if (/\s/.test(rawText) || isCjk) {
      curWord.text = curWord.text.trim();
      if (curWord.text) words.push(curWord);
      curWord = null;
    }
  }
  if (curWord) {
    curWord.text = curWord.text.trim();
    if (curWord.text) words.push(curWord);
  }
  return words;
}

function parseYrc(yrcText) {
  const lines = [];
  const lineRegex = /^\[(\d+),(\d+)\](.*)$/;

  for (const rawLine of (yrcText || '').split('\n')) {
    const trimmed = rawLine.trim();
    if (!trimmed) continue;
    const match = lineRegex.exec(trimmed);
    if (!match) continue;

    const lineStartMs = parseInt(match[1], 10);
    const lineDurMs = parseInt(match[2], 10);
    const lineStart = lineStartMs / 1000;
    const lineEnd = (lineStartMs + lineDurMs) / 1000;
    const rest = match[3];

    const words = parseYrcWords(rest);
    if (words.length > 0) {
      const fullText = words.map(w => w.text).join(' ').trim();
      if (fullText && !fullText.startsWith('作词') && !fullText.startsWith('作曲') && !fullText.startsWith('制作人')) {
        lines.push({
          start: lineStart,
          end: lineEnd,
          text: fullText,
          words: words
        });
      }
    }
  }
  return lines;
}

let musixmatchToken = null;
let musixmatchTokenExpires = 0;

async function getMusixmatchToken() {
  if (musixmatchToken && Date.now() < musixmatchTokenExpires) {
    return musixmatchToken;
  }
  try {
    const res = await fetch('https://apic-desktop.musixmatch.com/ws/1.1/token.get?app_id=web-desktop-app-v1.0', {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
    });
    if (res.ok) {
      const data = await res.json();
      const token = data?.message?.body?.user_token;
      if (token) {
        musixmatchToken = token;
        musixmatchTokenExpires = Date.now() + 24 * 3600 * 1000;
        return token;
      }
    }
  } catch (_) {}
  return musixmatchToken || '';
}

const fetchLrcLib = fetchLyricsDirect;

function normalizeLyricMeta(str) {
  return String(str || '')
    .toLowerCase()
    .replace(/\s*[\(\[][^)\]]*(?:official|audio|video|lyrics?|prod|feat|ft|remaster|version|edit)[^)\]]*[\)\]]/gi, '')
    .replace(/[^\p{L}\p{N}]+/gu, ' ')
    .trim();
}

function isStrictLyricMatch(reqTitle, reqArtist, candTitle, candArtist) {
  const t1 = normalizeLyricMeta(reqTitle);
  const t2 = normalizeLyricMeta(candTitle);
  if (!t1 || !t2 || t1 !== t2) return false;
  const a1 = normalizeLyricMeta(reqArtist);
  const a2 = normalizeLyricMeta(candArtist);
  if (!a1 || !a2) return false;
  if (a1 === a2) return true;
  const parts1 = a1.split(/\s+/).filter(Boolean);
  const parts2 = a2.split(/\s+/).filter(Boolean);
  return parts1.length > 0 && parts1.join(' ') === parts2.join(' ');
}

async function fetchMusixmatchLyrics(title, artist) {
  try {
    const token = await getMusixmatchToken();
    const cleanTitle = (title || '').replace(/\s*[\(\[][^\)\]]*(?:feat|ft|remix|official|audio)[^\)\]]*[\)\]]/gi, '').trim();
    const cleanArtist = (artist || '').replace(/ - Topic|VEVO/gi, '').trim();
    const qTrack = encodeURIComponent(cleanTitle || title);
    const qArtist = encodeURIComponent(cleanArtist || artist);
    const tokParam = token ? `&usertoken=${encodeURIComponent(token)}` : '';

    const macroUrl = `https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get?app_id=web-desktop-app-v1.0${tokParam}&q_track=${qTrack}&q_artist=${qArtist}`;
    const res = await fetch(macroUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
    });
    if (res.ok) {
      const data = await res.json();
      const macro = data?.message?.body?.macro_calls;
      if (macro) {
        const trackObj = macro['matcher.track.get']?.message?.body?.track || macro['track.get']?.message?.body?.track;
        if (!trackObj || !isStrictLyricMatch(title, artist, trackObj.track_name, trackObj.artist_name)) {
          return null;
        }
        const subList = macro['track.subtitles.get']?.message?.body?.subtitle_list;
        const subBody = subList?.[0]?.subtitle?.subtitle_body || '';
        const lyrBody = macro['track.lyrics.get']?.message?.body?.lyrics?.lyrics_body || '';
        if (subBody || lyrBody) {
          return { syncedLyrics: subBody || '', plainLyrics: lyrBody || '' };
        }
      }
    }

    const trackRes = await fetch(`https://apic-desktop.musixmatch.com/ws/1.1/matcher.track.get?app_id=web-desktop-app-v1.0${tokParam}&q_track=${qTrack}&q_artist=${qArtist}`, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
    }).catch(() => null);
    if (trackRes && trackRes.ok) {
      const trackData = await trackRes.json().catch(() => null);
      const trackObj = trackData?.message?.body?.track;
      if (!trackObj || !isStrictLyricMatch(title, artist, trackObj.track_name, trackObj.artist_name)) {
        return null;
      }
    } else {
      return null;
    }

    let synced = '';
    let plain = '';
    const subUrl = `https://apic-desktop.musixmatch.com/ws/1.1/matcher.subtitle.get?app_id=web-desktop-app-v1.0${tokParam}&q_track=${qTrack}&q_artist=${qArtist}`;
    const subRes = await fetch(subUrl, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }).catch(() => null);
    if (subRes && subRes.ok) {
      const subData = await subRes.json().catch(() => null);
      synced = subData?.message?.body?.subtitle?.subtitle_body || '';
    }

    const lyrUrl = `https://apic-desktop.musixmatch.com/ws/1.1/matcher.lyrics.get?app_id=web-desktop-app-v1.0${tokParam}&q_track=${qTrack}&q_artist=${qArtist}`;
    const lyrRes = await fetch(lyrUrl, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }).catch(() => null);
    if (lyrRes && lyrRes.ok) {
      const lyrData = await lyrRes.json().catch(() => null);
      plain = lyrData?.message?.body?.lyrics?.lyrics_body || '';
    }

    if (synced || plain) {
      return { syncedLyrics: synced, plainLyrics: plain };
    }
  } catch (_) {}
  return null;
}

async function fetchKugouLyrics(title, artist, durationSec = 0) {
  try {
    const durMs = Math.round((durationSec || 0) * 1000);
    const searchUrl = `http://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=${encodeURIComponent(artist + ' - ' + title)}&duration=${durMs}&hash=`;
    const res = await fetch(searchUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
    });
    if (!res.ok) return null;
    const json = await res.json();
    const candidates = Array.isArray(json?.candidates) ? json.candidates : [];
    const validCandidates = candidates.filter(cand =>
      cand && isStrictLyricMatch(title, artist, cand.songname || cand.filename?.split('-')[1], cand.singername || cand.filename?.split('-')[0])
    );
    if (!validCandidates.length) return null;
    const cand = validCandidates[0];
    if (!cand || !cand.id || !cand.accesskey) return null;

    const dlUrl = `http://lyrics.kugou.com/download?ver=1&client=pc&id=${cand.id}&accesskey=${cand.accesskey}&fmt=lrc&charset=utf8`;
    const dlRes = await fetch(dlUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
    });
    if (!dlRes.ok) return null;
    const dlJson = await dlRes.json();
    if (!dlJson || !dlJson.content) return null;

    let lrcText = Buffer.from(dlJson.content, 'base64').toString('utf8');
    lrcText = lrcText.replace(/^\[(id|ti|ar|al|by|hash|sign|qq|total|offset):[^\]]*\]\r?\n?/gim, '').trim();

    if (lrcText) {
      return { syncedLyrics: lrcText, plainLyrics: '' };
    }
  } catch (_) {}
  return null;
}

function fetchNeteaseLyrics(title, artist, targetDuration) {
  return new Promise((resolve) => {
    const cleanQuery = (title + ' ' + artist)
      .replace(/\s*\(feat\.[^)]*\)/gi, '')
      .replace(/\s*\[feat\.[^\]]*\]/gi, '')
      .replace(/\s*\(featuring[^)]*\)/gi, '')
      .replace(/\s*feat\.[^\s]+/gi, '')
      .replace(/\s*-\s*remaster(ed)?(\s*\d+)?/gi, '')
      .replace(/\s*\(live[^)]*\)/gi, '')
      .replace(/\s*\[live[^\]]*\]/gi, '')
      .replace(/\s+/g, ' ')
      .trim();

    const fetchLyric = (songId) => {
      const lyricUrl = `http://music.163.com/api/song/lyric?id=${songId}&lv=1&kv=1&tv=-1`;
      const lReq = http.get(lyricUrl, {
        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': 'https://music.163.com' },
        timeout: 8000
      }, res => {
        const chunks = [];
        res.on('data', chunk => chunks.push(chunk));
        res.on('end', () => {
          const d = decodeUtf8Chunks(chunks);
          if (res.statusCode !== 200) return resolve(null);
          try {
            const j = JSON.parse(d);
            const lrc = j.lrc && j.lrc.lyric;
            if (lrc && /\[\d{2}:\d{2}/.test(lrc)) {
              return resolve({ syncedLyrics: lrc, plainLyrics: '' });
            } else if (lrc) {
              return resolve({ syncedLyrics: '', plainLyrics: lrc });
            }
          } catch (_) {}
          resolve(null);
        });
      });
      lReq.on('timeout', () => { lReq.destroy(); resolve(null); });
      lReq.on('error', () => resolve(null));
    };

    const searchUrl = `http://music.163.com/api/search/get/web?s=${encodeURIComponent(cleanQuery)}&type=1&limit=5`;
    const u = new URL(searchUrl);
    const sReq = http.get({
      hostname: u.hostname,
      path: u.pathname + u.search,
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': 'https://music.163.com' },
      timeout: 8000
    }, res => {
      const chunks = [];
      res.on('data', chunk => chunks.push(chunk));
      res.on('end', () => {
        const d = decodeUtf8Chunks(chunks);
        if (res.statusCode !== 200) return resolve(null);
        try {
          const j = JSON.parse(d);
          const songs = (j.result && j.result.songs) || [];
          const validSongs = songs.filter(s =>
            s && isStrictLyricMatch(title, artist, s.name, (s.artists || s.ar || []).map(a => a.name).join(' '))
          );
          if (!validSongs.length) return resolve(null);
          const matched = validSongs.find(song => {
            if (targetDuration && targetDuration > 0) {
              const songDur = (song.dt || song.duration || 0) / 1000;
              if (Math.abs(songDur - targetDuration) > 4) return false;
            }
            return true;
          }) || validSongs[0];
          if (matched && matched.id) {
            return fetchLyric(matched.id);
          }
        } catch (_) {}
        resolve(null);
      });
    });
    sReq.on('timeout', () => { sReq.destroy(); resolve(null); });
    sReq.on('error', () => resolve(null));
  });
}

function fetchNetEaseWords(title, artist, targetDuration) {
  return new Promise((resolve) => {
    const normSimple = s => (s || '').toLowerCase().replace(/[^a-z0-9\u0400-\u04ff\u4e00-\u9fa5]/gi, '');
    const cleanQuery = (title + ' ' + artist)
      .replace(/\s*\(feat\.[^)]*\)/gi, '')
      .replace(/\s*\[feat\.[^\]]*\]/gi, '')
      .replace(/\s*\(featuring[^)]*\)/gi, '')
      .replace(/\s*feat\.[^\s]+/gi, '')
      .replace(/\s*-\s*remaster(ed)?(\s*\d+)?/gi, '')
      .replace(/\s*\(live[^)]*\)/gi, '')
      .replace(/\s*\[live[^\]]*\]/gi, '')
      .replace(/\s+/g, ' ')
      .trim();

    const fetchLyric = (songId) => {
      const lyricUrl = `http://music.163.com/api/song/lyric?id=${songId}&lv=1&kv=1&tv=-1&yv=1`;
      const lReq = http.get(lyricUrl, {
        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': 'https://music.163.com' },
        timeout: 8000
      }, res => {
        const chunks = [];
        res.on('data', chunk => chunks.push(chunk));
        res.on('end', () => {
          const d = decodeUtf8Chunks(chunks);
          if (res.statusCode !== 200) {
            logNetworkFailure(lyricUrl);
            return resolve(null);
          }
          try {
            const j = JSON.parse(d);
            if (j.yrc && j.yrc.lyric) {
              const lines = parseYrc(j.yrc.lyric);
              if (lines.length > 0) return resolve(lines);
            }
          } catch (_) {}
          resolve(null);
        });
      });
      lReq.on('timeout', () => {
        lReq.destroy();
        logNetworkFailure(lyricUrl);
        resolve(null);
      });
      lReq.on('error', () => {
        logNetworkFailure(lyricUrl);
        resolve(null);
      });
    };

    const trySearch = (searchUrl, isWeb) => {
      const u = new URL(searchUrl);
      const sReq = http.get({
        hostname: u.hostname,
        path: u.pathname + u.search,
        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': 'https://music.163.com' },
        timeout: 8000
      }, res => {
        const chunks = [];
        res.on('data', chunk => chunks.push(chunk));
        res.on('end', () => {
          const d = decodeUtf8Chunks(chunks);
          if (res.statusCode !== 200) {
            logNetworkFailure(searchUrl);
            if (isWeb) {
              return trySearch(`http://music.163.com/api/search/get?s=${encodeURIComponent(cleanQuery)}&type=1&limit=5`, false);
            }
            return resolve(null);
          }
          try {
            const j = JSON.parse(d);
            const songs = (j.result && j.result.songs) || [];
            if (songs.length > 0) {
              const matched = songs.find(song => {
                if (targetDuration && targetDuration > 0) {
                  const songDur = (song.dt || song.duration || 0) / 1000;
                  if (Math.abs(songDur - targetDuration) > 3) return false;
                }
                const normTargetTitle = normSimple(title);
                const normCandTitle = normSimple(song.name);
                if (!normTargetTitle || !normCandTitle || (!normTargetTitle.includes(normCandTitle) && !normCandTitle.includes(normTargetTitle))) return false;
                const targetArtists = artist.split(/[,&\/\+]/).map(normSimple).filter(Boolean);
                const candArtists = (song.artists || []).map(a => normSimple(a.name)).filter(Boolean);
                if (targetArtists.length > 0 && !candArtists.some(ca => targetArtists.some(ta => ca.includes(ta) || ta.includes(ca)))) return false;
                return true;
              });
              if (matched) {
                return fetchLyric(matched.id);
              }
            }
          } catch (_) {}
          if (isWeb) {
            trySearch(`http://music.163.com/api/search/get?s=${encodeURIComponent(cleanQuery)}&type=1&limit=5`, false);
          } else {
            resolve(null);
          }
        });
      });
      sReq.on('timeout', () => {
        sReq.destroy();
        logNetworkFailure(searchUrl);
        if (isWeb) {
          trySearch(`http://music.163.com/api/search/get?s=${encodeURIComponent(cleanQuery)}&type=1&limit=5`, false);
        } else {
          resolve(null);
        }
      });
      sReq.on('error', () => {
        logNetworkFailure(searchUrl);
        if (isWeb) {
          trySearch(`http://music.163.com/api/search/get?s=${encodeURIComponent(cleanQuery)}&type=1&limit=5`, false);
        } else {
          resolve(null);
        }
      });
    };

    trySearch(`http://music.163.com/api/search/get/web?s=${encodeURIComponent(cleanQuery)}&type=1&limit=5`, true);
  });
}

const SERVER_TOKEN = crypto.randomBytes(24).toString('hex');

const TOKEN_PROTECTED_ROUTES = new Set([
  '/api/state',
  '/api/translate',
  '/api/i18n-batch',
  '/api/vibe',
  '/api/search',
  '/api/artist',
  '/api/lyrics',
  '/api/download',
  '/api/spotify-import',
  '/api/spotify-auth-start',
  '/api/spotify-status',
  '/api/spotify-library',
  '/api/spotify-logout',
  '/api/prefetch',
  '/api/check-cache',
  '/api/stream-info',
  '/api/track-analysis',
  '/api/tools-status',
  '/api/tools-install',
  '/api/tools/status',
  '/api/tools/install',
  '/api/library',
  '/api/library-add',
  '/api/library-update',
  '/api/library-remove',
  '/api/offline-library',
  '/api/offline-delete',
  '/api/clear-offline-library',
  '/api/save-custom-lyrics',
  '/api/encode',
  '/api/yandex-import'
]);

function getServerToken() {
  return SERVER_TOKEN;
}

function tokenMatches(provided) {
  if (typeof provided !== 'string') return false;
  const expected = Buffer.from(SERVER_TOKEN);
  const given = Buffer.from(provided);
  if (given.length !== expected.length) return false;
  try {
    return crypto.timingSafeEqual(given, expected);
  } catch (e) {
    return false;
  }
}

function legacyCustomLyricsPath(title, artist) {
  const safeName = `${(artist || 'unknown').replace(/[^\w\s-]/g, '_')}_${(title || 'untitled').replace(/[^\w\s-]/g, '_')}.lrc`.toLowerCase().replace(/\s+/g, '_');
  return path.join(CUSTOM_LYRICS_DIR, safeName);
}

function legacyFetchedLyricsPath(title, artist) {
  const cleanTitle = (title || '').replace(/[^a-zA-Z0-9_-]/g, '_').toLowerCase();
  const cleanArtist = (artist || '').replace(/[^a-zA-Z0-9_-]/g, '_').toLowerCase();
  return path.join(LYRICS_DIR, `${cleanArtist}___${cleanTitle}.lrc`);
}

function lyricsKey(title, artist) {
  return crypto.createHash('sha1').update(`${(artist || '').trim().toLowerCase()}\u0000${(title || '').trim().toLowerCase()}`).digest('hex').slice(0, 16);
}

function getCustomLyricsPath(title, artist) {
  return path.join(CUSTOM_LYRICS_DIR, lyricsKey(title, artist) + '.lrc');
}

function getFetchedLyricsPath(title, artist, source = 'lrclib') {
  return path.join(LYRICS_DIR, lyricsKey(title, artist) + (source === 'genius' ? '_genius' : '') + '.lrc');
}

function isLyricsExplicit(lyrics) {
  if (!lyrics || typeof lyrics !== 'string') return false;
  return /\b(fuck|fucking|fucked|fucker|fucks|shit|bitch|bitches|bitching|asshole|cunt|nigger|nigga|dick|pussy)\b|ху[йеяёюи]|пизд|залуп|муда[кч]|мудил|г[ао]ндон|дерьм|дроч|(?:^|[^а-яё])(?:бля[а-яё]*|сук[аиуео]|суч[екьяа][а-яё]*)|(?:^|[^а-яё])(?:[её]б[а-яё]+|[её]б\b|(?:вы|за|на|до|по|пере|про|разъ|раз|съ|въ|под|от|у)[её]б[а-яё]*)/i.test(lyrics);
}

async function handleLyrics(req, res, title, artist, allowOnline = true, source = 'lrclib', duration = 0) {
  const normSource = source || 'lrclib';
  const cacheKey = `${artist || ''}:${title || ''}:${normSource}`.toLowerCase();
  let cached = lyricsCache.get(cacheKey);
  if (cached?.promise) {
    const outcome = await cached.promise;
    if (!hasBrokenLyricText(outcome.lyrics)) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify(outcome));
    }
    if (lyricsCache.get(cacheKey) === cached) lyricsCache.delete(cacheKey);
    cached = null;
  }
  if (cached) {
    if (cached.instrumental) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: true, instrumental: true, syncedLyrics: '', plainLyrics: '' }));
    }
    if (!hasBrokenLyricText(cached.text)) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: true, lyrics: cached.text, isCustom: cached.isCustom || false, isExplicit: isLyricsExplicit(cached.text) }));
    }
    lyricsCache.delete(cacheKey);
  }

  let resolveLyrics;
  const lyricsPromise = new Promise(resolve => { resolveLyrics = resolve; });
  const inFlightEntry = { promise: lyricsPromise };
  lyricsCache.set(cacheKey, inFlightEntry);
  let outcome = null;
  const sendLyrics = (lyrics, isCustom = false) => {
    outcome = { success: true, lyrics, isCustom, isExplicit: isLyricsExplicit(lyrics) };
    resolveLyrics(outcome);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(outcome));
  };
  const sendInstrumental = () => {
    outcome = { success: true, instrumental: true, syncedLyrics: '', plainLyrics: '' };
    if (lyricsCache.get(cacheKey) === inFlightEntry) {
      lyricsCache.set(cacheKey, { text: '', instrumental: true, isCustom: false });
    }
    resolveLyrics(outcome);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(outcome));
  };

  try {
    let customPath = getCustomLyricsPath(title, artist);
    if (!fs.existsSync(customPath) && /^[\x00-\x7F]*$/.test((title || '') + (artist || ''))) {
      const legacyPath = legacyCustomLyricsPath(title, artist);
      if (fs.existsSync(legacyPath)) {
        customPath = legacyPath;
      }
    }

    if (fs.existsSync(customPath)) {
      try {
        const customLyrics = await fs.promises.readFile(customPath, 'utf-8');
        if (lyricsCache.get(cacheKey) === inFlightEntry) {
          lyricsCache.set(cacheKey, { text: customLyrics, isCustom: true });
        }
        return sendLyrics(customLyrics, true);
      } catch (e) {}
    }

    if (/\b(instrumental|type\s+beat)\b/i.test(title || '')) {
      return sendInstrumental();
    }

    let fetchedLyricsPath = getFetchedLyricsPath(title, artist, normSource);
    if (!fs.existsSync(fetchedLyricsPath) && /^[\x00-\x7F]*$/.test((title || '') + (artist || ''))) {
      const legacyFetched = legacyFetchedLyricsPath(title, artist);
      if (fs.existsSync(legacyFetched)) {
        fetchedLyricsPath = legacyFetched;
      }
    }

    if (normSource !== 'genius' && fs.existsSync(fetchedLyricsPath)) {
      try {
        const diskLyrics = await fs.promises.readFile(fetchedLyricsPath, 'utf-8');
        if (hasBrokenLyricText(diskLyrics)) {
          await fs.promises.unlink(fetchedLyricsPath);
        } else {
          if (lyricsCache.get(cacheKey) === inFlightEntry) {
            lyricsCache.set(cacheKey, { text: diskLyrics, isCustom: false });
          }
          return sendLyrics(diskLyrics);
        }
      } catch (e) {}
    }

    if (!allowOnline) {
      return sendLyrics('');
    }

    try {
      let rawLyrics = null;
      let successfulBackend = null;

      if (normSource === 'musixmatch') {
        const mm = await fetchMusixmatchLyrics(title, artist);
        rawLyrics = mm ? (mm.syncedLyrics || mm.plainLyrics || '') : '';
        successfulBackend = 'musixmatch';
      } else if (normSource === 'kugou') {
        const kg = await fetchKugouLyrics(title, artist, duration);
        rawLyrics = kg ? (kg.syncedLyrics || kg.plainLyrics || '') : '';
        successfulBackend = 'kugou';
      } else if (normSource === 'netease') {
        const ne = await fetchNeteaseLyrics(title, artist, duration);
        rawLyrics = ne ? (ne.syncedLyrics || ne.plainLyrics || '') : '';
        successfulBackend = 'netease';
      } else if (normSource === 'genius') {
        rawLyrics = await fetchGeniusLyrics(title, artist);
        successfulBackend = 'genius';
      } else {
        let plainFallback = '';

        

        const lrcLib = await fetchLrcLib(title, artist);
        if (lrcLib && lrcLib.instrumental) {
          return sendInstrumental();
        }
        const lrcText = typeof lrcLib === 'string' ? lrcLib : (lrcLib?.syncedLyrics || lrcLib?.plainLyrics || '');
        if (lrcText && /\[\d{2}:\d{2}/.test(lrcText)) {
          rawLyrics = lrcText;
          successfulBackend = 'lrclib';
        } else if (lrcText) {
          plainFallback = plainFallback || lrcText;
        }

        

        if (!rawLyrics) {
          const mm = await fetchMusixmatchLyrics(title, artist);
          if (mm?.syncedLyrics && /\[\d{2}:\d{2}/.test(mm.syncedLyrics)) {
            rawLyrics = mm.syncedLyrics;
            successfulBackend = 'musixmatch';
          } else if (mm?.plainLyrics) {
            plainFallback = plainFallback || mm.plainLyrics;
          }
        }

        

        if (!rawLyrics) {
          const kg = await fetchKugouLyrics(title, artist, duration);
          if (kg?.syncedLyrics && /\[\d{2}:\d{2}/.test(kg.syncedLyrics)) {
            rawLyrics = kg.syncedLyrics;
            successfulBackend = 'kugou';
          } else if (kg?.plainLyrics) {
            plainFallback = plainFallback || kg.plainLyrics;
          }
        }

        

        if (!rawLyrics) {
          const ne = await fetchNeteaseLyrics(title, artist, duration);
          if (ne?.syncedLyrics && /\[\d{2}:\d{2}/.test(ne.syncedLyrics)) {
            rawLyrics = ne.syncedLyrics;
            successfulBackend = 'netease';
          } else if (ne?.plainLyrics) {
            plainFallback = plainFallback || ne.plainLyrics;
          }
        }

        

        if (!rawLyrics) {
          if (plainFallback) {
            rawLyrics = plainFallback;
          } else {
            const gn = await fetchGeniusLyrics(title, artist);
            if (gn) {
              rawLyrics = gn;
              successfulBackend = 'genius';
            }
          }
        }
      }

      if (hasBrokenLyricText(rawLyrics)) rawLyrics = '';

      if (rawLyrics && rawLyrics.trim().length > 0) {
        if (successfulBackend && successfulBackend !== 'genius') {
          try {
            await fs.promises.writeFile(fetchedLyricsPath, rawLyrics, 'utf-8');
          } catch (e) {}
        }
        if (lyricsCache.get(cacheKey) === inFlightEntry) {
          lyricsCache.set(cacheKey, { text: rawLyrics, isCustom: false });
        }
        return sendLyrics(rawLyrics);
      }

      if (lyricsCache.get(cacheKey) === inFlightEntry) {
        lyricsCache.set(cacheKey, { text: '', isCustom: false });
      }
      return sendLyrics('');
    } catch (err) {
      return sendLyrics('');
    }
  } finally {
    if (!outcome) {
      outcome = { success: true, lyrics: '', isCustom: false, isExplicit: false };
      resolveLyrics(outcome);
    }
    if (lyricsCache.get(cacheKey) === inFlightEntry) {
      lyricsCache.delete(cacheKey);
    }
  }
}

async function handleSaveCustomLyrics(req, res, title, artist) {
  let body = '';
  req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
  req.on('end', async () => {
    try {
      const { lyrics } = JSON.parse(body);
      if (typeof lyrics !== 'string') {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: false, error: 'No lyrics provided' }));
      }

      const customPath = getCustomLyricsPath(title, artist);
      await fs.promises.writeFile(customPath, lyrics, 'utf-8');

      lyricsCache.clear();

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    } catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: e.message }));
    }
  });
}

const SPOTIFY_PATHFINDER_URL = 'https://api-partner.spotify.com/pathfinder/v1/query';
const SPOTIFY_FETCH_PLAYLIST_HASH = '8964e8eafb21aa992a7d951d256d83285c04be2105d209262901de70cb97584a';

function pathfinderRequest(accessToken, variables) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify({
      variables,
      operationName: 'fetchPlaylist',
      extensions: { persistedQuery: { version: 1, sha256Hash: SPOTIFY_FETCH_PLAYLIST_HASH } }
    });
    const req = https.request(SPOTIFY_PATHFINDER_URL, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        'Accept': 'text/html,application/json',
        'Accept-Language': 'en-US,en;q=0.9',
        'Referer': 'https://open.spotify.com/',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      },
      timeout: 15000
    }, (r) => {
      if (r.statusCode !== 200) {
        try { logger.log('error', `api-partner.spotify.com status ${r.statusCode}`); } catch (_) {}
      }
      let b = '';
      r.on('data', c => b += c);
      r.on('end', () => {
        if (r.statusCode !== 200) return reject(new Error(`Pathfinder status ${r.statusCode}`));
        try { resolve(JSON.parse(b)); } catch (e) { reject(new Error('Invalid pathfinder response')); }
      });
    });
    req.on('error', (err) => {
      try { logger.log('error', `api-partner.spotify.com error: ${err.message}`); } catch (_) {}
      reject(err);
    });
    req.on('timeout', () => {
      req.destroy();
      try { logger.log('error', 'api-partner.spotify.com timeout'); } catch (_) {}
      reject(new Error('Pathfinder request timed out'));
    });
    req.write(payload);
    req.end();
  });
}

async function fetchFullSpotifyPlaylist(playlistId, accessToken) {
  const uri = `spotify:playlist:${playlistId}`;
  const first = await pathfinderRequest(accessToken, { uri, offset: 0, limit: 100, enableWatchFeedEntrypoint: true });
  const content = first?.data?.playlistV2?.content;
  if (!content || !Array.isArray(content.items)) return null;
  const total = content.totalCount || content.items.length;
  const items = content.items.slice();
  let offset = items.length;
  let guard = 0;
  while (offset < total && guard++ < 100) {
    const page = await pathfinderRequest(accessToken, { uri, offset, limit: 100, enableWatchFeedEntrypoint: true });
    const pageItems = page?.data?.playlistV2?.content?.items;
    if (!Array.isArray(pageItems) || pageItems.length === 0) throw new Error('Incomplete Pathfinder playlist');
    items.push(...pageItems);
    offset += pageItems.length;
  }
  if (offset < total) throw new Error('Incomplete Pathfinder playlist');
  const tracks = items.map(it => {
    const t = it?.itemV2?.data || it?.item?.data || {};
    const artists = (t.albumOfTrack?.artists?.items || []).map(a => a.profile?.name).filter(Boolean);
    const durationMs = t.trackDuration?.totalMilliseconds || t.duration?.totalMilliseconds || t.duration_ms;
    const duration = typeof durationMs === 'number' ? Math.round(durationMs / 1000) : 0;
    const sources = t.albumOfTrack?.coverArt?.sources || [];
    const cover = sources.length ? sources[sources.length - 1].url : '';
    return { title: (t.name || '').trim(), artist: artists.join(', '), duration, cover, thumbnail: cover };
  }).filter(t => t.title);
  return { tracks, total };
}

async function trySpotifyWebApi(playlistId) {
  loadSpotifyAuth();
  if (!spotifyAuth.token && !spotifyAuth.refreshToken) return null;
  try {
    const resp = await spotifyGetWithRetry(`https://api.spotify.com/v1/playlists/${encodeURIComponent(playlistId)}`);
    if (resp.status >= 200 && resp.status < 300) {
      const d = JSON.parse(resp.body);
      const name = d.name || 'Spotify Playlist';
      const cover = (d.images && Array.isArray(d.images) && d.images[0] && d.images[0].url) ? d.images[0].url : '';
      const playlistItems = d.items || d.tracks;
      if (!Array.isArray(playlistItems?.items)) return null;
      let items = playlistItems.items.slice();
      let nextUrl = playlistItems?.next;
      let guard = 0;
      while (nextUrl && guard++ < 100) {
        const nextResp = await spotifyGetWithRetry(nextUrl);
        if (nextResp.status < 200 || nextResp.status >= 300) return null;
        const nextData = JSON.parse(nextResp.body);
        if (!Array.isArray(nextData.items) || !nextData.items.length) return null;
        items.push(...nextData.items);
        nextUrl = nextData.next;
      }
      if (nextUrl || (Number.isInteger(playlistItems.total) && items.length < playlistItems.total)) return null;
      const tracks = items.map(it => {
        const t = it?.item || it?.track || it || {};
        const imgs = t.album?.images || [];
        const cover = imgs.length ? imgs[0].url : '';
        return {
          title: t.name || '',
          artist: Array.isArray(t.artists) ? t.artists.map(a => a.name).join(', ') : '',
          duration: t.duration_ms ? Math.round(t.duration_ms / 1000) : undefined,
          cover,
          thumbnail: cover
        };
      }).filter(t => t.title);
      return { success: true, name, cover, tracks, total: tracks.length };
    }
  } catch (e) {}
  return null;
}

function fetchSpotifyEmbed(playlistId, ua) {
  return new Promise((resolve, reject) => {
    const embedUrl = `https://open.spotify.com/embed/playlist/${playlistId}`;
    const req = https.get(embedUrl, {
      headers: {
        'User-Agent': ua,
        'Accept-Language': 'en-US,en;q=0.9',
        'Accept': 'text/html,application/json',
        'Referer': 'https://open.spotify.com/'
      },
      timeout: 10000
    }, (res) => {
      if (res.statusCode !== 200) {
        try { logger.log('error', `open.spotify.com status ${res.statusCode}`); } catch (_) {}
      }
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => resolve({ status: res.statusCode, body }));
    });
    req.on('error', (err) => {
      try { logger.log('error', `open.spotify.com error: ${err.message}`); } catch (_) {}
      reject(err);
    });
    req.on('timeout', () => {
      req.destroy();
      try { logger.log('error', 'open.spotify.com timeout'); } catch (_) {}
      reject(new Error('timeout'));
    });
  });
}

const BRAND_ICON_COLORS = {
  spotify: '1ED760',
  youtubemusic: 'FF0000',
  youtube: 'FF0000',
  soundcloud: 'FF5500',
  applemusic: 'FA243C',
  deezer: 'A238FF',
  yandexmusic: 'FFCC00',
  json: '7E7A8A'
};

async function handleBrandIcon(req, res, brandName) {
  const brand = String(brandName || '').toLowerCase().replace(/[^a-z0-9]/g, '');
  if (!brand) {
    res.writeHead(400); return res.end();
  }
  const color = BRAND_ICON_COLORS[brand] || '';
  const cachedFile = path.join(THUMBNAILS_DIR, `brand_${brand}.svg`);
  if (fs.existsSync(cachedFile)) {
    res.writeHead(200, { 'Content-Type': 'image/svg+xml', 'Cache-Control': 'public, max-age=604800' });
    return fs.createReadStream(cachedFile).pipe(res);
  }
  try {
    const cdnUrl = color ? `https://cdn.simpleicons.org/${brand}/${color}` : `https://cdn.simpleicons.org/${brand}`;
    const resp = await fetch(cdnUrl, { signal: AbortSignal.timeout(6000) });
    if (resp.ok) {
      const svgText = await resp.text();
      if (svgText && svgText.includes('<svg')) {
        await fs.promises.writeFile(cachedFile, svgText, 'utf8');
        res.writeHead(200, { 'Content-Type': 'image/svg+xml', 'Cache-Control': 'public, max-age=604800' });
        return res.end(svgText);
      }
    }
  } catch (_) {}
  res.writeHead(302, { Location: `https://cdn.simpleicons.org/${brand}${color ? '/' + color : ''}` });
  res.end();
}

async function handleSpotifyImport(req, res, inputUrl) {
  const m = (inputUrl || '').match(/^(?:(?:https:\/\/)?open\.spotify\.com\/(?:embed\/)?playlist\/|spotify:playlist:)([a-zA-Z0-9]{22})(?:[/?#]|$)/);
  if (!m) {
    res.writeHead(400, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, code: 'spotify_error', error: 'Invalid Spotify playlist URL' }));
  }
  const playlistId = m[1];
  const apiResult = await trySpotifyWebApi(playlistId);
  if (apiResult && apiResult.tracks.length) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(apiResult));
  }
  const primaryUa = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
  const retryUa = 'Mozilla/5.0 (X11; Linux x86_64; rv:125.0) Gecko/20100101 Firefox/125.0';

  let embedRes;
  try {
    embedRes = await fetchSpotifyEmbed(playlistId, primaryUa);
    if (embedRes.status === 403 || embedRes.status === 429) {
      await new Promise(r => setTimeout(r, 800));
      embedRes = await fetchSpotifyEmbed(playlistId, retryUa);
    }
  } catch (err) {
    res.writeHead(500, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, code: 'spotify_error', error: 'Network error connecting to Spotify' }));
  }

  if (embedRes.status === 404) {
    res.writeHead(404, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, code: 'spotify_not_found', error: 'Playlist not found' }));
  }
  if (embedRes.status === 403) {
    res.writeHead(403, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, code: 'spotify_forbidden', error: 'Spotify refused request (403)' }));
  }
  if (embedRes.status === 429) {
    res.writeHead(429, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, code: 'spotify_rate_limited', error: 'Spotify rate limited (429)' }));
  }
  if (embedRes.status !== 200) {
    res.writeHead(embedRes.status || 500, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, code: 'spotify_error', error: `Spotify returned status ${embedRes.status}` }));
  }

  try {
    const body = embedRes.body;
    const nextDataMatch = body.match(/<script id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
    if (!nextDataMatch) {
      const isPriv = /private/i.test(body);
      res.writeHead(403, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, code: isPriv ? 'spotify_forbidden' : 'spotify_error', error: isPriv ? 'Private playlist' : 'Could not parse playlist' }));
    }

    const data = JSON.parse(nextDataMatch[1]);
    const pageProps = data?.props?.pageProps;
    if (pageProps?.status === 404) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, code: 'spotify_not_found', error: 'Playlist not found' }));
    }

    const entity = pageProps?.state?.data?.entity;
    const name = entity?.name || pageProps?.title || 'Spotify Playlist';
    const ogMatch = body.match(/<meta\s+(?:property|name)=["'](?:og:image|twitter:image)["']\s+content=["']([^"']+)["']/i);
    let cover = (entity?.images && Array.isArray(entity.images) && entity.images[0]?.url) ||
                (entity?.coverArt?.sources && Array.isArray(entity.coverArt.sources) && entity.coverArt.sources[0]?.url) ||
                entity?.image ||
                pageProps?.metadata?.image ||
                (ogMatch ? ogMatch[1] : '') ||
                '';

    let tracks = [];
    let limited = false;
    const accessToken = pageProps?.state?.settings?.session?.accessToken;
    let total = 0;
    if (accessToken) {
      try {
        const full = await fetchFullSpotifyPlaylist(playlistId, accessToken);
        if (full && full.tracks && full.tracks.length) {
          tracks = full.tracks;
          total = full.total || full.tracks.length;
        } else {
          limited = true;
          try { logger.log('warn', 'Spotify pathfinder non-200 or empty, falling back to embed trackList'); } catch (_) {}
        }
      } catch (e) {
        limited = true;
        try { logger.log('warn', `Spotify pathfinder error: ${e.message}, falling back to embed trackList`); } catch (_) {}
      }
    } else {
      limited = true;
    }

    if (!tracks.length) {
      if (Array.isArray(entity?.trackList) && entity.trackList.length > 0) {
        tracks = entity.trackList.slice(0, 100).map(t => {
          const duration = typeof t.duration === 'number' ? Math.round(t.duration / 1000) : (typeof t.duration_ms === 'number' ? Math.round(t.duration_ms / 1000) : 0);
          const trackCover = t.image || t.coverArt?.sources?.[0]?.url || '';
          return {
            title: (t.title || t.name || '').trim(),
            artist: (t.subtitle || t.artist || '').trim(),
            duration,
            thumbnail: trackCover,
            cover: trackCover
          };
        }).filter(t => t.title);
      }
      if (!tracks.length) {
        const seen = new Set();
        function walk(node) {
          if (!node || typeof node !== 'object') return;
          if (Array.isArray(node)) { for (const item of node) walk(item); return; }
          const uri = typeof node.uri === 'string' ? node.uri : '';
          const title = node.title || node.name;
          const subtitle = node.subtitle || node.artist || (Array.isArray(node.artists) ? node.artists.map(a => typeof a === 'string' ? a : (a.name || a.profile?.name)).filter(Boolean).join(', ') : (node.albumOfTrack?.artists?.items || []).map(a => a.profile?.name).filter(Boolean).join(', '));
          const duration = node.duration?.totalMilliseconds || node.trackDuration?.totalMilliseconds || node.duration_ms || (typeof node.duration === 'number' ? node.duration : 0);
          if (title && typeof title === 'string' && (subtitle || uri.startsWith('spotify:track:')) && (uri.startsWith('spotify:track:') || node.entityType === 'track' || duration)) {
            const key = `${title}__${subtitle || ''}`;
            if (!seen.has(key)) {
              seen.add(key);
              const durSec = typeof duration === 'number' ? (duration > 1000 ? Math.round(duration / 1000) : Math.round(duration)) : 0;
              const trackCover = node.image || node.coverArt?.sources?.[0]?.url || '';
              tracks.push({ title: title.trim(), artist: typeof subtitle === 'string' ? subtitle.trim() : '', duration: durSec, thumbnail: trackCover, cover: trackCover });
            }
            return;
          }
          for (const k of Object.keys(node)) walk(node[k]);
        }
        walk(data);
        tracks = tracks.slice(0, 100);
      }
    }

    if (!total) {
      total = entity?.totalCount || (Array.isArray(entity?.trackList) ? entity.trackList.length : 0) || tracks.length;
    }

    if (!tracks.length) {
      const isPriv = /private/i.test(body) || /private/i.test(JSON.stringify(pageProps || {}));
      res.writeHead(403, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, code: isPriv ? 'spotify_forbidden' : 'spotify_error', error: isPriv ? 'Private playlist' : 'Could not read tracks' }));
    }

    if (limited && (!Array.isArray(entity?.trackList) || !Number.isInteger(entity.totalCount) || entity.totalCount > entity.trackList.length || entity.trackList.length > 100)) {
      res.writeHead(409, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, code: 'spotify_incomplete', error: 'Spotify exposed only part of this playlist. Connect your account or use Export files.' }));
    }
    const hasMore = false;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, name, cover, tracks, hasMore, total, limited }));
  } catch (err) {
    try { logger.log('error', `Spotify import failed: ${err.message}`); } catch (_) {}
    res.writeHead(500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: false, code: 'spotify_error', error: 'Internal server error' }));
  }
}

function fetchHtmlWithRedirects(targetUrl, maxRedirects = 5, timeoutMs = 15000) {
  return new Promise((resolve, reject) => {
    let redirects = 0;
    function fetchUrl(currentUrl) {
      let parsed;
      try { parsed = new URL(currentUrl); } catch (e) { return reject(e); }
      const client = parsed.protocol === 'http:' ? http : https;
      const req = client.get(currentUrl, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept-Language': 'en-US,en;q=0.9'
        },
        timeout: timeoutMs
      }, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          if (++redirects > maxRedirects) {
            return reject(new Error('Too many redirects'));
          }
          const nextUrl = new URL(res.headers.location, currentUrl).toString();
          return fetchUrl(nextUrl);
        }
        let body = '';
        res.on('data', c => body += c);
        res.on('end', () => resolve({ statusCode: res.statusCode, headers: res.headers, body }));
      });
      req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
      req.on('error', reject);
    }
    fetchUrl(targetUrl);
  });
}

async function handleAppleImport(req, res, inputUrl) {
  const albumMatch = (inputUrl || '').match(/music\.apple\.com\/(?:[a-z]{2}\/)?album\/(?:[^/]+\/)?(\d+)/i);
  if (albumMatch) {
    try {
      const data = await fetchItunesJson(`https://itunes.apple.com/lookup?id=${albumMatch[1]}&entity=song`);
      const results = (data && Array.isArray(data.results)) ? data.results : [];
      const albumItem = results.find(r => r && r.wrapperType === 'collection');
      if (albumItem) {
        const tracks = results
          .filter(r => r && r.wrapperType === 'track')
          .map(item => {
            const artUrl = (item.artworkUrl100 || albumItem.artworkUrl100 || '').replace('100x100', '600x600');
            return {
              title: item.trackName || '',
              artist: item.artistName || albumItem.artistName || '',
              duration: item.trackTimeMillis ? Math.round(item.trackTimeMillis / 1000) : 0,
              thumbnail: artUrl,
              cover: artUrl
            };
          });
        if (tracks.length > 0) {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({
            success: true,
            name: albumItem.collectionName || 'Album',
            cover: (albumItem.artworkUrl100 || '').replace('100x100', '600x600'),
            tracks
          }));
        }
      }
    } catch (_) {}
  }

  const m = (inputUrl || '').match(/music\.apple\.com\/.*?(pl\.(?:u-)?[a-zA-Z0-9_-]+)/i);
  if (!m) {
    res.writeHead(400, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'Invalid Apple Music playlist URL' }));
  }

  try {
    const fetched = await fetchHtmlWithRedirects(inputUrl, 5, 15000);
    const { statusCode, body } = fetched;

    if (statusCode === 404) {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'not_found', status: 404 }));
    }
    if (statusCode === 429) {
      res.writeHead(429, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'rate_limit', status: 429 }));
    }
    if (statusCode === 401 || statusCode === 403) {
      res.writeHead(403, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'private', status: 403 }));
    }

    let parsedData = null;
    const serverDataMatch = body.match(/<script\s+type=["']application\/json["']\s+id=["']serialized-server-data["'][^>]*>([\s\S]*?)<\/script>/i);
    if (serverDataMatch) {
      try { parsedData = JSON.parse(serverDataMatch[1]); } catch (e) {}
    }
    if (!parsedData) {
      const ldMatch = body.match(/<script\s+id=["']schema:music-playlist["']\s+type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/i);
      if (ldMatch) {
        try { parsedData = JSON.parse(ldMatch[1]); } catch (e) {}
      }
    }

    if (!parsedData) {
      const isPriv = /private|sign\s*in|login/i.test(body);
      res.writeHead(403, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: isPriv ? 'private' : 'restricted', status: 403 }));
    }

    const tracks = [];
    const seen = new Set();
    let plTitle = '';
    let plCover = '';
    let totalCount = 0;

    function walk(node) {
      if (!node || typeof node !== 'object') return;
      if (Array.isArray(node)) {
        for (const item of node) walk(item);
        return;
      }

      if (!plTitle) {
        if (node.contentDescriptor?.kind === 'playlist' || node.type === 'playlists' || node.type === 'playlist') {
          plTitle = node.title || node.name || node.attributes?.name || '';
          if (node.artwork?.dictionary?.url) plCover = node.artwork.dictionary.url;
          else if (node.artwork?.url) plCover = node.artwork.url;
        } else if (node['@type'] === 'MusicPlaylist') {
          plTitle = node.name || '';
        }
      }

      if (!totalCount && typeof node.trackCount === 'number') {
        totalCount = node.trackCount;
      }

      const title = (typeof node.title === 'string' ? node.title : (typeof node.name === 'string' && (node['@type'] === 'MusicRecording' || node.type === 'songs') ? node.name : '')).trim();
      const artist = (typeof node.artistName === 'string' ? node.artistName : (typeof node.artist === 'string' ? node.artist : (typeof node.byArtist?.name === 'string' ? node.byArtist.name : ''))).trim();
      const durRaw = node.durationInMillis || node.duration || node.duration_ms;
      let duration;
      if (typeof durRaw === 'number') {
        duration = durRaw > 1000 ? Math.round(durRaw / 1000) : Math.round(durRaw);
      } else if (typeof durRaw === 'string' && durRaw.startsWith('PT')) {
        const mDur = durRaw.match(/PT(?:(\d+)M)?(?:(\d+)S)?/);
        if (mDur) duration = (parseInt(mDur[1] || '0', 10) * 60) + parseInt(mDur[2] || '0', 10);
      }

      if (title && artist && (node.contentDescriptor?.kind === 'song' || node.contentDescriptor?.kind === 'music-video' || node.type === 'songs' || node.type === 'song' || node.type === 'library-songs' || node['@type'] === 'MusicRecording' || duration || node.audioTraits)) {
        const key = `${title.toLowerCase()}___${artist.toLowerCase()}`;
        if (!seen.has(key)) {
          seen.add(key);
          let artUrl = node.artwork?.dictionary?.url || node.artwork?.url || '';
          if (artUrl) artUrl = artUrl.replace('{w}x{h}bb.{f}', '600x600bb.jpg').replace('{w}x{h}', '600x600');
          tracks.push({ title, artist, duration, thumbnail: artUrl, cover: artUrl });
        }
        return;
      }

      for (const k of Object.keys(node)) {
        walk(node[k]);
      }
    }

    walk(parsedData);

    if (!plTitle) {
      const ogMatch = body.match(/<meta\s+property=["']og:title["']\s+content=["']([^"']*)["']/i);
      if (ogMatch) {
        plTitle = ogMatch[1].replace(/\s*on Apple Music\s*$/i, '').trim();
      } else {
        const titleMatch = body.match(/<title>([^<]*)<\/title>/i);
        if (titleMatch) {
          plTitle = titleMatch[1].replace(/\s*-\s*Playlist\s*-\s*Apple Music\s*$/i, '').replace(/^[^\w\s]+/, '').trim();
        }
      }
    }

    if (plCover && typeof plCover === 'string' && plCover.includes('{w}x{h}bb.{f}')) {
      plCover = plCover.replace('{w}x{h}bb.{f}', '600x600bb.jpg');
    }

    if (!tracks.length) {
      const isPriv = /private|sign\s*in|login/i.test(body) || !tracks.length;
      res.writeHead(403, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: isPriv ? 'private' : 'restricted', status: 403 }));
    }

    const total = totalCount || tracks.length;
    const hasMore = total > tracks.length;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, name: plTitle || 'Apple Music Playlist', cover: plCover, tracks, hasMore, total }));
  } catch (err) {
    try { logger.error('Apple import failed: ' + inputUrl, err); } catch (_) {}
    const isNet = err.message === 'timeout' || /fetch|network|dns|econnrefused|etimedout/i.test(err.message || '');
    res.writeHead(isNet ? 504 : 500, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: isNet ? 'network' : 'unknown', status: isNet ? 504 : 500 }));
  }
}

const YANDEX_API = 'https://api.music.yandex.net';
const YANDEX_HEADERS = {
  'User-Agent': 'Yandex-Music-API',
  'X-Yandex-Music-Client': 'YandexMusicAndroid/24023231',
  'Accept': 'application/json'
};

function parseYandexLink(input) {
  const text = String(input || '').trim();
  let m = text.match(/users\/([^/?#]+)\/playlists\/(\d+)/i);
  if (m) return { type: 'playlist', path: `/users/${m[1]}/playlists/${m[2]}` };
  m = text.match(/playlists\/((?:lk\.)?[a-f0-9-]{8,})/i);
  if (m) return { type: 'playlist', path: `/playlist/${m[1]}` };
  m = text.match(/album\/(\d+)\/track\/(\d+)/i);
  if (m) return { type: 'track', ids: [`${m[2]}:${m[1]}`] };
  m = text.match(/track\/(\d+)/i);
  if (m) return { type: 'track', ids: [m[1]] };
  m = text.match(/album\/(\d+)/i);
  if (m) return { type: 'album', id: m[1] };
  return null;
}

async function yandexRequest(apiPath, options = {}) {
  let lastErr;
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const response = await fetch(YANDEX_API + apiPath, {
        method: options.method || 'GET',
        headers: Object.assign({}, YANDEX_HEADERS, options.headers || {}),
        body: options.body,
        signal: AbortSignal.timeout(15000)
      });
      if (response.status >= 500 && attempt === 0) {
        await new Promise(r => setTimeout(r, 400));
        continue;
      }
      return response;
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr || new Error('network');
}

async function fetchYandexTracksByIds(ids) {
  const found = new Map();
  for (let i = 0; i < ids.length; i += 100) {
    const body = new URLSearchParams({ 'track-ids': ids.slice(i, i + 100).join(',') });
    const response = await yandexRequest('/tracks', {
      method: 'POST',
      body,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
    if (!response.ok) continue;
    const data = await response.json().catch(() => null);
    const list = data && Array.isArray(data.result) ? data.result : [];
    for (const t of list) {
      if (t && t.id != null) found.set(String(t.id), t);
    }
  }
  return found;
}

function yandexCoverUrl(uri, size = '400x400') {
  if (!uri || typeof uri !== 'string') return '';
  const value = uri.replace('%%', size);
  return /^https?:\/\//i.test(value) ? value : 'https://' + value;
}

function normalizeYandexTrack(t, fallbackAlbum = '') {
  if (!t || !t.title) return null;
  let title = String(t.title).trim();
  const version = typeof t.version === 'string' ? t.version.trim() : '';
  if (version && !title.toLowerCase().includes(version.toLowerCase())) title += ` (${version})`;
  const names = Array.isArray(t.artists) ? t.artists.map(a => a && a.name).filter(Boolean) : [];
  const artist = names.join(', ') || (typeof t.artist === 'string' ? t.artist : '');
  const album = Array.isArray(t.albums) && t.albums[0] ? t.albums[0] : null;
  const cover = yandexCoverUrl(t.ogImage || t.coverUri || (album && album.coverUri) || (typeof t.cover === 'string' ? t.cover : '')) || t.thumbnail || t.image || '';
  const duration = t.durationMs ? Math.round(t.durationMs / 1000) : (t.duration ? Math.round(t.duration) : 0);
  return { title, artist, album: (album && album.title) || fallbackAlbum, duration, cover, thumbnail: cover, image: cover };
}

async function handleYandexImport(req, res, inputUrl) {
  const send = (status, body) => {
    res.writeHead(status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(body));
  };
  const fail = (status) => {
    if (status === 401 || status === 403) return send(403, { success: false, error: 'private', status: 403 });
    if (status === 404) return send(404, { success: false, error: 'not found', status: 404 });
    if (status === 429) return send(429, { success: false, error: 'too many requests', status: 429 });
    return send(status || 502, { success: false, error: 'Failed to fetch from Yandex Music', status: status || 502 });
  };

  const link = parseYandexLink(inputUrl);
  if (!link) return send(400, { success: false, error: 'unsupported link', status: 400 });

  try {
    let name = 'Yandex Music Playlist';
    let cover = '';
    let rawTracks = [];

    if (link.type === 'playlist') {
      const response = await yandexRequest(link.path);
      if (!response.ok) return fail(response.status);
      const data = await response.json();
      const r = data && data.result ? data.result : (data || {});
      name = r.title || r.name || name;
      cover = yandexCoverUrl(r.ogImage || r.coverUrl || (r.cover && r.cover.uri) || (typeof r.cover === 'string' ? r.cover : ''));
      const items = Array.isArray(r.tracks) ? r.tracks : [];
      const resolved = new Array(items.length).fill(null);
      const missing = [];
      items.forEach((item, idx) => {
        const t = item && item.track ? item.track : (item && item.title ? item : null);
        if (t && t.title) resolved[idx] = t;
        else if (item && item.id != null) missing.push({ idx, id: String(item.id), key: item.albumId != null ? `${item.id}:${item.albumId}` : String(item.id) });
      });
      if (missing.length) {
        const found = await fetchYandexTracksByIds(missing.map(m => m.key));
        for (const m of missing) resolved[m.idx] = found.get(m.id) || null;
      }
      rawTracks = resolved;
    } else if (link.type === 'album') {
      const response = await yandexRequest(`/albums/${link.id}/with-tracks`);
      if (!response.ok) return fail(response.status);
      const data = await response.json();
      const r = data && data.result ? data.result : {};
      name = r.title || 'Yandex Music Album';
      cover = yandexCoverUrl(r.ogImage || r.coverUri);
      rawTracks = (Array.isArray(r.volumes) ? r.volumes : []).flat();
      rawTracks = rawTracks.map(t => (t && !t.albums ? Object.assign({}, t, { albums: [{ title: r.title, coverUri: r.coverUri }] }) : t));
    } else {
      const found = await fetchYandexTracksByIds(link.ids);
      rawTracks = Array.from(found.values());
      if (!rawTracks.length) return fail(404);
      const first = normalizeYandexTrack(rawTracks[0]);
      name = first ? first.title : 'Yandex Music Track';
      cover = first ? first.cover : '';
    }

    const tracks = rawTracks.map(t => normalizeYandexTrack(t)).filter(Boolean);
    if (!tracks.length) return fail(404);
    return send(200, { success: true, name, cover, tracks, total: rawTracks.length, skipped: rawTracks.length - tracks.length });
  } catch (err) {
    try { logger.error('Yandex import failed: ' + inputUrl, err); } catch (_) {}
    const isNet = err && (err.name === 'TimeoutError' || err.name === 'AbortError' || /fetch|network|dns|econnrefused|etimedout|timeout/i.test(err.message || ''));
    return send(isNet ? 504 : 500, { success: false, error: isNet ? 'network' : (err.message || 'unknown'), status: isNet ? 504 : 500 });
  }
}

function parseSmartArtistAndTitle(rawTitle, rawArtist) {
  let title = String(rawTitle || '').trim();
  let artist = String(rawArtist || '').replace(/\s*-\s*Topic$/i, '').replace(/\bVEVO$/i, '').trim();

  title = title
    .replace(/\s*[\(\[]\s*(?:official\s*(?:music\s*)?(?:video|audio|lyric\s*video|visualizer)|lyrics?\s*video|lyrics?|visualizer|hd|hq|4k|remastered\s*\d*|mv|clip\s*officiel|официальный\s*клип|премьера\s*клипа|премьера\s*трека)\s*[\)\]]/gi, '')
    .trim();

  const sepMatch = title.match(/^(.+?)\s+(?:[-–—]|\/\/)\s+(.+)$/);
  if (sepMatch) {
    const left = sepMatch[1].trim();
    const right = sepMatch[2].trim();
    const normLeft = left.toLowerCase();
    const normUploader = artist.toLowerCase();
    const isGenericChannel = !normUploader || /unknown|various|music|records|channel|archive|vibes|lyrics|topic|sound|audio|prod|beats|ost|ch\./i.test(normUploader);
    if (left.length >= 2 && left.length <= 60 && right.length >= 2) {
      if (isGenericChannel || normUploader === normLeft || normUploader.includes(normLeft) || !right.toLowerCase().includes(normUploader)) {
        artist = left;
        title = right;
      }
    }
  }

  return {
    title: title || 'untitled',
    artist: artist || 'unknown artist'
  };
}

async function fetchYoutubePlaylistDirect(listId, platform) {
  const result = { name: 'YouTube Playlist', cover: '', tracks: [] };
  try {
    const browseId = listId.startsWith('VL') ? listId : ('VL' + listId);
    const body = {
      context: {
        client: {
          clientName: 'WEB',
          clientVersion: '2.20250101.00.00',
          hl: 'en',
          gl: 'US'
        }
      },
      browseId
    };

    const res = await fetch('https://www.youtube.com/youtubei/v1/browse?prettyPrint=false', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      },
      body: JSON.stringify(body)
    });

    if (!res.ok) return result;
    const json = await res.json();

    function getText(obj) {
      if (!obj) return '';
      if (typeof obj === 'string') return obj;
      if (obj.simpleText) return obj.simpleText;
      if (Array.isArray(obj.runs)) return obj.runs.map(r => r && r.text ? r.text : '').join('');
      return '';
    }

    if (json.header) {
      const phr = json.header.playlistHeaderRenderer;
      if (phr) {
        if (phr.title) result.name = getText(phr.title) || result.name;
        if (phr.playlistHeaderBannerRenderer?.heroImage?.thumbnails) {
          const thumbs = phr.playlistHeaderBannerRenderer.heroImage.thumbnails;
          if (thumbs.length > 0) result.cover = thumbs[thumbs.length - 1].url || '';
        }
      }
      const mdhr = json.header.musicDetailHeaderRenderer;
      if (mdhr) {
        if (mdhr.title) result.name = getText(mdhr.title) || result.name;
        if (mdhr.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails) {
          const thumbs = mdhr.thumbnail.croppedSquareThumbnailRenderer.thumbnail.thumbnails;
          if (thumbs.length > 0) result.cover = thumbs[thumbs.length - 1].url || '';
        }
      }
    }
    if (!result.cover && json.microformat?.microformatDataRenderer?.thumbnail?.thumbnails) {
      const thumbs = json.microformat.microformatDataRenderer.thumbnail.thumbnails;
      if (thumbs.length > 0) result.cover = thumbs[thumbs.length - 1].url || '';
    }
    if (json.microformat?.microformatDataRenderer?.title) {
      result.name = getText(json.microformat.microformatDataRenderer.title) || result.name;
    }

    function parseDurationStr(str) {
      if (!str) return 0;
      const parts = str.trim().split(':').map(Number);
      if (parts.some(isNaN)) return 0;
      if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
      if (parts.length === 2) return parts[0] * 60 + parts[1];
      return parts[0] || 0;
    }

    const seenIds = new Set();

    function walk(node) {
      if (!node || typeof node !== 'object') return;

      if (node.playlistVideoRenderer) {
        const item = node.playlistVideoRenderer;
        const id = item.videoId;
        if (id && !seenIds.has(id)) {
          seenIds.add(id);
          const rawT = getText(item.title) || 'untitled';
          const rawA = getText(item.shortBylineText) || 'unknown artist';
          const smartMeta = parseSmartArtistAndTitle(rawT, rawA);
          let title = smartMeta.title;
          let artist = smartMeta.artist;

          let duration = 0;
          if (item.lengthSeconds) {
            duration = parseInt(item.lengthSeconds, 10) || 0;
          } else if (item.lengthText) {
            duration = parseDurationStr(getText(item.lengthText));
          }

          let thumbnail = `https://i.ytimg.com/vi/${id}/hqdefault.jpg`;
          if (item.thumbnail?.thumbnails?.length) {
            thumbnail = item.thumbnail.thumbnails[item.thumbnail.thumbnails.length - 1].url || thumbnail;
          }

          result.tracks.push({
            id: String(id),
            title,
            artist: artist || 'unknown artist',
            duration,
            thumbnail,
            url: `https://www.youtube.com/watch?v=${id}`,
            platform: platform === 'ytmusic' ? 'ytmusic' : 'youtube',
            isCached: false
          });
        }
      }

      if (node.musicResponsiveListItemRenderer) {
        const item = node.musicResponsiveListItemRenderer;
        const id = item.playlistItemData?.videoId || item.doubleTapCommand?.watchEndpoint?.videoId;
        if (id && !seenIds.has(id)) {
          seenIds.add(id);
          const flex = item.flexColumns || [];
          let rawT = '';
          let rawA = '';
          if (flex[0]?.musicResponsiveListItemFlexColumnRenderer?.text) {
            rawT = getText(flex[0].musicResponsiveListItemFlexColumnRenderer.text);
          }
          if (flex[1]?.musicResponsiveListItemFlexColumnRenderer?.text) {
            const runs = flex[1].musicResponsiveListItemFlexColumnRenderer.text.runs || [];
            if (runs.length > 0) rawA = runs[0].text || '';
          }
          const smartMeta = parseSmartArtistAndTitle(rawT, rawA);
          let title = smartMeta.title;
          let artist = smartMeta.artist;

          let duration = 0;
          const fixed = item.fixedColumns || [];
          if (fixed[0]?.musicResponsiveListItemFixedColumnRenderer?.text) {
            duration = parseDurationStr(getText(fixed[0].musicResponsiveListItemFixedColumnRenderer.text));
          }

          let thumbnail = `https://i.ytimg.com/vi/${id}/hqdefault.jpg`;
          if (item.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.length) {
            const ths = item.thumbnail.musicThumbnailRenderer.thumbnail.thumbnails;
            thumbnail = ths[ths.length - 1].url || thumbnail;
          }

          result.tracks.push({
            id: String(id),
            title,
            artist: artist || 'unknown artist',
            duration,
            thumbnail,
            url: `https://www.youtube.com/watch?v=${id}`,
            platform: platform === 'ytmusic' ? 'ytmusic' : 'youtube',
            isCached: false
          });
        }
      }

      for (const key of Object.keys(node)) {
        walk(node[key]);
      }
    }

    walk(json);
    if (!result.cover && result.tracks.length > 0 && result.tracks[0].thumbnail) {
      result.cover = result.tracks[0].thumbnail;
    }
  } catch (err) {
    try { logger.error('fetchYoutubePlaylistDirect failed: ' + err.message); } catch (_) {}
  }
  return result;
}

async function handlePlaylistImport(req, res, inputUrl) {
  if (typeof inputUrl === 'string' && (inputUrl.includes('on.soundcloud.com') || inputUrl.includes('soundcloud.app.goo.gl') || inputUrl.includes('link.deezer.com'))) {
    try {
      const resp = await fetch(inputUrl, { redirect: 'follow', headers: { 'User-Agent': 'Mozilla/5.0' } });
      if (resp && resp.url) inputUrl = resp.url;
    } catch (_) {}
  }

  let parsed;
  try {
    parsed = new URL(inputUrl);
  } catch (e) {
    res.writeHead(400, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'Unsupported link' }));
  }

  const host = (parsed.hostname || '').toLowerCase();
  const isSoundCloud = host === 'soundcloud.com' || host === 'www.soundcloud.com' || host.endsWith('.soundcloud.com');
  const isYouTube = host === 'youtube.com' || host === 'music.youtube.com' || host === 'www.youtube.com' || host === 'youtu.be' || host.endsWith('.youtube.com');
  const isDeezer = host === 'deezer.com' || host === 'www.deezer.com';

  if (isDeezer) {
    const playlistMatch = parsed.pathname.match(/^\/playlist\/(\d+)(?:\/|$)/i);
    if (!playlistMatch) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'Unsupported Deezer link' }));
    }

    try {
      const playlist = await fetchItunesJson(`https://api.deezer.com/playlist/${playlistMatch[1]}`);
      if (!playlist || playlist.error || !playlist.id) throw new Error('Deezer playlist was not found');
      const items = [];
      let next = playlist.tracks && playlist.tracks.next;
      if (playlist.tracks && Array.isArray(playlist.tracks.data)) items.push(...playlist.tracks.data);
      let pages = 0;
      while (next && pages < 100) {
        const page = await fetchItunesJson(next);
        if (page && Array.isArray(page.data)) items.push(...page.data);
        next = page && page.next;
        pages++;
      }

      const tracks = items.filter(track => track && track.id && track.title && track.artist && track.artist.name)
        .map(track => ({
          id: String(track.id),
          title: String(track.title),
          artist: String(track.artist.name),
          duration: Number(track.duration) || 0,
          thumbnail: track.album && (track.album.cover_xl || track.album.cover_big || track.album.cover) || '',
          url: track.link || '',
          platform: 'deezer',
          isExplicit: Boolean(track.explicit_lyrics)
        }));

      if (!tracks.length) throw new Error('No tracks found in this Deezer playlist');
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        success: true,
        name: playlist.title || 'Deezer Playlist',
        cover: playlist.picture_xl || playlist.picture_big || playlist.picture || '',
        tracks
      }));
    } catch (error) {
      try { logger.error('Deezer playlist import failed: ' + error.message); } catch (_) {}
      res.writeHead(502, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: error.message || 'Deezer playlist import failed' }));
    }
  }

  if (!isSoundCloud && !isYouTube) {
    res.writeHead(400, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'Unsupported link' }));
  }

  if (isSoundCloud && !inputUrl.includes('/s-')) {
    try {
      const cleanUrlObj = new URL(inputUrl);
      const toDelete = [];
      for (const key of cleanUrlObj.searchParams.keys()) {
        if (key === 'si' || key.toLowerCase().startsWith('utm_')) {
          toDelete.push(key);
        }
      }
      for (const k of toDelete) cleanUrlObj.searchParams.delete(k);
      inputUrl = cleanUrlObj.toString();
      parsed = new URL(inputUrl);
    } catch (_) {}
  }

  const platform = isSoundCloud ? 'soundcloud' : (host === 'music.youtube.com' ? 'ytmusic' : 'youtube');
  const listId = parsed.searchParams.get('list') || '';

  if (isSoundCloud && /\/likes\/?(?:[?#]|$)/i.test(inputUrl)) {
    try {
      let cid = await refreshSoundCloudClientId(false);
      let user = null;

      try {
        const pageRes = await fetch(inputUrl, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/131.0.0.0 Safari/537.36' } });
        if (pageRes.ok) {
          const pageHtml = await pageRes.text();
          const hyd = pageHtml.match(/window\.__sc_hydration\s*=\s*(\[[\s\S]*?\]);\s*<\/script>/);
          if (hyd) {
            const list = JSON.parse(hyd[1]);
            user = list.find(x => x.hydratable === 'user')?.data;
          }
        }
      } catch (_) {}

      if (!user) {
        const cleanUserUrl = inputUrl.replace(/\/likes\/?(?:[?#].*)?$/i, '');
        let resolveUrl = `https://api-v2.soundcloud.com/resolve?url=${encodeURIComponent(cleanUserUrl)}&client_id=${cid}`;
        let resolveRes = await fetch(resolveUrl, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        if (resolveRes.status === 401 || resolveRes.status === 403) {
          cid = await refreshSoundCloudClientId(true);
          resolveUrl = `https://api-v2.soundcloud.com/resolve?url=${encodeURIComponent(cleanUserUrl)}&client_id=${cid}`;
          resolveRes = await fetch(resolveUrl, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        }
        if (resolveRes.ok) {
          user = await resolveRes.json();
        }
      }

      const userId = user && (user.id || user.urn?.split(':').pop());
      if (userId) {
        const userName = user.username || user.name || 'User';
        const cover = user.avatar_url || (user.visuals && user.visuals.visuals?.[0]?.visual_url) || '';
        let nextUrl = `https://api-v2.soundcloud.com/users/${userId}/likes?limit=200&client_id=${cid}`;
        let pageCount = 0;
        const allItems = [];

        while (nextUrl && pageCount < 25) {
          pageCount++;
          const fetchUrl = nextUrl.includes('client_id=') ? nextUrl : `${nextUrl}&client_id=${cid}`;
          let lRes = await fetch(fetchUrl, { headers: { 'User-Agent': 'Mozilla/5.0' } });
          if (lRes.status === 401 || lRes.status === 403) {
            cid = await refreshSoundCloudClientId(true);
            const retryUrl = fetchUrl.replace(/client_id=[^&]+/, `client_id=${cid}`);
            lRes = await fetch(retryUrl, { headers: { 'User-Agent': 'Mozilla/5.0' } });
          }
          if (!lRes.ok) break;
          const lData = await lRes.json();
          if (Array.isArray(lData.collection)) {
            allItems.push(...lData.collection);
          }
          nextUrl = lData.next_href || null;
        }

        const tracks = [];
        for (const item of allItems) {
          const t = item?.track || (item?.title ? item : null);
          if (!t || !t.id || !t.title) continue;
          const smartMeta = parseSmartArtistAndTitle(t.title || 'untitled', (t.user && (t.user.username || t.user.name)) || 'unknown artist');
          tracks.push({
            id: String(t.id),
            title: smartMeta.title,
            artist: smartMeta.artist,
            duration: Math.round((t.duration || t.full_duration || 0) / 1000),
            thumbnail: t.artwork_url || (t.user && t.user.avatar_url) || '',
            url: t.permalink_url || '',
            platform: 'soundcloud',
            isCached: false
          });
        }


        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          success: true,
          name: userName + ' - Likes',
          cover,
          tracks
        }));
      }
    } catch (scLikesErr) {
      try { logger.error('SoundCloud likes import failed: ' + scLikesErr.message); } catch (_) {}
    }
  }

  if (isSoundCloud && inputUrl.includes('/sets/')) {
    try {
      const secretMatch = inputUrl.match(/\/(s-[a-zA-Z0-9_-]+)/);
      const secretToken = secretMatch ? secretMatch[1] : '';
      let cid = await refreshSoundCloudClientId(false);
      let pl = null;

      try {
        const page = await fetch(inputUrl, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/131.0.0.0 Safari/537.36' } }).then(r => r.text());
        const hyd = page.match(/window\.__sc_hydration\s*=\s*(\[[\s\S]*?\]);\s*<\/script>/);
        if (hyd) {
          const list = JSON.parse(hyd[1]);
          pl = list.find(x => x.hydratable === 'playlist')?.data;
          if (!pl) {
            for (const item of list) {
              if (item.data && (item.data.kind === 'playlist' || Array.isArray(item.data.tracks))) {
                pl = item.data;
                break;
              }
            }
          }
        }
      } catch (_) {}

      if (!pl) {
        let resolveUrl = `https://api-v2.soundcloud.com/resolve?url=${encodeURIComponent(inputUrl)}&client_id=${cid}${secretToken ? '&secret_token=' + secretToken : ''}`;
        let resolveRes = await fetch(resolveUrl, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        if (resolveRes.status === 401 || resolveRes.status === 403) {
          cid = await refreshSoundCloudClientId(true);
          resolveUrl = `https://api-v2.soundcloud.com/resolve?url=${encodeURIComponent(inputUrl)}&client_id=${cid}${secretToken ? '&secret_token=' + secretToken : ''}`;
          resolveRes = await fetch(resolveUrl, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        }
        if (resolveRes.ok) {
          pl = await resolveRes.json();
        }
      }

      if (pl && Array.isArray(pl.tracks)) {
        const tracksById = new Map();
        for (const t of pl.tracks) {
          if (t && t.title) tracksById.set(t.id, t);
        }
        const missing = pl.tracks.filter(t => t && !t.title).map(t => t.id);
        for (let i = 0; i < missing.length; i += 50) {
          const chunk = missing.slice(i, i + 50);
          try {
            let u = `https://api-v2.soundcloud.com/tracks?ids=${chunk.join(',')}&client_id=${cid}${secretToken ? '&secret_token=' + secretToken : ''}`;
            let tRes = await fetch(u, { headers: { 'User-Agent': 'Mozilla/5.0' } });
            if (tRes.status === 401 || tRes.status === 403) {
              cid = await refreshSoundCloudClientId(true);
              u = `https://api-v2.soundcloud.com/tracks?ids=${chunk.join(',')}&client_id=${cid}${secretToken ? '&secret_token=' + secretToken : ''}`;
              tRes = await fetch(u, { headers: { 'User-Agent': 'Mozilla/5.0' } });
            }
            if (tRes.ok) {
              const items = await tRes.json();
              if (Array.isArray(items)) {
                for (const it of items) {
                  if (it && it.id) tracksById.set(it.id, it);
                }
              }
            }
          } catch (_) {}
        }
        const full = pl.tracks.map(t => (t && t.id ? (tracksById.get(t.id) || t) : t)).filter(t => t && t.title);
        const tracks = full.map(t => {
          const smartMeta = parseSmartArtistAndTitle(t.title || 'untitled', (t.user && (t.user.username || t.user.name)) || 'unknown artist');
          return {
            id: String(t.id),
            title: smartMeta.title,
            artist: smartMeta.artist,
            duration: Math.round((t.duration || t.full_duration || 0) / 1000),
            thumbnail: t.artwork_url || (t.user && t.user.avatar_url) || '',
            url: t.permalink_url || '',
            platform: 'soundcloud',
            isCached: false
          };
        });

        const plCover = pl.artwork_url || '';
        if (tracks.length > 0) {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({
            success: true,
            name: pl.title || 'SoundCloud Playlist',
            cover: plCover,
            tracks
          }));
        }
      }
    } catch (_) {}
  }
  const targetUrl = (isYouTube && listId) ? ('https://www.youtube.com/playlist?list=' + encodeURIComponent(listId)) : inputUrl;
  const args = [
    '--dump-single-json',
    '--ignore-errors',
    '--no-warnings',
    '--no-check-certificates',
    '--playlist-end',
    '5000'
  ];
  if (isYouTube) {
    args.unshift('--flat-playlist');
    args.push('--extractor-args', YT_EXTRACTOR_ARGS);
  }
  args.push(targetUrl);

  try {
    let raw = await executeYtDlp(args, 60000);
    let data = JSON.parse(raw);
    let entries = Array.isArray(data.entries) ? data.entries : [data];

    if (entries.length > 0 && !entries[0].title && args.includes('--flat-playlist')) {
      const deepArgs = args.filter(a => a !== '--flat-playlist');
      const deepRaw = await executeYtDlp(deepArgs, 60000);
      data = JSON.parse(deepRaw);
      entries = Array.isArray(data.entries) ? data.entries : [data];
    }

    const tracks = [];

    for (const item of entries) {
      if (!item || !item.id) continue;
      if (item.title === '[Private video]' || item.title === '[Deleted video]') continue;

      const trackUrl = isYouTube
        ? `https://www.youtube.com/watch?v=${item.id}`
        : (item.webpage_url || item.url);
      if (!trackUrl) continue;

      let rawTitleVal = (item.title || item.track || item.fulltitle || item.name || '').trim();
      let rawArtistVal = (item.artist || (Array.isArray(item.artists) ? item.artists.join(', ') : '') || item.creator || item.channel || item.uploader || item.album_artist || '').trim();
      const smartMeta = parseSmartArtistAndTitle(rawTitleVal, rawArtistVal);
      let titleVal = smartMeta.title;
      let artistVal = smartMeta.artist;

      let thumbnail = '';
      if (isYouTube) {
        thumbnail = `https://i.ytimg.com/vi/${item.id}/hqdefault.jpg`;
      } else {
        if (Array.isArray(item.thumbnails) && item.thumbnails.length > 0) {
          const t = item.thumbnails.find(x => x.id === 't500x500' || x.id === 'large' || x.id === 'crop' || x.id === 'original') || item.thumbnails[item.thumbnails.length - 1];
          thumbnail = (t && t.url) ? t.url : '';
        }
        if (!thumbnail) thumbnail = item.thumbnail || item.artwork_url || item.avatar_url || '';
      }

      tracks.push({
        id: String(item.id),
        title: titleVal,
        artist: artistVal,
        duration: Math.round(item.duration || 0),
        thumbnail,
        url: trackUrl,
        platform,
        isCached: false
      });
    }

    if (!tracks.length) {
      if (isYouTube && listId) {
        const fallbackPl = await fetchYoutubePlaylistDirect(listId, platform);
        if (fallbackPl && fallbackPl.tracks && fallbackPl.tracks.length > 0) {
          const fbCover = fallbackPl.cover || '';
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({
            success: true,
            name: fallbackPl.name || data.title || 'YouTube Playlist',
            cover: fbCover,
            tracks: fallbackPl.tracks
          }));
        }
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'No tracks found' }));
    }

    let cover = data.coverUrl || data.ogImage || '';
    if (!cover && Array.isArray(data.thumbnails) && data.thumbnails.length > 0) {
      const last = data.thumbnails[data.thumbnails.length - 1];
      cover = (last && last.url) ? last.url : (typeof last === 'string' ? last : '');
    }
    if (!cover && data.thumbnail) {
      cover = typeof data.thumbnail === 'string' ? data.thumbnail : (data.thumbnail.url || '');
    }
    if (!cover && tracks.length === 1 && tracks[0] && tracks[0].thumbnail) {
      cover = tracks[0].thumbnail;
    }
    if (!cover) cover = '';

    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, name: data.title || 'Imported playlist', cover, tracks }));
  } catch (err) {
    if (isYouTube && listId) {
      try {
        const fallbackPl = await fetchYoutubePlaylistDirect(listId, platform);
        if (fallbackPl && fallbackPl.tracks && fallbackPl.tracks.length > 0) {
          const fbCover = fallbackPl.cover || '';
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({
            success: true,
            name: fallbackPl.name || 'YouTube Playlist',
            cover: fbCover,
            tracks: fallbackPl.tracks
          }));
        }
      } catch (_) {}
    }
    try { logger.error('Playlist import failed: ' + inputUrl, err); } catch (_) {}
    res.writeHead(500, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: err.message }));
  }
}

const SPOTIFY_SCOPES = 'user-library-read playlist-read-private playlist-read-collaborative';
let spotifyAuth = { verifier: '', state: '', clientId: '', token: '', refreshToken: '', expiresAt: 0 };
let spotifyAuthError = '';
let spotifyAuthLoaded = false;

let spotifyRefreshPromise = null;
const spotifyAuthPath = path.join(DATA_DIR, 'spotify-auth.enc');
const spotifySafeStorage = process.versions.electron ? require('electron').safeStorage : null;

function canStoreSpotifyAuth() {
  return spotifySafeStorage?.isEncryptionAvailable() &&
    (process.platform !== 'linux' || spotifySafeStorage.getSelectedStorageBackend() !== 'basic_text');
}

function loadSpotifyAuth() {
  if (spotifyAuthLoaded) return;
  if (!canStoreSpotifyAuth()) return;
  spotifyAuthLoaded = true;
  try {
    const saved = JSON.parse(spotifySafeStorage.decryptString(fs.readFileSync(spotifyAuthPath)));
    if (/^[a-f0-9]{32}$/i.test(saved.clientId) && typeof saved.refreshToken === 'string' && saved.refreshToken) {
      spotifyAuth = { ...spotifyAuth, clientId: saved.clientId, refreshToken: saved.refreshToken };
    }
  } catch (e) {
    if (e.code !== 'ENOENT') spotifyAuthError = 'Stored Spotify login could not be read. Please connect again.';
  }
}

function saveSpotifyAuth() {
  if (!canStoreSpotifyAuth()) return false;
  const tmp = `${spotifyAuthPath}.tmp`;
  try {
    fs.mkdirSync(DATA_DIR, { recursive: true });
    fs.writeFileSync(tmp, spotifySafeStorage.encryptString(JSON.stringify({ clientId: spotifyAuth.clientId, refreshToken: spotifyAuth.refreshToken })), { mode: 0o600 });
    fs.renameSync(tmp, spotifyAuthPath);
    return true;
  } catch (e) {
    try { fs.rmSync(tmp, { force: true }); } catch (_) {}
    return false;
  }
}

function clearStoredSpotifyAuth() {
  fs.rmSync(spotifyAuthPath, { force: true });
}

function getSpotifyRedirectUri() {
  const a = server.address();
  const port = (a && typeof a === 'object' && a.port) ? a.port : PORT;
  return `http://127.0.0.1:${port}/api/spotify-callback`;
}

function requestSpotifyToken(params) {
  return new Promise((resolve, reject) => {
    const postData = new URLSearchParams(params).toString();

    const req = https.request('https://accounts.spotify.com/api/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(postData)
      },
      timeout: 15000
    }, (resp) => {
      let body = '';
      resp.on('data', c => body += c);
      resp.on('end', () => {
        try {
          const data = JSON.parse(body);
          resolve({ status: resp.statusCode, data });
        } catch (e) {
          resolve({ status: resp.statusCode, error: e });
        }
      });
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('Token exchange timed out')); });
    req.write(postData);
    req.end();
  });
}

async function getSpotifyAccessToken(forceRefresh = false) {
  loadSpotifyAuth();
  if (!forceRefresh && spotifyAuth.token && (!spotifyAuth.expiresAt || Date.now() < spotifyAuth.expiresAt - 60000)) return spotifyAuth.token;
  if (!spotifyAuth.refreshToken) throw new Error('Spotify session expired. Connect your account again.');
  if (!spotifyRefreshPromise) {
    spotifyRefreshPromise = (async () => {
      const session = spotifyAuth;
      let result;
      for (let attempt = 0; attempt < 3; attempt++) {
        try {
          result = await requestSpotifyToken({ grant_type: 'refresh_token', refresh_token: session.refreshToken, client_id: session.clientId });
        } catch (err) {
          result = undefined;
          if (attempt === 2) throw err;
        }
        if (result && result.status !== 429 && result.status < 500) break;
        if (attempt < 2) await new Promise(r => setTimeout(r, 250 * (attempt + 1)));
      }
      if (spotifyAuth !== session) throw new Error('Spotify session changed. Try again.');
      if (result.data?.error === 'invalid_grant') {
        spotifyAuth.token = '';
        spotifyAuth.refreshToken = '';
        spotifyAuth.expiresAt = 0;
        clearStoredSpotifyAuth();
        throw new Error('Spotify login expired. Connect your account again.');
      }
      if (result.status !== 200 || typeof result.data?.access_token !== 'string' || !result.data.access_token || !Number.isFinite(result.data.expires_in) || result.data.expires_in <= 0) {
        throw new Error(`Spotify token refresh failed (HTTP ${result.status}). Try again later.`);
      }
      spotifyAuth.token = result.data.access_token;
      spotifyAuth.expiresAt = Date.now() + result.data.expires_in * 1000;
      spotifyAuthError = '';
      if (typeof result.data.refresh_token === 'string' && result.data.refresh_token) {
        spotifyAuth.refreshToken = result.data.refresh_token;
        if (!saveSpotifyAuth()) spotifyAuthError = 'Spotify login could not be saved securely; reconnect after restarting Riff.';
      }
      return spotifyAuth.token;
    })().finally(() => { spotifyRefreshPromise = null; });
  }
  return spotifyRefreshPromise;
}

function spotifyApiGet(requestUrl, token) {
  return new Promise((resolve, reject) => {
    const u = new URL(requestUrl);
    if (u.protocol !== 'https:' || u.hostname !== 'api.spotify.com') return reject(new Error('Invalid Spotify API URL'));
    const req = https.get(u, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      },
      timeout: 15000
    }, (resp) => {
      let body = '';
      resp.on('data', c => body += c);
      resp.on('end', () => {
        resolve({ status: resp.statusCode, headers: resp.headers, body });
      });
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('Spotify API timed out')); });
  });
}

async function spotifyGetWithRetry(requestUrl) {
  let token = await getSpotifyAccessToken();
  let resp = await spotifyApiGet(requestUrl, token);
  if (resp.status === 401 && spotifyAuth.refreshToken) {
    token = await getSpotifyAccessToken(true);
    resp = await spotifyApiGet(requestUrl, token);
  }
  for (let attempt = 0; resp.status === 429 && attempt < 3; attempt++) {
    const retryAfter = spotifyRetryAfterSeconds(resp);
    if (retryAfter > 30) return resp;
    await new Promise(r => setTimeout(r, retryAfter * 1000));
    resp = await spotifyApiGet(requestUrl, token);
  }
  return resp;
}

function spotifyRetryAfterSeconds(resp) {
  const raw = resp.headers?.['retry-after'];
  const seconds = Number(raw);
  if (raw !== undefined && Number.isFinite(seconds) && seconds >= 0) return seconds;
  const date = Date.parse(raw);
  return Number.isFinite(date) ? Math.max(0, (date - Date.now()) / 1000) : 1;
}

function spotifyApiFailure(resp, endpoint) {
  let detail = '';
  try {
    const error = JSON.parse(resp.body).error;
    detail = typeof error === 'string' ? error : (error?.message || '');
  } catch (e) {}
  const failure = new Error(resp.status === 429
    ? 'Spotify rate limited the request. Try again later.'
    : `Spotify ${endpoint} returned HTTP ${resp.status}${detail ? `: ${detail}` : ''}`);
  failure.status = resp.status;
  return failure;
}

async function handleSpotifyLibrary(req, res, query) {
  loadSpotifyAuth();
  if (!spotifyAuth.token && !spotifyAuth.refreshToken) {
    res.writeHead(401, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'Unauthorized' }));
  }

  const wantLikes = query.likes === '1' || query.likes === 'true';
  const wantPlaylists = query.playlists === '1' || query.playlists === 'true';

  let likes = [];
  let playlists = [];
  const skippedPlaylists = [];

  try {
    if (wantLikes) {
      let nextUrl = 'https://api.spotify.com/v1/me/tracks?limit=50';
      while (nextUrl && likes.length < 5000) {
        const resp = await spotifyGetWithRetry(nextUrl);
        if (resp.status < 200 || resp.status >= 300) throw spotifyApiFailure(resp, 'saved tracks');
        const data = JSON.parse(resp.body);
        if (!Array.isArray(data.items)) throw new Error('Invalid Spotify saved tracks response');
        const items = data.items;
        for (const it of items) {
          const t = it?.item || it?.track;
          if (!t || t.is_local || t.type === 'episode') continue;
          const artist = (Array.isArray(t.artists) && t.artists[0]?.name) || '';
          const title = t.name || '';
          const duration = t.duration_ms ? Math.round(t.duration_ms / 1000) : undefined;
          if (title) likes.push({ title, artist, duration });
          if (likes.length >= 5000) break;
        }
        nextUrl = data.next || null;
      }
    }

    if (wantPlaylists) {
      let userPlaylists = [];
      let plNext = 'https://api.spotify.com/v1/me/playlists?limit=50';
      while (plNext) {
        const resp = await spotifyGetWithRetry(plNext);
        if (resp.status < 200 || resp.status >= 300) throw spotifyApiFailure(resp, 'playlists');
        const data = JSON.parse(resp.body);
        if (!Array.isArray(data.items)) throw new Error('Invalid Spotify playlists response');
        const items = data.items;
        for (const pl of items) {
          if (pl && pl.id && pl.name) {
            userPlaylists.push({ id: pl.id, name: pl.name });
          }
        }
        plNext = data.next || null;
      }

      playlistLoop: for (const pl of userPlaylists) {
        const plTracks = [];
        let itemUrl = `https://api.spotify.com/v1/playlists/${encodeURIComponent(pl.id)}/items?limit=50`;
        let resp = await spotifyGetWithRetry(itemUrl);
        if (resp.status === 404 || resp.status === 400) {
          itemUrl = `https://api.spotify.com/v1/playlists/${encodeURIComponent(pl.id)}/tracks?limit=50`;
          resp = await spotifyGetWithRetry(itemUrl);
        }

        if (resp.status === 403) {
          skippedPlaylists.push({ name: pl.name, error: spotifyApiFailure(resp, `playlist items (${pl.name})`).message });
          continue;
        }
        if (resp.status < 200 || resp.status >= 300) throw spotifyApiFailure(resp, `playlist items (${pl.name})`);

        const pageData = JSON.parse(resp.body);
        if (!Array.isArray(pageData.items)) throw new Error(`Invalid Spotify playlist items (${pl.name}) response`);
        const pageItems = pageData.items;
        for (const it of pageItems) {
          const t = it?.item || it?.track;
          if (!t || t.is_local || t.type === 'episode') continue;
          const artist = (Array.isArray(t.artists) && t.artists[0]?.name) || '';
          const title = t.name || '';
          const duration = t.duration_ms ? Math.round(t.duration_ms / 1000) : undefined;
          if (title) plTracks.push({ title, artist, duration });
        }
        let trackNext = pageData.next || null;
        while (trackNext) {
          const nextResp = await spotifyGetWithRetry(trackNext);
          if (nextResp.status === 403) {
            skippedPlaylists.push({ name: pl.name, error: spotifyApiFailure(nextResp, `playlist items (${pl.name})`).message });
            continue playlistLoop;
          }
          if (nextResp.status < 200 || nextResp.status >= 300) throw spotifyApiFailure(nextResp, `playlist items (${pl.name})`);
          const nextData = JSON.parse(nextResp.body);
          if (!Array.isArray(nextData.items)) throw new Error(`Invalid Spotify playlist items (${pl.name}) response`);
          const nextItems = nextData.items;
          for (const it of nextItems) {
            const t = it?.item || it?.track;
            if (!t || t.is_local || t.type === 'episode') continue;
            const artist = (Array.isArray(t.artists) && t.artists[0]?.name) || '';
            const title = t.name || '';
            const duration = t.duration_ms ? Math.round(t.duration_ms / 1000) : undefined;
            if (title) plTracks.push({ title, artist, duration });
          }
          trackNext = nextData.next || null;
        }
        playlists.push({ name: pl.name, tracks: plTracks });
      }
    }

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true, partial: skippedPlaylists.length > 0, likes, playlists, skippedPlaylists }));
  } catch (err) {
    res.writeHead(err.status === 403 ? 403 : err.status === 429 ? 429 : 500, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: false, error: err.message }));
  }
}

const DEEPL_LANG_MAP = {
  'en': 'EN', 'ru': 'RU', 'uk': 'UK', 'de': 'DE', 'fr': 'FR',
  'es': 'ES', 'it': 'IT', 'pt': 'PT', 'pl': 'PL', 'nl': 'NL',
  'ja': 'JA', 'zh-CN': 'ZH', 'zh-TW': 'ZH', 'ko': 'KO', 'tr': 'TR',
  'sv': 'SV', 'da': 'DA', 'fi': 'FI', 'cs': 'CS', 'ro': 'RO',
  'hu': 'HU', 'el': 'EL', 'bg': 'BG', 'id': 'ID'
};

async function queryOfficialDeepLApi(text, targetLang, key) {
  const isFree = key.endsWith(':fx');
  const host = isFree ? 'api-free.deepl.com' : 'api.deepl.com';
  const postData = JSON.stringify({
    text: text.split('\n'),
    target_lang: targetLang,
    preserve_formatting: true
  });
  return new Promise((resolve, reject) => {
    const req = https.request({
      hostname: host,
      path: '/v2/translate',
      method: 'POST',
      headers: {
        'Authorization': `DeepL-Auth-Key ${key}`,
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      },
      timeout: 9000
    }, (res) => {
      let b = '';
      res.on('data', chunk => b += chunk);
      res.on('end', () => {
        if (res.statusCode !== 200) return reject(new Error(`DeepL API HTTP ${res.statusCode}`));
        try {
          const json = JSON.parse(b);
          if (Array.isArray(json?.translations)) {
            resolve(json.translations.map(t => t.text));
          } else reject(new Error('Invalid DeepL response'));
        } catch (e) { reject(e); }
      });
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('DeepL timeout')); });
    req.write(postData);
    req.end();
  });
}

async function queryDeepLXInstance(text, targetLang) {
  const instances = [
    'https://deeplx.mingming.dev/translate',
    'https://api.deeplx.org/translate',
    'https://deepl.tr1ck.top/translate'
  ];
  for (const endpoint of instances) {
    try {
      const resp = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text, source_lang: 'auto', target_lang: targetLang }),
        signal: AbortSignal.timeout(5000)
      });
      if (resp.ok) {
        const json = await resp.json();
        const resText = json?.data || json?.text || (json?.translations && json.translations[0]?.text);
        if (typeof resText === 'string' && resText.trim()) {
          return resText.split('\n');
        }
      }
    } catch (_) {}
  }
  return null;
}

function queryGoogleTranslateFallback(text, to) {
  return new Promise((resolve) => {
    if (!text || !to) return resolve(null);
    const cleanTo = String(to).split(/[-_]/)[0];
    const postData = 'q=' + encodeURIComponent(text);
    const req = https.request({
      hostname: 'translate.googleapis.com',
      path: `/translate_a/single?client=gtx&sl=auto&tl=${encodeURIComponent(cleanTo)}&dt=t`,
      method: 'POST',
      family: 4,
      agent: keepAliveHttpsAgent,
      timeout: 6000,
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
        'Content-Length': Buffer.byteLength(postData),
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/131.0.0.0 Safari/537.36'
      }
    }, (res) => {
      let b = '';
      res.setEncoding('utf8');
      res.on('data', chunk => { b += chunk; });
      res.on('end', () => {
        if (res.statusCode !== 200) return resolve(null);
        try {
          const data = JSON.parse(b);
          if (data && Array.isArray(data[0])) {
            const out = data[0].map(s => s[0] || '').join('');
            return resolve(out.split('\n').map(s => s.trim()));
          }
        } catch (_) {}
        resolve(null);
      });
    });
    req.on('timeout', () => { req.destroy(); resolve(null); });
    req.on('error', () => resolve(null));
    req.write(postData);
    req.end();
  });
}

async function callDeepLTranslate(text, toLang, apiKey = '') {
  if (!text || typeof text !== 'string') return null;
  const safeText = text.replace(/\bRiff\b/g, '__RIFF_BRAND__');
  if (!DEEPL_LANG_MAP[toLang]) {
    const googleResult = await queryGoogleTranslateFallback(safeText, toLang);
    const gArr = Array.isArray(googleResult) ? googleResult : (typeof googleResult === 'string' ? googleResult.split('\n') : null);
    if (gArr) {
      return gArr
        .map(str => str.replace(/__RIFF_BRAND__/g, 'Riff').replace(/__RIFF_APP__/g, 'Riff'))
        .filter(str => str && !str.includes('linux.do') && !/https?:\/\//i.test(str));
    }
    return null;
  }
  const targetCode = DEEPL_LANG_MAP[toLang];

  let results = null;
  if (apiKey && apiKey.trim()) {
    try {
      results = await queryOfficialDeepLApi(safeText, targetCode, apiKey.trim());
    } catch (err) {
      console.warn('[DeepL] Official API failed, falling back to Google Translate:', err.message);
      results = await queryGoogleTranslateFallback(safeText, toLang);
    }
  } else {
    results = await queryGoogleTranslateFallback(safeText, toLang);
  }

  if (!results) {
    results = await queryGoogleTranslateFallback(safeText, toLang);
  }

  const resArr = Array.isArray(results) ? results : (typeof results === 'string' ? results.split('\n') : null);
  if (resArr) {
    return resArr
      .map(str => str.replace(/__RIFF_BRAND__/g, 'Riff').replace(/__RIFF_APP__/g, 'Riff'))
      .filter(str => str && !str.includes('linux.do') && !/https?:\/\//i.test(str));
  }
  return null;
}

async function handleTranslate(req, res, lines, to = 'en', apiKey = '') {
  if (!Array.isArray(lines) || lines.length === 0) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, translations: [] }));
  }

  const chunks = [];
  let curChunk = [];
  let curLen = 0;
  for (const line of lines) {
    if (curChunk.length > 0 && curLen + line.length + 1 > 1800) {
      chunks.push(curChunk);
      curChunk = [];
      curLen = 0;
    }
    curChunk.push(line);
    curLen += line.length + 1;
  }
  if (curChunk.length > 0) chunks.push(curChunk);

  const allTranslations = [];
  for (const chunk of chunks) {
    const joined = chunk.join('\n');
    let chunkTrans = await callDeepLTranslate(joined, to, apiKey);
    if (!chunkTrans || chunkTrans.length !== chunk.length) {
      chunkTrans = [];
      for (const line of chunk) {
        if (!line.trim()) {
          chunkTrans.push('');
          continue;
        }
        const single = await callDeepLTranslate(line, to, apiKey);
        chunkTrans.push((single && single[0]) ? single[0] : line);
      }
    }
    allTranslations.push(...chunkTrans);
  }

  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ success: true, translations: allTranslations }));
}

const albumCache = new Map();

function getAlbumCache(key) {
  const entry = albumCache.get(key);
  if (!entry) return null;
  if (Date.now() > entry.expires) {
    albumCache.delete(key);
    return null;
  }
  return entry.data;
}

function setAlbumCache(key, data) {
  if (albumCache.has(key)) {
    albumCache.delete(key);
  } else if (albumCache.size >= 100) {
    const oldestKey = albumCache.keys().next().value;
    albumCache.delete(oldestKey);
  }
  albumCache.set(key, { data, expires: Date.now() + 15 * 60 * 1000 });
}

function fetchItunesJson(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: { 'User-Agent': 'Mozilla/5.0' },
      timeout: 10000
    }, (res) => {
      if (res.statusCode < 200 || res.statusCode >= 300) {
        res.resume();
        return reject(new Error(`HTTP ${res.statusCode}`));
      }
      let raw = '';
      res.on('data', c => raw += c);
      res.on('end', () => {
        try {
          resolve(JSON.parse(raw));
        } catch (e) {
          reject(e);
        }
      });
    });
    req.on('timeout', () => {
      req.destroy();
      reject(new Error('timeout'));
    });
    req.on('error', err => reject(err));
  });
}

async function handleAlbumSearch(req, res, parsedUrl) {
  const q = (parsedUrl.query.q || '').trim();

  if (!q) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, albums: [] }));
  }

  const cacheKey = `search:${q.toLowerCase()}`;
  const cached = getAlbumCache(cacheKey);
  if (cached) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(cached));
  }

  const inFlight = albumCache.get(cacheKey);
  if (inFlight?.promise) {
    const outcome = await inFlight.promise;
    res.writeHead(outcome.status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(outcome.body));
  }

  let resolveSearch;
  const searchPromise = new Promise(resolve => { resolveSearch = resolve; });
  const inFlightEntry = { promise: searchPromise };
  albumCache.set(cacheKey, inFlightEntry);
  let outcome = null;
  const sendResponse = (status, body) => {
    outcome = { status, body };
    resolveSearch(outcome);
    res.writeHead(status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(body));
  };

  try {
    const albumUrlMatch = q.match(/music\.apple\.com\/(?:[a-z]{2}\/)?album\/(?:[^/]+\/)?(\d+)/i);
    if (albumUrlMatch) {
      const data = await fetchItunesJson(`https://itunes.apple.com/lookup?id=${albumUrlMatch[1]}&entity=album`);
      const results = (data && Array.isArray(data.results)) ? data.results : [];
      const item = results.find(r => r && r.wrapperType === 'collection');
      if (item) {
        const responseData = {
          success: true,
          albums: [{
            id: String(item.collectionId),
            title: item.collectionName || '',
            artist: item.artistName || '',
            year: item.releaseDate ? item.releaseDate.slice(0, 4) : '',
            trackCount: item.trackCount || 0,
            cover: (item.artworkUrl100 || '').replace('100x100', '600x600')
          }]
        };
        setAlbumCache(cacheKey, responseData);
        return sendResponse(200, responseData);
      }
    }

    const cleanQ = q.replace(/\s+-\s+/g, ' ').replace(/\bby\b/gi, ' ').replace(/\s+/g, ' ').trim();
    const urls = [
      `https://itunes.apple.com/search?term=${encodeURIComponent(q)}&entity=album&media=music&limit=25`,
      `https://itunes.apple.com/search?term=${encodeURIComponent(q)}&media=music&limit=25`
    ];
    if (cleanQ && cleanQ.toLowerCase() !== q.toLowerCase()) {
      urls.push(`https://itunes.apple.com/search?term=${encodeURIComponent(cleanQ)}&entity=album&media=music&limit=25`);
      urls.push(`https://itunes.apple.com/search?term=${encodeURIComponent(cleanQ)}&media=music&limit=25`);
    }

    const responses = await Promise.all(urls.map(u => fetchItunesJson(u).catch(() => null)));
    const results = [];
    for (const data of responses) {
      if (data && Array.isArray(data.results)) {
        results.push(...data.results);
      }
    }

    const albums = [];
    const seen = new Set();
    const normalize = (s) => (s || '').toLowerCase().replace(/[^\p{L}\p{N}]/gu, '');

    for (const item of results) {
      if (!item || item.collectionId == null) continue;
      const name = item.collectionName || '';
      if (/\s-\s*Single$/i.test(name)) continue;
      const trackCount = item.trackCount || 0;
      if (trackCount < 4 && !/\s-\s*EP$/i.test(name)) continue;

      const normKey = `${normalize(name)}:${normalize(item.artistName)}`;
      if (seen.has(normKey)) continue;
      seen.add(normKey);

      albums.push({
        id: String(item.collectionId),
        title: name,
        artist: item.artistName || '',
        year: item.releaseDate ? item.releaseDate.slice(0, 4) : '',
        trackCount: trackCount,
        cover: (item.artworkUrl100 || '').replace('100x100', '600x600')
      });
    }

    const words = cleanQ.toLowerCase().replace(/[^\p{L}\p{N}\s]/gu, ' ').split(/\s+/).filter(w => w.length > 1);
    const scoreAlbum = (album) => {
      const title = (album.title || '').toLowerCase();
      const artist = (album.artist || '').toLowerCase();
      const full = title + ' ' + artist;
      let score = 0;
      for (const w of words) {
        if (title.includes(w)) score += 15;
        else if (artist.includes(w)) score += 10;
      }
      if (title === cleanQ.toLowerCase() || full === cleanQ.toLowerCase()) score += 50;
      else if (full.includes(cleanQ.toLowerCase()) || cleanQ.toLowerCase().includes(title)) score += 25;
      return score;
    };

    albums.sort((a, b) => scoreAlbum(b) - scoreAlbum(a));
    const responseData = { success: true, albums: albums.slice(0, 12) };
    setAlbumCache(cacheKey, responseData);
    return sendResponse(200, responseData);
  } catch (err) {
    return sendResponse(502, { success: false, error: 'Album service unavailable' });
  } finally {
    if (!outcome) {
      outcome = { status: 502, body: { success: false, error: 'Album service unavailable' } };
      resolveSearch(outcome);
    }
    if (albumCache.get(cacheKey) === inFlightEntry) {
      albumCache.delete(cacheKey);
    }
  }
}

async function handleAlbumTracks(req, res, parsedUrl) {
  const id = (parsedUrl.query.id || '').trim();
  const title = (parsedUrl.query.title || '').trim();
  const artist = (parsedUrl.query.artist || '').trim();

  if (!id && !title) {
    res.writeHead(400, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'Invalid album id' }));
  }

  const cacheKey = `tracks:${id || (artist + ':' + title)}`;
  const cached = getAlbumCache(cacheKey);
  if (cached) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(cached));
  }

  const inFlight = albumCache.get(cacheKey);
  if (inFlight?.promise) {
    const outcome = await inFlight.promise;
    res.writeHead(outcome.status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(outcome.body));
  }

  let resolveLookup;
  const lookupPromise = new Promise(resolve => { resolveLookup = resolve; });
  const inFlightEntry = { promise: lookupPromise };
  albumCache.set(cacheKey, inFlightEntry);
  let outcome = null;
  const sendResponse = (status, body) => {
    outcome = { status, body };
    resolveLookup(outcome);
    res.writeHead(status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(body));
  };

  try {
    let album = null;
    let tracks = [];

    if (id) {
      try {
        const url = `https://itunes.apple.com/lookup?id=${encodeURIComponent(id)}&entity=song`;
        const data = await fetchItunesJson(url);
        const results = (data && Array.isArray(data.results)) ? data.results : [];
        const albumItem = results.find(r => r && (r.wrapperType === 'collection' || r.collectionType === 'Album'));
        const songTracks = results
          .filter(r => r && (r.wrapperType === 'track' || r.kind === 'song'))
          .map(item => ({
            id: String(item.trackId || ''),
            no: item.trackNumber || 1,
            disc: item.discNumber || 1,
            title: item.trackName || '',
            artist: item.artistName || artist || '',
            duration: Math.round((item.trackTimeMillis || 0) / 1000),
            durationMs: item.trackTimeMillis || 0,
            thumbnail: item.artworkUrl100 ? item.artworkUrl100.replace('100x100', '600x600') : '',
            isExplicit: Boolean(item.trackExplicitness === 'explicit' || isTextExplicit(item.trackName || ''))
          }))
          .sort((a, b) => (a.disc - b.disc) || (a.no - b.no));

        if (songTracks.length > 0) {
          tracks = songTracks;
          album = {
            id: String(albumItem ? albumItem.collectionId : id),
            title: (albumItem && albumItem.collectionName) || title || '',
            artist: (albumItem && albumItem.artistName) || artist || '',
            year: (albumItem && albumItem.releaseDate) ? albumItem.releaseDate.slice(0, 4) : '',
            trackCount: (albumItem && albumItem.trackCount) || tracks.length,
            cover: albumItem ? upgradeArtworkUrl(albumItem.artworkUrl100 || '') : ''
          };
        }
      } catch (_) {}
    }

    if (tracks.length === 0 && id) {
      try {
        const dzUrl = `https://api.deezer.com/album/${encodeURIComponent(id)}/tracks?limit=50`;
        const dzData = await new Promise((resolve) => {
          https.get(dzUrl, { headers: { 'User-Agent': 'Mozilla/5.0' }, timeout: 4500 }, (dRes) => {
            let d = '';
            dRes.on('data', c => d += c);
            dRes.on('end', () => {
              try { resolve(JSON.parse(d)); } catch (_) { resolve(null); }
            });
          }).on('error', () => resolve(null));
        });

        if (dzData && Array.isArray(dzData.data) && dzData.data.length > 0) {
          tracks = dzData.data.map((item, idx) => ({
            id: String(item.id || ''),
            no: item.track_position || idx + 1,
            disc: item.disk_number || 1,
            title: item.title || '',
            artist: item.artist?.name || artist || '',
            duration: item.duration || 0,
            durationMs: (item.duration || 0) * 1000,
            thumbnail: item.album?.cover_xl || item.album?.cover_big || item.album?.cover_medium || '',
            isExplicit: Boolean(item.explicit_lyrics || isTextExplicit(item.title || ''))
          })).sort((a, b) => (a.disc - b.disc) || (a.no - b.no));

          album = {
            id: String(id),
            title: title || (tracks[0] && tracks[0].title) || '',
            artist: artist || (tracks[0] && tracks[0].artist) || '',
            year: '',
            trackCount: tracks.length,
            cover: ''
          };
        }
      } catch (_) {}
    }

    if (tracks.length === 0 && (title || artist)) {
      try {
        const term = `${artist || ''} ${title || ''}`.trim();
        const searchUrl = `https://itunes.apple.com/search?term=${encodeURIComponent(term)}&entity=song&limit=25`;
        const searchData = await fetchItunesJson(searchUrl);
        const results = (searchData && Array.isArray(searchData.results)) ? searchData.results : [];
        const songTracks = results
          .filter(r => r && (r.wrapperType === 'track' || r.kind === 'song'))
          .map(item => ({
            id: String(item.trackId || ''),
            no: item.trackNumber || 1,
            disc: item.discNumber || 1,
            title: item.trackName || '',
            artist: item.artistName || artist || '',
            duration: Math.round((item.trackTimeMillis || 0) / 1000),
            durationMs: item.trackTimeMillis || 0,
            thumbnail: item.artworkUrl100 ? item.artworkUrl100.replace('100x100', '600x600') : '',
            isExplicit: Boolean(item.trackExplicitness === 'explicit' || isTextExplicit(item.trackName || ''))
          }))
          .sort((a, b) => (a.disc - b.disc) || (a.no - b.no));

        if (songTracks.length > 0) {
          tracks = songTracks;
          const first = results[0];
          album = {
            id: String(id || (first && first.collectionId) || ''),
            title: title || (first && first.collectionName) || '',
            artist: artist || (first && first.artistName) || '',
            year: (first && first.releaseDate) ? first.releaseDate.slice(0, 4) : '',
            trackCount: tracks.length,
            cover: first ? upgradeArtworkUrl(first.artworkUrl100 || '') : ''
          };
        }
      } catch (_) {}
    }

    if (tracks.length > 0) {
      const responseData = { success: true, album: album || { id, title, artist, year: '', trackCount: tracks.length, cover: '' }, tracks };
      setAlbumCache(cacheKey, responseData);
      return sendResponse(200, responseData);
    }

    return sendResponse(404, { success: false, error: 'Failed to load album' });
  } catch (err) {
    return sendResponse(502, { success: false, error: 'Album service unavailable' });
  } finally {
    if (!outcome) {
      outcome = { status: 502, body: { success: false, error: 'Album service unavailable' } };
      resolveLookup(outcome);
    }
    if (albumCache.get(cacheKey) === inFlightEntry) {
      albumCache.delete(cacheKey);
    }
  }
}

let hotPicksCache = { data: null, expires: 0 };

async function handleHotPicks(req, res, parsedUrl) {
  const limit = Math.max(1, Math.min(25, parseInt(parsedUrl.query.limit || '12', 10) || 12));

  if (hotPicksCache.promise) {
    const outcome = await hotPicksCache.promise;
    res.writeHead(outcome.status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(outcome.body.success ? {
      success: true,
      title: outcome.body.title,
      tracks: outcome.body.tracks.slice(0, limit)
    } : outcome.body));
  }

  if (hotPicksCache.data && Date.now() < hotPicksCache.expires) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      success: true,
      title: hotPicksCache.data.title,
      tracks: hotPicksCache.data.tracks.slice(0, limit)
    }));
  }

  let resolveLookup;
  const lookupPromise = new Promise(resolve => { resolveLookup = resolve; });
  hotPicksCache = { data: null, expires: 0, promise: lookupPromise };
  let outcome = null;
  const sendResponse = (status, body) => {
    outcome = { status, body };
    resolveLookup(outcome);
    res.writeHead(status, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(body));
  };

  try {
    const data = await fetchItunesJson('https://api.deezer.com/chart/0/tracks?limit=40');
    const rawItems = (data && Array.isArray(data.data)) ? data.data : [];
    const candidates = rawItems
      .filter(item => item && item.title && item.artist && item.artist.name)
      .map(item => ({
        artist: item.artist.name,
        title: item.title,
        duration: Number(item.duration) || 0,
        isExplicit: Boolean(item.explicit_lyrics || item.explicit_content_lyrics === 1 || isTextExplicit(item.title))
      }));

    if (candidates.length === 0) {
      return sendResponse(200, { success: false });
    }

    const resolvedTracks = [];
    const seenIds = new Set();
    let cIdx = 0;

    const worker = async () => {
      while (cIdx < candidates.length && resolvedTracks.length < 12) {
        const c = candidates[cIdx++];
        if (!c) break;
        try {
          const track = await resolveCandidate(c);
          if (track && !seenIds.has(track.id)) {
            if (c.isExplicit) track.isExplicit = true;
            seenIds.add(track.id);
            if (resolvedTracks.length < 12) {
              resolvedTracks.push(track);
            }
          }
        } catch (_) {}
      }
    };

    await Promise.all([worker(), worker(), worker(), worker()]);

    if (resolvedTracks.length === 0) {
      return sendResponse(200, { success: false });
    }

    const title = 'Hot picks ' + new Date().getFullYear();
    hotPicksCache = {
      data: { title, tracks: resolvedTracks },
      expires: Date.now() + 60 * 60 * 1000
    };

    return sendResponse(200, {
      success: true,
      title,
      tracks: resolvedTracks
    });
  } catch (err) {
    return sendResponse(200, { success: false });
  } finally {
    if (!outcome) {
      outcome = { status: 200, body: { success: false } };
      resolveLookup(outcome);
    }
    if (hotPicksCache.promise === lookupPromise) {
      hotPicksCache = { data: null, expires: 0 };
    }
  }
}

const DEFAULT_LFM_KEY = '4b4f73bda181868353f9b438604adf52';
const DEFAULT_LFM_SECRET = '716cc0a784bb62835de5bd674e65eb57';

function signLastFmParams(params, secret) {
  const keys = Object.keys(params).filter(k => k !== 'format' && k !== 'callback').sort();
  let sigStr = '';
  for (const k of keys) sigStr += k + String(params[k]);
  sigStr += (secret || DEFAULT_LFM_SECRET);
  return crypto.createHash('md5').update(sigStr, 'utf8').digest('hex');
}

function callLastFmApi(params, secret, method = 'POST') {
  return new Promise((resolve) => {
    const apiKey = params.api_key || DEFAULT_LFM_KEY;
    const apiSecret = secret || DEFAULT_LFM_SECRET;
    const fullParams = { ...params, api_key: apiKey };
    fullParams.api_sig = signLastFmParams(fullParams, apiSecret);
    fullParams.format = 'json';
    const bodyData = new URLSearchParams(fullParams).toString();
    const isGet = method === 'GET';
    const reqPath = isGet ? ('/2.0/?' + bodyData) : '/2.0/';
    const req = https.request({
      hostname: 'ws.audioscrobbler.com',
      path: reqPath,
      method,
      headers: isGet ? { 'User-Agent': 'Riff-Desktop-Player/43.0' } : {
        'User-Agent': 'Riff-Desktop-Player/43.0',
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(bodyData)
      },
      timeout: 10000
    }, (res) => {
      let raw = '';
      res.on('data', chunk => { raw += chunk; });
      res.on('end', () => {
        try { resolve(JSON.parse(raw)); } catch (_) { resolve({ error: 1, message: 'Invalid Last.fm response' }); }
      });
    });
    req.on('error', (e) => resolve({ error: 1, message: e.message }));
    req.on('timeout', () => { req.destroy(); resolve({ error: 1, message: 'Last.fm request timeout' }); });
    if (!isGet) req.write(bodyData);
    req.end();
  });
}

async function handleLastFmRoute(req, res, pathname) {
  let body = {};
  if (req.method === 'POST') {
    body = await new Promise((r) => {
      let d = '';
      req.on('data', c => { if (d.length < 100000) d += c; });
      req.on('end', () => { try { r(JSON.parse(d || '{}')); } catch (_) { r({}); } });
    });
  }
  const hasCustomPair = Boolean(body.apiKey && String(body.apiKey).trim() && body.apiSecret && String(body.apiSecret).trim());
  const apiKey = hasCustomPair ? String(body.apiKey).trim() : DEFAULT_LFM_KEY;
  const apiSecret = hasCustomPair ? String(body.apiSecret).trim() : DEFAULT_LFM_SECRET;

  if (pathname === '/api/lastfm/token') {
    const r = await callLastFmApi({ method: 'auth.getToken', api_key: apiKey }, apiSecret, 'GET');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      success: !!r.token,
      token: r.token || '',
      authUrl: r.token ? ('https://www.last.fm/api/auth/?api_key=' + encodeURIComponent(apiKey) + '&token=' + encodeURIComponent(r.token)) : '',
      error: r.message || ''
    }));
    return;
  }

  if (pathname === '/api/lastfm/session') {
    const r = await callLastFmApi({ method: 'auth.getSession', api_key: apiKey, token: String(body.token || '') }, apiSecret, 'GET');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      success: !!(r.session && r.session.key),
      sessionKey: r.session?.key || '',
      username: r.session?.name || '',
      error: r.message || ''
    }));
    return;
  }

  if (pathname === '/api/lastfm/mobile-session') {
    const r = await callLastFmApi({
      method: 'auth.getMobileSession',
      api_key: apiKey,
      username: String(body.username || '').trim(),
      password: String(body.password || '')
    }, apiSecret, 'POST');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      success: !!(r.session && r.session.key),
      sessionKey: r.session?.key || '',
      username: r.session?.name || '',
      error: r.message || ''
    }));
    return;
  }

  if (pathname === '/api/lastfm/now-playing') {
    if (!body.sk || !body.artist || !body.track) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Missing sk/artist/track' }));
      return;
    }
    const p = { method: 'track.updateNowPlaying', api_key: apiKey, sk: body.sk, artist: body.artist, track: body.track };
    if (body.album) p.album = body.album;
    if (body.duration && Number(body.duration) > 0) p.duration = String(Math.round(Number(body.duration)));
    const r = await callLastFmApi(p, apiSecret, 'POST');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: !r.error, data: r, error: r.message || '' }));
    return;
  }

  if (pathname === '/api/lastfm/scrobble') {
    if (!body.sk || !body.artist || !body.track) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: false, error: 'Missing sk/artist/track' }));
      return;
    }
    const p = {
      method: 'track.scrobble',
      api_key: apiKey,
      sk: body.sk,
      'artist[0]': body.artist,
      'track[0]': body.track,
      'timestamp[0]': String(body.timestamp || Math.floor(Date.now() / 1000))
    };
    if (body.album) p['album[0]'] = body.album;
    if (body.duration && Number(body.duration) > 0) p['duration[0]'] = String(Math.round(Number(body.duration)));
    const r = await callLastFmApi(p, apiSecret, 'POST');
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: !r.error, data: r, error: r.message || '' }));
    return;
  }
}

let i18nJobRunning = false;
const activeRequests = new Map();
let activeReqSeq = 0;
const LONG_LIVED_ROUTES = new Set(['/api/audio-proxy', '/api/lyrics-stream', '/api/local-track', '/api/downloaded-track', '/api/library-file', '/api/download', '/api/encode']);
setInterval(() => {
  try {
    const now = Date.now();
    const stuck = [];
    for (const r of activeRequests.values()) if (now - r.t > 8000) stuck.push(`${r.path} ${Math.round((now - r.t) / 1000)}s${r.sent ? ' (headers sent)' : ' (NO RESPONSE)'}`);
    if (stuck.length) logger.log('warn', `Open requests (${activeRequests.size} total): ` + stuck.slice(0, 12).join(' | '));
  } catch (_) {}
}, 10000).unref();

const server = http.createServer((req, res) => {
  const _reqId = ++activeReqSeq;
  const _reqPath = String(req.url || '').split('?')[0];
  const _rec = { t: Date.now(), path: _reqPath, sent: false };
  activeRequests.set(_reqId, _rec);
  const _done = () => activeRequests.delete(_reqId);
  res.once('close', _done);
  res.once('finish', _done);
  if (!LONG_LIVED_ROUTES.has(_reqPath)) {
    const wd = setTimeout(() => {
      try {
        if (!res.headersSent && !res.writableEnded) {
          logger.log('error', `Watchdog 504: ${_reqPath} did not respond in 25s`);
          res.writeHead(504, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: 'server timeout' }));
        }
      } catch (_) {}
    }, 25000);
    res.once('close', () => clearTimeout(wd));
    res.once('finish', () => clearTimeout(wd));
  }
  const _wh = res.writeHead.bind(res);
  res.writeHead = (...a) => { if (res.headersSent || res.writableEnded) return res; _rec.sent = true; return _wh(...a); };
  const _end = res.end.bind(res);
  res.end = (...a) => res.writableEnded ? res : _end(...a);
  Promise.resolve(requestHandler(req, res)).catch((err) => {
    try { logger.log('error', 'Request failed: ' + (req.url || '').split('?')[0] + ' - ' + (err && err.message)); } catch (_) {}
    try {
      if (!res.headersSent) { res.writeHead(500, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'internal error' })); }
      else res.destroy();
    } catch (_) {}
  });
});
async function requestHandler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');

  const requestOrigin = req.headers.origin;
  if (requestOrigin && requestOrigin !== 'null') {
    let originAllowed = false;
    try {
      const originUrl = new URL(requestOrigin);
      originAllowed = originUrl.protocol === 'file:' || originUrl.hostname === '127.0.0.1' || originUrl.hostname === 'localhost' || originUrl.hostname === '::1';
    } catch (e) {
      originAllowed = false;
    }
    if (!originAllowed) {
      res.writeHead(403, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'forbidden' }));
    }
  }

  const hostName = (req.headers.host || '').replace(/:\d+$/, '').replace(/^\[|\]$/g, '');
  if (hostName && hostName !== '127.0.0.1' && hostName !== 'localhost' && hostName !== '::1') {
    res.writeHead(403, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'forbidden' }));
  }

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    return res.end();
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  if (pathname.startsWith('/api/lastfm/')) { handleLastFmRoute(req, res, pathname); return; }

  if (TOKEN_PROTECTED_ROUTES.has(parsedUrl.pathname) && !tokenMatches(req.headers['x-riff-token'])) {
    res.writeHead(403, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: false, error: 'forbidden' }));
  }

  if (parsedUrl.pathname === '/api/audio-output-info' && req.method === 'GET') {
    const info = await getSystemAudioOutputInfo();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(info));
  }

  if (parsedUrl.pathname === '/api/devsize-memes' && req.method === 'GET') {
    let files = [];
    try {
      const dir = path.join(__dirname, '../../assets/devsize');
      if (fs.existsSync(dir)) {
        files = fs.readdirSync(dir)
          .filter(f => /\.(png|jpe?g|webp|gif|svg)$/i.test(f))
          .map(f => '../../assets/devsize/' + encodeURIComponent(f));
      }
    } catch (_) {}
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, files }));
  }

  if (parsedUrl.pathname === '/api/translate' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', () => {
      const payload = safeJsonParse(body, {});
      const texts = payload.texts || payload.lines || [];
      const targetLang = payload.targetLang || payload.to || 'en';
      const apiKey = String(payload.apiKey || payload.deeplKey || '').trim();
      handleTranslate(req, res, texts, targetLang, apiKey);
    });
    return;
  }

  if (parsedUrl.pathname === '/api/i18n-batch' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => {
      if (res.writableEnded) return;
      body += chunk;
      if (body.length > MAX_BODY_BYTES) {
        body = '';
        req.removeAllListeners('data');
        req.removeAllListeners('end');
        res.writeHead(413, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: 'payload too large' }));
        req.resume();
      }
    });
    req.on('end', async () => {
      const payload = safeJsonParse(body, {});
      const apiKey = String(payload.apiKey || payload.deeplKey || '').trim();
      const target = payload.target || payload.targetLang || payload.to || 'en';
      const texts = Array.isArray(payload.texts) ? payload.texts : [];
      const safeLang = String(target || 'en').replace(/[^a-zA-Z0-9_-]/g, '');
      const langFile = path.join(I18N_DIR, safeLang + '.json');
      let dict = {};
      try { dict = JSON.parse(fs.readFileSync(langFile, 'utf8')); } catch (_) {}
      let dictDirty = false;
      if (safeLang !== 'en') {
        for (const k of Object.keys(dict)) {
          if (dict[k] === k || i18nValueBroken(k, dict[k])) {
            delete dict[k];
            dictDirty = true;
          }
        }
      }
      Object.assign(dict, I18N_FIX[safeLang] || {});

      const missing = [];
      for (const str of texts) {
        if (typeof str === 'string' && str.trim() && !dict[str] && !missing.includes(str)) {
          missing.push(str);
        }
      }

      if (missing.length > 0) {
        let changed = false;
        if (safeLang === 'en') {
          for (const str of missing) { dict[str] = str; changed = true; }
        } else {
          const batches = [];
          for (let i = 0; i < missing.length; i += 25) {
            batches.push(missing.slice(i, i + 25));
          }
          for (const batch of batches) {
            const joined = batch.join('\n');
            const resLines = await callDeepLTranslate(joined, safeLang, apiKey);
            if (resLines && Array.isArray(resLines)) {
              for (let j = 0; j < Math.min(batch.length, resLines.length); j++) {
                const tr = (resLines[j] || '').trim();
                if (tr && !i18nValueBroken(batch[j], tr)) {
                  dict[batch[j]] = tr;
                  changed = true;
                }
              }
            }
          }
          Object.assign(dict, I18N_FIX[safeLang] || {});
        }
        if (changed || dictDirty) {
          try { fs.writeFileSync(langFile, JSON.stringify(dict, null, 2), 'utf8'); } catch (_) {}
        }
      } else if (dictDirty) {
        try { fs.writeFileSync(langFile, JSON.stringify(dict, null, 2), 'utf8'); } catch (_) {}
      }

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, translations: texts.map(s => (!i18nValueBroken(s, dict[s]) ? dict[s] : s)) }));
    });
    return;
  }

  if (parsedUrl.pathname === '/api/state' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', () => {
      try {
        const state = JSON.parse(body);
        currentAppState = state;
        broadcastSSE(state);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true }));
      } catch (e) {
        res.writeHead(400); res.end();
      }
    });
    return;
  }

  if (parsedUrl.pathname === '/api/proxy-config' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', async () => {
      const payload = safeJsonParse(body, {});
      const enabled = Boolean(payload.enabled);
      const proxyUrl = payload.proxyUrl;
      const candidateProxy = enabled ? String(proxyUrl || '').trim() : '';
      if (candidateProxy) {
        const alive = await checkProxyAlive(candidateProxy, 1500);
        serverActiveProxy = alive ? candidateProxy : '';
      } else {
        serverActiveProxy = '';
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true }));
    });
    return;
  }

  if (parsedUrl.pathname === '/api/proxy-test' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', async () => {
      const payload = safeJsonParse(body, {});
      const proxyUrl = String(payload.proxyUrl || '').trim();
      const t0 = Date.now();
      try {
        await executeYtDlp(['--no-config', '--proxy', proxyUrl, '--simulate', '--no-warnings', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'], 8000);
        const pingMs = Date.now() - t0;
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, pingMs }));
      } catch (err) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  if (parsedUrl.pathname === '/api/lyrics-stream') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive',
      'Access-Control-Allow-Origin': '*'
    });
    sseClients.add(res);
    res.write(`data: ${JSON.stringify(currentAppState)}\n\n`);
    req.on('close', () => sseClients.delete(res));
    return;
  }

  if (parsedUrl.pathname === '/api/download') {
    const trackUrl = parsedUrl.query.url || '';
    const trackId = parsedUrl.query.id || '';
    const streamUrl = parsedUrl.query.streamUrl || '';
    const title = parsedUrl.query.title || '';
    const artist = parsedUrl.query.artist || '';
    const format = parsedUrl.query.format || 'mp3';
    const quality = parsedUrl.query.quality || '0';
    const raw = parsedUrl.query.raw === '1' || parsedUrl.query.raw === 'true';
    let start = null;
    if (parsedUrl.query.start !== undefined && parsedUrl.query.start !== '') {
      const n = parseFloat(parsedUrl.query.start);
      if (!isNaN(n)) start = Math.max(0, n);
    }
    let end = null;
    if (parsedUrl.query.end !== undefined && parsedUrl.query.end !== '') {
      const n = parseFloat(parsedUrl.query.end);
      if (!isNaN(n)) end = Math.max(0, n);
    }
    return handleDownload(req, res, trackUrl, title, artist, format, quality, start, end, raw, trackId, streamUrl);
  }

  if (parsedUrl.pathname === '/api/tmp-file') {
    return handleTmpFile(req, res, parsedUrl);
  }

  if (parsedUrl.pathname === '/api/encode' && req.method === 'POST') {
    return handleEncode(req, res, parsedUrl);
  }

  if (parsedUrl.pathname === '/api/brand-icon') {
    return handleBrandIcon(req, res, parsedUrl.query.name || '');
  }

  if (parsedUrl.pathname === '/api/spotify-import') {
    const spotifyUrl = parsedUrl.query.url || '';
    return handleSpotifyImport(req, res, spotifyUrl);
  }

  if (parsedUrl.pathname === '/api/apple-import') {
    const appleUrl = parsedUrl.query.url || '';
    return handleAppleImport(req, res, appleUrl);
  }

  if (parsedUrl.pathname === '/api/yandex-import') {
    const yandexUrl = parsedUrl.query.url || '';
    return handleYandexImport(req, res, yandexUrl);
  }

  if (parsedUrl.pathname === '/api/playlist-import') {
    const playlistUrl = parsedUrl.query.url || '';
    return handlePlaylistImport(req, res, playlistUrl);
  }

  if (parsedUrl.pathname === '/api/track-cover') {
    const artist = (parsedUrl.query && parsedUrl.query.artist) || '';
    const title = (parsedUrl.query && parsedUrl.query.title) || '';
    const cover = await fetchTrackCoverUrl(artist, title);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: !!cover, cover: cover || '' }));
  }

  if (parsedUrl.pathname === '/api/albums/search') {
    return handleAlbumSearch(req, res, parsedUrl);
  }

  if (parsedUrl.pathname === '/api/albums/tracks' || parsedUrl.pathname === '/api/album' || parsedUrl.pathname === '/api/album-tracks') {
    return handleAlbumTracks(req, res, parsedUrl);
  }

  if (parsedUrl.pathname === '/api/hot') {
    return handleHotPicks(req, res, parsedUrl);
  }

  if (parsedUrl.pathname === '/api/spotify-auth-start') {
    loadSpotifyAuth();
    spotifyAuthLoaded = true;
    const clientId = parsedUrl.query.clientId || '';
    if (!/^[a-f0-9]{32}$/i.test(clientId)) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'Invalid Client ID' }));
    }
    const verifier = crypto.randomBytes(48).toString('base64url');
    const challenge = crypto.createHash('sha256').update(verifier).digest('base64url');
    const state = crypto.randomBytes(16).toString('hex');
    spotifyAuth = { verifier, state, clientId, token: '', refreshToken: '', expiresAt: 0 };
    spotifyAuthError = '';
    const redirectUri = getSpotifyRedirectUri();
    const authUrl = `https://accounts.spotify.com/authorize?response_type=code&client_id=${encodeURIComponent(clientId)}&scope=${encodeURIComponent(SPOTIFY_SCOPES)}&redirect_uri=${encodeURIComponent(redirectUri)}&state=${encodeURIComponent(state)}&code_challenge_method=S256&code_challenge=${encodeURIComponent(challenge)}`;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, url: authUrl, redirectUri }));
  }

  if (parsedUrl.pathname === '/api/spotify-callback') {
    const code = parsedUrl.query.code;
    const state = parsedUrl.query.state;
    const errParam = parsedUrl.query.error;
    if (!spotifyAuth.state || !state || state !== spotifyAuth.state) {
      res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
      return res.end('<!DOCTYPE html><html><body><p>Invalid Spotify login session.</p></body></html>');
    }
    if (errParam || !code) {
      spotifyAuthError = errParam === 'access_denied' ? 'Spotify authorization was denied.' : 'Spotify login failed. Check the Client ID, allowed account and redirect URI.';
      spotifyAuth.state = '';
      res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
      return res.end('<!DOCTYPE html><html><body><p>Spotify login failed. Return to Riff for details.</p></body></html>');
    }
    const redirectUri = getSpotifyRedirectUri();
    try {
      const session = spotifyAuth;
      const tokenResult = await requestSpotifyToken({ grant_type: 'authorization_code', code, redirect_uri: redirectUri, client_id: spotifyAuth.clientId, code_verifier: spotifyAuth.verifier });
      if (spotifyAuth !== session) {
        res.writeHead(409, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end('<!DOCTYPE html><html><body><p>Spotify login session changed. Return to Riff and try again.</p></body></html>');
      }
      spotifyAuth.state = '';
      if (tokenResult.status >= 200 && tokenResult.status < 300 && typeof tokenResult.data?.access_token === 'string' && tokenResult.data.access_token) {
        spotifyAuth.token = tokenResult.data.access_token;
        spotifyAuth.refreshToken = typeof tokenResult.data.refresh_token === 'string' ? tokenResult.data.refresh_token : '';
        spotifyAuth.expiresAt = Date.now() + (Number.isFinite(tokenResult.data.expires_in) && tokenResult.data.expires_in > 0 ? tokenResult.data.expires_in : 3600) * 1000;
        if (!spotifyAuth.refreshToken) spotifyAuthError = 'Spotify did not provide a refresh token; reconnect when this session expires.';
        else if (!saveSpotifyAuth()) spotifyAuthError = 'Spotify login could not be saved securely; reconnect after restarting Riff.';
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end('<!DOCTYPE html><html><body><p>Riff is connected to Spotify. You can close this tab.</p></body></html>');
      } else {
        const reason = tokenResult.data?.error_description || tokenResult.data?.error || `HTTP ${tokenResult.status}`;
        spotifyAuthError = `Spotify token exchange failed: ${reason}`;
        res.writeHead(400, { 'Content-Type': 'text/html; charset=utf-8' });
        return res.end('<!DOCTYPE html><html><body><p>Spotify login failed. Return to Riff for details.</p></body></html>');
      }
    } catch (err) {
      spotifyAuth.state = '';
      spotifyAuthError = `Could not reach Spotify: ${err.message}`;
      res.writeHead(500, { 'Content-Type': 'text/html; charset=utf-8' });
      return res.end('<!DOCTYPE html><html><body><p>Spotify login failed. Return to Riff for details.</p></body></html>');
    }
  }

  if (parsedUrl.pathname === '/api/spotify-status') {
    loadSpotifyAuth();
    if (spotifyAuth.refreshToken) {
      try { await getSpotifyAccessToken(); }
      catch (e) { spotifyAuthError = e.message; }
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ connected: Boolean(spotifyAuth.token && (!spotifyAuth.expiresAt || Date.now() < spotifyAuth.expiresAt)), error: spotifyAuthError }));
  }

  if (parsedUrl.pathname === '/api/spotify-logout') {
    try { clearStoredSpotifyAuth(); }
    catch (e) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'Could not remove saved Spotify login' }));
    }
    spotifyAuth = { verifier: '', state: '', clientId: '', token: '', refreshToken: '', expiresAt: 0 };
    spotifyAuthError = '';
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true }));
  }

  if (parsedUrl.pathname === '/api/spotify-library') {
    return handleSpotifyLibrary(req, res, parsedUrl.query);
  }

  if (parsedUrl.pathname === '/api/search') {
    const q = parsedUrl.query.q || '';
    const platform = parsedUrl.query.platform || 'ytmusic';
    const limit = parseInt(parsedUrl.query.limit || '12', 10);
    const offset = parseInt(parsedUrl.query.offset || '0', 10);
    return handleSearch(req, res, q, platform, limit, offset);
  }

  if (parsedUrl.pathname === '/api/vibe' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', () => {
      const payload = safeJsonParse(body, {});
      handleVibeRecommendations(req, res, payload);
    });
    return;
  }

  if (parsedUrl.pathname === '/api/trending') {
    return handleTrending(req, res);
  }

  if (parsedUrl.pathname === '/api/artist') {
    const name = parsedUrl.query.name || '';
    const hintTrack = parsedUrl.query.hintTrack || '';
    if (!name) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ success: false, error: 'Missing artist name' }));
    }
    const info = await fetchArtistInfo(name, hintTrack);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, artist: info }));
  }

  if (parsedUrl.pathname === '/api/prefetch') {
    const trackUrl = parsedUrl.query.url || '';
    const trackId = parsedUrl.query.id || '';
    const platform = parsedUrl.query.platform || 'ytmusic';
    prefetchTrack(trackUrl, trackId, platform);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true }));
  }

  if (parsedUrl.pathname === '/api/check-cache') {
    const trackId = parsedUrl.query.id || '';
    const cleanId = cleanTrackId(trackId);
    const cachedFile = await findCachedFile(cleanId);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ 
      cached: !!cachedFile, 
      streamUrl: cachedFile ? `http://127.0.0.1:${ACTIVE_PORT}/api/local-track?id=${encodeURIComponent(cleanId)}` : null 
    }));
  }

  if (parsedUrl.pathname === '/api/tools-status' || parsedUrl.pathname === '/api/tools/status') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(require('./tools').toolsStatus()));
  }

  if (parsedUrl.pathname === '/api/tools-install' || parsedUrl.pathname === '/api/tools/install') {
    require('./tools').ensureTools().catch(() => {});
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ started: true }));
  }

  if (parsedUrl.pathname === '/api/stream-info') {
    const trackUrl = parsedUrl.query.url || '';
    const trackId = parsedUrl.query.id || '';
    const platform = parsedUrl.query.platform || 'ytmusic';
    const title = parsedUrl.query.title || '';
    const artist = parsedUrl.query.artist || '';
    const duration = parsedUrl.query.duration || 0;
    const thumbnail = parsedUrl.query.thumbnail || '';
    const rawTimeout = parseInt(parsedUrl.query.timeout, 10);
    const timeoutSec = isNaN(rawTimeout) ? 20 : Math.max(5, Math.min(30, rawTimeout));
    return handleStreamInfo(req, res, trackUrl, trackId, platform, title, artist, duration, thumbnail, timeoutSec);
  }

  if (parsedUrl.pathname === '/api/local-track') {
    const trackId = parsedUrl.query.id || '';
    return handleLocalTrack(req, res, trackId);
  }

  if (parsedUrl.pathname === '/api/downloaded-track') {
    const name = parsedUrl.query.name || '';
    return handleDownloadedTrack(req, res, name);
  }

  if (parsedUrl.pathname === '/api/library-add' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body || '{}');
        const items = Array.isArray(payload.items) ? payload.items : [];
        const fileRegex = /^local_[a-f0-9]{12}\.[a-z0-9]+$/;
        let addedCount = 0;
        for (const item of items) {
          if (!item || !item.id || !item.file) continue;
          if (!fileRegex.test(item.file)) continue;
          const fullPath = path.join(LIBRARY_DIR, item.file);
          if (!fs.existsSync(fullPath)) continue;

          let rawName = (item.name || '').trim();
          let cleanName = rawName.replace(/^\d+(?:[.\s-]+)\s*/, '').trim() || rawName || 'Untitled';

          const dashIdx = cleanName.indexOf(' - ');
          let artist = 'Unknown artist';
          let title = cleanName;
          if (dashIdx !== -1) {
            artist = cleanName.slice(0, dashIdx).trim() || 'Unknown artist';
            title = cleanName.slice(dashIdx + 3).trim() || cleanName;
          }

          libraryIndex[item.id] = {
            id: item.id,
            file: item.file,
            ext: item.ext || path.extname(item.file).slice(1).toLowerCase(),
            title: title,
            artist: artist,
            duration: 0,
            addedAt: Date.now()
          };
          addedCount++;
        }
        saveLibraryIndex();
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, count: addedCount }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  if (parsedUrl.pathname === '/api/library-update' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body || '{}');
        const { id, duration } = payload;
        if (id && libraryIndex[id] && typeof duration === 'number' && !isNaN(duration)) {
          libraryIndex[id].duration = duration;
          saveLibraryIndex();
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  if (parsedUrl.pathname === '/api/library') {
    const list = Object.values(libraryIndex)
      .sort((a, b) => (b.addedAt || 0) - (a.addedAt || 0))
      .map(item => ({
        id: item.id,
        platform: 'local',
        title: item.title,
        artist: item.artist,
        duration: Number(item.duration) || 0,
        thumbnail: ''
      }));
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(list));
  }

  if (parsedUrl.pathname === '/api/library-file') {
    const trackId = parsedUrl.query.id || '';
    return handleLibraryFile(req, res, trackId);
  }

  if (parsedUrl.pathname === '/api/library-remove' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body || '{}');
        if (payload.clearAll) {
          try {
            if (fs.existsSync(LIBRARY_DIR)) {
              const files = fs.readdirSync(LIBRARY_DIR);
              for (const f of files) {
                try { fs.unlinkSync(path.join(LIBRARY_DIR, f)); } catch (e) {}
              }
            }
          } catch (e) {}
          libraryIndex = {};
          saveLibraryIndex();
        } else if (Array.isArray(payload.ids)) {
          for (const id of payload.ids) {
            if (id && libraryIndex[id]) {
              const item = libraryIndex[id];
              if (item.file) {
                const fullPath = path.join(LIBRARY_DIR, item.file);
                try {
                  if (fs.existsSync(fullPath)) fs.unlinkSync(fullPath);
                } catch (e) {}
              }
              delete libraryIndex[id];
            }
          }
          saveLibraryIndex();
        } else {
          const id = payload.id || parsedUrl.query.id || '';
          if (id && libraryIndex[id]) {
            const item = libraryIndex[id];
            if (item.file) {
              const fullPath = path.join(LIBRARY_DIR, item.file);
              try {
                if (fs.existsSync(fullPath)) fs.unlinkSync(fullPath);
              } catch (e) {}
            }
            delete libraryIndex[id];
            saveLibraryIndex();
          }
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true }));
      } catch (err) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: false, error: err.message }));
      }
    });
    return;
  }

  if (parsedUrl.pathname === '/api/image-proxy') {
    const target = parsedUrl.query.url || '';
    if (!/^https?:\/\//i.test(target)) {
      res.writeHead(400);
      return res.end();
    }
    const key = crypto.createHash('sha1').update(target).digest('hex');
    const cachePath = path.join(THUMBNAILS_DIR, `proxy-${key}.img`);
    let buffer = null;
    try {
      if (fs.existsSync(cachePath)) buffer = await fs.promises.readFile(cachePath);
    } catch (e) {}
    let host = '';
    try { host = new URL(target).hostname; } catch (e) {}
    const hostDead = !!host && (thumbHostFails.get(host) || 0) > Date.now();
    if (!buffer && !hostDead) {
      try {
        const r = await fetch(target, {
          signal: AbortSignal.timeout(6000),
          headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120 Safari/537.36' }
        });
        if (!r.ok) throw new Error(String(r.status));
        const data = Buffer.from(await r.arrayBuffer());
        if (data.length < 500) throw new Error('empty image');
        buffer = data;
        fs.promises.writeFile(cachePath, data).catch(() => {});
      } catch (e) {
        if (host) thumbHostFails.set(host, Date.now() + 600000);
      }
    }
    if (!buffer && parsedUrl.query.artist && parsedUrl.query.title) {
      const alt = await lookupCoverArt(parsedUrl.query.artist, parsedUrl.query.title);
      if (alt) {
        try { buffer = await fs.promises.readFile(alt); } catch (e) {}
      }
    }
    if (!buffer) {
      res.writeHead(302, { Location: target });
      return res.end();
    }
    res.writeHead(200, { 'Content-Type': imageContentType(buffer), 'Cache-Control': 'public, max-age=31536000' });
    return res.end(buffer);
  }

  if (parsedUrl.pathname === '/api/local-thumbnail') {
    const trackId = parsedUrl.query.id || '';
    const cleanId = cleanTrackId(trackId);
    const thumbPath = path.join(THUMBNAILS_DIR, `${cleanId}.jpg`);
    if (fs.existsSync(thumbPath)) {
      res.writeHead(200, { 'Content-Type': 'image/jpeg', 'Cache-Control': 'public, max-age=31536000' });
      return streamFileToResponse(fs.createReadStream(thumbPath), res);
    }
    res.writeHead(404);
    return res.end('Thumbnail not cached');
  }

  if (parsedUrl.pathname === '/api/offline-library') {
    const tracks = [];
    let totalBytes = 0;
    try {
      const files = await fs.promises.readdir(CACHE_DIR);
      const audioExtRegex = /\.(opus|webm|m4a|mp3)$/i;
      for (const file of files) {
        if (audioExtRegex.test(file) && !file.includes('.temp.')) {
          const cleanId = file.replace(audioExtRegex, '');
          const stat = await fs.promises.stat(path.join(CACHE_DIR, file));
          totalBytes += stat.size;
          const meta = trackMetadataIndex[cleanId] || {};
          tracks.push({
            id: meta.id || cleanId,
            cleanId: cleanId,
            title: meta.title || cleanId,
            artist: meta.artist || 'Unknown artist',
            duration: meta.duration || 0,
            thumbnail: meta.thumbnail || '',
            localThumbnail: fs.existsSync(path.join(THUMBNAILS_DIR, `${cleanId}.jpg`)) ? `http://127.0.0.1:${ACTIVE_PORT}/api/local-thumbnail?id=${cleanId}` : (meta.thumbnail || ''),
            platform: meta.platform || 'youtube',
            fileSize: stat.size,
            lastPlayed: meta.lastPlayed || stat.mtimeMs,
            analysis: meta.analysis || null
          });
        }
      }
    } catch (e) {}
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, count: tracks.length, totalBytes, tracks }));
  }

  if (parsedUrl.pathname === '/api/track-analysis') {
    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => { if (res.writableEnded) return; body += chunk; if (body.length > MAX_BODY_BYTES) { body = ''; req.removeAllListeners('data'); req.removeAllListeners('end'); res.writeHead(413, { 'Content-Type': 'application/json' }); res.end(JSON.stringify({ success: false, error: 'payload too large' })); req.resume(); } });
      req.on('end', () => {
        try {
          const payload = JSON.parse(body || '{}');
          const cleanId = cleanTrackId(payload.id || '');
          if (cleanId && payload.analysis) {
            saveTrackMetadata(cleanId, { analysis: payload.analysis });
          }
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: true }));
        } catch (e) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: e.message }));
        }
      });
      return;
    }
    const cleanId = cleanTrackId(parsedUrl.query.id || '');
    const meta = trackMetadataIndex[cleanId] || {};
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true, analysis: meta.analysis || null }));
  }

  if (parsedUrl.pathname === '/api/offline-delete' && req.method === 'POST') {
    const trackId = parsedUrl.query.id || '';
    const cleanId = cleanTrackId(trackId);
    const audioPath = await findCachedFile(cleanId);
    const thumbPath = path.join(THUMBNAILS_DIR, `${cleanId}.jpg`);
    try { if (audioPath && fs.existsSync(audioPath)) await fs.promises.unlink(audioPath); } catch (e) {}
    try { if (fs.existsSync(thumbPath)) await fs.promises.unlink(thumbPath); } catch (e) {}
    delete trackMetadataIndex[cleanId];
    try { fs.writeFileSync(METADATA_PATH, JSON.stringify(trackMetadataIndex, null, 2), 'utf-8'); } catch (e) {}
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true }));
  }

  if (parsedUrl.pathname === '/api/clear-offline-library' && req.method === 'POST') {
    try {
      const files = await fs.promises.readdir(CACHE_DIR);
      const audioExtRegex = /\.(opus|webm|m4a|mp3)$/i;
      for (const file of files) {
        if (audioExtRegex.test(file)) {
          try { await fs.promises.unlink(path.join(CACHE_DIR, file)); } catch (e) {}
        }
      }
      const thumbFiles = await fs.promises.readdir(THUMBNAILS_DIR);
      for (const file of thumbFiles) {
        try { await fs.promises.unlink(path.join(THUMBNAILS_DIR, file)); } catch (e) {}
      }
      trackMetadataIndex = {};
      try { fs.writeFileSync(METADATA_PATH, '{}', 'utf-8'); } catch (e) {}
    } catch (e) {}
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true }));
  }

  if (parsedUrl.pathname === '/api/audio-proxy') {
    const targetUrl = parsedUrl.query.url || '';
    return handleAudioProxy(req, res, targetUrl);
  }

  if (parsedUrl.pathname === '/api/save-custom-lyrics' && req.method === 'POST') {
    const title = parsedUrl.query.title || '';
    const artist = parsedUrl.query.artist || '';
    return handleSaveCustomLyrics(req, res, title, artist);
  }

  if (parsedUrl.pathname === '/api/lyrics-words') {
    const title = parsedUrl.query.title || '';
    const artist = parsedUrl.query.artist || '';
    const duration = parseFloat(parsedUrl.query.duration) || 0;
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'application/json');
    try {
      const lines = await fetchNetEaseWords(title, artist, duration);
      if (lines && lines.length > 0) {
        res.writeHead(200);
        return res.end(JSON.stringify({ success: true, lines: lines }));
      }
    } catch (_) {}
    res.writeHead(200);
    return res.end(JSON.stringify({ success: false, lines: [] }));
  }

  if (parsedUrl.pathname === '/api/lyrics') {
    const title = parsedUrl.query.title || '';
    const artist = parsedUrl.query.artist || '';
    const allowOnline = parsedUrl.query.online !== '0';
    const source = parsedUrl.query.source || 'lrclib';
    const duration = parseFloat(parsedUrl.query.duration) || 0;
    return handleLyrics(req, res, title, artist, allowOnline, source, duration);
  }

  res.writeHead(404);
  res.end('Not found');
}

function startServer(port = PORT) {
  if (server.listening) {
    const address = server.address();
    return Promise.resolve(address && typeof address === 'object' ? address.port : port);
  }
  return new Promise((resolve, reject) => {
    server.listen(port, '127.0.0.1', () => {
      const p = server.address().port;
      ACTIVE_PORT = p;
      try { logger.log('info', `server.js build fix9 listening on ${p} (ACTIVE_PORT)`); } catch (_) {}
      console.log(`devsize internal server running on http://127.0.0.1:${p}`);
      require('./tools').updateYtDlpIfManaged().catch(() => {});
      resolve(p);
    }).on('error', (err) => {
      if (err.code === 'EADDRINUSE') {
        server.listen(0, '127.0.0.1', () => {
          const p = server.address().port;
          ACTIVE_PORT = p;
          try { logger.log('warn', `server.js build fix9: port ${PORT} busy, fallback to ${p}`); } catch (_) {}
          console.log(`devsize internal server running on fallback port http://127.0.0.1:${p}`);
          require('./tools').updateYtDlpIfManaged().catch(() => {});
          resolve(p);
        });
      } else {
        reject(err);
      }
    });
  });
}

function stopServer() {
  if (!server.listening) return Promise.resolve();
  return new Promise((resolve, reject) => {
    server.close((error) => {
      if (error) reject(error);
      else resolve();
    });
  });
}

module.exports = { startServer, stopServer, PORT, getServerToken, fetchLyricsDirect, fetchLrcLib, fetchGeniusLyrics, fetchNetEaseWords, fetchKugouLyrics, fetchMusixmatchLyrics, fetchNeteaseLyrics, fetchSoundCloudSearch, ytmusicSearch, parseYrc, cleanSearchResults };

if (require.main === module) {
  startServer();
}