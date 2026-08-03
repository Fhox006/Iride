---
description: Autonomous release — commit, bump version, build fossRelease APK, tag, push, publish GitHub pre-release
---

Autonomo. Utente è via, NON fare domande, NON chiedere conferma. Scegli default sensati e procedi fino in fondo. Segui [[project_release_process]] (memory) come recipe di riferimento, ma verifica sempre lo stato reale del repo prima di agire (non fidarti ciecamente di numeri in memory, potrebbero essere vecchi).

Obiettivo finale: nuovo tag GitHub con pre-release e `Iride.apk` (fossRelease, NON fossDebug) scaricabile.

## Step

1. **Commit lavoro corrente.** `git status` + `git diff` per capire cosa è cambiato (file modificati elencati + eventuali file untracked come nuovi screen/feature). Raggruppa in commit sensati per tipo di modifica (feat/fix/chore), messaggio conciso in inglese, focus sul "why" se non ovvio. Non includere file di build/apk vecchi o segreti.

2. **Bump versione.** Leggi `versionCode`/`versionName` attuali in `app/build.gradle.kts`. Incrementa `versionCode` di 1, `versionName` alpha successiva (es. alpha22 → alpha23). Aggiorna il file.

3. **Changelog.** Prepend nuova sezione in cima a `changelog.md` (stesso formato delle sezioni esistenti: `## <versionName>`, poi `### New features` / `### Improvements` / `### Fixes` a seconda di cosa serve). Ricava il contenuto da `git log` dei commit dall'ultimo tag `v*` a oggi + dal diff committato allo step 1. Breve, orientato a utente finale, non lista tecnica di file. Non perderci troppo tempo — poche righe chiare bastano.

4. Commit version+changelog: `chore: bump version to <versionName> (versionCode <N>)`.

5. **Build fossRelease** (MAI fossDebug — vedi [[bug_debug_apk_shipped_alpha22]]):
   ```
   JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew.bat :app:assembleFossRelease
   ```
   Output atteso: `app/build/outputs/apk/foss/release/app-foss-release.apk`.

6. **Verifica prima di spedire** (obbligatorio, non skippare):
   - `aapt2 dump badging <apk>` → appId deve essere `com.iride.music` (NO `.debug`), label `Iride` (NON "Iride Debug").
   - `apksigner.bat verify --print-certs <apk>` → stesso cert di sempre (continuità OTA per utenti esistenti).
   Se una delle due non torna, NON procedere: build è fossDebug o mis-signed, indaga (probabile signingConfig rotto) e ricostruisci.

7. Copia apk a `iride.apk` in repo root. Commit: `chore: add signed <versionName> release APK (iride.apk)`.

8. Tag annotato `v<versionName>` su quel commit. Push: `git push origin <branch-corrente>` + `git push origin v<versionName>`.

9. **GitHub pre-release** (no `gh` CLI installato, usa REST API + curl):
   - Token: `printf "protocol=https\nhost=github.com\n\n" | git credential fill` → campo password (`gho_...`).
   - POST `https://api.github.com/repos/Fhox006/Iride/releases`: `tag_name=v<versionName>`, `name=Iride <versionName human-readable>`, `body=<sezione changelog appena scritta>`, `prerelease=true`.
   - Upload asset: POST a `https://uploads.github.com/repos/Fhox006/Iride/releases/<id>/assets?name=Iride.apk` (capital I), content-type `application/vnd.android.package-archive`, body = bytes di `iride.apk`.
   - Scrivi json/body temporanei sotto scratchpad dir (non `/tmp`, Windows Git Bash lo rimappa male). `python` (non `python3`) per JSON building se serve.

10. Fine: riporta a utente (in caveman/ponytail style com'è la sessione) tag creato + link release + versionCode/versionName, in 2-3 righe max.

## Guardrail

- Se build Kotlin daemon si corrompe → ferma, non ritentare in loop, riassumi stato e stop (vedi regola sessione).
- Se qualcosa nello step 6 non torna → stop, non pubblicare mai un debug apk come release.
- Non serve chiedere nulla all'utente in nessun punto di questo comando: se manca un'informazione, scegli il default più sicuro (es. changelog minimale ma corretto) e procedi.
