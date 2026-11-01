'use strict';

const test = require('node:test');
const assert = require('node:assert');

const { sortTracks, backfillAddedAt } = require('../src/renderer/track-sort');

function track(id, addedAt) {
  return addedAt === undefined ? { id } : { id, addedAt };
}

test('manual order keeps the stored order', () => {
  const list = [track('b', 3), track('a', 1), track('c', 2)];
  assert.deepStrictEqual(sortTracks(list, 'manual').map((t) => t.id), ['b', 'a', 'c']);
});

test('date added newest first', () => {
  const list = [track('old', 1), track('new', 3), track('mid', 2)];
  assert.deepStrictEqual(sortTracks(list, 'added-desc').map((t) => t.id), ['new', 'mid', 'old']);
});

test('date added oldest first', () => {
  const list = [track('old', 1), track('new', 3), track('mid', 2)];
  assert.deepStrictEqual(sortTracks(list, 'added-asc').map((t) => t.id), ['old', 'mid', 'new']);
});

test('tracks without a timestamp count as oldest and keep their order', () => {
  const list = [track('plain-b'), track('dated', 5), track('plain-a')];
  assert.deepStrictEqual(sortTracks(list, 'added-desc').map((t) => t.id), ['dated', 'plain-b', 'plain-a']);
});

test('does not mutate the input list', () => {
  const list = [track('a', 1), track('b', 2)];
  sortTracks(list, 'added-desc');
  assert.deepStrictEqual(list.map((t) => t.id), ['a', 'b']);
});

test('backfill dates untimestamped tracks so newest-first keeps their current order', () => {
  const list = [track('first'), track('second'), track('third')];
  const filled = backfillAddedAt(list, true);
  assert.ok(filled.every((t) => typeof t.addedAt === 'number'));
  assert.deepStrictEqual(sortTracks(filled, 'added-desc').map((t) => t.id), ['first', 'second', 'third']);
  assert.deepStrictEqual(sortTracks(filled, 'added-asc').map((t) => t.id), ['third', 'second', 'first']);
});

test('backfill dates playlists so the last track is treated as the newest', () => {
  const list = [track('first'), track('second'), track('third')];
  const filled = backfillAddedAt(list, false);
  assert.deepStrictEqual(sortTracks(filled, 'added-desc').map((t) => t.id), ['third', 'second', 'first']);
});

test('backfill leaves existing timestamps alone', () => {
  const list = [track('old', 1), track('plain'), track('new', 9)];
  const filled = backfillAddedAt(list, true);
  assert.strictEqual(filled[0].addedAt, 1);
  assert.strictEqual(filled[2].addedAt, 9);
  assert.strictEqual(typeof filled[1].addedAt, 'number');
});

test('backfill returns the same list when every track is already dated', () => {
  const list = [track('a', 1), track('b', 2)];
  assert.strictEqual(backfillAddedAt(list, true), list);
});

test('backfill does not mutate the input list', () => {
  const list = [track('a')];
  backfillAddedAt(list, true);
  assert.strictEqual(list[0].addedAt, undefined);
});
