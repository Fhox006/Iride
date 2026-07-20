#!/usr/bin/env node
// Nudges an automatic impeccable design pass after Compose UI files are edited.
// Impeccable's own detector is HTML/CSS-shaped and skips native (android) platforms,
// so it never fires on Kotlin. This hook fills that gap for this Compose codebase.

let data = '';
process.stdin.on('data', (c) => { data += c; });
process.stdin.on('end', () => {
  try {
    const evt = JSON.parse(data || '{}');
    const filePath = evt?.tool_input?.file_path || evt?.tool_input?.path || '';
    const isComposeUiFile = /[\\/]ui[\\/].*\.kt$/i.test(filePath);
    if (isComposeUiFile) {
      process.stdout.write(JSON.stringify({
        hookSpecificOutput: {
          hookEventName: 'PostToolUse',
          additionalContext: `Compose UI file just edited: ${filePath}. Invoke the impeccable skill (audit/critique) on this file before finishing the turn — check spacing, visual hierarchy, accessibility, and Material 3 conventions.`,
        },
      }));
    }
  } catch {
    // never break the turn
  }
  process.exit(0);
});
