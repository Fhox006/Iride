'use strict';

const test = require('node:test');
const assert = require('node:assert');
const http = require('node:http');
const https = require('node:https');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { PassThrough } = require('node:stream');

const { startServer, stopServer, getServerToken } = require('../src/server/server');

function request(port, path, headers = {}) {
  return new Promise((resolve, reject) => {
    const req = http.get({ host: '127.0.0.1', port, path, headers }, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => resolve({ statusCode: res.statusCode, body }));
    });
    req.on('error', reject);
  });
}

test('startServer resolves the real bound port (regression)', async () => {
  const port = await startServer(0);
  try {
    assert.ok(Number.isInteger(port) && port > 0, `expected a real port, got ${port}`);
    assert.ok(port <= 65535);
  } finally {
    await stopServer();
  }
});

test('server responds and rejects untrusted routes', async () => {
  const port = await startServer(0);
  try {
    const missing = await request(port, '/');
    assert.strictEqual(missing.statusCode, 404);

    const guarded = await request(port, '/api/tools-status');
    assert.strictEqual(guarded.statusCode, 403, 'token-protected route must reject requests without a token');

    const authorized = await request(port, '/api/tools-status', { 'x-riff-token': getServerToken() });
    assert.strictEqual(authorized.statusCode, 200);
    assert.doesNotThrow(() => JSON.parse(authorized.body));
  } finally {
    await stopServer();
  }
});

test('requests with a foreign Origin are rejected', async () => {
  const port = await startServer(0);
  try {
    const res = await request(port, '/', { origin: 'https://evil.example.com' });
    assert.strictEqual(res.statusCode, 403);
  } finally {
    await stopServer();
  }
});

test('Spotify login reports callback errors and protects account routes', async () => {
  const port = await startServer(0);
  try {
    for (const route of ['/api/spotify-auth-start?clientId=' + 'a'.repeat(32), '/api/spotify-status', '/api/spotify-library?likes=1', '/api/spotify-logout']) {
      const denied = await request(port, route);
      assert.strictEqual(denied.statusCode, 403, route);
    }
    const headers = { 'x-riff-token': getServerToken() };
    const start = await request(port, '/api/spotify-auth-start?clientId=' + 'a'.repeat(32), headers);
    const auth = JSON.parse(start.body);
    assert.strictEqual(start.statusCode, 200);
    assert.strictEqual(new URL(auth.url).searchParams.get('redirect_uri'), auth.redirectUri);
    const state = new URL(auth.url).searchParams.get('state');
    const callback = await request(port, `/api/spotify-callback?state=${state}&error=access_denied`);
    assert.strictEqual(callback.statusCode, 400);
    const status = JSON.parse((await request(port, '/api/spotify-status', headers)).body);
    assert.strictEqual(status.connected, false);
    assert.match(status.error, /denied/i);
  } finally {
    await stopServer();
  }
});

test('renderer fetch sends the server token to Spotify account routes', async () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/renderer/app.js'), 'utf8');
  const renderer = { electronAPI: { serverToken: getServerToken() }, fetch: fetch.bind(globalThis) };
  const patchStart = source.indexOf("    if (typeof window === 'undefined' || window.__riffFetchPatched)");
  const wrapperStart = source.lastIndexOf('(() => {', patchStart);
  const wrapperEnd = source.indexOf('})();', patchStart) + 5;
  assert.ok(wrapperStart >= 0 && wrapperEnd > patchStart);
  vm.runInNewContext(source.slice(wrapperStart, wrapperEnd), { window: renderer, Headers, URL });
  const port = await startServer(0);
  try {
    for (const route of ['/api/spotify-auth-start?clientId=' + 'a'.repeat(32), '/api/spotify-status', '/api/spotify-library?likes=1', '/api/spotify-logout']) {
      const res = await renderer.fetch(`http://127.0.0.1:${port}${route}`);
      assert.notStrictEqual(res.status, 403, route);
    }
  } finally {
    await stopServer();
  }
});

test('Spotify library reports skipped playlists and rejects malformed pages', async () => {
  let scenario = 'normal';
  const originalRequest = https.request;
  const originalGet = https.get;
  const reply = (callback, statusCode, data) => {
    const response = new PassThrough();
    response.statusCode = statusCode;
    response.headers = {};
    process.nextTick(() => {
      callback(response);
      response.end(typeof data === 'string' ? data : JSON.stringify(data));
    });
    return { on() { return this; }, write() {}, end() {}, destroy() {} };
  };
  https.request = (_url, _options, callback) => reply(callback, 200, { access_token: 'mock-token' });
  https.get = (url, _options, callback) => {
    const pathname = new URL(url).pathname;
    if (pathname === '/v1/me/tracks') return reply(callback, 200, scenario === 'malformed' ? '{' : scenario === 'missing-items' ? { next: null } : { items: [{ track: { name: 'Saved', artists: [{ name: 'Artist' }] } }], next: null });
    if (pathname === '/v1/me/playlists') return reply(callback, 200, { items: [{ id: 'followed', name: 'Followed' }, { id: 'owned', name: 'Owned' }], next: null });
    if (pathname === '/v1/playlists/followed/items') return reply(callback, scenario === 'first-403' ? 403 : 200, { items: [], next: null });
    if (pathname === '/v1/playlists/owned/items' && new URL(url).searchParams.has('offset')) return reply(callback, scenario === 'later-malformed' ? 200 : 403, scenario === 'later-malformed' ? '{' : { error: { message: 'Forbidden' } });
    if (pathname === '/v1/playlists/owned/items') return reply(callback, 200, { items: [{ item: { name: 'Track', artists: [{ name: 'Artist' }] } }], next: scenario === 'later-403' || scenario === 'later-malformed' ? 'https://api.spotify.com/v1/playlists/owned/items?offset=50' : null });
    throw new Error(`Unexpected Spotify request: ${pathname}`);
  };
  try {
    const port = await startServer(0);
    try {
      const headers = { 'x-riff-token': getServerToken() };
      const auth = JSON.parse((await request(port, '/api/spotify-auth-start?clientId=' + 'a'.repeat(32), headers)).body);
      const state = new URL(auth.url).searchParams.get('state');
      assert.strictEqual((await request(port, `/api/spotify-callback?state=${state}&code=mock`)).statusCode, 200);
      const response = await request(port, '/api/spotify-library?likes=1&playlists=1', headers);
      assert.strictEqual(response.statusCode, 200);
      const data = JSON.parse(response.body);
      assert.strictEqual(data.likes[0].title, 'Saved');
      assert.strictEqual(data.playlists[1].name, 'Owned');
      assert.strictEqual(data.playlists[1].tracks[0].title, 'Track');

      scenario = 'first-403';
      const deniedFirstPage = await request(port, '/api/spotify-library?likes=1&playlists=1', headers);
      assert.strictEqual(deniedFirstPage.statusCode, 200);
      const firstPageData = JSON.parse(deniedFirstPage.body);
      assert.strictEqual(firstPageData.partial, true);
      assert.strictEqual(firstPageData.skippedPlaylists[0].name, 'Followed');
      assert.match(firstPageData.skippedPlaylists[0].error, /HTTP 403/);
      assert.deepStrictEqual(firstPageData.playlists.map(pl => pl.name), ['Owned']);

      scenario = 'later-403';
      const deniedPage = await request(port, '/api/spotify-library?likes=1&playlists=1', headers);
      assert.strictEqual(deniedPage.statusCode, 200);
      const laterPageData = JSON.parse(deniedPage.body);
      assert.strictEqual(laterPageData.partial, true);
      assert.deepStrictEqual(laterPageData.playlists.map(pl => pl.name), ['Followed']);
      assert.strictEqual(laterPageData.skippedPlaylists[0].name, 'Owned');

      scenario = 'later-malformed';
      const malformedPlaylist = await request(port, '/api/spotify-library?likes=1&playlists=1', headers);
      assert.strictEqual(malformedPlaylist.statusCode, 500);
      assert.strictEqual(JSON.parse(malformedPlaylist.body).success, false);

      scenario = 'malformed';
      const malformedPage = await request(port, '/api/spotify-library?likes=1&playlists=1', headers);
      assert.strictEqual(malformedPage.statusCode, 500);
      assert.strictEqual(JSON.parse(malformedPage.body).success, false);

      scenario = 'missing-items';
      const invalidPage = await request(port, '/api/spotify-library?likes=1&playlists=1', headers);
      assert.strictEqual(invalidPage.statusCode, 500);
      assert.strictEqual(JSON.parse(invalidPage.body).success, false);
    } finally {
      await stopServer();
    }
  } finally {
    https.request = originalRequest;
    https.get = originalGet;
  }
});

test('renderer requires consent before importing a partial Spotify library', async () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/renderer/app.js'), 'utf8');
  const start = source.indexOf('  async function handleSpotifyLoginImport()');
  const end = source.indexOf('  async function handleSpotifyImportAction()', start);
  assert.ok(start > 0 && end > start);
  let approved = false;
  let imported = null;
  let prompt = '';
  const context = {
    spChkLikes2: { checked: true }, spChkPlaylists2: { checked: true },
    spotifyStatus: { textContent: '', style: {}, classList: { remove() {} } },
    btnConfirmSpotifyImport: { disabled: false }, btnCancelSpotifyImport: { disabled: false },
    state: { serverPort: 1234 }, spotifyHeaders: { 'X-Riff-Token': 'secret' },
    window: { confirm(message) { prompt = message; return approved; } },
    closeSpotifyImportModal() {},
    showImportSpinnerToast() {},
    dismissImportSpinnerToast() {},
    fetch: async (url, options) => {
      assert.match(url, /\/api\/spotify-library\?/);
      assert.strictEqual(options.headers['X-Riff-Token'], 'secret');
      return { json: async () => ({ success: true, partial: true, likes: [{ title: 'Saved' }], playlists: [], skippedPlaylists: [{ name: 'Blocked', error: 'HTTP 403' }] }) };
    },
    runSpotifyImport: async data => { imported = data; }
  };
  const handler = vm.runInNewContext(`${source.slice(start, end)}\nhandleSpotifyLoginImport`, context);
  await handler();
  assert.match(prompt, /Blocked: HTTP 403/);
  assert.strictEqual(imported, null);
  assert.strictEqual(context.btnConfirmSpotifyImport.disabled, false);
  approved = true;
  await handler();
  assert.strictEqual(imported.skippedPlaylists[0].name, 'Blocked');
});
