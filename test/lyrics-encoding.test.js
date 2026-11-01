'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { decodeUtf8Chunks, hasBrokenLyricText } = require('../src/server/server');

test('decodes UTF-8 characters split across response chunks', () => {
  const encoded = Buffer.from('Привет');

  assert.strictEqual(
    decodeUtf8Chunks([encoded.subarray(0, 3), encoded.subarray(3)]),
    'Привет'
  );
});

test('detects lyrics containing replacement characters', () => {
  assert.strictEqual(hasBrokenLyricText('Куплет �а'), true);
  assert.strictEqual(hasBrokenLyricText('Куплет песни'), false);
});
