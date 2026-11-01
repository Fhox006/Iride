'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { cleanSearchResults, scoreImportedCandidate, findDownloadedFileName } = require('../src/server/server');

test('removes irrelevant tracks and keeps matching results in relevance order', () => {
  const tracks = [
    { id: 'unrelated', title: 'Different Song', artist: 'Other Artist', platform: 'soundcloud' },
    { id: 'matching', title: 'One Way Ticket', artist: 'ONDA ANDAR', platform: 'soundcloud' },
    { id: 'partial', title: 'Ticket', artist: 'Onda Andar', platform: 'soundcloud' },
    { id: 'weak-match', title: 'Maple (Slowed)', artist: 'Other Artist', platform: 'soundcloud' }
  ];

  assert.deepStrictEqual(
    cleanSearchResults(tracks, 'one way ticket ONDA ANDAR').map(track => track.id),
    ['matching', 'partial']
  );
});

test('keeps tracks when every search term matches the artist name', () => {
  const tracks = [
    { id: 'song', title: 'Kape', artist: 'Onda Andar', platform: 'youtube' },
    { id: 'unrelated', title: 'Different Song', artist: 'Other Artist', platform: 'youtube' }
  ];

  assert.deepStrictEqual(
    cleanSearchResults(tracks, 'onda andar').map(track => track.id),
    ['song']
  );
});

test('removes duplicate releases without changing the displayed title', () => {
  const tracks = [
    { id: 'first', title: 'Maple (Official Audio)', artist: 'Chainsaww', platform: 'soundcloud' },
    { id: 'second', title: 'Maple', artist: 'Chainsaww', platform: 'soundcloud' }
  ];

  const result = cleanSearchResults(tracks, 'chainsaww Maple');
  assert.strictEqual(result.length, 1);
  assert.strictEqual(result[0].title, 'Maple (Official Audio)');
});

test('requires a meaningful match for multiword artist and title searches', () => {
  const tracks = [
    { id: 'matching', title: 'Chainsaww (feat. Siouxxie)', artist: 'Maple', platform: 'soundcloud' },
    { id: 'weak-match', title: 'Maple (Slowed)', artist: 'Other Artist', platform: 'soundcloud' }
  ];

  assert.deepStrictEqual(
    cleanSearchResults(tracks, 'chainsaww Maple').map(track => track.id),
    ['matching']
  );
});

test('removes hour-long and audio compilation results', () => {
  const tracks = [
    { id: 'song', title: 'Masquerade', artist: 'Sioxxie', platform: 'soundcloud' },
    { id: 'long', title: 'SCP-3008 Friday Theme 1 hour', artist: 'Sioxxie', platform: 'soundcloud' },
    { id: 'mix', title: 'Nightcore TikTok Audios May Part 4', artist: 'Sioxxie', platform: 'soundcloud' }
  ];

  assert.deepStrictEqual(
    cleanSearchResults(tracks, 'Sioxxie').map(track => track.id),
    ['song']
  );
});

test('rejects imported stream candidates with a very different duration', () => {
  const expected = { title: 'as the light fades', artist: 'a vow', duration: 138 };

  const correctMatch = scoreImportedCandidate(expected.title, expected.artist, expected.duration, {
    title: 'as the light fades',
    artist: 'a vow',
    duration: 140
  });
  const wrongArtist = scoreImportedCandidate(expected.title, expected.artist, expected.duration, {
    title: 'as the light fades',
    artist: 'another artist',
    duration: 140
  });

  assert.ok(scoreImportedCandidate(expected.title, expected.artist, expected.duration, {
    title: 'as the light fades',
    artist: 'a vow',
    duration: 3588
  }) < 0);
  assert.ok(correctMatch >= 70);
  assert.ok(wrongArtist < 70);
});

test('downloaded track lookup requires an exact artist and title', () => {
  const files = new Map([
    ['a vow - as the light fades (1 hour mix)', 'wrong-audio.mp3']
  ]);

  assert.strictEqual(findDownloadedFileName(files, 'as the light fades', 'a vow'), null);
});
