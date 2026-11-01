'use strict';

const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const dataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'riff-bin-'));
process.env.RIFF_DATA_DIR = dataDir;
process.env.PATH = ['/usr/bin', '/bin', process.env.PATH || ''].join(path.delimiter);

const { resolveBinary, clearBinaryCache } = require('../src/shared/platform');

test('resolveBinary re-resolves after a cached managed binary disappears', () => {
  const binDir = path.join(dataDir, 'bin');
  const exe = process.platform === 'win32' ? 'yt-dlp.exe' : 'yt-dlp';
  const managed = path.join(binDir, exe);

  fs.mkdirSync(binDir, { recursive: true });
  fs.writeFileSync(managed, '');
  try { fs.chmodSync(managed, 0o755); } catch (_) {}

  clearBinaryCache();
  assert.strictEqual(resolveBinary('yt-dlp'), managed, 'managed binary should win over PATH');

  fs.unlinkSync(managed);
  const fallback = resolveBinary('yt-dlp');
  assert.notStrictEqual(fallback, managed, 'a deleted cached binary must not be returned');
});

test('resolveBinary keeps returning a cached binary that still exists', () => {
  const binDir = path.join(dataDir, 'bin');
  const exe = process.platform === 'win32' ? 'ffmpeg.exe' : 'ffmpeg';
  const managed = path.join(binDir, exe);

  fs.mkdirSync(binDir, { recursive: true });
  fs.writeFileSync(managed, '');
  try { fs.chmodSync(managed, 0o755); } catch (_) {}

  clearBinaryCache();
  const first = resolveBinary('ffmpeg');
  const second = resolveBinary('ffmpeg');
  assert.strictEqual(first, managed);
  assert.strictEqual(second, managed);
});
