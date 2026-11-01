const { app, ipcMain, shell } = require('electron');
const fs = require('fs');
const path = require('path');

const CHECK_DELAY_MS = 1500;
const CHECK_EVERY_MS = 6 * 60 * 60 * 1000;

function readIgnored(file) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')).version || ''; } catch (e) { return ''; }
}

function writeIgnored(file, version) {
  try { fs.writeFileSync(file, JSON.stringify({ version })); } catch (e) {}
}

function isNewer(latest, current) {
  const p1 = String(latest || '').split('.').map(n => parseInt(n, 10) || 0);
  const p2 = String(current || '').split('.').map(n => parseInt(n, 10) || 0);
  for (let i = 0; i < Math.max(p1.length, p2.length); i++) {
    const a = p1[i] || 0;
    const b = p2[i] || 0;
    if (a > b) return true;
    if (a < b) return false;
  }
  return false;
}

function initUpdater(getWindow) {
  const ignoreFile = path.join(app.getPath('userData'), 'ignored-update.json');
  const pkg = require('../../package.json');
  if (pkg.version) {
    app.setVersion(pkg.version);
  }
  const releasesUrl = `${pkg.homepage || 'https://github.com/rootscripts/riff'}/releases`;
  let manualCheck = false;
  let installNow = false;
  let current = null;

  try { ipcMain.removeHandler('get-app-version'); } catch (_) {}
  ipcMain.handle('get-app-version', () => app.getVersion());

  const isDev = !app.isPackaged;
  const devConfigFile = path.join(__dirname, '../../dev-app-update.yml');
  const canInstall = !isDev && (process.platform === 'win32' || (process.platform === 'linux' && !!process.env.APPIMAGE));

  const send = (channel, payload) => {
    const win = getWindow();
    if (win && !win.isDestroyed()) win.webContents.send(channel, payload);
  };

  const checkViaGithubApi = async () => {
    try {
      const res = await fetch('https://api.github.com/repos/rootscripts/riff/releases/latest', {
        headers: { 'User-Agent': 'Riff-Player' }
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const json = await res.json();
      const latestVer = (json.tag_name || '').replace(/^v/i, '');
      const currentVersion = pkg.version || app.getVersion();
      if (isNewer(latestVer, currentVersion)) {
        current = { version: latestVer, releaseNotes: json.body || '', publishedAt: json.published_at || '', htmlUrl: json.html_url || '' };
        const ignored = readIgnored(ignoreFile) === latestVer;
        if (ignored && !manualCheck) return;
        manualCheck = false;
        send('update-available', {
          version: latestVer,
          releaseNotes: json.body || '',
          publishedAt: json.published_at || '',
          htmlUrl: json.html_url || '',
          canInstall: false,
          url: json.html_url || `${releasesUrl}/tag/v${latestVer}`
        });
      } else {
        if (manualCheck) {
          send('update-not-available', { version: currentVersion });
        }
        manualCheck = false;
      }
    } catch (err) {
      if (manualCheck) {
        send('update-error', { message: err && err.message ? err.message : 'Update failed' });
      }
      manualCheck = false;
    }
  };
  const fallbackFetchGitHub = checkViaGithubApi;

  let autoUpdater = null;
  const hasUpdaterSupport = !isDev || fs.existsSync(devConfigFile);
  if (hasUpdaterSupport && canInstall) {
    try {
      const eu = require('electron-updater');
      autoUpdater = eu.autoUpdater;
      if (isDev) autoUpdater.forceDevUpdateConfig = true;
      autoUpdater.autoDownload = false;
      autoUpdater.autoInstallOnAppQuit = false;

      let isDownloaded = false;

      autoUpdater.on('update-available', async (info) => {
        current = info;
        const ignored = readIgnored(ignoreFile) === info.version;
        if (ignored && !manualCheck) return;
        manualCheck = false;

        let releaseNotes = info.releaseNotes || info.releaseName || '';
        let publishedAt = info.releaseDate || '';
        let htmlUrl = `${releasesUrl}/tag/v${info.version}`;

        try {
          const res = await fetch('https://api.github.com/repos/rootscripts/riff/releases/latest', {
            headers: { 'User-Agent': 'Riff-Player' }
          });
          if (res.ok) {
            const json = await res.json();
            if (json.body) releaseNotes = json.body;
            if (json.published_at) publishedAt = json.published_at;
            if (json.html_url) htmlUrl = json.html_url;
          }
        } catch (_) {}

        current.releaseNotes = releaseNotes;
        current.publishedAt = publishedAt;
        current.htmlUrl = htmlUrl;

        send('update-available', {
          version: info.version,
          canInstall,
          url: `${releasesUrl}/tag/v${info.version}`,
          releaseNotes,
          publishedAt,
          htmlUrl
        });

        autoUpdater.downloadUpdate().catch(() => {});
      });

      autoUpdater.on('update-not-available', () => {
        if (manualCheck) send('update-not-available', { version: app.getVersion() });
        manualCheck = false;
      });

      autoUpdater.on('download-progress', (p) => send('update-progress', { percent: Math.round(p.percent) }));

      autoUpdater.on('update-downloaded', () => {
        isDownloaded = true;
        if (installNow) {
          autoUpdater.quitAndInstall();
        } else {
          send('update-ready', { version: current && current.version });
          send('update-downloaded', {
            version: current && current.version,
            releaseNotes: current && (current.releaseNotes || current.body || ''),
            publishedAt: current && (current.publishedAt || current.published_at || ''),
            htmlUrl: current && (current.htmlUrl || current.html_url || ''),
            canInstall: true
          });
        }
      });

      autoUpdater.on('error', () => {
        fallbackFetchGitHub();
      });
    } catch (_) {
      autoUpdater = null;
    }
  }

  const check = () => {
    if (!autoUpdater || !canInstall || isDev) {
      return fallbackFetchGitHub();
    }
    return autoUpdater.checkForUpdates().catch(() => {
      return fallbackFetchGitHub();
    });
  };

  try { ipcMain.removeHandler('update-check'); } catch (_) {}
  ipcMain.handle('update-check', () => {
    manualCheck = true;
    check();
    return { ok: true };
  });

  const doInstallNow = () => {
    installNow = true;
    if (autoUpdater && canInstall) {
      try {
        autoUpdater.quitAndInstall();
      } catch (_) {
        autoUpdater.downloadUpdate().then(() => {
          autoUpdater.quitAndInstall();
        }).catch(() => {});
      }
    } else {
      shell.openExternal((current && current.htmlUrl) || releasesUrl);
    }
  };

  try { ipcMain.removeHandler('install-update-now'); } catch (_) {}
  ipcMain.on('install-update-now', doInstallNow);

  try { ipcMain.removeAllListeners('update-action'); } catch (_) {}
  ipcMain.on('update-action', (event, action) => {
    if (action === 'ignore') {
      if (current) writeIgnored(ignoreFile, current.version);
      return;
    }
    if (!canInstall || !autoUpdater) return;
    if (action === 'now') {
      doInstallNow();
    } else if (action === 'later') {
      installNow = false;
      autoUpdater.autoInstallOnAppQuit = true;
      autoUpdater.downloadUpdate().catch(() => {});
    }
  });

  setTimeout(check, CHECK_DELAY_MS);
  setInterval(check, CHECK_EVERY_MS).unref();
}

module.exports = { initUpdater };
