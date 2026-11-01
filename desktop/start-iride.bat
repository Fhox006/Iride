@echo off
setlocal
set "HOME_DIR=C:\Users\mrffh\Desktop\Iride Desktop\iride-desktop"
set "SRC=%HOME_DIR%\.src"
set "URL=https://github.com/Fhox006/Iride.git"
set "BRANCH=ccr-c6b40c55-5eayby"
where git >nul 2>nul || (echo Git non trovato. & pause & exit /b 1)
where node >nul 2>nul || (echo Node.js non trovato. Installalo da https://nodejs.org & pause & exit /b 1)
if not exist "%HOME_DIR%" mkdir "%HOME_DIR%"
if not exist "%SRC%\.git" (
  echo Prima installazione: scarico solo la cartella desktop in "%SRC%" ...
  git clone --depth 1 --filter=blob:none --sparse --branch "%BRANCH%" "%URL%" "%SRC%" || (echo Clone non riuscito. & pause & exit /b 1)
  git -C "%SRC%" sparse-checkout set desktop || (echo sparse-checkout non riuscito. & pause & exit /b 1)
) else (
  echo Aggiornamento da GitHub...
  git -C "%SRC%" fetch --depth 1 origin "%BRANCH%" || (echo Download non riuscito. & pause & exit /b 1)
  git -C "%SRC%" reset --hard FETCH_HEAD || (echo Aggiornamento non riuscito. & pause & exit /b 1)
)
echo Versione in uso:
git -C "%SRC%" log --oneline -1
if not exist "%SRC%\desktop\package.json" (echo desktop\package.json non trovato in "%SRC%": & dir /b "%SRC%" & pause & exit /b 1)
cd /d "%SRC%\desktop"
if not exist node_modules\electron call npm install
if errorlevel 1 (echo npm install non riuscito. & pause & exit /b 1)
echo Avvio Iride Desktop...
call npm start
echo.
echo Uscito con codice %errorlevel%.
pause
