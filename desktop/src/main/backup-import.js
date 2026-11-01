// Importa un backup Android (zip con song.db + settings.preferences_pb) nel formato di Riff desktop.
// Usa node:sqlite (solo lettura) e un lettore zip minimale: nessuna dipendenza nuova.
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

function readZipEntries(buf) {
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65557); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) { eocd = i; break; }
  }
  if (eocd < 0) throw new Error('Archivio non valido');
  const count = buf.readUInt16LE(eocd + 10);
  let p = buf.readUInt32LE(eocd + 16);
  const out = {};
  for (let n = 0; n < count; n++) {
    if (buf.readUInt32LE(p) !== 0x02014b50) throw new Error('Directory zip corrotta');
    const method = buf.readUInt16LE(p + 10);
    const compSize = buf.readUInt32LE(p + 20);
    const nameLen = buf.readUInt16LE(p + 28), extraLen = buf.readUInt16LE(p + 30), commentLen = buf.readUInt16LE(p + 32);
    const localOff = buf.readUInt32LE(p + 42);
    const name = buf.toString('utf8', p + 46, p + 46 + nameLen);
    const lNameLen = buf.readUInt16LE(localOff + 26), lExtraLen = buf.readUInt16LE(localOff + 28);
    const start = localOff + 30 + lNameLen + lExtraLen;
    const raw = buf.subarray(start, start + compSize);
    out[name] = method === 8 ? zlib.inflateRawSync(raw) : raw;
    p += 46 + nameLen + extraLen + commentLen;
  }
  return out;
}

function trackFrom(row, artistName, now) {
  return {
    id: row.id,
    title: row.title || '',
    artist: artistName || '',
    album: row.albumId ? { id: row.albumId, name: row.albumName || '' } : null,
    thumbnail: row.thumbnailUrl || '',
    duration: row.duration || 0,
    addedAt: now
  };
}

function buildPayload(dbPath) {
  const { DatabaseSync } = require('node:sqlite');
  const db = new DatabaseSync(dbPath, { readOnly: true });
  const now = Date.now();
  const artistOf = new Map();
  for (const r of db.prepare(`SELECT m.songId, a.name FROM song_artist_map m JOIN artist a ON a.id = m.artistId ORDER BY m.position`).all()) {
    if (!artistOf.has(r.songId)) artistOf.set(r.songId, r.name);
  }
  const songs = new Map();
  for (const r of db.prepare(`SELECT id, title, duration, thumbnailUrl, albumId, albumName FROM song`).all()) {
    songs.set(r.id, r);
  }
  const trackOf = (id) => {
    const s = songs.get(id);
    return s ? trackFrom(s, artistOf.get(id), now) : null;
  };

  const favorites = [];
  for (const r of db.prepare(`SELECT id FROM song WHERE liked = 1 ORDER BY likedDate DESC`).all()) {
    const t = trackOf(r.id); if (t) favorites.push(t);
  }

  const playlists = [];
  const plRows = db.prepare(`SELECT id, name, thumbnailUrl FROM playlist WHERE name IS NOT NULL ORDER BY createdAt`).all();
  const plSongs = db.prepare(`SELECT songId FROM playlist_song_map WHERE playlistId = ? ORDER BY position`);
  for (const pl of plRows) {
    const tracks = [];
    for (const r of plSongs.all(pl.id)) { const t = trackOf(r.songId); if (t) tracks.push(t); }
    const entry = { id: 'pl_' + String(pl.id).replace(/[^a-zA-Z0-9]/g, '').slice(0, 24), name: pl.name, tracks };
    if (pl.thumbnailUrl) entry.cover = pl.thumbnailUrl;
    playlists.push(entry);
  }

  const artists = db.prepare(`SELECT name, thumbnailUrl FROM artist WHERE bookmarkedAt IS NOT NULL AND name IS NOT NULL`).all()
    .map((a) => ({ name: a.name, avatar: a.thumbnailUrl || '', fans: 0 }));

  const albums = db.prepare(`SELECT al.id, al.title, al.thumbnailUrl, al.albumType,
      (SELECT a.name FROM album_artist_map m JOIN artist a ON a.id = m.artistId WHERE m.albumId = al.id ORDER BY m."order" LIMIT 1) AS artist
    FROM album al WHERE al.bookmarkedAt IS NOT NULL AND al.title IS NOT NULL`).all()
    .map((a) => ({ id: a.id, title: a.title, artist: a.artist || '', cover: a.thumbnailUrl || '', type: a.albumType || 'Album' }));

  db.close();
  return { devsize_favorites: favorites, devsize_playlists: playlists, riff_favorite_artists: artists, riff_saved_albums: albums };
}

function importBackupBuffer(buf, tmpDir) {
  const entries = readZipEntries(buf);
  if (!entries['song.db']) throw new Error('song.db non trovato nel backup');
  fs.mkdirSync(tmpDir, { recursive: true });
  const dbPath = path.join(tmpDir, 'song-import.db');
  fs.writeFileSync(dbPath, entries['song.db']);
  try {
    return buildPayload(dbPath);
  } finally {
    try { fs.unlinkSync(dbPath); } catch (_) {}
  }
}

module.exports = { importBackupBuffer, readZipEntries };
