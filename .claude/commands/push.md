---
description: Autonomous release — commit, bump version, push tag, let GitHub Actions build/sign/release
---

Autonomo. Utente è via, NON fare domande, MAI. Niente "vuoi che...", "preferisci...", "devo...". Scegli default sensato e procedi fino in fondo, anche per scelte non ovvie (changelog wording, raggruppamento commit, ecc). Solo per azioni distruttive irreversibili (force-push, reset --hard) fermati — tutto il resto di questo comando è già approvato dall'utente invocandolo.

Obiettivo: nuovo tag pushato che fa buildare, firmare e pubblicare la release **a GitHub Actions** (`.github/workflows/release.yml`, trigger su push di tag `v*`). NON buildare/firmare in locale — è lento, blocca la sessione, e il vero flusso già pronto nel repo è CI. Repo: `Fhox006/Iride`.

## Step

1. **Commit lavoro corrente.** `git status` + `git diff` per capire cosa è cambiato (modificati + untracked). Raggruppa in commit sensati per tema (feat/fix/chore/polish), messaggio conciso in inglese, focus sul "why" se non ovvio. Non committare apk/build output/segreti.

2. **Bump versione.** Leggi `versionCode`/`versionName` in `app/build.gradle.kts`. Incrementa `versionCode` di 1, `versionName` alpha successiva (alphaN → alphaN+1).

3. **Changelog.** Prepend sezione in `changelog.md`, stesso formato delle sezioni esistenti (`## <versionName>`, poi `### New features` / `### Improvements` / `### Fixes`). Ricava contenuto da `git log`/diff dei commit dello step 1. Breve, orientato a utente finale. Non perderci troppo tempo.

4. Commit: `chore: bump version to <versionName> (versionCode <N>)`.

5. **Push branch + tag**:
   ```
   git push origin <branch-corrente>
   git tag -a v<versionName> -m "Iride <versionName human-readable>"
   git push origin v<versionName>
   ```
   Il push del tag triggera `release.yml`: build fossRelease, sign con `ANDROID_SIGNING_KEY`/`KEY_ALIAS`/ecc (secrets repo, continuità OTA garantita da CI, non serve replicarla in locale), crea GitHub pre-release con asset `Iride.apk` + changelog estratto automaticamente dalla sezione appena scritta.

6. **Monitora il run** invece di aspettare bloccato:
   - Token: `printf "protocol=https\nhost=github.com\n\n" | git credential fill` → campo password.
   - Trova il run: `GET /repos/Fhox006/Iride/actions/workflows/release.yml/runs?per_page=1` (o filtra per `head_branch=v<versionName>`).
   - Lancialo come **comando bash in background** (poll ogni ~20s su `GET /actions/runs/<id>`, break quando `status=completed`) così non blocchi la sessione — non fare sleep nel foreground.
   - Se `conclusion=failure`: leggi i job/annotations del run (`/actions/runs/<id>/jobs`), diagnosi root cause (es. step "Build FOSS release APK" — vedi fallimento noto alpha22), fixa se possibile e ripush un tag patch (`v<versionName>` non è riusabile: bump ulteriore versionCode/versionName, nuovo tag), altrimenti riporta il problema chiaro all'utente. Non lasciare mai un tag rotto silenzioso.

7. Fine: riporta tag, versionCode/versionName, link release, in 2-3 righe max (stile sessione corrente).

## Guardrail

- Mai buildare/firmare APK in locale per una release pubblica salvo che l'utente lo chieda esplicitamente o CI sia irreparabilmente rotta E l'utente lo sappia — CI è la fonte di verità del certificato di firma ora.
- Se build Kotlin daemon si corrompe (solo se per qualche motivo si builda in locale) → ferma, non ritentare in loop, riassumi stato e stop.
- Se `release.yml` fallisce → NON ripiegare in silenzio su un flusso alternativo che pubblica un apk diverso/non firmato correttamente: capisci perché ha fallito prima di pubblicare qualsiasi cosa.
- Nessuna domanda in nessun punto: se manca un'info, scegli il default più sicuro e procedi, menzionalo in una riga nel riepilogo finale se rilevante.
