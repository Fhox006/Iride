'use strict';

const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

process.env.RIFF_DATA_DIR = fs.mkdtempSync(path.join(os.tmpdir(), 'riff-data-'));

const { copyDataEntries, removeDataEntries } = require('../src/shared/platform');

function write(file, content = 'x') {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content);
}

test('moving the data dir only touches Riff entries', async () => {
  const from = fs.mkdtempSync(path.join(os.tmpdir(), 'riff-from-'));
  const to = fs.mkdtempSync(path.join(os.tmpdir(), 'riff-to-'));

  // Riff data
  write(path.join(from, 'tracks', 'abc.opus'), 'audio');
  write(path.join(from, 'library.json'), '{}');
  // Electron profile (shares the folder on macOS) and unrelated user files
  write(path.join(from, 'Local Storage', 'leveldb', '000003.log'), 'ls');
  write(path.join(from, 'storage.json'), '{"playlists":[]}');
  write(path.join(from, 'My Album', 'song.flac'), 'keep');

  await copyDataEntries(from, to);
  assert.strictEqual(fs.readFileSync(path.join(to, 'tracks', 'abc.opus'), 'utf8'), 'audio');
  assert.ok(fs.existsSync(path.join(to, 'library.json')));
  assert.ok(!fs.existsSync(path.join(to, 'Local Storage')), 'Electron profile must not be copied');
  assert.ok(!fs.existsSync(path.join(to, 'My Album')), 'unrelated files must not be copied');

  await removeDataEntries(from);
  assert.ok(!fs.existsSync(path.join(from, 'tracks')));
  assert.ok(!fs.existsSync(path.join(from, 'library.json')));
  assert.ok(fs.existsSync(path.join(from, 'Local Storage', 'leveldb', '000003.log')), 'Electron profile must survive');
  assert.ok(fs.existsSync(path.join(from, 'storage.json')), 'storage.json must survive');
  assert.ok(fs.existsSync(path.join(from, 'My Album', 'song.flac')), 'user files must survive');
});
