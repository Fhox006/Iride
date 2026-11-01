'use strict';

const crypto = require('crypto');

const GOOGLE_CLIENT_ID = '171523925886-acon5eeqsfuffmjpfvp315ev7d96s2pj.apps.googleusercontent.com';
const GOOGLE_SCOPES = [
  'https://www.googleapis.com/auth/youtube.readonly'
];

function createCodeVerifier() {
  return crypto.randomBytes(32).toString('base64url');
}

function createCodeChallenge(verifier) {
  return crypto.createHash('sha256').update(verifier).digest('base64url');
}

function createOAuthState() {
  return crypto.randomBytes(32).toString('base64url');
}

function createAuthorizationUrl({ redirectUri, state, codeChallenge }) {
  if (!redirectUri || !state || !codeChallenge) {
    throw new Error('Missing Google OAuth request parameters');
  }
  const url = new URL('https://accounts.google.com/o/oauth2/v2/auth');
  url.searchParams.set('client_id', GOOGLE_CLIENT_ID);
  url.searchParams.set('redirect_uri', redirectUri);
  url.searchParams.set('response_type', 'code');
  url.searchParams.set('scope', GOOGLE_SCOPES.join(' '));
  url.searchParams.set('state', state);
  url.searchParams.set('code_challenge', codeChallenge);
  url.searchParams.set('code_challenge_method', 'S256');
  url.searchParams.set('access_type', 'offline');
  url.searchParams.set('prompt', 'select_account consent');
  return url.toString();
}

async function requestToken(params, fetchImpl = fetch) {
  const response = await fetchImpl('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams(params),
    signal: AbortSignal.timeout(30000)
  });
  let data;
  try {
    data = await response.json();
  } catch (_) {
    data = {};
  }
  if (!response.ok || !data.access_token) {
    const description = data.error_description || data.error || `HTTP ${response.status}`;
    throw new Error(`Google token request failed: ${description}`);
  }
  return data;
}

function exchangeAuthorizationCode({ code, redirectUri, codeVerifier }, fetchImpl = fetch) {
  return requestToken({
    client_id: GOOGLE_CLIENT_ID,
    code,
    code_verifier: codeVerifier,
    grant_type: 'authorization_code',
    redirect_uri: redirectUri
  }, fetchImpl);
}

function refreshAccessToken(refreshToken, fetchImpl = fetch) {
  return requestToken({
    client_id: GOOGLE_CLIENT_ID,
    refresh_token: refreshToken,
    grant_type: 'refresh_token'
  }, fetchImpl);
}

module.exports = {
  GOOGLE_CLIENT_ID,
  GOOGLE_SCOPES,
  createCodeVerifier,
  createCodeChallenge,
  createOAuthState,
  createAuthorizationUrl,
  exchangeAuthorizationCode,
  refreshAccessToken
};
