const { app, BrowserWindow, dialog, ipcMain, screen, shell, Tray, Menu, nativeImage, globalShortcut, powerMonitor, session, safeStorage } = require('electron');
const fs = require('fs');
const path = require('path');
const os = require('os');
const http = require('http');
const url = require('url');
const { pathToFileURL } = url;
const { spawn, execFile } = require('child_process');
const crypto = require('crypto');
const zlib = require('zlib');
const {
  createCodeVerifier,
  createCodeChallenge,
  createOAuthState,
  createAuthorizationUrl,
  exchangeAuthorizationCode,
  refreshAccessToken
} = require('./google-oauth');
const { fetchYouTubeLibrary } = require('./youtube-data');
const RFACC_KEY = crypto.createHash('sha256').update('riff-account-key-v1').digest();
const APP_ID = 'io.github.rootscripts.riff';
const IS_MAC = process.platform === 'darwin';

app.setName('Riff');
if (process.platform === 'win32') app.setAppUserModelId(APP_ID);

function encryptAccount(dataObj) {
  const jsonStr = typeof dataObj === 'string' ? dataObj : JSON.stringify(dataObj);
  const compressed = zlib.deflateRawSync(Buffer.from(jsonStr, 'utf8'));
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', RFACC_KEY, iv);
  const encrypted = Buffer.concat([cipher.update(compressed), cipher.final()]);
  const tag = cipher.getAuthTag();
  const header = Buffer.from('RFAC');
  return Buffer.concat([header, iv, tag, encrypted]);
}

function decryptAccount(buffer) {
  if (!Buffer.isBuffer(buffer) || buffer.length < 32) throw new Error('Invalid account file');
  if (buffer.subarray(0, 4).toString('utf8') !== 'RFAC') throw new Error('Invalid file header');
  const iv = buffer.subarray(4, 16);
  const tag = buffer.subarray(16, 32);
  const encrypted = buffer.subarray(32);
  const decipher = crypto.createDecipheriv('aes-256-gcm', RFACC_KEY, iv);
  decipher.setAuthTag(tag);
  const decrypted = Buffer.concat([decipher.update(encrypted), decipher.final()]);
  const decompressed = zlib.inflateRawSync(decrypted);
  return JSON.parse(decompressed.toString('utf8'));
}
const { LIBRARY_DIR, DATA_DIR, resolveBinary, tutorialDone, markTutorialDone, readStartupConfig, writeStartupConfig, cookieJarHasYtLogin } = require('../shared/platform');
const { getLauncherInfo, installLauncher } = require('./launcher');
const { getSystemAudioOutputInfo } = require('../shared/audio-output');
const { log, tail, LOG_DIR } = require('../shared/logger');
const logger = { log, tail, LOG_DIR, error: (msg, err) => log('ERROR', `${msg}: ${err && err.message ? err.message : err}`) };

try {
  if (process.stdout && typeof process.stdout.on === 'function') process.stdout.on('error', () => {});
  if (process.stderr && typeof process.stderr.on === 'function') process.stderr.on('error', () => {});
} catch (_) {}

process.on('uncaughtException', (err) => {
  if (err && (err.code === 'EPIPE' || /EPIPE|broken pipe/i.test(err.message || ''))) return;
  try { logger.error('Uncaught exception in main', err); } catch (_) {}
});
process.on('unhandledRejection', (reason) => {
  try {
    const err = reason instanceof Error ? reason : new Error(String(reason));
    logger.error('Unhandled rejection in main', err);
  } catch (_) {}
});

const enabledFeatures = [];
if (process.platform === 'linux') {
  enabledFeatures.push('WaylandWindowDecorations', 'UseOzonePlatform', 'WaylandFractionalScaleV1', 'MediaSessionService', 'HardwareMediaKeyHandling');
  app.commandLine.appendSwitch('ozone-platform-hint', 'auto');
  app.commandLine.appendSwitch('disable-features', 'Vulkan,AudioServiceOutOfProcess');

  if (process.env.RIFF_SKIP_GPU_BUFFER_FLAGS !== '1') {
    app.commandLine.appendSwitch('disable-gpu-memory-buffer-video-frames');
    app.commandLine.appendSwitch('disable-gpu-memory-buffer-compositor-resources');
  }
}

if (process.platform === 'win32') {
  enabledFeatures.push('HardwareMediaKeyHandling', 'MediaSessionService');
  app.commandLine.appendSwitch('disable-features', 'CalculateNativeWinOcclusion');
}

if (enabledFeatures.length > 0) {
  app.commandLine.appendSwitch('enable-features', enabledFeatures.join(','));
}

app.commandLine.appendSwitch('autoplay-policy', 'no-user-gesture-required');
app.commandLine.appendSwitch('ignore-gpu-blocklist');
app.commandLine.appendSwitch('enable-gpu-rasterization');
app.commandLine.appendSwitch('enable-zero-copy');
app.commandLine.appendSwitch('renderer-process-limit', '1');
app.commandLine.appendSwitch('disable-component-update');
app.commandLine.appendSwitch('disable-background-networking');
app.commandLine.appendSwitch('disable-domain-reliability');
app.commandLine.appendSwitch('disable-sync');
app.commandLine.appendSwitch('disable-speech-api');
app.commandLine.appendSwitch('js-flags', '--max-old-space-size=256 --expose-gc');
app.commandLine.appendSwitch('disable-renderer-backgrounding');
app.commandLine.appendSwitch('disable-blink-features', 'AutomationControlled');
app.commandLine.appendSwitch('disk-cache-size', '25165824');
app.commandLine.appendSwitch('media-cache-size', '25165824');

setInterval(() => {
  try {
    session.defaultSession.clearCache().catch(() => {});
    if (global.gc) global.gc();
  } catch (_) {}
}, 90000);
app.on('ready', () => {
  const cleanUA = (app.userAgentFallback || '').replace(/\s*Electron\/[^\s]+/gi, '').replace(/\s*riff\/[^\s]+/gi, '');
  if (cleanUA) app.userAgentFallback = cleanUA;
});

const extraFlags = (process.env.RIFF_FLAGS || '').split(';').map((s) => s.trim()).filter(Boolean);
for (const flag of extraFlags) {
  const eq = flag.indexOf('=');
  if (eq > 0) app.commandLine.appendSwitch(flag.slice(0, eq), flag.slice(eq + 1));
  else app.commandLine.appendSwitch(flag);
}

if (process.env.RIFF_DEBUG === '1') {
  app.whenReady().then(async () => {
    console.log('[gpu] features', JSON.stringify(app.getGPUFeatureStatus()));
    try {
      console.log('[gpu] info', JSON.stringify(await app.getGPUInfo('basic')));
    } catch (e) {}
    setInterval(() => {
      const rows = app.getAppMetrics().map((m) => `${m.type}:${Math.round(m.cpu.percentCPUUsage)}%`);
      console.log('[cpu]', rows.join('  '));
    }, 5000).unref();
  });
}

if (process.argv.includes('lyrics') || process.argv.includes('--lyrics')) {
  const cliPath = path.join(__dirname, 'cli.js');
  const child = spawn(process.execPath, [cliPath, 'lyrics'], { stdio: 'inherit', env: Object.assign({}, process.env, { ELECTRON_RUN_AS_NODE: '1' }) });
  child.on('exit', (code) => process.exit(code || 0));
} else {
  runGUI();
}

function runGUI() {
  const { startServer, PORT, getServerToken } = require('../server/server');
  const discordRPC = require('./discord');

  let mainWindow = null;
  const allowMulti = !!readStartupConfig().multiInstance;
  const gotLock = app.requestSingleInstanceLock();
  if (!gotLock && !allowMulti) { app.quit(); return; }
  if (!gotLock && allowMulti) app.setPath('userData', `${app.getPath('userData')}-instance-${process.pid}`);
  let youtubePlaybackWindow = null;
  app.on('second-instance', () => {
    if (allowMulti) return;
    if (mainWindow && !mainWindow.isDestroyed()) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.show();
      mainWindow.focus();
    }
  });
  let serverPort = PORT;
  let tray = null;
  let isQuitting = false;
  let trayOnClose = process.platform !== 'linux';
  let isMiniWindow = false;
  let preMiniBounds = null;

  const windowStatePath = path.join(app.getPath('userData'), 'window-state.json');
  const CACHE_FILE = path.join(app.getPath('userData'), 'cache.json');

  const PRIMARY_STORAGE_FILE = path.join(__dirname, '../storage.json');
  const FALLBACK_STORAGE_FILE = path.join(app.getPath('userData'), 'storage.json');

  function getActiveStorageFile() {
    if (process.platform === 'darwin' || app.isPackaged) {
      return FALLBACK_STORAGE_FILE;
    }
    try {
      fs.accessSync(path.dirname(PRIMARY_STORAGE_FILE), fs.constants.W_OK);
      return PRIMARY_STORAGE_FILE;
    } catch (_) {
      return FALLBACK_STORAGE_FILE;
    }
  }

  function readStorageDiskSync() {
    const file = getActiveStorageFile();
    try {
      if (fs.existsSync(file)) {
        const raw = fs.readFileSync(file, 'utf8').trim();
        if (raw) {
          const parsed = JSON.parse(raw);
          if (parsed && typeof parsed === 'object' && Object.keys(parsed).length > 0) return parsed;
        }
      }
    } catch (e) {
      console.warn('[riff] storage.json read warning:', e.message);
    }
    return null;
  }

  function writeStorageDiskSync(data) {
    if (!data || typeof data !== 'object') return;
    const file = getActiveStorageFile();
    const tmpFile = file + '.tmp';
    try {
      fs.writeFileSync(tmpFile, JSON.stringify(data, null, 2), 'utf8');
      fs.renameSync(tmpFile, file);
    } catch (_) {
      try {
        fs.writeFileSync(FALLBACK_STORAGE_FILE, JSON.stringify(data, null, 2), 'utf8');
      } catch (_) {}
    }
  }

  let storageFlushTimer = null;
  let latestStorageSnapshot = null;
  let storageDirty = false;

  ipcMain.on('storage-get-all-sync', (event) => {
    event.returnValue = latestStorageSnapshot || readStorageDiskSync();
  });

  ipcMain.on('storage-save-batch', (event, data) => {
    if (!data || typeof data !== 'object') return;
    latestStorageSnapshot = data;
    storageDirty = true;
    if (storageFlushTimer) clearTimeout(storageFlushTimer);
    storageFlushTimer = setTimeout(() => {
      storageFlushTimer = null;
      storageDirty = false;
      writeStorageDiskSync(latestStorageSnapshot);
    }, 250);
  });

  ipcMain.on('storage-save-sync', (event, data) => {
    if (data && typeof data === 'object') {
      if (storageFlushTimer) { clearTimeout(storageFlushTimer); storageFlushTimer = null; }
      latestStorageSnapshot = data;
      storageDirty = false;
      writeStorageDiskSync(latestStorageSnapshot);
    }
    event.returnValue = true;
  });

  app.on('before-quit', () => {
    if (storageFlushTimer) {
      clearTimeout(storageFlushTimer);
      storageFlushTimer = null;
    }
    if (storageDirty && latestStorageSnapshot) {
      storageDirty = false;
      writeStorageDiskSync(latestStorageSnapshot);
    }
  });

  ipcMain.handle('cache-read', async () => {
    try {
      if (fs.existsSync(CACHE_FILE)) {
        return JSON.parse(await fs.promises.readFile(CACHE_FILE, 'utf8'));
      }
    } catch (_) {}
    return null;
  });

  ipcMain.on('cache-write', (event, data) => {
    if (!data || typeof data !== 'object') return;
    const tmp = CACHE_FILE + '.tmp';
    try {
      fs.writeFileSync(tmp, JSON.stringify(data), 'utf8');
      fs.renameSync(tmp, CACHE_FILE);
    } catch (_) {
      try { fs.unlinkSync(tmp); } catch (_) {}
    }
  });

  app.on('before-quit', () => {
    if (storageFlushTimer) {
      clearTimeout(storageFlushTimer);
      storageFlushTimer = null;
      if (latestStorageSnapshot) writeStorageDiskSync(latestStorageSnapshot);
    }
  });

  function readWindowState() {
    try {
      const raw = fs.readFileSync(windowStatePath, 'utf8');
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') return null;
      return parsed;
    } catch (error) {
      return null;
    }
  }

  function clampBoundsToDisplay(bounds) {
    const primaryDisplay = screen.getPrimaryDisplay();
    const primaryWorkArea = primaryDisplay.workArea;
    const scaleFactor = primaryDisplay.scaleFactor;

    const sourceBounds = bounds && Number.isFinite(bounds.width) && Number.isFinite(bounds.height)
    ? { ...bounds }
    : {};

    const minW = 800;
    const minH = 540;

    const width = Math.max(minW, Math.min(sourceBounds.width || 0, primaryWorkArea.width));
    const height = Math.max(minH, Math.min(sourceBounds.height || 0, primaryWorkArea.height));

    const display = screen.getDisplayMatching({
      x: sourceBounds.x ?? primaryWorkArea.x,
      y: sourceBounds.y ?? primaryWorkArea.y,
      width,
      height
    }) || primaryDisplay;

    const workArea = display.workArea;
    const x = Math.min(
      Math.max(sourceBounds.x ?? workArea.x + Math.round((workArea.width - width) / 2), workArea.x),
                       workArea.x + Math.max(0, workArea.width - width)
    );
    const y = Math.min(
      Math.max(sourceBounds.y ?? workArea.y + Math.round((workArea.height - height) / 2), workArea.y),
                       workArea.y + Math.max(0, workArea.height - height)
    );

    return { x, y, width, height };
  }

  function getResponsiveWindowBounds() {
    const { workArea } = screen.getPrimaryDisplay();
    const persisted = readWindowState();
    const responsiveBounds = {
      width: Math.round(workArea.width * 0.82),
      height: Math.round(workArea.height * 0.86),
      x: workArea.x + Math.round(workArea.width * 0.09),
      y: workArea.y + Math.round(workArea.height * 0.07)
    };
    const merged = {
      ...responsiveBounds,
      ...(persisted && persisted.bounds ? persisted.bounds : {})
    };
    return clampBoundsToDisplay(merged);
  }

  function persistWindowState() {
    if (isMiniWindow) return;
    if (!mainWindow || mainWindow.isDestroyed()) return;
    const isMaximized = mainWindow.isMaximized();
    const isFullScreen = mainWindow.isFullScreen();
    const bounds = isMaximized || isFullScreen ? mainWindow.getNormalBounds() : mainWindow.getBounds();
    try {
      fs.mkdirSync(path.dirname(windowStatePath), { recursive: true });
      fs.writeFileSync(windowStatePath, JSON.stringify({
        bounds,
        isMaximized,
        isFullScreen
      }, null, 2));
    } catch (error) {
      console.warn('Failed to persist window state:', error);
    }
  }

  async function createWindow() {
    serverPort = await startServer(PORT);
    const firstRun = !tutorialDone();
    const initialBounds = getResponsiveWindowBounds();
    const savedState = readWindowState();

    const primaryDisplay = screen.getPrimaryDisplay();
    const scaleFactor = primaryDisplay.scaleFactor;

    mainWindow = new BrowserWindow({
      x: initialBounds.x,
      y: initialBounds.y,
      width: initialBounds.width,
      height: initialBounds.height,
      minWidth: 800,
      minHeight: 540,
                                   backgroundColor: firstRun ? '#8d5ef2' : '#000000',
                                   title: 'Riff',
                                   icon: path.join(__dirname, '../../assets', 'icon.png'),
                                   frame: IS_MAC,
                                   titleBarStyle: IS_MAC ? 'hiddenInset' : 'hidden',
                                   ...(IS_MAC ? { trafficLightPosition: { x: 14, y: 13 } } : {}),
                                   show: false,
                                   useContentSize: true,
                                   webPreferences: {
                                     preload: path.join(__dirname, 'preload.js'),
                                   additionalArguments: [`--riff-token=${getServerToken()}`],
                                   nodeIntegration: false,
                                   contextIsolation: true,
                                   webSecurity: false,
                                   backgroundThrottling: true,
                                   spellcheck: false
                                   }
    });

    if (savedState?.isMaximized) {
      mainWindow.maximize();
    }

    mainWindow.once('ready-to-show', () => {
      if (!mainWindow || mainWindow.isDestroyed()) return;
      mainWindow.show();
    });

    mainWindow.loadFile(path.join(__dirname, '../renderer', firstRun ? 'welcome.html' : 'index.html'));
    if (firstRun) {
      ipcMain.removeAllListeners('welcome-finish');
      ipcMain.once('welcome-finish', (e, detail) => {
        markTutorialDone();
        if (detail && detail.launcher) installLauncher();
        if (!mainWindow || mainWindow.isDestroyed()) return;
        const result = detail && !detail.skipped ? detail : null;
        mainWindow.setBackgroundColor('#000000');
        mainWindow.webContents.once('did-finish-load', () => { if (result) mainWindow.webContents.send('welcome-result', result); });
        mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));
        require('./updater').initUpdater(() => mainWindow);
      });
    }

    mainWindow.webContents.setWindowOpenHandler(({ url }) => {
      shell.openExternal(url);
      return { action: 'deny' };
    });

    mainWindow.webContents.on('did-finish-load', () => {
      try {
        const curUrl = mainWindow.webContents.getURL() || '';
        if (curUrl.includes('index.html')) {
          if (mainWindow.webContents.navigationHistory?.clear) {
            mainWindow.webContents.navigationHistory.clear();
          } else if (mainWindow.webContents.clearHistory) {
            mainWindow.webContents.clearHistory();
          }
        }
      } catch (_) {}
    });

    mainWindow.on('minimize', () => {
      if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('window-visibility', false);
        if (global.gc) global.gc();
      }
    });

    mainWindow.on('blur', () => {
      if (global.gc) global.gc();
    });

    mainWindow.on('restore', () => {
      if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('window-visibility', true);
      }
    });

    mainWindow.on('hide', () => {
      if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('window-visibility', false);
      }
    });

    mainWindow.on('show', () => {
      if (mainWindow && !mainWindow.isDestroyed()) {
        mainWindow.webContents.send('window-visibility', true);
      }
    });
    mainWindow.on('resize', persistWindowState);
    mainWindow.on('move', persistWindowState);
    mainWindow.on('maximize', persistWindowState);
    mainWindow.on('unmaximize', persistWindowState);
    mainWindow.on('enter-full-screen', persistWindowState);
    mainWindow.on('leave-full-screen', persistWindowState);
    mainWindow.on('close', (e) => {
      persistWindowState();
      if (trayOnClose && tray && !isQuitting) {
        e.preventDefault();
        mainWindow.hide();
      }
    });

    discordRPC.initRPC();
  }

  ipcMain.removeHandler('get-server-port');
  ipcMain.removeHandler('discord-rpc-get-enabled');
  ipcMain.removeHandler('discord-rpc-get-connected');
  ipcMain.removeHandler('discord-rpc-reconnect');
  ipcMain.removeHandler('account-export');
  ipcMain.removeHandler('account-import');
  ipcMain.removeHandler('pick-media');
  ipcMain.removeHandler('import-audio-files');
  ipcMain.removeHandler('pick-local-files');
  ipcMain.removeHandler('pick-local-folder');
  ipcMain.removeHandler('rescan-local-metadata');

  ipcMain.on('window-minimize', () => {
    if (mainWindow && !mainWindow.isDestroyed()) mainWindow.minimize();
  });

    ipcMain.on('window-maximize', () => {
      if (mainWindow && !mainWindow.isDestroyed()) {
        if (mainWindow.isMaximized()) mainWindow.unmaximize();
        else mainWindow.maximize();
      }
    });

    ipcMain.on('window-close', () => {
      if (mainWindow && !mainWindow.isDestroyed()) mainWindow.close();
    });

    ipcMain.on('open-external', (e, u) => {
      if (/^https:\/\/(?:accounts\.spotify\.com|(?:www\.)?last\.fm|github\.com)\//i.test(String(u))) {
        shell.openExternal(String(u));
      }
    });

    let mainLogCount = 0;
    let mainLogReset = Date.now();
    ipcMain.on('log-error', (e, data) => {
      const now = Date.now();
      if (now - mainLogReset > 60000) {
        mainLogReset = now;
        mainLogCount = 0;
      }
      if (mainLogCount < 20) {
        mainLogCount++;
        if (data && typeof data === 'object') {
          logger.log(data.level || 'ERROR', data.message || '', data.stack || null);
        }
      }
    });

    ipcMain.handle('get-diagnostics', async () => {
      const lines = [`Riff ${app.getVersion()}`, `OS ${process.platform} ${process.arch} ${os.release()}`, `Electron ${process.versions.electron} Chromium ${process.versions.chrome} Node ${process.versions.node}`, `Locale ${app.getLocale()}`, `Data ${DATA_DIR}`];
      for (const name of ['yt-dlp', 'ffmpeg']) {
        const bin = resolveBinary(name);
        lines.push(`${name}: ${bin || 'NOT FOUND'}`);
      }
      lines.push('--- last log lines ---', ...tail(60));
      return lines.join('\n');
    });
    ipcMain.on('open-logs-folder', () => { shell.openPath(LOG_DIR); });

      ipcMain.handle('get-server-port', () => serverPort);
      ipcMain.handle('set-app-proxy', async (event, proxyUrl) => {
        try {
          const clean = String(proxyUrl || '').trim();
          const config = clean
            ? { proxyRules: clean, proxyBypassRules: '<local>,127.0.0.1,localhost' }
            : { mode: 'direct' };
          await session.defaultSession.setProxy(config);
          const googleSess = session.fromPartition('persist:riff-google');
          if (googleSess) await googleSess.setProxy(config);
          return { success: true };
        } catch (e) {
          return { success: false, error: e.message };
        }
      });
      ipcMain.handle('get-gpu-compositing', () => { try { return String(app.getGPUFeatureStatus().gpu_compositing || ''); } catch (e) { return ''; } });
      ipcMain.handle('launcher-info', () => getLauncherInfo());
      ipcMain.handle('install-launcher', () => {
        try {
          return installLauncher();
        } catch (err) {
          return { ok: false, error: err.message || String(err) };
        }
      });
      ipcMain.handle('get-audio-output-info', async () => getSystemAudioOutputInfo());
      require('./themes').register(() => mainWindow);
      require('./plugins').register(() => mainWindow);

      ipcMain.on('discord-rpc-update', (event, trackInfo) => {
        discordRPC.updatePresence(trackInfo);
      });

      ipcMain.on('discord-rpc-clear', () => {
        discordRPC.clearPresence();
      });

      ipcMain.on('discord-rpc-set-enabled', (event, enabled) => {
        discordRPC.setEnabled(enabled);
      });

      ipcMain.handle('discord-rpc-get-enabled', () => discordRPC.getEnabled());
      ipcMain.handle('discord-rpc-get-connected', () => discordRPC.getConnected());
      ipcMain.handle('discord-rpc-reconnect', () => {
        if (discordRPC && discordRPC.reconnectRPC) discordRPC.reconnectRPC();
        return true;
      });
      ipcMain.on('discord-rpc-reconnect', () => {
        if (discordRPC && discordRPC.reconnectRPC) discordRPC.reconnectRPC();
      });

      ipcMain.handle('get-cache-size', async () => {
        const fs = require('fs');
        const path = require('path');
        const os = require('os');
        const { session } = require('electron');

        let webSize = 0;
        try { webSize = await session.defaultSession.getCacheSize(); } catch(e) {}

        let audioSize = 0;
        const tracksDir = require('../shared/platform').CACHE_DIR;
        const thumbnailsDir = require('../shared/platform').THUMBNAILS_DIR;
        const audioExtRegex = /\.(opus|webm|m4a|mp3)$/i;
        try {
          if (fs.existsSync(tracksDir)) {
            for (const file of fs.readdirSync(tracksDir)) {
              if (audioExtRegex.test(file)) {
                try { audioSize += fs.statSync(path.join(tracksDir, file)).size; } catch(e) {}
              }
            }
          }
        } catch(e) {}
        try {
          if (fs.existsSync(thumbnailsDir)) {
            for (const file of fs.readdirSync(thumbnailsDir)) {
              try { audioSize += fs.statSync(path.join(thumbnailsDir, file)).size; } catch(e) {}
            }
          }
        } catch(e) {}

        return { audio: audioSize, web: webSize };
      });

      ipcMain.handle('clear-cache', async () => {
        const fs = require('fs');
        const path = require('path');
        const os = require('os');
        const { session } = require('electron');
        
        try { await session.defaultSession.clearCache(); } catch(e) {}
        try { await session.defaultSession.clearStorageData({ storages: ['serviceworkers', 'cachestorage'] }); } catch(e) {}

        const tracksDir = require('../shared/platform').CACHE_DIR;
        const thumbnailsDir = require('../shared/platform').THUMBNAILS_DIR;
        const audioExtRegex = /\.(opus|webm|m4a|mp3)$/i;
        try {
          if (fs.existsSync(tracksDir)) {
            for (const file of fs.readdirSync(tracksDir)) {
              if (audioExtRegex.test(file)) {
                try { fs.unlinkSync(path.join(tracksDir, file)); } catch(e) {}
              }
            }
          }
        } catch(e) {}
        try {
          if (fs.existsSync(thumbnailsDir)) {
            for (const file of fs.readdirSync(thumbnailsDir)) {
              try { fs.unlinkSync(path.join(thumbnailsDir, file)); } catch(e) {}
            }
          }
        } catch(e) {}

        return true;
      });

      ipcMain.handle('pick-media', async () => {
        const r = await dialog.showOpenDialog(mainWindow, { properties: ['openFile'], filters: [{ name: 'Media', extensions: ['png','jpg','jpeg','webp','gif','mp4','webm'] }] });
        return r.canceled ? null : pathToFileURL(r.filePaths[0]).href;
      });

      ipcMain.handle('import-audio-files', async () => {
        const r = await dialog.showOpenDialog(mainWindow, { properties: ['openFile', 'multiSelections', 'openDirectory'], filters: [{ name: 'Audio', extensions: ['mp3','wav','flac','opus','ogg','oga','m4a','aac','webm','weba'] }] });
        if (r.canceled) return [];
        const AUDIO_EXT = new Set(['mp3','wav','flac','opus','ogg','oga','m4a','aac','webm','weba']);
        const sources = [];
        for (const p of r.filePaths) {
          const st = fs.statSync(p);
          if (st.isDirectory()) {
            for (const f of fs.readdirSync(p)) {
              const fp = path.join(p, f);
              if (AUDIO_EXT.has(path.extname(f).slice(1).toLowerCase()) && fs.statSync(fp).isFile()) sources.push(fp);
            }
          } else {
            sources.push(p);
          }
        }
        fs.mkdirSync(LIBRARY_DIR, { recursive: true });
        const out = [];
        for (const src of sources) {
          const st = fs.statSync(src), ext = path.extname(src).slice(1).toLowerCase();
          const id = 'local_' + crypto.createHash('sha1').update(path.basename(src) + st.size).digest('hex').slice(0, 12);
          const file = `${id}.${ext}`;
          if (!fs.existsSync(path.join(LIBRARY_DIR, file))) await fs.promises.copyFile(src, path.join(LIBRARY_DIR, file));
          out.push({ id, file, ext, name: path.basename(src, path.extname(src)) });
        }
        return out;
      });

      async function extractLocalTrackMetadata(filePath) {
        if (!filePath || typeof filePath !== 'string') return null;
        const coversDir = path.join(app.getPath('userData'), 'local-covers');
        fs.mkdirSync(coversDir, { recursive: true });

        const fileHash = crypto.createHash('md5').update(filePath).digest('hex');
        const coverOut = path.join(coversDir, fileHash + '.jpg');

        let title = '';
        let artist = '';
        let album = '';
        let trackNum = 0;
        let duration = 0;

        let fd = null;
        try {
          fd = fs.openSync(filePath, 'r');
          const header = Buffer.alloc(10);
          const bytesRead = fs.readSync(fd, header, 0, 10, 0);

          if (bytesRead >= 10 && header[0] === 0x49 && header[1] === 0x44 && header[2] === 0x33) {
            const id3Version = header[3];
            const id3Flags = header[5];
            const tagSize = (header[6] << 21) | (header[7] << 14) | (header[8] << 7) | header[9];
            const readLimit = Math.min(tagSize, 12 * 1024 * 1024);
            const tagBuf = Buffer.alloc(readLimit);
            fs.readSync(fd, tagBuf, 0, tagBuf.length, 10);

            let offset = 0;
            if (id3Flags & 0x40) {
              if (id3Version === 4 && offset + 4 <= tagBuf.length) {
                const extSize = (tagBuf[offset] << 21) | (tagBuf[offset + 1] << 14) | (tagBuf[offset + 2] << 7) | tagBuf[offset + 3];
                offset += extSize;
              } else if (id3Version === 3 && offset + 4 <= tagBuf.length) {
                const extSize = tagBuf.readUInt32BE(offset);
                offset += 4 + extSize;
              }
            }

            function decodeId3Text(buf) {
              if (!buf || buf.length <= 1) return '';
              const enc = buf[0];
              const data = buf.subarray(1);
              let s = '';
              if (enc === 0) {
                s = data.toString('latin1');
              } else if (enc === 1) {
                if (data.length >= 2 && data[0] === 0xfe && data[1] === 0xff) {
                  s = data.subarray(2).swap16().toString('utf16le');
                } else if (data.length >= 2 && data[0] === 0xff && data[1] === 0xfe) {
                  s = data.subarray(2).toString('utf16le');
                } else {
                  s = data.toString('utf16le');
                }
              } else if (enc === 2) {
                s = data.swap16().toString('utf16le');
              } else {
                s = data.toString('utf8');
              }
              return s.replace(/^[\uFEFF\0]+|[\uFEFF\0]+$/g, '').trim();
            }

            while (offset + 10 <= tagBuf.length) {
              if (tagBuf[offset] === 0) break;
              const frameId = tagBuf.toString('latin1', offset, offset + 4);
              if (!/^[A-Z0-9]{4}$/.test(frameId)) break;
              let frameSize = 0;
              if (id3Version === 4) {
                frameSize = (tagBuf[offset + 4] << 21) | (tagBuf[offset + 5] << 14) | (tagBuf[offset + 6] << 7) | tagBuf[offset + 7];
              } else {
                frameSize = tagBuf.readUInt32BE(offset + 4);
              }
              offset += 10;
              if (frameSize <= 0 || offset + frameSize > tagBuf.length) break;

              const frameData = tagBuf.subarray(offset, offset + frameSize);
              if (frameId === 'TIT2' && !title) {
                title = decodeId3Text(frameData).trim();
              } else if (frameId === 'TPE1' && !artist) {
                artist = decodeId3Text(frameData).trim();
              } else if (frameId === 'TPE2' && !artist) {
                artist = decodeId3Text(frameData).trim();
              } else if (frameId === 'TALB' && !album) {
                album = decodeId3Text(frameData).trim();
              } else if (frameId === 'TRCK' && !trackNum) {
                const val = decodeId3Text(frameData).trim();
                const n = parseInt(val.split('/')[0], 10);
                if (!isNaN(n)) trackNum = n;
              } else if (frameId === 'TLEN' && !duration) {
                const ms = parseInt(decodeId3Text(frameData).trim(), 10);
                if (!isNaN(ms) && ms > 0) duration = ms / 1000;
              } else if (frameId === 'APIC' && !fs.existsSync(coverOut)) {
                try {
                  const enc = frameData[0];
                  let p = 1;
                  while (p < frameData.length && frameData[p] !== 0) p++;
                  p++;
                  p++;
                  if (enc === 1 || enc === 2) {
                    while (p + 1 < frameData.length && !(frameData[p] === 0 && frameData[p + 1] === 0)) p += 2;
                    p += 2;
                  } else {
                    while (p < frameData.length && frameData[p] !== 0) p++;
                    p++;
                  }
                  if (p < frameData.length) {
                    const imgBuf = frameData.subarray(p);
                    if (imgBuf.length > 32) {
                      fs.writeFileSync(coverOut, imgBuf);
                    }
                  }
                } catch (e) {}
              }
              offset += frameSize;
            }
          } else if (bytesRead >= 4 && header.subarray(0, 4).toString('latin1') === 'fLaC') {
            let pos = 4;
            let isLast = false;
            while (!isLast) {
              const blkHeader = Buffer.alloc(4);
              if (fs.readSync(fd, blkHeader, 0, 4, pos) < 4) break;
              pos += 4;
              isLast = (blkHeader[0] & 0x80) !== 0;
              const blockType = blkHeader[0] & 0x7F;
              const blockLength = (blkHeader[1] << 16) | (blkHeader[2] << 8) | blkHeader[3];

              if (blockType === 0 && blockLength >= 18) {
                const sBuf = Buffer.alloc(Math.min(blockLength, 34));
                fs.readSync(fd, sBuf, 0, sBuf.length, pos);
                const sampleRate = (sBuf[10] << 12) | (sBuf[11] << 4) | (sBuf[12] >> 4);
                const totalSamples = ((sBuf[13] & 0x0f) * 0x100000000) +
                  (sBuf[14] << 24 >>> 0) + (sBuf[15] << 16) + (sBuf[16] << 8) + sBuf[17];
                if (sampleRate > 0 && totalSamples > 0 && !duration) {
                  duration = totalSamples / sampleRate;
                }
              } else if (blockType === 4) {
                const vBuf = Buffer.alloc(Math.min(blockLength, 1024 * 1024));
                fs.readSync(fd, vBuf, 0, vBuf.length, pos);
                let vPos = 0;
                if (vPos + 4 <= vBuf.length) {
                  const vendorLen = vBuf.readUInt32LE(vPos);
                  vPos += 4 + vendorLen;
                  if (vPos + 4 <= vBuf.length) {
                    const count = vBuf.readUInt32LE(vPos);
                    vPos += 4;
                    for (let i = 0; i < count && vPos + 4 <= vBuf.length; i++) {
                      const commentLen = vBuf.readUInt32LE(vPos);
                      vPos += 4;
                      if (vPos + commentLen > vBuf.length) break;
                      const comment = vBuf.toString('utf8', vPos, vPos + commentLen);
                      vPos += commentLen;
                      const eq = comment.indexOf('=');
                      if (eq > 0) {
                        const key = comment.slice(0, eq).toUpperCase();
                        const val = comment.slice(eq + 1).trim();
                        if (key === 'TITLE' && !title) title = val;
                        else if (key === 'ARTIST' && !artist) artist = val;
                        else if (key === 'ALBUM' && !album) album = val;
                        else if (key === 'TRACKNUMBER' && !trackNum) {
                          const n = parseInt(val.split('/')[0], 10);
                          if (!isNaN(n)) trackNum = n;
                        }
                      }
                    }
                  }
                }
              } else if (blockType === 6 && !fs.existsSync(coverOut)) {
                try {
                  const pBuf = Buffer.alloc(Math.min(blockLength, 12 * 1024 * 1024));
                  fs.readSync(fd, pBuf, 0, pBuf.length, pos);
                  let pPos = 4;
                  const mimeLen = pBuf.readUInt32BE(pPos);
                  pPos += 4 + mimeLen;
                  const descLen = pBuf.readUInt32BE(pPos);
                  pPos += 4 + descLen + 16;
                  const picLen = pBuf.readUInt32BE(pPos);
                  pPos += 4;
                  if (picLen > 0 && pPos + picLen <= pBuf.length) {
                    fs.writeFileSync(coverOut, pBuf.subarray(pPos, pPos + picLen));
                  }
                } catch (e) {}
              }
              pos += blockLength;
            }
          }
        } catch (e) {
        } finally {
          if (fd !== null) {
            try { fs.closeSync(fd); } catch (e) {}
          }
        }

        const platform = require('../shared/platform');
        const ffprobeBin = platform.resolveBinary('ffprobe');
        const ffmpegBin = platform.resolveBinary('ffmpeg');

        if ((!title || !artist || !duration) && ffprobeBin) {
          await new Promise(resolve => {
            execFile(ffprobeBin, [
              '-v', 'quiet',
              '-print_format', 'json',
              '-show_format',
              '-show_streams',
              filePath
            ], { timeout: 5000 }, (err, stdout) => {
              if (!err && stdout) {
                try {
                  const info = JSON.parse(stdout);
                  const tags = (info.format && info.format.tags) || {};
                  const getTag = (name) => {
                    const k = Object.keys(tags).find(x => x.toLowerCase() === name.toLowerCase());
                    return k ? String(tags[k]).trim() : '';
                  };
                  if (!title) title = getTag('title');
                  if (!artist) artist = getTag('artist') || getTag('album_artist') || getTag('composer');
                  if (!album) album = getTag('album');
                  if (!trackNum) {
                    const trk = getTag('track');
                    const n = parseInt(trk.split('/')[0], 10);
                    if (!isNaN(n)) trackNum = n;
                  }
                  if (!duration && info.format && info.format.duration) {
                    const d = parseFloat(info.format.duration);
                    if (!isNaN(d) && d > 0) duration = d;
                  }
                } catch (e) {}
              }
              resolve();
            });
          });
        }

        if ((!title || !artist || !album || !duration) && ffmpegBin) {
          await new Promise(resolve => {
            execFile(ffmpegBin, [
              '-i', filePath,
              '-f', 'ffmetadata',
              '-'
            ], { timeout: 5000 }, (err, stdout, stderr) => {
              if (stdout) {
                try {
                  const lines = stdout.split(/\r?\n/);
                  const tags = {};
                  for (const line of lines) {
                    const eq = line.indexOf('=');
                    if (eq > 0) {
                      const k = line.slice(0, eq).trim().toLowerCase();
                      const v = line.slice(eq + 1).trim();
                      if (v) tags[k] = v;
                    }
                  }
                  if (!title && tags.title) title = tags.title;
                  if (!artist && (tags.artist || tags.album_artist || tags.composer)) artist = tags.artist || tags.album_artist || tags.composer;
                  if (!album && tags.album) album = tags.album;
                  if (!trackNum && tags.track) {
                    const n = parseInt(tags.track.split('/')[0], 10);
                    if (!isNaN(n)) trackNum = n;
                  }
                } catch (e) {}
              }
              if (!duration && stderr) {
                const m = stderr.match(/Duration:\s*(\d+):(\d+):([\d.]+)/i);
                if (m) {
                  const d = parseInt(m[1], 10) * 3600 + parseInt(m[2], 10) * 60 + parseFloat(m[3]);
                  if (!isNaN(d) && d > 0) duration = d;
                }
              }
              resolve();
            });
          });
        }

        if (!fs.existsSync(coverOut) && ffmpegBin) {
          await new Promise(resolve => {
            execFile(ffmpegBin, [
              '-y',
              '-i', filePath,
              '-an',
              '-vcodec', 'mjpeg',
              '-frames:v', '1',
              coverOut
            ], { timeout: 5000 }, () => resolve());
          });
        }

        const ext = path.extname(filePath);
        const base = path.basename(filePath, ext);
        if (!title) {
          if (base.includes(' - ')) {
            const parts = base.split(' - ');
            if (!artist) artist = parts[0].trim();
            title = parts.slice(1).join(' - ').trim();
          } else {
            title = base.trim();
          }
        }
        if (!artist) {
          artist = 'Unknown Artist';
        }

        return {
          id: 'local_' + fileHash,
          title,
          artist: artist || 'Unknown Artist',
          album: album || '',
          trackNumber: trackNum || 0,
          duration: Math.round(duration || 0),
          path: filePath,
          url: url.pathToFileURL(filePath).toString(),
          thumbnail: fs.existsSync(coverOut) ? url.pathToFileURL(coverOut).toString() : '',
          cover: fs.existsSync(coverOut) ? url.pathToFileURL(coverOut).toString() : '',
          platform: 'local'
        };
      }

      ipcMain.handle('pick-local-files', async () => {
        const r = await dialog.showOpenDialog(mainWindow, {
          properties: ['openFile', 'multiSelections'],
          filters: [{ name: 'Audio', extensions: ['mp3', 'flac', 'wav', 'ogg', 'm4a', 'aac', 'opus', 'wma'] }]
        });
        if (r.canceled || !r.filePaths) return [];
        const tracks = [];
        for (const fp of r.filePaths) {
          try {
            const meta = await extractLocalTrackMetadata(fp);
            if (meta) tracks.push(meta);
          } catch (e) {}
        }
        return tracks;
      });

      ipcMain.handle('pick-local-folder', async () => {
        const r = await dialog.showOpenDialog(mainWindow, {
          properties: ['openDirectory', 'multiSelections']
        });
        if (r.canceled || !r.filePaths) return [];
        const AUDIO_EXT = new Set(['.mp3', '.flac', '.wav', '.ogg', '.m4a', '.aac', '.opus', '.wma']);
        const sources = [];

        function scanDir(dir) {
          try {
            const entries = fs.readdirSync(dir, { withFileTypes: true });
            for (const entry of entries) {
              const fullPath = path.join(dir, entry.name);
              if (entry.isDirectory()) {
                scanDir(fullPath);
              } else if (entry.isFile()) {
                const ext = path.extname(entry.name).toLowerCase();
                if (AUDIO_EXT.has(ext)) {
                  sources.push(fullPath);
                }
              }
            }
          } catch (e) {}
        }

        for (const dir of r.filePaths) {
          scanDir(dir);
        }

        const tracks = [];
        for (const fp of sources) {
          try {
            const meta = await extractLocalTrackMetadata(fp);
            if (meta) tracks.push(meta);
          } catch (e) {}
        }
        return tracks;
      });

      ipcMain.handle('rescan-local-metadata', async (event, paths) => {
        if (!Array.isArray(paths)) return [];
        const tracks = [];
        for (const fp of paths) {
          if (!fp || typeof fp !== 'string') continue;
          try {
            if (fs.existsSync(fp)) {
              const meta = await extractLocalTrackMetadata(fp);
              if (meta) tracks.push(meta);
            }
          } catch (e) {}
        }
        return tracks;
      });

      ipcMain.handle('get-data-dir', () => {
        const platform = require('../shared/platform');
        return {
          dir: platform.DATA_DIR,
          defaultDir: platform.DEFAULT_DATA_DIR,
          isCustom: platform.DATA_DIR !== platform.DEFAULT_DATA_DIR
        };
      });

      ipcMain.handle('pick-data-dir', async () => {
        const platform = require('../shared/platform');
        const r = await dialog.showOpenDialog(mainWindow, {
          title: 'Choose music folder',
          defaultPath: platform.DATA_DIR,
          properties: ['openDirectory', 'createDirectory']
        });
        return r.canceled ? null : r.filePaths[0];
      });

      ipcMain.handle('set-data-dir', async (event, payload) => {
        const platform = require('../shared/platform');
        const target = payload && payload.dir ? path.resolve(String(payload.dir)) : null;
        const move = !!(payload && payload.move);
        if (!target) return { ok: false, error: 'No folder selected' };

        const current = path.resolve(platform.DATA_DIR);
        if (target === current) return { ok: true, dir: target, moved: false };

        const rel = path.relative(current, target);
        const insideCurrent = rel === '' || (!rel.startsWith('..') && !path.isAbsolute(rel));
        const relBack = path.relative(target, current);
        const currentInsideTarget = relBack === '' || (!relBack.startsWith('..') && !path.isAbsolute(relBack));
        if (insideCurrent || currentInsideTarget) {
          return { ok: false, error: 'Choose a folder outside the current music folder' };
        }

        try {
          fs.mkdirSync(target, { recursive: true });
          fs.accessSync(target, fs.constants.W_OK);
        } catch (e) {
          return { ok: false, error: 'That folder is not writable' };
        }

        if (move) {
          try {
            await platform.copyDataEntries(current, target);
          } catch (e) {
            return { ok: false, error: 'Could not move files: ' + e.message };
          }
        }

        platform.setDataDir(target);

        if (move) {
          try { await platform.removeDataEntries(current); } catch (e) {}
        }

        return { ok: true, dir: target, moved: move };
      });

      const defaultDownloadsDir = () => require('../shared/platform').defaultDownloadDir();

      ipcMain.handle('get-download-dir', () => {
        const platform = require('../shared/platform');
        const def = defaultDownloadsDir();
        const custom = platform.resolveDownloadDir();
        return {
          dir: custom || def,
          defaultDir: def,
          isCustom: !!custom
        };
      });

      ipcMain.handle('pick-download-dir', async () => {
        const platform = require('../shared/platform');
        const r = await dialog.showOpenDialog(mainWindow, {
          title: 'Choose download folder',
          defaultPath: platform.resolveDownloadDir() || defaultDownloadsDir() || undefined,
          properties: ['openDirectory', 'createDirectory']
        });
        return r.canceled ? null : r.filePaths[0];
      });

      ipcMain.handle('set-download-dir', async (event, payload) => {
        const platform = require('../shared/platform');
        const reset = !!(payload && payload.reset);
        if (reset) {
          platform.setDownloadDir(null);
          return { ok: true, dir: defaultDownloadsDir(), reset: true };
        }
        const target = payload && payload.dir ? path.resolve(String(payload.dir)) : null;
        if (!target) return { ok: false, error: 'No folder selected' };
        try {
          fs.mkdirSync(target, { recursive: true });
          fs.accessSync(target, fs.constants.W_OK);
        } catch (e) {
          return { ok: false, error: 'That folder is not writable' };
        }
        platform.setDownloadDir(target);
        return { ok: true, dir: target };
      });

      ipcMain.on('restart-app', () => {
        app.relaunch();
        app.exit(0);
      });

      ipcMain.on('set-tray-on-close', (e, v) => {
        trayOnClose = !!v;
      });

      ipcMain.handle('get-multi-instance', () => !!readStartupConfig().multiInstance);
      ipcMain.on('set-multi-instance', (e, value) => { writeStartupConfig({ multiInstance: !!value }); });

      const MEDIA_KEYS = { MediaPlayPause: 'toggle', MediaNextTrack: 'next', MediaPreviousTrack: 'prev', MediaStop: 'stop' };
      let mediaKeysWanted = true;
      let mediaKeysActive = false;
      const sendMedia = (cmd) => {
        if (process.env.RIFF_DEBUG === '1') console.log('[media] globalShortcut', cmd);
        if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('media-command', cmd);
      };
      function unregisterMediaKeys() {
        for (const k of Object.keys(MEDIA_KEYS)) {
          try { globalShortcut.unregister(k); } catch (e) {}
        }
      }
      function registerMediaKeys() {
        if (process.platform !== 'win32') return;
        unregisterMediaKeys();
        if (!mediaKeysWanted || !mediaKeysActive) return;
        for (const [k, cmd] of Object.entries(MEDIA_KEYS)) {
          try {
            if (!globalShortcut.register(k, () => sendMedia(cmd))) {
              console.warn('[media] could not register', k);
            }
          } catch (e) {}
        }
      }
      ipcMain.on('set-media-keys', (e, enabled) => { mediaKeysWanted = !!enabled; registerMediaKeys(); });
      ipcMain.on('media-keys-active', () => { if (!mediaKeysActive) { mediaKeysActive = true; registerMediaKeys(); } });
      powerMonitor.on('resume', registerMediaKeys);
      powerMonitor.on('unlock-screen', registerMediaKeys);
      app.on('will-quit', () => {
        try {
          globalShortcut.unregister('MediaPlayPause');
          globalShortcut.unregister('MediaNextTrack');
          globalShortcut.unregister('MediaPreviousTrack');
        } catch (e) {}
        globalShortcut.unregisterAll();
      });

      let windowButtonsWanted = true;
      function syncWindowButtons() {
        if (!IS_MAC || !mainWindow || mainWindow.isDestroyed()) return;
        try { mainWindow.setWindowButtonVisibility(windowButtonsWanted && !isMiniWindow); } catch (_) {}
      }
      ipcMain.on('set-window-buttons-visible', (e, visible) => {
        windowButtonsWanted = !!visible;
        syncWindowButtons();
      });

      let preMiniLyricsH = null;
      ipcMain.removeHandler('toggle-mini-window');
      ipcMain.handle('toggle-mini-window', () => {
        if (!mainWindow || mainWindow.isDestroyed()) return false;
        if (!isMiniWindow) {
          isMiniWindow = true;
          preMiniBounds = mainWindow.isMaximized() ? mainWindow.getNormalBounds() : mainWindow.getBounds();
          if (mainWindow.isMaximized()) mainWindow.unmaximize();
          mainWindow.setMinimumSize(280, 115);
          mainWindow.setSize(380, 160);
          mainWindow.setAlwaysOnTop(true);
          syncWindowButtons();
          return true;
        } else {
          isMiniWindow = false;
          preMiniLyricsH = null;
          mainWindow.setAlwaysOnTop(false);
          syncWindowButtons();
          mainWindow.setMinimumSize(800, 540);
          if (preMiniBounds) {
            mainWindow.setBounds(clampBoundsToDisplay(preMiniBounds));
          } else {
            mainWindow.setSize(1000, 700);
          }
          return false;
        }
      });

      ipcMain.removeHandler('set-mini-lyrics');
      ipcMain.handle('set-mini-lyrics', (e, enabled) => {
        if (!mainWindow || mainWindow.isDestroyed() || !isMiniWindow) return false;
        const bounds = mainWindow.getBounds();
        const display = screen.getDisplayMatching(bounds);
        const maxH = Math.floor(display.workArea.height * 0.9);
        if (enabled) {
          if (!preMiniLyricsH) preMiniLyricsH = bounds.height;
          mainWindow.setMinimumSize(320, 240);
          const targetH = Math.min(Math.max(bounds.height, 360), maxH);
          if (bounds.y + targetH > display.workArea.y + display.workArea.height) {
            const newY = Math.max(display.workArea.y, display.workArea.y + display.workArea.height - targetH);
            mainWindow.setBounds({ x: bounds.x, y: newY, width: bounds.width, height: targetH });
          } else {
            mainWindow.setSize(bounds.width, targetH);
          }
        } else {
          mainWindow.setMinimumSize(320, 140);
          const targetH = preMiniLyricsH || 160;
          mainWindow.setSize(bounds.width, targetH);
          preMiniLyricsH = null;
        }
        return true;
      });

      ipcMain.handle('get-app-version', () => app.getVersion());

      ipcMain.handle('save-playlists-json', async (event, payload) => {
        const win = BrowserWindow.fromWebContents(event.sender) || mainWindow;
        const defaultName = payload && payload.defaultName ? payload.defaultName : 'playlists.json';
        const content = payload && payload.content ? payload.content : '';
        const r = await dialog.showSaveDialog(win, {
          title: 'Export playlists as JSON',
          defaultPath: defaultName,
          filters: [{ name: 'JSON', extensions: ['json'] }]
        });
        if (r.canceled || !r.filePath) return { canceled: true };
        await fs.promises.writeFile(r.filePath, content, 'utf8');
        return { success: true, filePath: r.filePath };
      });

      ipcMain.handle('account-export', async (event, payload) => {
        try {
          const win = BrowserWindow.fromWebContents(event.sender) || mainWindow;
          const r = await dialog.showSaveDialog(win, {
            title: 'Export Riff account',
            defaultPath: 'riff-account.rfacc',
            filters: [{ name: 'Riff Account (.rfacc)', extensions: ['rfacc'] }]
          });
          if (r.canceled || !r.filePath) return { canceled: true };
          const buffer = encryptAccount(payload || {});
          await fs.promises.writeFile(r.filePath, buffer);
          return { success: true, filePath: r.filePath };
        } catch (err) {
          return { success: false, error: err.message };
        }
      });

      ipcMain.handle('account-import', async (event) => {
        try {
          const win = BrowserWindow.fromWebContents(event.sender) || mainWindow;
          const r = await dialog.showOpenDialog(win, {
            title: 'Import Riff account',
            properties: ['openFile'],
            filters: [{ name: 'Riff Account (.rfacc)', extensions: ['rfacc'] }]
          });
          if (r.canceled || !r.filePaths || !r.filePaths.length) return { canceled: true };
          const buffer = await fs.promises.readFile(r.filePaths[0]);
          const data = decryptAccount(buffer);
          return { success: true, data, filePath: r.filePaths[0] };
        } catch (err) {
          return { success: false, error: err.message };
        }
      });

      ipcMain.handle('show-playlist-context-menu', async (event) => {
        const win = BrowserWindow.fromWebContents(event.sender) || mainWindow;
        return new Promise((resolve) => {
          const menu = Menu.buildFromTemplate([
            {
              label: 'Export as JSON',
              click: () => resolve('export')
            }
          ]);
          menu.popup({
            window: win,
            callback: () => resolve(null)
          });
        });
      });

      const YT_COOKIES_FILE = path.join(DATA_DIR, 'yt-cookies.txt');
      const YT_LOGOUT_FLAG = path.join(DATA_DIR, '.yt-logged-out');
      let youtubeCookieImportPromise = null;

      function hasValidYtCookiesFile(file = YT_COOKIES_FILE) {
        try {
          if (!fs.existsSync(file)) return false;
          return cookieJarHasYtLogin(fs.readFileSync(file, 'utf8'));
        } catch (_) {
          return false;
        }
      }

      async function importYoutubeCookiesToSession() {
        if (youtubeCookieImportPromise) return youtubeCookieImportPromise;
        youtubeCookieImportPromise = (async () => {
          const txt = fs.readFileSync(YT_COOKIES_FILE, 'utf8');
          const googleSession = session.fromPartition('persist:riff-google');
          await googleSession.clearStorageData({ storages: ['cookies'] });
          const rows = txt.split(/\r?\n/);
          for (const row of rows) {
            const line = row.startsWith('#HttpOnly_') ? row.slice(10) : row;
            if (!line || line.startsWith('#')) continue;
            const fields = line.split('\t');
            if (fields.length < 7) continue;
            const [domain, , cookiePath, secure, expires, name, ...valueParts] = fields;
            if (!domain || !name || valueParts.length === 0) continue;
            const host = domain.replace(/^\./, '');
            if (host !== 'google.com' && !host.endsWith('.google.com') &&
                host !== 'youtube.com' && !host.endsWith('.youtube.com')) continue;
            const details = {
              url: `https://${host}${cookiePath.startsWith('/') ? cookiePath : '/'}`,
              name,
              value: valueParts.join('\t'),
              domain,
              path: cookiePath || '/',
              secure: secure.toUpperCase() === 'TRUE',
              httpOnly: row.startsWith('#HttpOnly_')
            };
            const expiration = Number(expires);
            if (Number.isFinite(expiration) && expiration > 0) details.expirationDate = expiration;
            await googleSession.cookies.set(details);
          }
        })();
        try {
          await youtubeCookieImportPromise;
        } finally {
          youtubeCookieImportPromise = null;
        }
      }

      function isYoutubeAuthUrl(value) {
        try {
          const parsed = new URL(value);
          return parsed.protocol === 'https:' &&
            (parsed.hostname === 'youtube.com' || parsed.hostname.endsWith('.youtube.com') ||
              parsed.hostname === 'google.com' || parsed.hostname.endsWith('.google.com'));
        } catch (_) {
          return false;
        }
      }

      async function saveYoutubePlaybackCookies() {
        const googleSession = session.fromPartition('persist:riff-google');
        const cookies = await googleSession.cookies.get({});
        const youtubeCookies = cookies.filter(cookie => {
          const domain = String(cookie.domain || '').replace(/^\./, '').toLowerCase();
          return domain === 'youtube.com' || domain.endsWith('.youtube.com') ||
            domain === 'google.com' || domain.endsWith('.google.com');
        });
        const hasAuthCookie = youtubeCookies.some(cookie =>
          ['SAPISID', '__Secure-3PAPISID', '__Secure-1PAPISID', 'LOGIN_INFO', 'SID'].includes(cookie.name)
        );
        if (!hasAuthCookie) {
          return {
            success: false,
            error: 'YouTube sign-in was not detected. Finish signing in, then close the window.'
          };
        }

        const lines = ['# Netscape HTTP Cookie File'];
        for (const cookie of youtubeCookies) {
          if (!cookie.domain || !cookie.name) continue;
          const domain = cookie.domain;
          const includeSubdomains = domain.startsWith('.') ? 'TRUE' : 'FALSE';
          const httpOnlyDomain = cookie.httpOnly ? `#HttpOnly_${domain}` : domain;
          const secure = cookie.secure ? 'TRUE' : 'FALSE';
          const expires = Number.isFinite(cookie.expirationDate) ? Math.floor(cookie.expirationDate) : 0;
          lines.push(`${httpOnlyDomain}\t${includeSubdomains}\t${cookie.path || '/'}\t${secure}\t${expires}\t${cookie.name}\t${cookie.value}`);
        }

        fs.mkdirSync(DATA_DIR, { recursive: true });
        const tempFile = `${YT_COOKIES_FILE}.${process.pid}.tmp`;
        await fs.promises.writeFile(tempFile, `${lines.join('\n')}\n`, { encoding: 'utf8', mode: 0o600 });
        await fs.promises.rm(YT_COOKIES_FILE, { force: true });
        await fs.promises.rename(tempFile, YT_COOKIES_FILE);
        try { fs.unlinkSync(YT_LOGOUT_FLAG); } catch (_) {}

        if (!hasValidYtCookiesFile()) {
          await fs.promises.rm(YT_COOKIES_FILE, { force: true });
          return { success: false, error: 'The YouTube session could not be saved.' };
        }
        return { success: true, loggedIn: true };
      }

      const CLEAN_LOGIN_UA = process.platform === 'darwin'
        ? 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
        : process.platform === 'win32'
        ? 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
        : 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36';

      async function openYoutubeSignInWindow() {
        if (!fs.existsSync(YT_LOGOUT_FLAG)) {
          const imported = await exportYoutubeCookiesToDisk().catch(() => false);
          if (imported && hasValidYtCookiesFile()) {
            if (mainWindow && !mainWindow.isDestroyed()) {
              mainWindow.webContents.send('youtube-playback-auth', { success: true, loggedIn: true });
            }
            return { success: true, loggedIn: true };
          }
        }

        if (youtubePlaybackWindow && !youtubePlaybackWindow.isDestroyed()) {
          youtubePlaybackWindow.show();
          youtubePlaybackWindow.focus();
          return new Promise((resolve) => {
            youtubePlaybackWindow.once('closed', () => {
              resolve({ success: hasValidYtCookiesFile(), loggedIn: hasValidYtCookiesFile() });
            });
          });
        }

        const googleSession = session.fromPartition('persist:riff-google');
        googleSession.setUserAgent(CLEAN_LOGIN_UA);

        return new Promise((resolve) => {
          const loginWindow = new BrowserWindow({
            width: 960,
            height: 720,
            title: 'Sign in to YouTube Music',
            autoHideMenuBar: true,
            parent: mainWindow && !mainWindow.isDestroyed() ? mainWindow : undefined,
            webPreferences: {
              session: googleSession,
              nodeIntegration: false,
              contextIsolation: true,
              sandbox: true
            }
          });
          youtubePlaybackWindow = loginWindow;
          loginWindow.webContents.setUserAgent(CLEAN_LOGIN_UA);

          let pollTimer = null;
          let settled = false;
          const finishAuth = async () => {
            if (settled) return;
            settled = true;
            if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
            const result = await saveYoutubePlaybackCookies().catch(error => ({
              success: false,
              loggedIn: false,
              error: error && error.message ? error.message : 'Could not save the YouTube session.'
            }));
            if (mainWindow && !mainWindow.isDestroyed()) {
              mainWindow.webContents.send('youtube-playback-auth', result);
            }
            resolve(result);
          };

          pollTimer = setInterval(async () => {
            if (!loginWindow || loginWindow.isDestroyed()) {
              clearInterval(pollTimer);
              return;
            }
            try {
              const cookies = await googleSession.cookies.get({});
              const hasAuth = cookies.some(c =>
                ['SAPISID', '__Secure-3PAPISID', '__Secure-1PAPISID', 'LOGIN_INFO'].includes(c.name) &&
                String(c.domain || '').includes('youtube.com')
              );
              if (hasAuth) {
                clearInterval(pollTimer);
                pollTimer = null;
                await finishAuth();
                if (!loginWindow.isDestroyed()) loginWindow.close();
              }
            } catch (_) {}
          }, 1200);

          loginWindow.webContents.setWindowOpenHandler(({ url: targetUrl }) => {
            if (isYoutubeAuthUrl(targetUrl)) return { action: 'allow' };
            shell.openExternal(targetUrl);
            return { action: 'deny' };
          });

          loginWindow.once('closed', async () => {
            if (youtubePlaybackWindow === loginWindow) youtubePlaybackWindow = null;
            await finishAuth();
          });

          loginWindow.loadURL('https://accounts.google.com/ServiceLogin?service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F').then(() => {
            if (!loginWindow.isDestroyed()) loginWindow.show();
          }).catch(() => {
            if (!loginWindow.isDestroyed()) loginWindow.loadURL('https://music.youtube.com/').catch(() => {});
          });
        });
      }

      ipcMain.handle('youtube-playback-status', () => ({
        loggedIn: hasValidYtCookiesFile()
      }));

      ipcMain.handle('youtube-playback-login', async () => {
        return await openYoutubeSignInWindow();
      });

      ipcMain.handle('youtube-playback-logout', async () => {
        try {
          fs.mkdirSync(DATA_DIR, { recursive: true });
          await fs.promises.writeFile(YT_LOGOUT_FLAG, '1', 'utf8');
          await fs.promises.rm(YT_COOKIES_FILE, { force: true });
          await session.fromPartition('persist:riff-google').clearStorageData({ storages: ['cookies'] });
          return { success: true, loggedIn: false };
        } catch (error) {
          return { success: false, error: error && error.message ? error.message : 'Could not sign out of YouTube.' };
        }
      });

      function getSystemDefaultBrowserSpec() {
        try {
          if (process.platform === 'linux') {
            const raw = execSync('xdg-settings get default-web-browser 2>/dev/null || true', { encoding: 'utf8' }).trim().toLowerCase();
            if (raw.includes('firefox')) return 'firefox';
            if (raw.includes('zen')) return 'firefox:' + path.join(os.homedir(), '.zen');
            if (raw.includes('brave')) return 'brave';
            if (raw.includes('google-chrome') || raw.includes('chrome')) return 'chrome';
            if (raw.includes('chromium')) return 'chromium';
            if (raw.includes('vivaldi')) return 'vivaldi';
            if (raw.includes('edge')) return 'edge';
          }
        } catch (_) {}
        return 'firefox';
      }

      function getCandidateBrowserSpecs() {
        const home = os.homedir();
        const specs = [getSystemDefaultBrowserSpec()];
        const addIfDir = (dirPath, spec) => {
          try {
            if (fs.existsSync(dirPath) && !specs.includes(spec)) specs.push(spec);
          } catch (_) {}
        };
        if (process.platform === 'linux') {
          addIfDir(path.join(home, '.config', 'BraveSoftware', 'Brave-Browser'), 'brave');
          addIfDir(path.join(home, '.mozilla', 'firefox'), 'firefox');
          addIfDir(path.join(home, '.config', 'google-chrome'), 'chrome');
          addIfDir(path.join(home, '.config', 'chromium'), 'chromium');
          addIfDir(path.join(home, '.zen'), 'firefox:' + path.join(home, '.zen'));
          addIfDir(path.join(home, '.config', 'zen'), 'firefox:' + path.join(home, '.config', 'zen'));
          addIfDir(path.join(home, '.var/app/app.zen_browser.zen/.zen'), 'firefox:' + path.join(home, '.var/app/app.zen_browser.zen/.zen'));
          addIfDir(path.join(home, '.librewolf'), 'firefox:' + path.join(home, '.librewolf'));
          addIfDir(path.join(home, '.floorp'), 'firefox:' + path.join(home, '.floorp'));
          addIfDir(path.join(home, '.config', 'vivaldi'), 'vivaldi');
          addIfDir(path.join(home, '.config', 'microsoft-edge'), 'edge');
        } else if (process.platform === 'win32') {
          const local = process.env.LOCALAPPDATA || '';
          const roaming = process.env.APPDATA || '';
          addIfDir(path.join(roaming, 'Mozilla', 'Firefox'), 'firefox');
          addIfDir(path.join(local, 'Google', 'Chrome'), 'chrome');
          addIfDir(path.join(local, 'BraveSoftware', 'Brave-Browser'), 'brave');
          addIfDir(path.join(local, 'Microsoft', 'Edge'), 'edge');
          addIfDir(path.join(roaming, 'Zen'), 'firefox:' + path.join(roaming, 'Zen'));
          addIfDir(path.join(local, 'Vivaldi'), 'vivaldi');
        } else if (process.platform === 'darwin') {
          const appSup = path.join(home, 'Library', 'Application Support');
          addIfDir(path.join(appSup, 'Firefox'), 'firefox');
          addIfDir(path.join(appSup, 'Google', 'Chrome'), 'chrome');
          addIfDir(path.join(appSup, 'BraveSoftware', 'Brave-Browser'), 'brave');
          addIfDir(path.join(appSup, 'zen'), 'firefox:' + path.join(appSup, 'zen'));
        }
        if (!specs.includes('firefox')) specs.push('firefox');
        if (!specs.includes('chrome')) specs.push('chrome');
        return specs;
      }

      let exportCookiesPromise = null;
      function exportYoutubeCookiesToDisk() {
        if (exportCookiesPromise) return exportCookiesPromise;
        exportCookiesPromise = (async () => {
          fs.mkdirSync(DATA_DIR, { recursive: true });
          if (hasValidYtCookiesFile()) {
            await importYoutubeCookiesToSession();
            return true;
          }

          const tmpTestCookie = '/tmp/test-yt-cookies.txt';
          try {
            if (fs.existsSync(tmpTestCookie)) {
              const tmpTxt = fs.readFileSync(tmpTestCookie, 'utf8');
              if (cookieJarHasYtLogin(tmpTxt)) {
                fs.writeFileSync(YT_COOKIES_FILE, tmpTxt, 'utf8');
                await importYoutubeCookiesToSession();
                try { if (fs.existsSync(YT_LOGOUT_FLAG)) fs.unlinkSync(YT_LOGOUT_FLAG); } catch (_) {}
                return true;
              }
            }
          } catch (_) {}

          const ytdlpBin = resolveBinary('yt-dlp');
          if (!ytdlpBin) return false;

          const specs = getCandidateBrowserSpecs();
          for (const spec of specs) {
            const tmpJar = path.join(DATA_DIR, `.yt-cookies-${process.pid}.tmp`);
            try { fs.rmSync(tmpJar, { force: true }); } catch (_) {}
            try {
              await new Promise((resolve) => {
                execFile(ytdlpBin, [
                  '--no-config',
                  '--cookies-from-browser', spec,
                  '--cookies', tmpJar,
                  '--skip-download',
                  '--no-warnings',
                  'https://www.youtube.com/watch?v=dQw4w9WgXcQ'
                ], { timeout: 15000, windowsHide: true }, () => resolve());
              });
              if (hasValidYtCookiesFile(tmpJar)) {
                fs.copyFileSync(tmpJar, YT_COOKIES_FILE);
                await importYoutubeCookiesToSession();
                try { if (fs.existsSync(YT_LOGOUT_FLAG)) fs.unlinkSync(YT_LOGOUT_FLAG); } catch (_) {}
                return true;
              }
            } catch (_) {
            } finally {
              try { fs.rmSync(tmpJar, { force: true }); } catch (_) {}
            }
          }
          return false;
        })().finally(() => { exportCookiesPromise = null; });
        return exportCookiesPromise;
      }

      const GOOGLE_OAUTH_FILE = path.join(DATA_DIR, 'google-oauth.enc');

      function readGoogleOAuthData() {
        if (!fs.existsSync(GOOGLE_OAUTH_FILE)) return null;
        if (!safeStorage.isEncryptionAvailable()) {
          throw new Error('Secure storage is unavailable on this system');
        }
        return JSON.parse(safeStorage.decryptString(fs.readFileSync(GOOGLE_OAUTH_FILE)));
      }

      function writeGoogleOAuthData(data) {
        if (!safeStorage.isEncryptionAvailable()) {
          throw new Error('Secure storage is unavailable on this system');
        }
        fs.mkdirSync(DATA_DIR, { recursive: true });
        fs.writeFileSync(GOOGLE_OAUTH_FILE, safeStorage.encryptString(JSON.stringify(data)));
      }

      async function getGoogleAccessToken() {
        const auth = readGoogleOAuthData();
        if (!auth || !auth.refreshToken) {
          throw new Error('Sign in to Google before importing your YouTube library');
        }
        if (auth.accessToken && auth.expiresAt > Date.now() + 60000) {
          return auth.accessToken;
        }
        const refreshed = await refreshAccessToken(auth.refreshToken);
        auth.accessToken = refreshed.access_token;
        auth.expiresAt = Date.now() + (Number(refreshed.expires_in) || 3600) * 1000;
        if (refreshed.refresh_token) auth.refreshToken = refreshed.refresh_token;
        writeGoogleOAuthData(auth);
        return auth.accessToken;
      }

      async function startGoogleOAuthLogin() {
        if (!safeStorage.isEncryptionAvailable()) {
          throw new Error('Secure storage is unavailable on this system');
        }
        const codeVerifier = createCodeVerifier();
        const state = createOAuthState();
        const server = http.createServer();
        await new Promise((resolve, reject) => {
          server.once('error', reject);
          server.listen(0, '127.0.0.1', resolve);
        });
        const address = server.address();
        const redirectUri = `http://127.0.0.1:${address.port}`;
        const authUrl = createAuthorizationUrl({
          redirectUri,
          state,
          codeChallenge: createCodeChallenge(codeVerifier)
        });
        let cancelCallback;
        try {
          const callback = new Promise((resolve, reject) => {
            let settled = false;
            const timeout = setTimeout(() => {
              finish(new Error('Google sign-in timed out. Complete the authorization in your browser and try again.'));
            }, 180000);
            const finish = (error, value) => {
              if (settled) return;
              settled = true;
              clearTimeout(timeout);
              server.removeListener('request', onRequest);
              if (error) reject(error);
              else resolve(value);
            };
            cancelCallback = () => finish(new Error('Google sign-in could not be completed'));
            const onRequest = (req, res) => {
              const isLoopback = req.socket.remoteAddress === '127.0.0.1' ||
                req.socket.remoteAddress === '::ffff:127.0.0.1';
              if (!isLoopback) {
                res.writeHead(403);
                res.end();
                return;
              }
              const requestUrl = new URL(req.url || '/', redirectUri);
              if (req.method !== 'GET' || requestUrl.pathname !== '/') {
                res.writeHead(404);
                res.end();
                return;
              }
              const returnedState = requestUrl.searchParams.get('state') || '';
              const expected = Buffer.from(state);
              const actual = Buffer.from(returnedState);
              if (actual.length !== expected.length || !crypto.timingSafeEqual(actual, expected)) {
                res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
                res.end('Authorization could not be verified. Return to Riff and try again.');
                finish(new Error('Google authorization state did not match'));
                return;
              }
              const providerError = requestUrl.searchParams.get('error');
              if (providerError) {
                res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
                res.end('<!doctype html><title>Riff</title><p>Google sign-in was cancelled. You can close this tab and return to Riff.</p>');
                finish(new Error(providerError === 'access_denied'
                  ? 'Google sign-in was cancelled'
                  : `Google authorization failed: ${providerError}`));
                return;
              }
              const code = requestUrl.searchParams.get('code');
              if (!code) {
                res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
                res.end('Google did not return an authorization code. Return to Riff and try again.');
                finish(new Error('Google did not return an authorization code'));
                return;
              }
              res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
              res.end('<!doctype html><title>Riff</title><p>Google account connected. You can close this tab and return to Riff.</p>');
              finish(null, { code });
            };
            server.on('request', onRequest);
          });
          await shell.openExternal(authUrl);
          const result = await callback;
          const tokens = await exchangeAuthorizationCode({
            code: result.code,
            redirectUri,
            codeVerifier
          });
          if (!tokens.refresh_token) {
            throw new Error('Google did not provide a refresh token. Revoke Riff access in your Google account and try again.');
          }
          writeGoogleOAuthData({
            accessToken: tokens.access_token,
            refreshToken: tokens.refresh_token,
            expiresAt: Date.now() + (Number(tokens.expires_in) || 3600) * 1000,
            scope: tokens.scope || ''
          });
        } finally {
          if (cancelCallback) cancelCallback();
          if (server.listening) {
            await new Promise(resolve => server.close(resolve));
          }
        }
      }

      ipcMain.handle('google-auth-status', async () => {
        const hasCookies = hasValidYtCookiesFile();
        if (hasCookies) {
          await importYoutubeCookiesToSession().catch(() => {});
          return { loggedIn: true };
        }
        return { loggedIn: false };
      });

      ipcMain.handle('google-logout', async () => {
        let revokeToken = '';
        let warning = '';
        try {
          const auth = readGoogleOAuthData();
          revokeToken = auth && auth.refreshToken ? auth.refreshToken : '';
        } catch (e) {
          warning = e && e.message ? e.message : 'Could not read the saved Google account';
        }
        try {
          fs.mkdirSync(DATA_DIR, { recursive: true });
          fs.writeFileSync(YT_LOGOUT_FLAG, '1', 'utf8');
          if (fs.existsSync(GOOGLE_OAUTH_FILE)) fs.unlinkSync(GOOGLE_OAUTH_FILE);
          if (fs.existsSync(YT_COOKIES_FILE)) fs.unlinkSync(YT_COOKIES_FILE);
          const s = session.fromPartition('persist:riff-google');
          await s.clearStorageData();
        } catch (e) {
          return { success: false, error: e && e.message ? e.message : 'Logout failed' };
        }
        if (revokeToken) {
          try {
            const response = await fetch('https://oauth2.googleapis.com/revoke', {
              method: 'POST',
              headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
              body: new URLSearchParams({ token: revokeToken }),
              signal: AbortSignal.timeout(10000)
            });
            if (!response.ok) warning = 'Signed out locally, but Google did not confirm token revocation.';
          } catch (_) {
            warning = 'Signed out locally, but Google could not be reached to revoke the token.';
          }
        }
        return { success: true, warning };
      });

      ipcMain.handle('google-login', async () => {
        return await openYoutubeSignInWindow();
      });

      ipcMain.handle('google-import-library', async (event, opts) => {
        const platform = (opts && opts.platform === 'ytmusic') ? 'ytmusic' : 'youtube';
        const doLikes = !opts || opts.doLikes !== false;
        const doPlaylists = !opts || opts.doPlaylists !== false;

        if (hasValidYtCookiesFile()) {
          await importYoutubeCookiesToSession().catch(() => {});
        }

        const s = session.fromPartition('persist:riff-google');
        const cookies = await s.cookies.get({});
        const sapisidCookie = Array.isArray(cookies) ? (
          cookies.find(c => c && c.name === 'SAPISID' && c.value) ||
          cookies.find(c => c && c.name === '__Secure-3PAPISID' && c.value) ||
          cookies.find(c => c && c.name === '__Secure-1PAPISID' && c.value)
        ) : null;
        if (!sapisidCookie) {
          return { success: false, error: 'YouTube import requires an active YouTube browser session.' };
        }
        const sapisid = sapisidCookie.value;

        const isYtm = platform === 'ytmusic';
        const browseUrl = isYtm
          ? 'https://music.youtube.com/youtubei/v1/browse?prettyPrint=false'
          : 'https://www.youtube.com/youtubei/v1/browse?prettyPrint=false';
        const origin = isYtm ? 'https://music.youtube.com' : 'https://www.youtube.com';
        const clientName = isYtm ? 'WEB_REMIX' : 'WEB';
        const clientVersion = isYtm ? '1.20240401.01.00' : '2.20240401.01.00';
        const likedBrowseId = isYtm ? 'VLLM' : 'VLLL';
        const playlistsBrowseId = isYtm ? 'FEmusic_liked_playlists' : 'FEplaylist_aggregation';

        function extractText(node) {
          if (!node) return '';
          if (typeof node === 'string') return node;
          if (typeof node.simpleText === 'string') return node.simpleText;
          if (Array.isArray(node.runs)) return node.runs.map(r => (r && r.text) || '').join('');
          return '';
        }

        function parseDurationSec(val) {
          if (!val) return 0;
          if (typeof val === 'number') return val;
          const str = String(val).trim();
          const m = str.match(/(?:(\d+):)?(\d+):(\d+)/);
          if (m) {
            const h = m[1] ? parseInt(m[1], 10) : 0;
            const min = parseInt(m[2], 10);
            const sec = parseInt(m[3], 10);
            return h * 3600 + min * 60 + sec;
          }
          const num = parseInt(str, 10);
          return Number.isFinite(num) ? num : 0;
        }

        function findContinuationToken(node, visited = new Set()) {
          if (!node || typeof node !== 'object') return null;
          if (visited.has(node)) return null;
          visited.add(node);
          if (typeof node.continuation === 'string' && node.continuation) return node.continuation;
          if (typeof node.token === 'string' && node.token) return node.token;
          if (node.continuationCommand && typeof node.continuationCommand.token === 'string') {
            return node.continuationCommand.token;
          }
          if (node.nextContinuationData && typeof node.nextContinuationData.continuation === 'string') {
            return node.nextContinuationData.continuation;
          }
          for (const k of Object.keys(node)) {
            const res = findContinuationToken(node[k], visited);
            if (res) return res;
          }
          return null;
        }

        function extractTracksGeneric(root) {
          const tracks = [];
          const seenIds = new Set();
          function walk(node, visited = new Set()) {
            if (!node || typeof node !== 'object') return;
            if (visited.has(node)) return;
            visited.add(node);
            if (node.musicResponsiveListItemRenderer) {
              const r = node.musicResponsiveListItemRenderer;
              let videoId = (r.playlistItemData && r.playlistItemData.videoId) ? r.playlistItemData.videoId : null;
              if (!videoId) {
                const findVid = (obj) => {
                  if (!obj || typeof obj !== 'object') return null;
                  if (obj.watchEndpoint && typeof obj.watchEndpoint.videoId === 'string') return obj.watchEndpoint.videoId;
                  for (const k of Object.keys(obj)) {
                    const v = findVid(obj[k]);
                    if (v) return v;
                  }
                  return null;
                };
                videoId = findVid(r);
              }
              if (videoId && !seenIds.has(videoId)) {
                seenIds.add(videoId);
                const title = extractText(r.flexColumns && r.flexColumns[0] && r.flexColumns[0].musicResponsiveListItemFlexColumnRenderer && r.flexColumns[0].musicResponsiveListItemFlexColumnRenderer.text) || 'untitled';
                let artist = '';
                if (r.flexColumns && r.flexColumns[1]) {
                  const runs = r.flexColumns[1].musicResponsiveListItemFlexColumnRenderer && r.flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text && r.flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text.runs;
                  if (Array.isArray(runs) && runs.length > 0) {
                    artist = runs[0].text || '';
                  } else {
                    artist = extractText(r.flexColumns[1].musicResponsiveListItemFlexColumnRenderer && r.flexColumns[1].musicResponsiveListItemFlexColumnRenderer.text);
                  }
                }
                let duration = 0;
                const findDur = (obj) => {
                  if (!obj || typeof obj !== 'object') return;
                  if (Array.isArray(obj.runs)) {
                    for (const ru of obj.runs) {
                      if (ru && ru.text && /^\d+:\d+(:\d+)?$/.test(ru.text.trim())) {
                        duration = parseDurationSec(ru.text.trim());
                        return;
                      }
                    }
                  }
                  for (const k of Object.keys(obj)) {
                    if (!duration) findDur(obj[k]);
                  }
                };
                findDur(r);
                const thumbs = r.thumbnail && r.thumbnail.musicThumbnailRenderer && r.thumbnail.musicThumbnailRenderer.thumbnail && r.thumbnail.musicThumbnailRenderer.thumbnail.thumbnails;
                const cover = (Array.isArray(thumbs) && thumbs.length > 0) ? thumbs[thumbs.length - 1].url : '';
                tracks.push({ videoId, title, artist, duration, cover });
              }
            } else if (node.playlistVideoRenderer || node.videoRenderer || node.compactVideoRenderer) {
              const r = node.playlistVideoRenderer || node.videoRenderer || node.compactVideoRenderer;
              const videoId = r.videoId;
              if (videoId && !seenIds.has(videoId)) {
                seenIds.add(videoId);
                const title = extractText(r.title) || 'untitled';
                const artist = extractText(r.shortBylineText) || extractText(r.ownerText) || '';
                const duration = r.lengthSeconds ? parseInt(r.lengthSeconds, 10) : parseDurationSec(extractText(r.lengthText));
                const thumbs = (r.thumbnail && r.thumbnail.thumbnails) || (r.thumbnails && r.thumbnails[0] && r.thumbnails[0].thumbnails);
                const cover = (Array.isArray(thumbs) && thumbs.length > 0) ? thumbs[thumbs.length - 1].url : '';
                tracks.push({ videoId, title, artist, duration, cover });
              }
            }
            if (Array.isArray(node)) {
              for (const item of node) walk(item, visited);
            } else {
              for (const k of Object.keys(node)) walk(node[k], visited);
            }
          }
          walk(root);
          return tracks;
        }

        function extractPlaylistsGeneric(root) {
          const playlists = [];
          const seenIds = new Set();
          function walk(node, visited = new Set()) {
            if (!node || typeof node !== 'object') return;
            if (visited.has(node)) return;
            visited.add(node);
            if (node.musicTwoRowItemRenderer) {
              const r = node.musicTwoRowItemRenderer;
              const title = extractText(r.title);
              let browseId = r.navigationEndpoint && r.navigationEndpoint.browseEndpoint && r.navigationEndpoint.browseEndpoint.browseId;
              if (!browseId) {
                const findBrowse = (obj) => {
                  if (!obj || typeof obj !== 'object') return null;
                  if (obj.browseEndpoint && typeof obj.browseEndpoint.browseId === 'string') return obj.browseEndpoint.browseId;
                  for (const k of Object.keys(obj)) {
                    const b = findBrowse(obj[k]);
                    if (b) return b;
                  }
                  return null;
                };
                browseId = findBrowse(r);
              }
              if (browseId && !seenIds.has(browseId) && (browseId.startsWith('VL') || browseId.startsWith('PL') || browseId.startsWith('MPREb_') || browseId.includes('playlist') || browseId.startsWith('RD'))) {
                seenIds.add(browseId);
                const thumbs = r.thumbnailRenderer && r.thumbnailRenderer.musicThumbnailRenderer && r.thumbnailRenderer.musicThumbnailRenderer.thumbnail && r.thumbnailRenderer.musicThumbnailRenderer.thumbnail.thumbnails;
                const cover = (Array.isArray(thumbs) && thumbs.length > 0) ? thumbs[thumbs.length - 1].url : '';
                playlists.push({ id: browseId, name: title || 'Playlist', cover });
              }
            } else if (node.gridPlaylistRenderer || node.playlistRenderer || node.compactPlaylistRenderer) {
              const r = node.gridPlaylistRenderer || node.playlistRenderer || node.compactPlaylistRenderer;
              const id = r.playlistId || (r.navigationEndpoint && r.navigationEndpoint.browseEndpoint && r.navigationEndpoint.browseEndpoint.browseId);
              const title = extractText(r.title);
              if (id && !seenIds.has(id)) {
                seenIds.add(id);
                const thumbs = (r.thumbnail && r.thumbnail.thumbnails) || (r.thumbnails && r.thumbnails[0] && r.thumbnails[0].thumbnails);
                const cover = (Array.isArray(thumbs) && thumbs.length > 0) ? thumbs[thumbs.length - 1].url : '';
                playlists.push({ id, name: title || 'Playlist', cover });
              }
            }
            if (Array.isArray(node)) {
              for (const item of node) walk(item, visited);
            } else {
              for (const k of Object.keys(node)) walk(node[k], visited);
            }
          }
          walk(root);
          return playlists;
        }

        async function fetchInnertube(browseId, continuationToken = null) {
          const ts = Math.floor(Date.now() / 1000);
          const hash = crypto.createHash('sha1').update(`${ts} ${sapisid} ${origin}`).digest('hex');
          const headers = {
            'Authorization': `SAPISIDHASH ${ts}_${hash}`,
            'Content-Type': 'application/json',
            'Origin': origin,
            'Referer': `${origin}/`,
            'X-Origin': origin,
            'X-Goog-AuthUser': '0'
          };
          const body = {
            context: {
              client: {
                clientName,
                clientVersion,
                hl: 'en'
              }
            }
          };
          let targetUrl = browseUrl;
          if (continuationToken) {
            body.continuation = continuationToken;
            targetUrl = `${browseUrl}&continuation=${encodeURIComponent(continuationToken)}`;
          } else if (browseId) {
            body.browseId = browseId;
          }
          const res = await s.fetch(targetUrl, {
            method: 'POST',
            headers,
            body: JSON.stringify(body)
          });
          if (!res.ok) {
            throw new Error(`Innertube request failed: ${res.status}`);
          }
          return await res.json();
        }

        async function fetchAllTracks(browseId) {
          let initial = await fetchInnertube(browseId);
          let tracks = extractTracksGeneric(initial);
          let token = findContinuationToken(initial);
          let safety = 0;
          while (token && safety < 30) {
            safety++;
            const contData = await fetchInnertube(null, token);
            const newTracks = extractTracksGeneric(contData);
            if (!newTracks.length) break;
            tracks = tracks.concat(newTracks);
            const nextToken = findContinuationToken(contData);
            if (!nextToken || nextToken === token) break;
            token = nextToken;
          }
          return tracks;
        }

        async function fetchAllPlaylists(browseId) {
          let initial = await fetchInnertube(browseId);
          let playlists = extractPlaylistsGeneric(initial);
          let token = findContinuationToken(initial);
          let safety = 0;
          while (token && safety < 20) {
            safety++;
            const contData = await fetchInnertube(null, token);
            const newPlaylists = extractPlaylistsGeneric(contData);
            if (!newPlaylists.length) break;
            playlists = playlists.concat(newPlaylists);
            const nextToken = findContinuationToken(contData);
            if (!nextToken || nextToken === token) break;
            token = nextToken;
          }
          return playlists;
        }

        try {
          let likes = [];
          if (doLikes) {
            const rawLikes = await fetchAllTracks(likedBrowseId);
            likes = rawLikes.map(t => ({
              title: t.title,
              artist: t.artist,
              duration: t.duration,
              id: t.videoId,
              url: `https://www.youtube.com/watch?v=${t.videoId}`,
              platform,
              thumbnail: t.cover,
              cover: t.cover
            }));
          }

          let playlists = [];
          if (doPlaylists) {
            const rawPlaylists = await fetchAllPlaylists(playlistsBrowseId);
            for (const pl of rawPlaylists) {
              try {
                const plBrowseId = pl.id.startsWith('VL') ? pl.id : `VL${pl.id}`;
                const plTracks = await fetchAllTracks(plBrowseId);
                playlists.push({
                  id: pl.id,
                  name: pl.name,
                  cover: pl.cover,
                  tracks: plTracks.map(t => ({
                    title: t.title,
                    artist: t.artist,
                    duration: t.duration,
                    id: t.videoId,
                    url: `https://www.youtube.com/watch?v=${t.videoId}`,
                    platform,
                    thumbnail: t.cover,
                    cover: t.cover
                  }))
                });
              } catch (e) {
                playlists.push({
                  id: pl.id,
                  name: pl.name,
                  cover: pl.cover,
                  tracks: []
                });
              }
            }
          }

          return { success: true, likes, playlists };
        } catch (err) {
          return { success: false, error: (err && err.message) ? err.message : 'Failed to fetch library' };
        }
      });

      function setupTray() {
        try {
          const iconPath = path.join(__dirname, '../../assets', 'icon.png');
          let trayIcon = nativeImage.createFromPath(iconPath);
          trayIcon = trayIcon.resize({ width: 22, height: 22 });
          tray = new Tray(trayIcon);
          tray.setToolTip('Riff');
          const contextMenu = Menu.buildFromTemplate([
            {
              label: 'Show/Hide',
              click: () => {
                if (!mainWindow || mainWindow.isDestroyed()) return;
                if (mainWindow.isVisible()) mainWindow.hide();
                else { mainWindow.show(); mainWindow.focus(); }
              }
            },
            {
              label: 'Play/Pause',
              click: () => {
                if (mainWindow && !mainWindow.isDestroyed()) {
                  mainWindow.webContents.send('media-command', 'toggle');
                }
              }
            },
            { type: 'separator' },
            {
              label: 'Quit',
              click: () => {
                isQuitting = true;
                app.quit();
              }
            }
          ]);
          tray.setContextMenu(contextMenu);
          tray.on('click', () => {
            if (!mainWindow || mainWindow.isDestroyed()) return;
            if (mainWindow.isVisible()) mainWindow.hide();
            else { mainWindow.show(); mainWindow.focus(); }
          });
        } catch (err) {
          console.warn('Tray creation failed:', err);
          tray = null;
        }
      }

      app.on('before-quit', () => {
        isQuitting = true;
      });

      app.whenReady().then(() => {
        if (!fs.existsSync(YT_LOGOUT_FLAG)) exportYoutubeCookiesToDisk().catch(() => {});
        global.riffSessionFetch = (urlStr, opts) => session.defaultSession.fetch(urlStr, opts);
        if (process.platform === 'darwin' && app.dock) {
          try {
            const dockIcon = nativeImage.createFromPath(path.join(__dirname, '../../assets', 'icon.png'));
            if (!dockIcon.isEmpty()) app.dock.setIcon(dockIcon);
          } catch (_) {}
        }
        setupTray();
        try { require('./launcher').refreshLauncherOnStartup?.(); } catch (_) {}
        try {
          session.defaultSession.webRequest.onBeforeSendHeaders(
            { urls: ['*://*.googlevideo.com/*'] },
            (details, callback) => {
              const h = details.requestHeaders || {};
              try {
                const u = new URL(details.url);
                const c = (u.searchParams.get('c') || '').toUpperCase();
                if (c === 'ANDROID_VR') {
                  h['User-Agent'] = 'com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip';
                } else if (c === 'IOS') {
                  h['User-Agent'] = 'com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)';
                } else if (c === 'ANDROID_MUSIC') {
                  h['User-Agent'] = 'com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14; en_US; sdk_gphone64_x86_64 Build/UE1A.230829.036) gzip';
                } else if (c === 'TVHTML5_SIMPLY_EMBEDDED_PLAYER' || c.startsWith('TVHTML5')) {
                  h['User-Agent'] = 'Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) 85.0.4183.93/6.5 TV Safari/537.36';
                } else if (c === 'ANDROID_TESTSUITE') {
                  h['User-Agent'] = 'com.google.android.youtube/1.9 (Linux; U; Android 11; en_US) gzip';
                } else if (c.startsWith('ANDROID')) {
                  h['User-Agent'] = 'com.google.android.youtube/19.44.38 (Linux; U; Android 14; en_US; sdk_gphone64_x86_64 Build/UE1A.230829.036) gzip';
                }
                delete h['Origin'];
                delete h['origin'];
                delete h['Referer'];
                delete h['referer'];
              } catch (_) {}
              callback({ requestHeaders: h });
            }
          );
        } catch (_) {}
        try {
          session.defaultSession.webRequest.onHeadersReceived(
            {
              urls: [
                '*://*.googlevideo.com/*',
                '*://*.ytimg.com/*',
                '*://*.ggpht.com/*',
                '*://*.googleusercontent.com/*',
                '*://*.scdn.co/*',
                '*://*.spotifycdn.com/*',
                '*://*.mzstatic.com/*',
                '*://*.sndcdn.com/*',
                '*://*.dzcdn.net/*',
                '*://*.yandex.net/*'
              ]
            },
            (details, callback) => {
              const rh = Object.assign({}, details.responseHeaders || {});
              delete rh['access-control-allow-origin'];
              delete rh['Access-Control-Allow-Origin'];
              rh['Access-Control-Allow-Origin'] = ['*'];
              rh['Access-Control-Allow-Headers'] = ['*'];
              rh['Access-Control-Expose-Headers'] = ['Content-Length, Content-Range, Accept-Ranges'];
              callback({ responseHeaders: rh });
            }
          );
        } catch (_) {}
        createWindow();
        const GLOBAL_SHORTCUTS = {
          'CommandOrControl+Shift+Space': 'play-pause',
          'CommandOrControl+Alt+Shift+Right': 'next',
          'CommandOrControl+Alt+Shift+Left': 'prev',
          'CommandOrControl+Alt+Shift+Up': 'volume-up',
          'CommandOrControl+Alt+Shift+Down': 'volume-down'
        };
        for (const [accel, action] of Object.entries(GLOBAL_SHORTCUTS)) {
          try {
            globalShortcut.register(accel, () => {
              if (mainWindow && !mainWindow.isDestroyed()) mainWindow.webContents.send('media-control', action);
            });
          } catch (e) {}
        }
        if (tutorialDone()) require('./updater').initUpdater(() => mainWindow);
        app.on('activate', () => {
          if (mainWindow && !mainWindow.isDestroyed()) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.show();
            mainWindow.focus();
          } else {
            createWindow();
          }
        });
      });

      app.on('window-all-closed', () => {
        discordRPC.destroyRPC();
        if (process.platform !== 'darwin') app.quit();
      });
}
