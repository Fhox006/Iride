#!/usr/bin/env bash
set -Eeuo pipefail

package=com.iride.music.debug
component="$package/com.metrolist.music.MainActivity"
mkdir -p artifacts/diagnostics
trap 'adb logcat -d > artifacts/diagnostics/logcat.txt 2>&1 || true' EXIT

mapfile -t apks < <(find app/build/outputs/apk/foss/debug -maxdepth 1 -name '*.apk' -type f)
if [[ ${#apks[@]} -ne 1 ]]; then
  echo "Expected one FOSS debug APK, found ${#apks[@]}" >&2
  exit 1
fi

adb wait-for-device
adb install -r "${apks[0]}"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb logcat -c
adb shell am start -W -n "$component" | tee artifacts/diagnostics/launch.txt
if ! grep -q 'Status: ok' artifacts/diagnostics/launch.txt; then
  echo 'Activity launch failed' >&2
  exit 1
fi

# Allow Compose and first-launch initialization to settle. Preserve onboarding:
# this is the real initial screen of a fresh installation, without seeded data.
sleep 20
adb shell pidof "$package" > artifacts/diagnostics/pid.txt
adb shell dumpsys activity activities > artifacts/diagnostics/activities.txt
if ! grep -E '(mResumedActivity|topResumedActivity).*com\.iride\.music\.debug/com\.metrolist\.music\.MainActivity([ }]|$)' artifacts/diagnostics/activities.txt; then
  echo 'Iride MainActivity is not in the foreground; refusing a misleading screenshot' >&2
  exit 1
fi
adb shell uiautomator dump /sdcard/iride-window.xml
adb pull /sdcard/iride-window.xml artifacts/diagnostics/window.xml
adb exec-out screencap -p > artifacts/iride-initial.png
python3 - <<'PY'
import pathlib
import struct
import zlib

data = pathlib.Path('artifacts/iride-initial.png').read_bytes()
assert data[:8] == b'\x89PNG\r\n\x1a\n', 'Invalid PNG signature'
offset = 8
pixels = bytearray()
while offset < len(data):
    length = struct.unpack_from('>I', data, offset)[0]
    kind = data[offset + 4:offset + 8]
    payload = data[offset + 8:offset + 8 + length]
    crc = struct.unpack_from('>I', data, offset + 8 + length)[0]
    assert zlib.crc32(kind + payload) & 0xffffffff == crc, 'Invalid PNG CRC'
    if kind == b'IHDR':
        width, height = struct.unpack_from('>II', payload)
        assert width > 0 and height > 0
    if kind == b'IDAT':
        pixels.extend(payload)
    offset += length + 12
    if kind == b'IEND':
        break
assert pixels and zlib.decompress(pixels), 'Missing PNG pixel data'
assert kind == b'IEND' and offset == len(data), 'Incomplete PNG'
print(f'Validated real emulator screenshot: {width}x{height}')
PY
