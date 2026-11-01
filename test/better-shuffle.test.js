'use strict';

const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');

test('Better Shuffle registers a provider and releases it on stop', async () => {
  const code = fs.readFileSync(path.join(__dirname, '..', 'plugin', 'better-shuffle.js'), 'utf8');
  let provider = null;
  const riff = {
    storage: {
      get: (key, fallback) => fallback,
      set: () => {}
    },
    shuffle: {
      setProvider: fn => {
        provider = fn;
        return () => { provider = null; };
      },
      isOn: () => true
    },
    queue: { append: () => [] },
    api: { search: async () => [], vibe: async () => [] },
    library: { favorites: () => [] },
    on: () => () => {},
    toast: () => {},
    addStyle: () => () => {},
    query: () => null,
    audio: () => null
  };
  const plugin = new Function('Riff', 'plugin', code)(riff, {});
  const current = { id: 'one', title: 'First', artist: 'First Artist', platform: 'youtube' };
  const queue = [
    current,
    { id: 'two', title: 'Second', artist: 'Second Artist', platform: 'youtube' },
    { id: 'three', title: 'Third', artist: 'Third Artist', platform: 'youtube' }
  ];

  assert.strictEqual(typeof provider, 'function');
  const next = await provider({ queue, queueIndex: 0, context: 'playlist', current });
  assert.ok(next);
  assert.notStrictEqual(next.id, current.id);
  plugin.stop();
  assert.strictEqual(provider, null);
});
