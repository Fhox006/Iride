const fs = require('fs');
const path = require('path');
const { DATA_DIR } = require('./platform');

const LOG_DIR = path.join(DATA_DIR, 'logs');
const LOG_PATH = path.join(LOG_DIR, 'riff.log');
const MAX_BYTES = 1024 * 1024;

function rotate() {
  try {
    if (fs.existsSync(LOG_PATH) && fs.statSync(LOG_PATH).size > MAX_BYTES) fs.renameSync(LOG_PATH, `${LOG_PATH}.1`);
  } catch (e) {}
}

function log(level, message) {
  try {
    fs.mkdirSync(LOG_DIR, { recursive: true });
    rotate();
    fs.appendFileSync(LOG_PATH, `${new Date().toISOString()} [${level}] ${String(message).slice(0, 800)}\n`);
  } catch (e) {}
}

function tail(lines) {
  try { return fs.readFileSync(LOG_PATH, 'utf8').split('\n').filter(Boolean).slice(-lines); } catch (e) { return []; }
}

module.exports = { log, tail, LOG_DIR };
