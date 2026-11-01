// Ad-hoc signs the macOS app after packaging. Releases have no Developer ID
// (`mac.identity: null`), and electron-builder then leaves Electron's
// signature invalid once it rewrites Info.plist, which Apple Silicon reports
// as "Riff is damaged". An ad-hoc signature turns that into the regular
// "unidentified developer" prompt that users can approve.
const { execFileSync } = require('child_process');
const path = require('path');

exports.default = async function adhocSign(context) {
  if (context.electronPlatformName !== 'darwin') return;
  const appPath = path.join(context.appOutDir, `${context.packager.appInfo.productFilename}.app`);
  execFileSync('codesign', ['--force', '--deep', '--sign', '-', appPath], { stdio: 'inherit' });
  execFileSync('codesign', ['--verify', '--deep', '--strict', appPath], { stdio: 'inherit' });
};
