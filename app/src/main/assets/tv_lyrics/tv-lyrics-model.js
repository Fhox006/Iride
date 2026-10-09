/* Whole-word timing and fixed phrase layouts, shared by the renderer and its tests. */
(function (root) {
    'use strict';
    function wholeWords(segments) {
        const words = [];
        let pending = null;
        for (const segment of segments || []) {
            if (!segment.text.trim() || !Number.isFinite(segment.start) || !Number.isFinite(segment.end)) continue;
            if (pending) {
                pending.text += segment.text.trim();
                pending.end = Math.max(pending.end, segment.end);
            } else {
                pending = { text: segment.text.trim(), start: segment.start, end: segment.end };
            }
            if (segment.space) { words.push(pending); pending = null; }
        }
        if (pending) words.push(pending);
        return words.filter(word => word.end > word.start);
    }
    function phrases(words) {
        const result = [];
        let group = [];
        for (const word of words) {
            const letters = group.reduce((sum, item) => sum + item.text.length + 1, 0);
            if (group.length && (group.length >= 3 || letters + word.text.length > 20)) {
                result.push(group); group = [];
            }
            group.push(word);
        }
        if (group.length) result.push(group);
        return result;
    }
    function prepare(lines) {
        return lines.filter(line => !line.background).map(line => {
            const words = wholeWords(line.words);
            return { ...line, words, phrases: phrases(words) };
        }).sort((a, b) => a.time - b.time);
    }
    function scene(lines, position, synced, manualIndex, message) {
        let index = synced ? -1 : 0;
        if (synced) {
            let low = 0, high = lines.length;
            while (low < high) {
                const middle = (low + high) >>> 1;
                if (lines[middle].time <= position) low = middle + 1; else high = middle;
            }
            index = low - 1;
        }
        if (manualIndex != null) index = Math.max(0, Math.min(lines.length - 1, manualIndex));
        const line = lines[index];
        if (!line || !line.text.trim()) return { key: 'empty', words: [], message: lines.length ? '' : message, index };
        // Romanized text has no reliable word mapping: illuminate the complete line.
        if (!line.phrases.length || line.romanized) {
            return { key: 'line:' + index, text: line.romanized || line.text, words: [], translation: line.translation, index };
        }
        let phraseIndex = 0;
        for (let i = 1; i < line.phrases.length; i++) {
            if (line.phrases[i][0].start <= position) phraseIndex = i; else break;
        }
        const words = line.phrases[phraseIndex];
        const last = words[words.length - 1];
        // Emphasis is decided from timestamps, never from playback progress, so layout stays fixed.
        const emphasis = last.end - last.start >= 1100 && last.text.length <= 12;
        return { key: 'phrase:' + index + ':' + phraseIndex, words, emphasis, index,
            translation: line.translation, preview: manualIndex != null };
    }
    const model = { wholeWords, phrases, prepare, scene };
    root.TvLyricsModel = model;
    if (typeof module !== 'undefined') module.exports = model;
})(typeof window !== 'undefined' ? window : globalThis);
