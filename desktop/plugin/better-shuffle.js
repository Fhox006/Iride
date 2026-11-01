/**
 * @name Better Shuffle
 * @author community
 * @version 2.0.0
 * @description Spotify-style shuffle that is not trapped in your search results, plus Smart Shuffle. Right-click the shuffle button to switch mode.
 */
if (!Riff.shuffle || !Riff.queue || !Riff.api || !Riff.library) {
  Riff.toast('Better Shuffle requires the current plugin API.', 'error');
  return {};
}

const MODES = ['classic', 'spotify', 'smart'];
const NAMES = {
  classic: 'Classic shuffle (Riff default)',
  spotify: 'Spotify-style shuffle',
  smart: 'Smart shuffle'
};
const CFG = {
  expandSearch: true,
  artistsToExpand: 4,
  perArtist: 10,
  maxExpand: 50,
  smartEvery: 3,
  smartBatch: 10,
  skipSec: 30,
  toastSmart: true,
  timeoutMs: 8000
};

let mode = Riff.storage.get('mode', 'smart');
if (!MODES.includes(mode)) mode = 'smart';

const rand = Math.random;
const artistKey = (t) => String((t && t.artist) || 'unknown').toLowerCase().replace(/\s+/g, ' ').trim();
const tkey = (t) => (t.platform || '') + ':' + (t.id || t.url || (t.title + '|' + t.artist));
const loose = (t) => String((t && t.title) || '').toLowerCase().trim() + '::' + artistKey(t);

function shuffled(arr) {
  const a = arr.slice();
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function withTimeout(promise, ms) {
  let timer;
  const timeout = new Promise((resolve) => { timer = setTimeout(() => resolve([]), ms); });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

function fixAdjacent(list, prevKey) {
  const a = list.slice();
  const bad = (i) => {
    const k = artistKey(a[i]);
    const before = i === 0 ? prevKey : artistKey(a[i - 1]);
    const after = i + 1 < a.length ? artistKey(a[i + 1]) : null;
    return k === before || k === after;
  };
  for (let i = 0; i < a.length; i++) {
    const before = i === 0 ? prevKey : artistKey(a[i - 1]);
    if (artistKey(a[i]) !== before) continue;
    for (let j = i + 1; j < a.length; j++) {
      [a[i], a[j]] = [a[j], a[i]];
      if (!bad(i) && !bad(j)) break;
      [a[i], a[j]] = [a[j], a[i]];
    }
  }
  return a;
}

function arrange(tracks, prevKey) {
  const groups = new Map();
  tracks.forEach((t) => {
    const k = artistKey(t);
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k).push(t);
  });
  const scored = [];
  groups.forEach((g) => {
    const s = shuffled(g);
    s.forEach((t, i) => scored.push({ t, pos: (i + rand()) / s.length }));
  });
  scored.sort((x, y) => x.pos - y.pos);
  return fixAdjacent(scored.map((x) => x.t), prevKey || '');
}

const taste = Riff.storage.get('taste', { artists: {}, skips: {} });
const saveTaste = () => { try { Riff.storage.set('taste', taste); } catch (e) {} };
function bump(t, d) {
  const k = artistKey(t);
  taste.artists[k] = Math.max(-10, Math.min(30, (taste.artists[k] || 0) + d));
  saveTaste();
}
function affinity(t) {
  let favBoost = 0;
  try {
    const k = artistKey(t);
    favBoost = Riff.library.favorites().filter((f) => artistKey(f) === k).length * 1.5;
  } catch (e) {}
  return (taste.artists[artistKey(t)] || 0) + Math.min(favBoost, 6) - (taste.skips[tkey(t)] || 0) * 1.5;
}

let S = null;
let sid = 0;

function newSession(ctx) {
  const cur = ctx.current;
  S = {
    id: ++sid,
    context: ctx.context,
    base: new Set(ctx.queue.map(tkey)),
    pool: new Map(),
    played: new Set(),
    upcoming: [],
    smartPool: [],
    smartKeys: new Set(),
    sinceSmart: 0,
    lastArtist: cur ? artistKey(cur) : ''
  };
  ctx.queue.forEach((t) => S.pool.set(tkey(t), t));
  if (cur) S.played.add(tkey(cur));
  S.upcoming = arrange(ctx.queue.filter((t) => !cur || tkey(t) !== tkey(cur)), S.lastArtist);
  enrich(S, ctx);
}

function sessionValid(ctx) {
  if (!S || S.context !== ctx.context || !S.base.size) return false;
  const have = new Set(ctx.queue.map(tkey));
  let kept = 0;
  S.base.forEach((k) => { if (have.has(k)) kept++; });
  return kept / S.base.size >= 0.5;
}

function syncSession(ctx) {
  const cur = ctx.current;
  if (cur) {
    const ck = tkey(cur);
    S.played.add(ck);
    S.upcoming = S.upcoming.filter((t) => tkey(t) !== ck);
  }
  ctx.queue.forEach((t) => {
    const k = tkey(t);
    if (S.pool.has(k)) return;
    S.pool.set(k, t);
    S.upcoming.splice(Math.floor(rand() * (S.upcoming.length + 1)), 0, t);
  });
}

function refill(ctx) {
  const cur = ctx.current;
  const ck = cur ? tkey(cur) : '';
  S.played = new Set(ck ? [ck] : []);
  S.upcoming = arrange(Array.from(S.pool.values()).filter((t) => tkey(t) !== ck), S.lastArtist);
  if (mode === 'smart') recommend(S, ctx).catch(() => {});
}

function addToSession(s, added, isSmart) {
  if (!s || s.id !== sid) return;
  added.forEach((t) => {
    const k = tkey(t);
    if (s.pool.has(k)) return;
    s.pool.set(k, t);
    if (isSmart) {
      s.smartKeys.add(k);
      s.smartPool.push(t);
    } else {
      s.upcoming.splice(Math.floor(rand() * (s.upcoming.length + 1)), 0, t);
    }
  });
  if (!isSmart) s.upcoming = fixAdjacent(s.upcoming, s.lastArtist);
}

async function enrich(s, ctx) {
  try {
    if (CFG.expandSearch && ctx.context === 'search') await expandSearch(s, ctx);
    if (mode === 'smart') await recommend(s, ctx);
  } catch (e) { console.warn('[better-shuffle] enrich failed', e); }
}

async function expandSearch(s, ctx) {
  const counts = new Map();
  ctx.queue.forEach((t) => {
    const k = artistKey(t);
    if (!k || k === 'unknown') return;
    const c = counts.get(k) || { name: t.artist, n: 0 };
    c.n++;
    counts.set(k, c);
  });
  const artists = Array.from(counts.entries()).sort((a, b) => b[1].n - a[1].n).slice(0, CFG.artistsToExpand);
  const batches = await Promise.all(artists.map(([k, c]) =>
    withTimeout(Riff.api.search(c.name, { limit: CFG.perArtist }).catch(() => []), CFG.timeoutMs)
      .then((list) => (list || []).filter((t) => { const a = artistKey(t); return a.includes(k) || k.includes(a); }))
  ));
  if (s.id !== sid) return;
  const found = shuffled([].concat(...batches)).slice(0, CFG.maxExpand);
  addToSession(s, Riff.queue.append(found), false);
}

async function recommend(s, ctx) {
  const pool = Array.from(s.pool.values());
  const seeds = shuffled(pool).slice(0, 12);
  if (ctx.current) seeds.unshift(ctx.current);
  const avoid = {
    ids: pool.map((t) => String(t.id || '')).filter(Boolean).slice(0, 120),
    keys: pool.map(loose).slice(0, 120),
    artists: []
  };
  const recs = await withTimeout(Riff.api.vibe({
    history: seeds,
    favorites: Riff.library.favorites().slice(0, 10),
    limit: CFG.smartBatch,
    avoid
  }).catch(() => []), CFG.timeoutMs * 2);
  if (s.id !== sid) return;
  const fresh = (recs || []).filter((t) => t && t.title && !s.pool.has(tkey(t)));
  addToSession(s, Riff.queue.append(fresh), true);
}

function takeSmart() {
  if (!S.smartPool.length) return null;
  let best = -1;
  let bestScore = -Infinity;
  S.smartPool.forEach((t, i) => {
    if (artistKey(t) === S.lastArtist) return;
    const sc = affinity(t) + rand() * 1.5;
    if (sc > bestScore) { bestScore = sc; best = i; }
  });
  return best < 0 ? null : S.smartPool.splice(best, 1)[0];
}

function takeUser() {
  const i = S.upcoming.findIndex((t) => artistKey(t) !== S.lastArtist);
  return S.upcoming.splice(i < 0 ? 0 : i, 1)[0];
}

async function pick(ctx) {
  if (mode === 'classic' || !ctx.queue.length) return null;
  if (ctx.queue.length === 1) return ctx.queue[0];
  if (!sessionValid(ctx)) newSession(ctx); else syncSession(ctx);
  if (!S.upcoming.length && !S.smartPool.length) refill(ctx);

  let next = null;
  if (mode === 'smart' && S.sinceSmart >= CFG.smartEvery) {
    next = takeSmart();
    if (next) S.sinceSmart = 0;
  }
  if (!next && S.upcoming.length) { next = takeUser(); S.sinceSmart++; }
  if (!next) next = takeSmart();
  if (!next) return null;

  S.played.add(tkey(next));
  S.lastArtist = artistKey(next);
  return next;
}

const offProvider = Riff.shuffle.setProvider(pick);

const audio = Riff.audio();
let lastPos = 0;
let prevTrack = null;
const onTime = () => { if (audio && audio.currentTime > 0) lastPos = audio.currentTime; };
if (audio && audio.addEventListener) audio.addEventListener('timeupdate', onTime);

const offTrack = Riff.on('trackchange', (track) => {
  if (prevTrack && Riff.shuffle.isOn()) {
    if (lastPos > 0 && lastPos < CFG.skipSec) {
      bump(prevTrack, -1);
      taste.skips[tkey(prevTrack)] = (taste.skips[tkey(prevTrack)] || 0) + 1;
      saveTaste();
    } else if (lastPos >= 60 || (prevTrack.duration && lastPos >= prevTrack.duration * 0.6)) {
      bump(prevTrack, 1);
    }
  }
  prevTrack = track;
  lastPos = 0;
  if (CFG.toastSmart && S && track && S.smartKeys.has(tkey(track))) Riff.toast('Smart shuffle pick: ' + track.title, 'info');
});

const removeStyle = Riff.addStyle(
  '#btn-shuffle{position:relative}' +
  '#btn-shuffle[data-shuffle-mode="smart"]::after{content:"\\2726";position:absolute;top:0;right:2px;font-size:11px;line-height:1;color:currentColor}' +
  '#btn-shuffle[data-shuffle-mode="classic"]::after{content:"";}'
);
const btn = Riff.query('#btn-shuffle');
const origTitle = btn ? btn.title : '';
function paint() {
  if (!btn) return;
  btn.setAttribute('data-shuffle-mode', mode);
  btn.title = NAMES[mode] + ' (right-click to change)';
}
const onContext = (e) => {
  e.preventDefault();
  mode = MODES[(MODES.indexOf(mode) + 1) % MODES.length];
  Riff.storage.set('mode', mode);
  S = null;
  paint();
  Riff.toast(NAMES[mode], 'info');
};
if (btn) btn.addEventListener('contextmenu', onContext);
paint();

return {
  stop() {
    offProvider();
    offTrack();
    removeStyle();
    if (audio && audio.removeEventListener) audio.removeEventListener('timeupdate', onTime);
    if (btn) {
      btn.removeEventListener('contextmenu', onContext);
      btn.removeAttribute('data-shuffle-mode');
      btn.title = origTitle;
    }
    S = null;
  }
};
