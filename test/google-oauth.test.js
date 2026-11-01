'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const {
  GOOGLE_CLIENT_ID,
  GOOGLE_SCOPES,
  createCodeVerifier,
  createCodeChallenge,
  createOAuthState,
  createAuthorizationUrl,
  exchangeAuthorizationCode,
  refreshAccessToken
} = require('../src/main/google-oauth');
const { fetchYouTubeLibrary } = require('../src/main/youtube-data');

test('Google OAuth creates a PKCE authorization request for the configured desktop client', () => {
  const verifier = createCodeVerifier();
  const challenge = createCodeChallenge(verifier);
  const state = createOAuthState();
  const authorizationUrl = new URL(createAuthorizationUrl({
    redirectUri: 'http://127.0.0.1:49152/oauth2callback',
    state,
    codeChallenge: challenge
  }));

  assert.equal(authorizationUrl.searchParams.get('client_id'), GOOGLE_CLIENT_ID);
  assert.equal(authorizationUrl.searchParams.get('redirect_uri'), 'http://127.0.0.1:49152/oauth2callback');
  assert.equal(authorizationUrl.searchParams.get('response_type'), 'code');
  assert.equal(authorizationUrl.searchParams.get('code_challenge_method'), 'S256');
  assert.equal(authorizationUrl.searchParams.get('code_challenge'), challenge);
  assert.equal(authorizationUrl.searchParams.get('state'), state);
  assert.equal(authorizationUrl.searchParams.get('access_type'), 'offline');
  assert.equal(authorizationUrl.searchParams.get('prompt'), 'select_account consent');
  assert.ok(GOOGLE_SCOPES.includes('https://www.googleapis.com/auth/youtube.readonly'));
  assert.equal(createCodeChallenge(verifier), crypto.createHash('sha256').update(verifier).digest('base64url'));
});

test('Google OAuth exchanges and refreshes tokens without a client secret', async () => {
  const requests = [];
  const fetchImpl = async (url, options) => {
    requests.push({ url, options, body: new URLSearchParams(options.body) });
    return {
      ok: true,
      status: 200,
      json: async () => ({ access_token: 'access-token', refresh_token: 'refresh-token', expires_in: 3600 })
    };
  };

  const exchanged = await exchangeAuthorizationCode({
    code: 'authorization-code',
    redirectUri: 'http://127.0.0.1:49152/oauth2callback',
    codeVerifier: 'verifier'
  }, fetchImpl);
  const refreshed = await refreshAccessToken('refresh-token', fetchImpl);

  assert.equal(exchanged.access_token, 'access-token');
  assert.equal(refreshed.refresh_token, 'refresh-token');
  assert.equal(requests[0].body.get('grant_type'), 'authorization_code');
  assert.equal(requests[0].body.get('code'), 'authorization-code');
  assert.equal(requests[0].body.get('code_verifier'), 'verifier');
  assert.equal(requests[0].body.get('client_id'), GOOGLE_CLIENT_ID);
  assert.equal(requests[0].body.has('client_secret'), false);
  assert.equal(requests[1].body.get('grant_type'), 'refresh_token');
  assert.equal(requests[1].body.get('refresh_token'), 'refresh-token');
});

test('YouTube library import paginates likes and playlists and adds video durations', async () => {
  const requests = [];
  const fetchImpl = async (input, options) => {
    const url = new URL(input);
    requests.push(url);
    assert.equal(options.headers.Authorization, 'Bearer access-token');

    if (url.pathname.endsWith('/videos') && url.searchParams.get('myRating') === 'like') {
      if (url.searchParams.get('pageToken') === 'likes-page-2') {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            items: [{
              id: 'video-2',
              contentDetails: { duration: 'PT1H2M3S' },
              snippet: { title: 'Second', channelTitle: 'Artist B', thumbnails: { high: { url: 'https://img/2' } } }
            }]
          })
        };
      }
      return {
        ok: true,
        status: 200,
        json: async () => ({
          items: [{
            id: 'video-1',
            contentDetails: { duration: 'PT3M14S' },
            snippet: { title: 'First', channelTitle: 'Artist A', thumbnails: { medium: { url: 'https://img/1' } } }
          }],
          nextPageToken: 'likes-page-2'
        })
      };
    }

    if (url.pathname.endsWith('/playlists')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          items: [{
            id: 'PL_ONE',
            snippet: { title: 'My playlist', thumbnails: { high: { url: 'https://img/playlist' } } }
          }]
        })
      };
    }

    if (url.pathname.endsWith('/playlistItems')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          items: [{
            contentDetails: { videoId: 'video-3' },
            snippet: { title: 'Playlist track', channelTitle: 'Artist C' }
          }]
        })
      };
    }

    if (url.pathname.endsWith('/videos')) {
      return {
        ok: true,
        status: 200,
        json: async () => ({
          items: [{ id: 'video-3', contentDetails: { duration: 'PT45S' } }]
        })
      };
    }

    throw new Error(`Unexpected request: ${url}`);
  };

  const result = await fetchYouTubeLibrary('access-token', { platform: 'youtube' }, fetchImpl);

  assert.deepEqual(result.likes.map(track => [track.title, track.artist, track.duration]), [
    ['First', 'Artist A', 194],
    ['Second', 'Artist B', 3723]
  ]);
  assert.equal(result.likes[0].cover, 'https://img/1');
  assert.equal(result.likes[0].url, 'https://www.youtube.com/watch?v=video-1');
  assert.equal(result.playlists.length, 1);
  assert.equal(result.playlists[0].name, 'My playlist');
  assert.equal(result.playlists[0].cover, 'https://img/playlist');
  assert.equal(result.playlists[0].tracks[0].duration, 45);
  assert.ok(requests.some(url => url.searchParams.get('pageToken') === 'likes-page-2'));
});
