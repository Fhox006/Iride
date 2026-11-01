'use strict';

const API_BASE = 'https://www.googleapis.com/youtube/v3';

function thumbnailUrl(thumbnails) {
  if (!thumbnails || typeof thumbnails !== 'object') return '';
  for (const key of ['maxres', 'standard', 'high', 'medium', 'default']) {
    if (thumbnails[key] && thumbnails[key].url) return thumbnails[key].url;
  }
  return '';
}

function durationInSeconds(value) {
  const match = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$/.exec(value || '');
  if (!match) return 0;
  return Number(match[1] || 0) * 3600 + Number(match[2] || 0) * 60 + Number(match[3] || 0);
}

async function fetchJson(url, accessToken, fetchImpl) {
  const response = await fetchImpl(url, {
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`
    },
    signal: AbortSignal.timeout(30000)
  });
  let data;
  try {
    data = await response.json();
  } catch (_) {
    data = {};
  }
  if (!response.ok) {
    const message = data.error && data.error.message
      ? data.error.message
      : `YouTube API request failed: HTTP ${response.status}`;
    throw new Error(message);
  }
  return data;
}

async function listAll(path, params, accessToken, fetchImpl) {
  const items = [];
  const pageTokens = new Set();
  let pageToken = '';
  let totalResults = 0;
  do {
    const url = new URL(`${API_BASE}/${path}`);
    for (const [key, value] of Object.entries(params)) {
      url.searchParams.set(key, String(value));
    }
    if (pageToken) url.searchParams.set('pageToken', pageToken);
    const page = await fetchJson(url, accessToken, fetchImpl);
    if (Array.isArray(page.items)) items.push(...page.items);
    totalResults = Math.max(totalResults, Number(page.pageInfo && page.pageInfo.totalResults) || 0);
    pageToken = page.nextPageToken || '';
    if (pageToken && pageTokens.has(pageToken)) {
      throw new Error('YouTube API returned a repeated page token');
    }
    if (pageToken) pageTokens.add(pageToken);
  } while (pageToken);
  return { items, totalResults };
}

function mapTrack(item, platform) {
  const snippet = item && item.snippet;
  const id = (item && item.contentDetails && item.contentDetails.videoId) ||
    (snippet && snippet.resourceId && snippet.resourceId.videoId);
  if (!id || !snippet) return null;
  return {
    title: snippet.title || 'untitled',
    artist: snippet.videoOwnerChannelTitle || snippet.channelTitle || '',
    duration: 0,
    id,
    url: `https://www.youtube.com/watch?v=${encodeURIComponent(id)}`,
    platform,
    thumbnail: thumbnailUrl(snippet.thumbnails),
    cover: thumbnailUrl(snippet.thumbnails)
  };
}

function mapVideo(video, platform) {
  const snippet = video && video.snippet;
  if (!video || !video.id || !snippet) return null;
  const cover = thumbnailUrl(snippet.thumbnails);
  return {
    title: snippet.title || 'untitled',
    artist: snippet.channelTitle || '',
    duration: durationInSeconds(video.contentDetails && video.contentDetails.duration),
    id: video.id,
    url: `https://www.youtube.com/watch?v=${encodeURIComponent(video.id)}`,
    platform,
    thumbnail: cover,
    cover
  };
}

async function addDurations(tracks, accessToken, fetchImpl) {
  const ids = [...new Set(tracks.map(track => track.id))];
  const durations = new Map();
  for (let index = 0; index < ids.length; index += 50) {
    const url = new URL(`${API_BASE}/videos`);
    url.searchParams.set('part', 'contentDetails');
    url.searchParams.set('id', ids.slice(index, index + 50).join(','));
    const result = await fetchJson(url, accessToken, fetchImpl);
    for (const video of result.items || []) {
      if (video.id && video.contentDetails) {
        durations.set(video.id, durationInSeconds(video.contentDetails.duration));
      }
    }
  }
  for (const track of tracks) {
    track.duration = durations.get(track.id) || 0;
  }
}

async function fetchYouTubeLibrary(accessToken, options = {}, fetchImpl = fetch) {
  if (!accessToken) throw new Error('Sign in to Google before importing your YouTube library');
  const platform = options.platform === 'ytmusic' ? 'ytmusic' : 'youtube';
  const doLikes = options.doLikes !== false;
  const doPlaylists = options.doPlaylists !== false;
  const likedVideos = doLikes
    ? await listAll('videos', {
      part: 'snippet,contentDetails',
      myRating: 'like',
      maxResults: 50
    }, accessToken, fetchImpl)
    : { items: [], totalResults: 0 };
  const likes = likedVideos.items.map(item => mapVideo(item, platform)).filter(Boolean);
  const userPlaylists = doPlaylists
    ? await listAll('playlists', {
      part: 'snippet,contentDetails',
      mine: true,
      maxResults: 50
    }, accessToken, fetchImpl)
    : { items: [] };
  const playlists = [];
  for (const item of userPlaylists.items) {
    if (!item.id || !item.snippet) continue;
    const playlistTracks = await listAll('playlistItems', {
      part: 'snippet,contentDetails',
      playlistId: item.id,
      maxResults: 50
    }, accessToken, fetchImpl);
    playlists.push({
      id: item.id,
      name: item.snippet.title || 'Playlist',
      cover: thumbnailUrl(item.snippet.thumbnails),
      tracks: playlistTracks.items.map(track => mapTrack(track, platform)).filter(Boolean)
    });
  }
  await addDurations(
    playlists.flatMap(playlist => playlist.tracks),
    accessToken,
    fetchImpl
  );
  return {
    likes,
    playlists,
    warning: likedVideos.totalResults > 1000
      ? 'YouTube API only provides the most recent 1,000 liked videos.'
      : ''
  };
}

module.exports = { fetchYouTubeLibrary, durationInSeconds };
