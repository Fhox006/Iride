const { contextBridge, ipcRenderer, webFrame } = require('electron');


contextBridge.exposeInMainWorld('electronAPI', {
  serverToken: (() => {
    const arg = process.argv.find((a) => a.startsWith('--riff-token='));
    return arg ? arg.slice('--riff-token='.length) : '';
  })(),
  minimizeWindow: () => ipcRenderer.send('window-minimize'),
  maximizeWindow: () => ipcRenderer.send('window-maximize'),
  closeWindow: () => ipcRenderer.send('window-close'),
  getServerPort: () => ipcRenderer.invoke('get-server-port'),
  setAppProxy: (proxyUrl) => ipcRenderer.invoke('set-app-proxy', proxyUrl),
  getGpuCompositing: () => ipcRenderer.invoke('get-gpu-compositing'),
  pickMedia: () => ipcRenderer.invoke('pick-media'),
  importAudioFiles: () => ipcRenderer.invoke('import-audio-files'),
  pickLocalFiles: () => ipcRenderer.invoke('pick-local-files'),
  pickLocalFolder: () => ipcRenderer.invoke('pick-local-folder'),
  rescanLocalMetadata: (paths) => ipcRenderer.invoke('rescan-local-metadata', paths),
  onWindowVisibility: (callback) => {
    ipcRenderer.on('window-visibility', (event, visible) => callback(visible));
  },
  setZoomFactor: (factor) => {
    try {
      webFrame.setZoomFactor(factor);
    } catch (e) {}
  },
  getZoomFactor: () => {
    try {
      return webFrame.getZoomFactor();
    } catch (e) {
      return 1.0;
    }
  },
  clearWebFrameCache: () => {
    try {
      webFrame.clearCache();
    } catch (_) {}
  },

  discordRpcUpdate: (trackInfo) => ipcRenderer.send('discord-rpc-update', trackInfo),
  discordRpcClear: () => ipcRenderer.send('discord-rpc-clear'),
  discordRpcSetEnabled: (enabled) => ipcRenderer.send('discord-rpc-set-enabled', enabled),
  discordRpcGetEnabled: () => ipcRenderer.invoke('discord-rpc-get-enabled'),
  discordRpcGetConnected: () => ipcRenderer.invoke('discord-rpc-get-connected'),
  discordRpcReconnect: () => ipcRenderer.invoke('discord-rpc-reconnect'),
  getCacheSize: () => ipcRenderer.invoke('get-cache-size'),
  clearCache: () => ipcRenderer.invoke('clear-cache'),
  getDataDir: () => ipcRenderer.invoke('get-data-dir'),
  pickDataDir: () => ipcRenderer.invoke('pick-data-dir'),
  setDataDir: (dir, move) => ipcRenderer.invoke('set-data-dir', { dir, move }),
  getDownloadDir: () => ipcRenderer.invoke('get-download-dir'),
  pickDownloadDir: () => ipcRenderer.invoke('pick-download-dir'),
  setDownloadDir: (dir) => ipcRenderer.invoke('set-download-dir', { dir }),
  resetDownloadDir: () => ipcRenderer.invoke('set-download-dir', { reset: true }),
  restartApp: () => ipcRenderer.send('restart-app'),
  getAppVersion: () => ipcRenderer.invoke('get-app-version'),
  checkForUpdates: () => ipcRenderer.invoke('update-check'),
  updateAction: (action) => ipcRenderer.send('update-action', action),
  installUpdateNow: () => {
    try { ipcRenderer.send('install-update-now'); } catch (_) {}
    try { ipcRenderer.send('update-action', 'now'); } catch (_) {}
  },
  onUpdateAvailable: (cb) => ipcRenderer.on('update-available', (e, d) => cb(d)),
  onUpdateDownloaded: (cb) => ipcRenderer.on('update-downloaded', (e, d) => cb(d)),
  onUpdateProgress: (cb) => ipcRenderer.on('update-progress', (e, d) => cb(d)),
  onUpdateReady: (cb) => ipcRenderer.on('update-ready', (e, d) => cb(d)),
  onUpdateError: (cb) => ipcRenderer.on('update-error', (e, d) => cb(d)),
  onUpdateNotAvailable: (cb) => ipcRenderer.on('update-not-available', (e, d) => cb(d)),
  setTrayOnClose: (enabled) => ipcRenderer.send('set-tray-on-close', enabled),
  getMultiInstance: () => ipcRenderer.invoke('get-multi-instance'),
  setMultiInstance: (v) => ipcRenderer.send('set-multi-instance', v),
  onMediaCommand: (cb) => ipcRenderer.on('media-command', (e, cmd) => cb(cmd)),
  onMediaControl: (cb) => ipcRenderer.on('media-control', (e, action) => cb(action)),
  toggleMiniWindow: () => ipcRenderer.invoke('toggle-mini-window'),
  setMiniLyrics: (enabled) => ipcRenderer.invoke('set-mini-lyrics', enabled),
  isWindows: process.platform === 'win32',
  platform: process.platform,
  setWindowButtonsVisible: (visible) => ipcRenderer.send('set-window-buttons-visible', visible),
  setMediaKeys: (v) => ipcRenderer.send('set-media-keys', v),
  mediaKeysActive: () => ipcRenderer.send('media-keys-active'),
  welcomeFinish: (detail) => ipcRenderer.send('welcome-finish', detail),
  onWelcomeResult: (cb) => ipcRenderer.on('welcome-result', (e, d) => cb(d)),
  getLauncherInfo: () => ipcRenderer.invoke('launcher-info'),
  installLauncher: () => ipcRenderer.invoke('install-launcher'),
  listThemes: () => ipcRenderer.invoke('themes-list'),
  importThemes: () => ipcRenderer.invoke('themes-import'),
  setThemeEnabled: (id, on) => ipcRenderer.send('themes-set-enabled', id, on),
  disableAllThemes: () => ipcRenderer.send('themes-disable-all'),
  removeTheme: (id) => ipcRenderer.send('themes-remove', id),
  listPlugins: () => ipcRenderer.invoke('plugins-list'),
  setPluginEnabled: (file, on) => ipcRenderer.send('plugins-set-enabled', file, on),
  disableAllPlugins: () => ipcRenderer.send('plugins-disable-all'),
  openPluginsFolder: () => ipcRenderer.send('plugins-open-folder'),
  removePlugin: (file) => ipcRenderer.send('plugins-remove', file),
  importPlugins: () => ipcRenderer.invoke('plugins-import'),
  getAppCss: () => ipcRenderer.invoke('read-app-css'),
  openExternal: (u) => ipcRenderer.send('open-external', u),
  logError: (level, message, stack) => ipcRenderer.send('log-error', { level, message, stack }),
  openLogsFolder: () => ipcRenderer.send('open-logs-folder'),
  getDiagnostics: () => ipcRenderer.invoke('get-diagnostics'),
  getAudioOutputInfo: () => ipcRenderer.invoke('get-audio-output-info'),
  googleLogin: () => ipcRenderer.invoke('google-login'),
  googleLogout: () => ipcRenderer.invoke('google-logout'),
  googleAuthStatus: () => ipcRenderer.invoke('google-auth-status'),
  googleImportLibrary: (opts) => ipcRenderer.invoke('google-import-library', opts),
  youtubePlaybackLogin: () => ipcRenderer.invoke('youtube-playback-login'),
  youtubePlaybackLogout: () => ipcRenderer.invoke('youtube-playback-logout'),
  youtubePlaybackStatus: () => ipcRenderer.invoke('youtube-playback-status'),
  onYoutubePlaybackAuth: (callback) => ipcRenderer.on('youtube-playback-auth', (event, result) => callback(result)),
  savePlaylistsJson: (payload) => ipcRenderer.invoke('save-playlists-json', payload),
  showPlaylistContextMenu: (playlistId) => ipcRenderer.invoke('show-playlist-context-menu', playlistId),
  accountExport: (payload) => ipcRenderer.invoke('account-export', payload),
  accountImport: () => ipcRenderer.invoke('account-import'),
  storageInitSync: () => ipcRenderer.sendSync('storage-get-all-sync'),
  storageSaveBatch: (data) => ipcRenderer.send('storage-save-batch', data),
  storageSaveSync: (data) => ipcRenderer.sendSync('storage-save-sync', data),
  readCacheFile: () => ipcRenderer.invoke('cache-read'),
  writeCacheFile: (data) => ipcRenderer.send('cache-write', data),
});
