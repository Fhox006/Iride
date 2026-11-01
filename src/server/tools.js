const fs = require('fs');
const os = require('os');
const path = require('path');
const https = require('https');
const crypto = require('crypto');
const { spawn } = require('child_process');
const { IS_WIN, BIN_DIR, resolveBinary, clearBinaryCache } = require('../shared/platform');
const logger = require('../shared/logger');

const state = { status: 'idle', tool: '', percent: 0, error: '', ffmpegError: '' };
const STAMP = path.join(BIN_DIR, '.ytdlp-update-stamp');

function ytDlpAsset() {
  const a = process.arch;
  if (IS_WIN) return a === 'arm64' ? 'yt-dlp_arm64.exe' : a === 'ia32' ? 'yt-dlp_x86.exe' : 'yt-dlp.exe';
  if (process.platform === 'darwin') return 'yt-dlp_macos';
  return a === 'arm64' ? 'yt-dlp_linux_aarch64' : 'yt-dlp_linux';
}

function ffmpegAsset() {
  const a = process.arch === 'arm64' ? 'arm64' : '64';
  if (IS_WIN) return { name: `ffmpeg-master-latest-win${a}-gpl.zip` };
  if (process.platform === 'linux') return { name: `ffmpeg-master-latest-linux${a}-gpl.tar.xz` };
  return null;
}

function get(url, redirects = 6, extraHeaders = {}) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: Object.assign({ 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Riff/43.1' }, extraHeaders),
      timeout: 25000
    }, (res) => {
      if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location && redirects > 0) {
        res.resume();
        return resolve(get(new URL(res.headers.location, url).toString(), redirects - 1, extraHeaders));
      }
      if (res.statusCode !== 200 && res.statusCode !== 206) {
        res.resume();
        return reject(new Error(`HTTP ${res.statusCode} for ${url}`));
      }
      resolve(res);
    });
    req.on('timeout', () => { req.destroy(new Error('Request timeout')); });
    req.on('error', reject);
  });
}

async function getText(url) {
  const res = await get(url);
  let out = '';
  res.setEncoding('utf8');
  for await (const chunk of res) out += chunk;
  return out;
}

async function downloadFile(url, dest) {
  try { fs.rmSync(dest, { force: true }); } catch (_) {}
  let lastErr = null;
  for (let attempt = 0; attempt < 5; attempt++) {
    let existingBytes = 0;
    try {
      if (fs.existsSync(dest)) existingBytes = fs.statSync(dest).size;
    } catch (_) {}
    try {
      const headers = existingBytes > 0 ? { Range: `bytes=${existingBytes}-` } : {};
      const res = await get(url, 6, headers);
      const isPartial = res.statusCode === 206;
      if (!isPartial && existingBytes > 0) {
        existingBytes = 0;
        try { fs.rmSync(dest, { force: true }); } catch (_) {}
      }
      const contentLen = Number(res.headers['content-length']) || 0;
      const total = contentLen ? (existingBytes + contentLen) : 0;
      let done = existingBytes;
      await new Promise((resolve, reject) => {
        const out = fs.createWriteStream(dest, { flags: isPartial ? 'a' : 'w' });
        res.on('data', (c) => {
          done += c.length;
          if (total) state.percent = Math.min(99, Math.round((done / total) * 100));
        });
        res.on('error', reject);
        out.on('error', reject);
        out.on('finish', resolve);
        res.pipe(out);
      });
      state.percent = 100;
      const fileBuf = await fs.promises.readFile(dest);
      return crypto.createHash('sha256').update(fileBuf).digest('hex');
    } catch (err) {
      lastErr = err;
      await new Promise(r => setTimeout(r, 1200 * (attempt + 1)));
    }
  }
  try {
    const curlBin = IS_WIN ? 'curl.exe' : 'curl';
    await run(curlBin, ['-L', '--fail', '--retry', '5', '--retry-delay', '2', '-C', '-', '-A', 'Mozilla/5.0', '-o', dest, url], 600000);
    state.percent = 100;
    const fileBuf = await fs.promises.readFile(dest);
    return crypto.createHash('sha256').update(fileBuf).digest('hex');
  } catch (_) {
    throw lastErr || new Error('Download failed');
  }
}

function expectedHash(sums, fileName) {
  for (const line of sums.split(/\r?\n/)) {
    const m = line.trim().match(/^([a-f0-9]{64})\s+\*?(?:\.\/)?(.+)$/i);
    if (m && m[2].trim() === fileName) return m[1].toLowerCase();
  }
  return null;
}

function run(cmd, args, timeoutMs = 600000) {
  return new Promise((resolve, reject) => {
    const p = spawn(cmd, args, { windowsHide: true });
    let err = '';
    const t = setTimeout(() => { p.kill(); reject(new Error(`${cmd} timed out`)); }, timeoutMs);
    p.stderr.on('data', (d) => { err += d; });
    p.on('error', (e) => {
      clearTimeout(t);
      try { logger.error(`${cmd} spawn error`, e); } catch (_) {}
      reject(e);
    });
    p.on('close', (code) => {
      clearTimeout(t);
      if (code === 0) {
        resolve();
      } else {
        const lastStderr = (err || '').split('\n').slice(-20).join('\n');
        try { logger.error(`${cmd} exited with ${code}`, new Error(lastStderr)); } catch (_) {}
        reject(new Error(err || `${cmd} exited with ${code}`));
      }
    });
  });
}

function findFile(dir, fileName) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      const hit = findFile(full, fileName);
      if (hit) return hit;
    } else if (entry.name === fileName) return full;
  }
  return null;
}

async function installYtDlp() {
  const asset = ytDlpAsset();
  const base = 'https://github.com/yt-dlp/yt-dlp/releases/latest/download';
  const sums = await getText(`${base}/SHA2-256SUMS`);
  const want = expectedHash(sums, asset);
  if (!want) throw new Error(`No checksum published for ${asset}`);
  const target = path.join(BIN_DIR, IS_WIN ? 'yt-dlp.exe' : 'yt-dlp');
  const tmp = `${target}.download`;
  const got = await downloadFile(`${base}/${asset}`, tmp);
  if (got !== want) { fs.rmSync(tmp, { force: true }); throw new Error('yt-dlp checksum mismatch'); }
  if (!IS_WIN) fs.chmodSync(tmp, 0o755);
  fs.renameSync(tmp, target);
}

async function installFfmpeg() {
  const asset = ffmpegAsset();
  if (process.platform === 'darwin' && !asset) {
    await installFfmpegMac();
    return;
  }
  if (!asset) throw new Error('No ffmpeg build for this system. Install ffmpeg with your package manager (for macOS: brew install ffmpeg).');
  const base = 'https://github.com/yt-dlp/FFmpeg-Builds/releases/latest/download';
  const sums = await getText(`${base}/checksums.sha256`);
  const want = expectedHash(sums, asset.name);
  if (!want) throw new Error(`No checksum published for ${asset.name}`);
  const archive = path.join(os.tmpdir(), `riff-${asset.name}`);
  const got = await downloadFile(`${base}/${asset.name}`, archive);
  if (got !== want) { fs.rmSync(archive, { force: true }); throw new Error('ffmpeg checksum mismatch'); }
  const work = fs.mkdtempSync(path.join(os.tmpdir(), 'riff-ffmpeg-'));
  try {
    if (IS_WIN) {
      try {
        await run('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-Command', `[System.IO.Compression.ZipFile]::ExtractToDirectory('${archive.replace(/'/g, "''")}', '${work.replace(/'/g, "''")}')`]);
      } catch (_) {
        try {
          await run('powershell.exe', ['-NoProfile', '-NonInteractive', '-Command', `Expand-Archive -LiteralPath '${archive.replace(/'/g, "''")}' -DestinationPath '${work.replace(/'/g, "''")}' -Force`]);
        } catch (_) {
          const sys32Tar = 'C:\\Windows\\System32\\tar.exe';
          const tarCmd = fs.existsSync(sys32Tar) ? sys32Tar : 'tar.exe';
          await new Promise((resolve, reject) => {
            const p = spawn(tarCmd, ['-xf', path.basename(archive), '-C', work], { cwd: path.dirname(archive), windowsHide: true });
            p.on('error', (err) => reject(new Error(`Failed to extract archive: ${err.message}`)));
            p.on('close', code => code === 0 ? resolve() : reject(new Error(`tar exited with ${code}`)));
          });
        }
      }
    } else {
      await run('tar', ['-xf', archive, '-C', work]);
    }
    for (const name of ['ffmpeg', 'ffprobe']) {
      const exe = IS_WIN ? `${name}.exe` : name;
      const src = findFile(work, exe);
      if (!src) throw new Error(`${exe} not found inside the archive`);
      const dest = path.join(BIN_DIR, exe);
      fs.copyFileSync(src, dest);
      if (!IS_WIN) fs.chmodSync(dest, 0o755);
    }
  } finally {
    fs.rmSync(work, { recursive: true, force: true });
    fs.rmSync(archive, { force: true });
  }
}

async function installFfmpegMac() {
  const failures = [];
  for (const name of ['ffmpeg', 'ffprobe']) {
    const archive = path.join(os.tmpdir(), `riff-${name}-macos.zip`);
    const dest = path.join(BIN_DIR, name);
    try {
      await downloadFile(`https://evermeet.cx/ffmpeg/getrelease/${name}/zip`, archive);
      await run('/usr/bin/unzip', ['-o', archive, '-d', BIN_DIR]);
      fs.chmodSync(dest, 0o755);
      await run(dest, ['-version'], 30000);
    } catch (e) {
      try { fs.rmSync(dest, { force: true }); } catch (_) {}
      failures.push(`${name}: ${e.message}`);
    } finally {
      try { fs.rmSync(archive, { force: true }); } catch (_) {}
    }
  }
  if (failures.length) {
    const hint = process.arch === 'arm64'
      ? ' On Apple Silicon this build needs Rosetta (softwareupdate --install-rosetta), or run: brew install ffmpeg'
      : ' Install it with: brew install ffmpeg';
    throw new Error(`Could not install ffmpeg (${failures.join('; ')}).${hint}`);
  }
}

function toolsStatus() {
  const ytdlp = !!resolveBinary('yt-dlp');
  const ffmpeg = !!resolveBinary('ffmpeg');
  const status = {
    ytDlp: ytdlp,
    ytdlp,
    ffmpeg,
    percent: state.percent,
    step: state.tool ? `downloading ${state.tool}` : '',
    error: state.error,
    progress: Object.assign({}, state)
  };
  status.ffmpegOptional = process.platform === 'darwin';
  status.ffmpegError = state.ffmpegError;
  return status;
}

const checkStatus = toolsStatus;

async function ensureTools() {
  if (state.status === 'downloading') return toolsStatus();
  fs.mkdirSync(BIN_DIR, { recursive: true });
  state.status = 'downloading';
  state.error = '';
  try {
    if (!resolveBinary('yt-dlp')) { state.tool = 'yt-dlp'; state.percent = 0; await installYtDlp(); clearBinaryCache(); }
    if (!resolveBinary('ffmpeg')) {
      state.tool = 'ffmpeg';
      state.percent = 0;
      try {
        await installFfmpeg();
        state.ffmpegError = '';
      } catch (err) {
        if (process.platform !== 'darwin') throw err;
        state.ffmpegError = err.message;
        try { logger.error('ffmpeg install failed', err); } catch (_) {}
      }
      clearBinaryCache();
    }
    state.status = 'done';
  } catch (e) {
    try { logger.error(`Tool download failed: ${state.tool}`, e); } catch (_) {}
    state.status = 'error';
    state.error = e.message;
  }
  return toolsStatus();
}

async function updateYtDlpIfManaged() {
  const bin = resolveBinary('yt-dlp');
  if (!bin || path.dirname(bin) !== BIN_DIR) return false;
  try {
    const last = Number(fs.readFileSync(STAMP, 'utf8')) || 0;
    if (Date.now() - last < 24 * 3600 * 1000) return false;
  } catch (e) {}
  try {
    await run(bin, ['-U'], 120000);
    if (!fs.existsSync(bin)) {
      try { logger.log('warn', 'yt-dlp disappeared after self-update, reinstalling'); } catch (_) {}
      await installYtDlp();
    }
    clearBinaryCache();
    resolveBinary('yt-dlp');
    fs.writeFileSync(STAMP, String(Date.now()));
    return true;
  } catch (e) {
    clearBinaryCache();
    try { logger.error('yt-dlp update failed', e); } catch (_) {}
    return false;
  }
}

module.exports = { ensureTools, toolsStatus, checkStatus, updateYtDlpIfManaged, expectedHash, ytDlpAsset, ffmpegAsset };
