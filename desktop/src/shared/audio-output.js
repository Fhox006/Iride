const { exec, execFile } = require('child_process');

function getSystemAudioOutputInfo() {
  return new Promise((resolve) => {
    if (process.platform === 'linux') {
      exec('wpctl inspect @DEFAULT_AUDIO_SINK@ 2>/dev/null', { timeout: 1500 }, (err, stdout) => {
        let name = '';
        let btAddress = '';

        if (!err && stdout) {
          const descMatch = stdout.match(/node\.description\s*=\s*"([^"]+)"/);
          if (descMatch) name = descMatch[1].trim();
          const addrMatch = stdout.match(/api\.bluez5\.address\s*=\s*"([^"]+)"/);
          if (addrMatch) btAddress = addrMatch[1].trim();
        }

        const getPactlFallback = () => new Promise((res) => {
          if (name) return res();
          exec('pactl info 2>/dev/null && pactl list sinks 2>/dev/null', { timeout: 1500 }, (err2, out2) => {
            if (!err2 && out2) {
              const defMatch = out2.match(/Default Sink:\s*([^\r\n]+)/);
              if (defMatch) {
                const defSink = defMatch[1].trim();
                const sinkBlocks = out2.split(/Sink #\d+/);
                const sinkBlock = sinkBlocks.find((b) => b.includes(defSink));
                if (sinkBlock) {
                  const dMatch = sinkBlock.match(/Description:\s*([^\r\n]+)/);
                  if (dMatch) name = dMatch[1].trim();
                }
              }
            }
            res();
          });
        });

        getPactlFallback().then(() => {
          exec('upower -d 2>/dev/null', { timeout: 1500 }, (err3, upowerOut) => {
            let battery = null;
            if (!err3 && upowerOut) {
              const devices = upowerOut.split(/Device:\s*/);
              for (const dev of devices) {
                if (
                  dev.includes('headset') ||
                  (btAddress && dev.includes(btAddress.replace(/:/g, '_'))) ||
                  (name && dev.includes(name))
                ) {
                  const pctMatch = dev.match(/percentage:\s*(\d+)%/);
                  if (pctMatch) {
                    battery = parseInt(pctMatch[1], 10);
                    break;
                  }
                }
              }
            }

            if (battery === null) {
              const cmd = btAddress
                ? `bluetoothctl info ${btAddress} 2>/dev/null`
                : 'bluetoothctl info 2>/dev/null';
              exec(cmd, { timeout: 1500 }, (err4, btOut) => {
                if (!err4 && btOut) {
                  const bMatch = btOut.match(/Battery Percentage:\s*(?:0x[0-9a-fA-F]+\s*)?\(?(\d+)\)?/);
                  if (bMatch) battery = parseInt(bMatch[1], 10);
                  if (!name) {
                    const aMatch = btOut.match(/Alias:\s*([^\r\n]+)/) || btOut.match(/Name:\s*([^\r\n]+)/);
                    if (aMatch) name = aMatch[1].trim();
                  }
                }
                resolve({ name: name || 'Default output', battery });
              });
            } else {
              resolve({ name: name || 'Default output', battery });
            }
          });
        });
      });
    } else if (process.platform === 'darwin') {
      execFile('/usr/sbin/system_profiler', ['SPAudioDataType', '-json'], { timeout: 4000 }, (err, stdout) => {
        let name = '';
        if (!err && stdout) {
          try {
            const groups = JSON.parse(stdout).SPAudioDataType || [];
            for (const group of groups) {
              const device = (group._items || []).find((d) => d.coreaudio_default_audio_output_device === 'spaudio_yes');
              if (device && device._name) { name = String(device._name); break; }
            }
          } catch (e) {}
        }
        resolve({ name: name || 'Default output', battery: null });
      });
    } else {
      resolve({ name: 'Default output', battery: null });
    }
  });
}

module.exports = { getSystemAudioOutputInfo };
