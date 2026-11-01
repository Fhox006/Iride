function dedupeTracks(tracks, same) {
  if (!Array.isArray(tracks)) return [];
  const kept = [];
  for (const track of tracks) {
    if (!kept.some((other) => same(other, track))) kept.push(track);
  }
  return kept;
}

if (typeof module !== 'undefined') module.exports = { dedupeTracks };
