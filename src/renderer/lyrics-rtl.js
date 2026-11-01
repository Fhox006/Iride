const RTL_CHAR_RE = /[\u0590-\u05FF\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/;
const RTL_STRONG_RE = /[\u0590-\u05FF\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]/g;
const LETTER_RE = /\p{L}/u;

const isRtlLanguageCode = (lang) => /^(ar|fa|per|fas|prs|pes|he|iw|ur|ps|ckb|sd|ug|yi)\b/i.test(String(lang || '').trim());

function isRtlText(text) {
  const clean = String(text || '').replace(/<[^>]*>/g, '');
  for (const ch of clean) {
    if (!LETTER_RE.test(ch)) continue;
    return RTL_CHAR_RE.test(ch);
  }
  return RTL_CHAR_RE.test(clean);
}

if (typeof window !== 'undefined') {
  window.RTL_CHAR_RE = RTL_CHAR_RE;
  window.RTL_STRONG_RE = RTL_STRONG_RE;
  window.isRtlLanguageCode = isRtlLanguageCode;
  window.isRtlText = isRtlText;
}

if (typeof module !== 'undefined') {
  module.exports = { isRtlText, isRtlLanguageCode, RTL_CHAR_RE, RTL_STRONG_RE };
}
