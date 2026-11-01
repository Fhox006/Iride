'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { cookieJarHasYtLogin } = require('../src/shared/platform');

const line = (domain, name, value = 'x') => [domain, 'TRUE', '/', 'TRUE', '9999999999', name, value].join('\t');
const jar = (...lines) => ['# Netscape HTTP Cookie File', ...lines].join('\n');

test('accepts a real YouTube login (LOGIN_INFO)', () => {
  assert.strictEqual(cookieJarHasYtLogin(jar(line('.youtube.com', 'LOGIN_INFO'))), true);
});

test('accepts a YouTube SAPISID + SID pair', () => {
  assert.strictEqual(cookieJarHasYtLogin(jar(
    line('.youtube.com', 'SAPISID'),
    line('.youtube.com', 'SID')
  )), true);
});

test('rejects Vivaldi-style partial cookies', () => {
  assert.strictEqual(cookieJarHasYtLogin(jar(
    line('.youtube.com', '__Secure-3PAPISID'),
    line('.youtube.com', '__Secure-3PSID')
  )), false);
});

test('rejects Google-only cookies without a YouTube login', () => {
  assert.strictEqual(cookieJarHasYtLogin(jar(
    line('.google.com', 'SID'),
    line('.google.com', 'SAPISID'),
    line('.google.de', 'SID')
  )), false);
});

test('rejects an empty or malformed jar', () => {
  assert.strictEqual(cookieJarHasYtLogin(''), false);
  assert.strictEqual(cookieJarHasYtLogin(null), false);
  assert.strictEqual(cookieJarHasYtLogin('# Netscape HTTP Cookie File'), false);
});
