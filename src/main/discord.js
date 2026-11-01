
const RPC = require('discord-rpc');

const CLIENT_ID = '1520100510877749358';
const RECONNECT_DELAY_MS = 15000;

let rpcClient = null;
let isConnected = false;
let isEnabled = true;
let lastTrackInfo = null;
let reconnectTimer = null;
let isDestroying = false;

function initRPC() {
  if (rpcClient || isDestroying) return;

  rpcClient = new RPC.Client({ transport: 'ipc' });

  rpcClient.on('ready', () => {
    isConnected = true;
    console.log('[Discord RPC] Connected as', rpcClient.user?.username || 'unknown');
    if (lastTrackInfo && isEnabled) {
      _setActivity(lastTrackInfo);
    }
  });

  rpcClient.on('disconnected', () => {
    console.log('[Discord RPC] Disconnected');
    isConnected = false;
    rpcClient = null;
    scheduleReconnect();
  });

  rpcClient.login({ clientId: CLIENT_ID }).catch(err => {
    console.warn('[Discord RPC] Login failed:', err.message || err);
    isConnected = false;
    rpcClient = null;
    scheduleReconnect();
  });
}

function scheduleReconnect() {
  if (reconnectTimer || isDestroying || !isEnabled) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    if (isEnabled && !isDestroying) {
      initRPC();
    }
  }, RECONNECT_DELAY_MS);
}

function _setActivity(trackInfo) {
  if (!rpcClient || !isConnected || !trackInfo) return;
  const now = Math.floor(Date.now() / 1000);
  const startEpoch = trackInfo.startTimestamp || now;
  const showArtistStatus = trackInfo.artistInStatus !== false;
  const cleanTitle = String(trackInfo.title || 'Unknown Track').substring(0, 128).padEnd(2, ' ');
  const cleanArtist = String(trackInfo.artist || 'Unknown Artist').substring(0, 128).padEnd(2, ' ');
  const baseActivity = {
    type: 2,
    details: cleanTitle,
    state: showArtistStatus ? cleanArtist : (`by ${trackInfo.artist || 'Unknown Artist'}`).substring(0, 128).padEnd(2, ' '),
    timestamps: { start: startEpoch },
    assets: { large_image: trackInfo.thumbnail || undefined },
    instance: false,
  };
  if (trackInfo.duration && trackInfo.duration > 0) {
    baseActivity.timestamps.end = startEpoch + Math.floor(trackInfo.duration);
  }
  if (trackInfo.url && typeof trackInfo.url === 'string' && trackInfo.url.startsWith('https://') && trackInfo.url.length <= 512) {
    baseActivity.buttons = [{ label: 'Listen', url: trackInfo.url }];
  }
  const primaryActivity = showArtistStatus
    ? Object.assign({}, baseActivity, { status_display_type: 1 })
    : baseActivity;
  rpcClient.request('SET_ACTIVITY', { pid: process.pid, activity: primaryActivity }).catch(() => {
    if (rpcClient && isConnected) {
      rpcClient.request('SET_ACTIVITY', { pid: process.pid, activity: baseActivity }).catch(err => {
        console.warn('[Discord RPC] SET_ACTIVITY error:', err.message || err);
      });
    }
  });
}


function updatePresence(trackInfo) {
  if (!trackInfo) return;

  lastTrackInfo = trackInfo;

  if (!isEnabled) return;

  if (isConnected && rpcClient) {
    _setActivity(trackInfo);
  } else if (!rpcClient && !reconnectTimer) {
    initRPC();
  }
}

function clearPresence() {
  lastTrackInfo = null;
  if (rpcClient && isConnected) {
    rpcClient.clearActivity().catch(() => {});
  }
}

function setEnabled(enabled) {
  isEnabled = !!enabled;

  if (isEnabled) {
    if (!rpcClient) {
      initRPC();
    } else if (lastTrackInfo) {
      _setActivity(lastTrackInfo);
    }
  } else {
    if (rpcClient && isConnected) {
      rpcClient.clearActivity().catch(() => {});
    }
    destroyRPC();
  }
}

function getEnabled() {
  return isEnabled;
}

function getConnected() {
  return isConnected;
}

function destroyRPC() {
  isDestroying = true;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (rpcClient) {
    try {
      rpcClient.destroy().catch(() => {});
    } catch (e) {}
    rpcClient = null;
  }
  isConnected = false;
  setTimeout(() => { isDestroying = false; }, 500);
}

function reconnectRPC() {
  destroyRPC();
  setTimeout(() => initRPC(), 600);
}

module.exports = {
  initRPC,
  updatePresence,
  clearPresence,
  setEnabled,
  getEnabled,
  getConnected,
  destroyRPC,
  reconnectRPC,
};
