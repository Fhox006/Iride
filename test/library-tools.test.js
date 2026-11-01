'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { dedupeTracks } = require('../library-tools');

const sameById = (a, b) => a.id === b.id;

test('dedupe keeps the first copy and the original order', () => {
  const list = [{ id: 'b' }, { id: 'a' }, { id: 'b' }, { id: 'c' }, { id: 'a' }];
  assert.deepStrictEqual(dedupeTracks(list, sameById).map((t) => t.id), ['b', 'a', 'c']);
});

test('dedupe uses the comparator, not raw ids', () => {
  const same = (a, b) => a.title.toLowerCase() === b.title.toLowerCase();
  const list = [{ title: 'Song' }, { title: 'SONG' }, { title: 'Other' }];
  assert.deepStrictEqual(dedupeTracks(list, same).map((t) => t.title), ['Song', 'Other']);
});

test('dedupe returns an empty list for non-arrays', () => {
  assert.deepStrictEqual(dedupeTracks(null, sameById), []);
});
