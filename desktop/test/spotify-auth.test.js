'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const http = require('node:http');
const https = require('node:https');
const Module = require('node:module');
const { PassThrough } = require('node:stream');

function get(port, route, token) {
  return new Promise((resolve, reject) => {
    http.get({ host: '127.0.0.1', port, path: route, headers: { 'X-Riff-Token': token } }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        let data;
        try { data = JSON.parse(body); } catch (_) { data = body; }
        resolve({ status: res.statusCode, data });
      });
    }).on('error', reject);
  });
}

test('Spotify login persists securely, refreshes, restores, and rejects partial link imports', async () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'riff-spotify-'));
  const oldDataDir = process.env.RIFF_DATA_DIR;
  const oldElectronVersion = process.versions.electron;
  const oldLoad = Module._load;
  const oldRequest = https.request;
  const oldGet = https.get;
  const safeStorage = {
    isEncryptionAvailable: () => true,
    encryptString: value => Buffer.from(`sealed:${Buffer.from(value).toString('base64')}`),
    decryptString: value => Buffer.from(value.toString().slice(7), 'base64').toString()
  };
  let refreshes = 0;
  let rejectRefresh = false;
  let embedRequests = 0;
  let embedTotal = 2;
  let embedUnavailable = false;
  let refreshFailures = 1;
  let refreshNetworkFailures = 1;
  let rateLimits = 2;
  let longRateLimit = '';
  let trackRequests = 0;
  let apiUnavailable = false;
  let server;

  const reply = (callback, status, data) => {
    const response = new PassThrough();
    response.statusCode = status;
    response.headers = status === 429 ? { 'retry-after': longRateLimit || '0' } : {};
    process.nextTick(() => { callback(response); response.end(typeof data === 'string' ? data : JSON.stringify(data)); });
    return response;
  };

  try {
    process.env.RIFF_DATA_DIR = dir;
    process.versions.electron = '41';
    Module._load = function (name, parent, isMain) {
      if (name === 'electron') return { safeStorage };
      return oldLoad.call(this, name, parent, isMain);
    };
    https.request = (_url, _options, callback) => {
      let body = '';
      return {
        on(event, handler) { if (event === 'error') this.errorHandler = handler; return this; },
        write(chunk) { body += chunk; },
        end() {
          const params = new URLSearchParams(body);
          if (params.get('grant_type') === 'authorization_code') {
            reply(callback, 200, { access_token: 'old', refresh_token: 'refresh-1', expires_in: 1 });
          } else {
            assert.strictEqual(params.get('grant_type'), 'refresh_token');
            assert.strictEqual(params.get('refresh_token'), `refresh-${refreshes + 1}`);
            if (rejectRefresh) return reply(callback, 400, { error: 'invalid_grant' });
            if (refreshNetworkFailures-- > 0) return process.nextTick(() => this.errorHandler(new Error('Temporary network failure')));
            if (refreshFailures-- > 0) return reply(callback, 503, { error: 'temporarily_unavailable' });
            refreshes++;
            reply(callback, 200, { access_token: `new-${refreshes}`, refresh_token: `refresh-${refreshes + 1}`, expires_in: 3600 });
          }
        },
        destroy() {}
      };
    };
    https.get = (url, options, callback) => {
      const parsed = new URL(url);
      if (parsed.hostname === 'open.spotify.com') {
        embedRequests++;
        const trackList = [{ title: 'One', subtitle: 'Artist', uri: `spotify:track:${'d'.repeat(22)}` }];
        if (embedUnavailable) trackList.push({ title: '', subtitle: '' });
        return reply(callback, 200, `<script id="__NEXT_DATA__">${JSON.stringify({ props: { pageProps: { state: { data: { entity: { name: 'Public', totalCount: embedTotal, trackList } } } } } })}</script>`);
      }
      assert.strictEqual(options.headers.Authorization, `Bearer new-${refreshes}`);
      if (parsed.pathname === '/v1/me/tracks') {
        trackRequests++;
        if (rateLimits-- > 0) return reply(callback, 429, { error: 'rate_limited' });
        return reply(callback, 200, { items: [], next: null });
      }
      if (parsed.pathname === `/v1/playlists/${'a'.repeat(22)}`) return reply(callback, 200, apiUnavailable
        ? { name: 'Available', items: { items: [{ item: { id: 'b'.repeat(22), name: 'One', artists: [{ name: 'Artist' }] } }, { item: null }], next: null, total: 2 } }
        : { name: 'Complete', items: { items: Array.from({ length: 100 }, (_, i) => ({ item: { id: String(i).padStart(22, '0'), name: `Track ${i}`, artists: [{ name: 'Artist' }] } })), next: `https://api.spotify.com/v1/playlists/${'a'.repeat(22)}/items?offset=100`, total: 101 } });
      if (parsed.pathname === `/v1/playlists/${'a'.repeat(22)}/items`) return reply(callback, 200, { items: [{ item: { id: 'c'.repeat(22), name: 'Track 100', artists: [{ name: 'Artist' }] } }], next: null });
      throw new Error(`Unexpected request: ${url}`);
    };

    server = require('../src/server/server');
    let port = await server.startServer(0);
    const token = server.getServerToken();
    const start = await get(port, `/api/spotify-auth-start?clientId=${'a'.repeat(32)}`, token);
    const state = new URL(start.data.url).searchParams.get('state');
    assert.strictEqual((await get(port, `/api/spotify-callback?state=${state}&code=ok`, token)).status, 200);
    assert.strictEqual((await get(port, '/api/spotify-status', token)).data.connected, true);
    assert.strictEqual(refreshes, 1);
    const stored = fs.readFileSync(path.join(dir, 'spotify-auth.enc'));
    assert.ok(!stored.includes(Buffer.from('refresh-2')));
    assert.strictEqual((await get(port, '/api/spotify-library?likes=1', token)).status, 200);
    for (const retryAfter of ['60', new Date(Date.now() + 60000).toUTCString()]) {
      longRateLimit = retryAfter;
      rateLimits = 1;
      const before = trackRequests;
      const limited = await get(port, '/api/spotify-library?likes=1', token);
      assert.strictEqual(limited.status, 429);
      assert.match(limited.data.error, /Try again later/);
      assert.strictEqual(trackRequests, before + 1);
    }
    longRateLimit = '';
    const linked = await get(port, `/api/spotify-import?url=https://open.spotify.com/playlist/${'a'.repeat(22)}`, token);
    assert.strictEqual(linked.status, 200);
    assert.strictEqual(linked.data.tracks.length, 101);
    assert.strictEqual(linked.data.tracks[100].title, 'Track 100');
    assert.strictEqual(linked.data.tracks[100].spotifyId, 'c'.repeat(22));
    assert.strictEqual(linked.data.tracks[100].spotifyUrl, `https://open.spotify.com/track/${'c'.repeat(22)}`);
    assert.strictEqual(embedRequests, 0);
    apiUnavailable = true;
    const available = await get(port, `/api/spotify-import?url=https://open.spotify.com/playlist/${'a'.repeat(22)}`, token);
    assert.strictEqual(available.status, 200);
    assert.deepStrictEqual(available.data.tracks.map(track => track.title), ['One']);
    assert.strictEqual(available.data.tracks[0].spotifyId, 'b'.repeat(22));

    await server.stopServer();
    delete require.cache[require.resolve('../src/server/server')];
    server = require('../src/server/server');
    port = await server.startServer(0);
    const restoredToken = server.getServerToken();
    assert.strictEqual((await get(port, '/api/spotify-status', restoredToken)).data.connected, true);
    assert.strictEqual(refreshes, 2);
    assert.strictEqual((await get(port, '/api/spotify-logout', restoredToken)).status, 200);
    assert.ok(!fs.existsSync(path.join(dir, 'spotify-auth.enc')));
    assert.strictEqual((await get(port, '/api/spotify-status', restoredToken)).data.connected, false);
    const partial = await get(port, `/api/spotify-import?url=https://open.spotify.com/playlist/${'a'.repeat(22)}`, restoredToken);
    assert.strictEqual(partial.status, 409);
    assert.strictEqual(partial.data.code, 'spotify_incomplete');
    embedTotal = 1;
    const publicPlaylist = await get(port, `/api/spotify-import?url=https://open.spotify.com/playlist/${'a'.repeat(22)}`, restoredToken);
    assert.strictEqual(publicPlaylist.status, 200);
    assert.strictEqual(publicPlaylist.data.tracks[0].title, 'One');
    assert.strictEqual(publicPlaylist.data.tracks[0].spotifyId, 'd'.repeat(22));
    embedTotal = 2;
    embedUnavailable = true;
    const withUnavailable = await get(port, `/api/spotify-import?url=https://open.spotify.com/playlist/${'a'.repeat(22)}`, restoredToken);
    assert.strictEqual(withUnavailable.status, 200);
    assert.strictEqual(withUnavailable.data.tracks.length, 1);
    assert.strictEqual(withUnavailable.data.hasMore, false);

    refreshes = 0;
    const again = await get(port, `/api/spotify-auth-start?clientId=${'a'.repeat(32)}`, restoredToken);
    const againState = new URL(again.data.url).searchParams.get('state');
    assert.strictEqual((await get(port, `/api/spotify-callback?state=${againState}&code=ok`, restoredToken)).status, 200);
    await server.stopServer();
    delete require.cache[require.resolve('../src/server/server')];
    server = require('../src/server/server');
    port = await server.startServer(0);
    rejectRefresh = true;
    const expired = await get(port, '/api/spotify-status', server.getServerToken());
    assert.strictEqual(expired.data.connected, false);
    assert.match(expired.data.error, /expired/i);
    assert.ok(!fs.existsSync(path.join(dir, 'spotify-auth.enc')));
  } finally {
    if (server) await server.stopServer();
    https.request = oldRequest;
    https.get = oldGet;
    Module._load = oldLoad;
    if (oldDataDir === undefined) delete process.env.RIFF_DATA_DIR;
    else process.env.RIFF_DATA_DIR = oldDataDir;
    if (oldElectronVersion === undefined) delete process.versions.electron;
    else process.versions.electron = oldElectronVersion;
    fs.rmSync(dir, { recursive: true, force: true });
  }
});
