'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { isRtlText } = require('../src/renderer/lyrics-rtl');

test('detects Arabic lyrics as right-to-left', () => {
  assert.strictEqual(isRtlText('أحبك يا حياتي'), true);
});

test('detects Hebrew lyrics as right-to-left', () => {
  assert.strictEqual(isRtlText('אני אוהב אותך'), true);
});

test('treats English lyrics as left-to-right', () => {
  assert.strictEqual(isRtlText('I love you tonight'), false);
});

test('ignores leading numbers and punctuation before the first strong character', () => {
  assert.strictEqual(isRtlText('123 - أحبك'), true);
  assert.strictEqual(isRtlText('123 - I love you'), false);
});

test('uses the first strong character for mixed lines', () => {
  assert.strictEqual(isRtlText('Hello مرحبا'), false);
  assert.strictEqual(isRtlText('مرحبا Hello'), true);
});

test('ignores markup and blank lyrics', () => {
  assert.strictEqual(isRtlText('<span>مرحبا</span>'), true);
  assert.strictEqual(isRtlText(''), false);
  assert.strictEqual(isRtlText('   '), false);
});
