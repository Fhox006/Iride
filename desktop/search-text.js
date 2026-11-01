function normalizeSearch(value) {
  if (typeof value !== 'string') return '';
  return value
    .normalize('NFKD')
    .replace(/[\u0300-\u036f\u0591-\u05c7\u0610-\u061a\u064b-\u065f\u0670\u06d6-\u06ed]/g, '')
    .replace(/\u0640/g, '')
    .replace(/[\u06cc\u0649]/g, '\u064a')
    .replace(/\u06a9/g, '\u0643')
    .replace(/\u0629/g, '\u0647')
    .replace(/\u06c1/g, '\u0647')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();
}

if (typeof module !== 'undefined') module.exports = { normalizeSearch };
