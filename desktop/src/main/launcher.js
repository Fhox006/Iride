const { app, shell } = require('electron');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFile } = require('child_process');

const NAME = 'Riff';
const COMMENT = 'Riff - music player';
const APP_ID = 'io.github.rootscripts.riff';

function winShortcutPath() {
  return path.join(app.getPath('appData'), 'Microsoft', 'Windows', 'Start Menu', 'Programs', `${NAME}.lnk`);
}

function linuxDesktopPath() {
  return path.join(os.homedir(), '.local', 'share', 'applications', 'riff.desktop');
}

function linuxIconPath() {
  return path.join(os.homedir(), '.local', 'share', 'icons', 'hicolor', '512x512', 'apps', 'riff.png');
}

function getLauncherInfo() {
  if (process.platform === 'win32') {
    return { platform: 'win32', canInstall: true, installed: fs.existsSync(winShortcutPath()), reason: '' };
  }
  if (process.platform === 'linux') {
    return { platform: 'linux', canInstall: true, installed: fs.existsSync(linuxDesktopPath()), reason: '' };
  }
  return { platform: process.platform, canInstall: false, installed: false, reason: 'Not supported on this system' };
}

function quoteExec(file) {
  return '"' + file.replace(/(["`$\\])/g, '\\$1').replace(/%/g, '%%') + '"';
}

function installWindows() {
  try {
    const target = process.env.PORTABLE_EXECUTABLE_FILE || process.execPath;
    const args = app.isPackaged ? '' : quoteExec(app.getAppPath());
    const options = {
      target,
      args,
      cwd: path.dirname(target),
      description: COMMENT,
      icon: target,
      iconIndex: 0
    };
    if (APP_ID) options.appUserModelId = APP_ID;

    const startMenuLnk = winShortcutPath();
    fs.mkdirSync(path.dirname(startMenuLnk), { recursive: true });
    shell.writeShortcutLink(startMenuLnk, 'create', options);

    let desktopPath = '';
    try {
      const desktopDir = app.getPath('desktop');
      if (fs.existsSync(desktopDir)) {
        const desktopLnk = path.join(desktopDir, `${NAME}.lnk`);
        shell.writeShortcutLink(desktopLnk, 'create', options);
        desktopPath = desktopLnk;
      }
    } catch (_) {}

    return { ok: true, path: desktopPath || startMenuLnk };
  } catch (e) {
    return { ok: false, error: e.message || String(e) };
  }
}

function installLinux() {
  try {
    let execCmd = '';
    let tryExec = '';

    if (process.env.APPIMAGE) {
      const image = process.env.APPIMAGE;
      try { fs.chmodSync(image, 0o755); } catch (_) {}
      execCmd = `${quoteExec(image)} %U`;
      tryExec = image;

      try {
        const binDir = path.join(os.homedir(), '.local', 'bin');
        fs.mkdirSync(binDir, { recursive: true });
        const symlinkPath = path.join(binDir, 'riff-appimage');
        try { fs.unlinkSync(symlinkPath); } catch (_) {}
        fs.symlinkSync(image, symlinkPath);
      } catch (_) {}
    } else {
      if (app.isPackaged) {
        execCmd = `${quoteExec(process.execPath)} %U`;
        tryExec = process.execPath;
      } else {
        execCmd = `${quoteExec(process.execPath)} ${quoteExec(app.getAppPath())} %U`;
        tryExec = process.execPath;
      }
    }

    const desktop = linuxDesktopPath();
    const iconDest = linuxIconPath();
    let icon = 'audio-x-generic';
    try {
      fs.mkdirSync(path.dirname(iconDest), { recursive: true });
      const assetIcon = path.join(__dirname, '../../assets', 'icon.png');
      if (fs.existsSync(assetIcon)) {
        fs.writeFileSync(iconDest, fs.readFileSync(assetIcon));
        icon = iconDest;
      }
    } catch (e) {}

    const lines = [
      '[Desktop Entry]',
      'Type=Application',
      'Version=1.0',
      `Name=${NAME}`,
      'GenericName=Music Player',
      `Comment=${COMMENT}`,
      `Exec=${execCmd}`,
      `TryExec=${tryExec}`,
      `Icon=${icon}`,
      'Terminal=false',
      'Categories=AudioVideo;Audio;Player;Music;',
      `StartupWMClass=${app.getName()}`,
      'StartupNotify=true',
      ''
    ];
    const content = lines.join('\n');

    fs.mkdirSync(path.dirname(desktop), { recursive: true });
    fs.writeFileSync(desktop, content, { mode: 0o755 });
    try { fs.chmodSync(desktop, 0o755); } catch (_) {}

    try {
      const userDesktop = path.join(os.homedir(), 'Desktop');
      if (fs.existsSync(userDesktop)) {
        const desktopFile = path.join(userDesktop, `${NAME}.desktop`);
        fs.writeFileSync(desktopFile, content, { mode: 0o755 });
        try { fs.chmodSync(desktopFile, 0o755); } catch (_) {}
      }
    } catch (_) {}

    execFile('update-desktop-database', [path.dirname(desktop)], () => {});
    return { ok: true, path: desktop };
  } catch (e) {
    return { ok: false, error: e.message || String(e) };
  }
}

function refreshLauncherOnStartup() {
  if (process.platform === 'linux') {
    if (fs.existsSync(linuxDesktopPath())) {
      try {
        installLinux();
      } catch (e) {
        console.warn('[launcher] Failed to refresh linux launcher:', e.message);
      }
    }
  }
}

function installLauncher() {
  let result;
  try {
    const info = getLauncherInfo();
    if (!info.canInstall) return { ok: false, success: false, error: info.reason || 'Cannot install launcher on this system' };
    if (process.platform === 'win32') result = installWindows();
    else if (process.platform === 'linux') result = installLinux();
    else return { ok: false, success: false, error: 'Unsupported platform: ' + process.platform };
  } catch (e) {
    result = { ok: false, error: e.message || String(e) };
  }
  return { ok: !!result.ok, success: !!result.ok, error: result.error || '', path: result.path || '' };
}

module.exports = { getLauncherInfo, installLauncher, refreshLauncherOnStartup };
