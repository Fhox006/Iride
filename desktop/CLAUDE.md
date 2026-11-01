# Iride Desktop: istruzioni per Claude

App Electron (Windows/Linux/macOS) con UI in `src/renderer/`. Lavora solo in `desktop/` salvo richiesta esplicita.

## Stile di risposta
- Italiano, brevissimo (stile caveman): frammenti, niente riempitivi, niente riassunti finali.
- Una sola foto/screenshot quando richiesta. Nessun testo extra attorno.

## Git
- Branch di lavoro: `desktop`. Niente PR se non richiesta esplicitamente.
- Prima di modificare: `git fetch origin && git rebase origin/desktop`.
- Commit e push solo su quel branch: `git push -u origin desktop`.
- Mai `force push`, mai `reset --hard` su commit non tuoi.

## Struttura
- `src/main/main.js`: processo principale, finestra, IPC. Flag debug: `RIFF_DEBUG=1`, flag Chromium extra: `RIFF_FLAGS="a;b=c"`.
- `src/shared/platform.js`: percorsi dati, marker del tutorial (`~/.rifftutorial` con versione app).
- `src/renderer/index.html`: ordine di caricamento CSS/JS. I CSS sovrascrivono in quest'ordine: `styles.css` → `iride-liner.css` → `iride-folio.css` → `iride-folio-surface.css`. Le regole di `iride-folio-surface.css` sono le più recenti.
- `start-iride.bat`: launcher Windows, aggiorna da GitHub a ogni avvio. Riga `set "BRANCH=..."` = branch sopra.

## Comandi
- Dipendenze: `npm install` (in `desktop/`).
- Avvio: `npm start`.
- Test: `npm test` (runner `node --test`, file in `test/`).
- Verifica UI in Linux senza display: `xvfb-run -a node_modules/.bin/electron --no-sandbox .` con `RIFF_FLAGS="remote-debugging-port=9222"`, poi screenshot via Playwright `connectOverCDP`. Imposta `HOME` su una cartella temporanea con file `.rifftutorial` (contenuto = versione in `package.json`) per saltare il welcome.

## Regole di modifica
- Se cambi branch di lavoro, fai merge o cancelli il branch: aggiorna `BRANCH` in `desktop/start-iride.bat` nello stesso commit, altrimenti il launcher sull'utente PC non prende più l'ultima versione.
- Ogni modifica a `start-iride.bat` va scritta con fine riga CRLF (cmd non gestisce bene LF).
- Il codice Android (`app/`) non va toccato da questo progetto: il riferimento è solo visivo/geometrico.
- Dopo modifiche al CSS/JS, verifica con uno screenshot prima di committare.
- Non introdurre dipendenze nuove senza richiesta.
