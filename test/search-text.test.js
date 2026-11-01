'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { normalizeSearch } = require('../search-text');

test('lowercases and trims', () => {
  assert.strictEqual(normalizeSearch('  Hello World  '), 'hello world');
});

test('ignores latin accents', () => {
  assert.strictEqual(normalizeSearch('Björk'), 'bjork');
});

test('matches alef with and without hamza', () => {
  assert.strictEqual(normalizeSearch('أحبك'), normalizeSearch('احبك'));
  assert.strictEqual(normalizeSearch('إلى'), normalizeSearch('الي'));
});

test('ignores arabic diacritics and tatweel', () => {
  assert.strictEqual(normalizeSearch('كَتَبَ'), normalizeSearch('كتب'));
  assert.strictEqual(normalizeSearch('مــوسيقى'), normalizeSearch('موسيقى'));
});

test('treats alef maqsura and ta marbuta like their plain forms', () => {
  assert.strictEqual(normalizeSearch('ليلى'), normalizeSearch('ليلي'));
  assert.strictEqual(normalizeSearch('أغنية'), normalizeSearch('اغنيه'));
});

test('handles persian letter variants', () => {
  assert.strictEqual(normalizeSearch('کتاب'), normalizeSearch('كتاب'));
  assert.strictEqual(normalizeSearch('یار'), normalizeSearch('يار'));
});

test('collapses repeated whitespace', () => {
  assert.strictEqual(normalizeSearch('  song   title '), 'song title');
});

test('returns an empty string for missing values', () => {
  assert.strictEqual(normalizeSearch(undefined), '');
  assert.strictEqual(normalizeSearch(null), '');
  assert.strictEqual(normalizeSearch(42), '');
});
