const test = require('node:test');
const assert = require('node:assert/strict');
const model = require('../../main/assets/tv_lyrics/tv-lyrics-model.js');

const word = (text, start, end, space = true) => ({ text, start, end, space });
const line = (text, time, words = [], extra = {}) => ({ text, time, words, ...extra });

test('syllable timestamps illuminate the complete word from its first syllable', () => {
    assert.deepEqual(model.wholeWords([
        word('beau', 1000, 1200, false), word('ti', 1200, 1300, false),
        word('ful', 1300, 1700), word('day', 1800, 2200, false),
    ]), [word('beautiful', 1000, 1700), word('day', 1800, 2200)].map(({space, ...item}) => item));
});

test('short phrases keep all words and their original timing', () => {
    const words = Array.from({ length: 12 }, (_, i) => word('word' + i, i * 400, i * 400 + 300));
    const groups = model.phrases(words);
    assert.deepEqual(groups.flat(), words);
    assert.ok(groups.every(group => group.length <= 3));
    assert.equal(model.phrases([word('supercalifragilistic', 0, 1000)])[0].length, 1);
});

test('seeking backwards selects the correct line and phrase', () => {
    const lines = model.prepare([
        line('ONE TWO THREE FOUR', 1000, [word('one', 1000, 1200), word('two', 1200, 1400), word('three', 1400, 1600), word('four', 1600, 3000)]),
        line('NEXT', 4000, [word('next', 4000, 5000)]),
    ]);
    assert.equal(model.scene(lines, 4100, true, null, '').index, 1);
    assert.equal(model.scene(lines, 1700, true, null, '').key, 'phrase:0:1');
    assert.equal(model.scene(lines, 1100, true, null, '').key, 'phrase:0:0');
    assert.deepEqual(model.scene(lines, 500, true, null, '').words, []);
});

test('a sustained last word has stable emphasis before and during playback', () => {
    const lines = model.prepare([line('HOLD ME', 0, [word('hold', 0, 500), word('me', 500, 2500)])]);
    const before = model.scene(lines, 100, true, null, '');
    const during = model.scene(lines, 1700, true, null, '');
    assert.equal(before.emphasis, true);
    assert.equal(during.emphasis, true);
    assert.equal(before.key, during.key);
    assert.deepEqual(before.words, during.words);
});

test('line-only and romanized lyrics never invent word timings', () => {
    const lines = model.prepare([line('Complete line', 1000), line('Original', 2000, [word('original', 2000, 3000)], { romanized: 'Romanized line' })]);
    assert.equal(model.scene(lines, 1500, true, null, '').text, 'Complete line');
    assert.equal(model.scene(lines, 2500, true, null, '').text, 'Romanized line');
    assert.deepEqual(model.scene(lines, 2500, true, null, '').words, []);
});

test('unsynced lines can be browsed manually without using sentinel timestamps', () => {
    const lines = model.prepare([line('First', 1000000), line('Second', 1000001)]);
    assert.equal(model.scene(lines, 30000, false, null, '').text, 'First');
    assert.equal(model.scene(lines, 30000, false, 1, '').text, 'Second');
    assert.equal(model.scene(lines, 30000, false, 99, '').text, 'Second');
});

test('empty intervals clear the TV and background voices do not replace the lead', () => {
    const lines = model.prepare([line('Lead', 1000), line('Backing', 1200, [], { background: true }), line('', 2000)]);
    assert.equal(model.scene(lines, 1300, true, null, '').text, 'Lead');
    assert.equal(model.scene(lines, 2100, true, null, '').words.length, 0);
    assert.equal(model.scene([], 0, true, null, 'Finding lyrics').message, 'Finding lyrics');
});
