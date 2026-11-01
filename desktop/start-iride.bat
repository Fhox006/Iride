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
  echo Prima installazione: scarico "%BRANCH%" in "%SRC%" ...
  git clone --depth 1 --sparse --branch "%BRANCH%" "%URL%" "%SRC%" || (echo Clone non riuscito. & pause & exit /b 1)
  git -C "%SRC%" sparse-checkout set desktop || (echo sparse-checkout non riuscito. & pause & exit /b 1)
) else (
  echo Aggiornamento da GitHub...
  git -C "%SRC%" fetch --depth 1 origin "%BRANCH%" || (echo Download non riuscito. & pause & exit /b 1)
  git -C "%SRC%" reset --hard FETCH_HEAD || (echo Aggiornamento non riuscito. & pause & exit /b 1)
)

echo Versione in uso:
git -C "%SRC%" log --oneline -1
if not exist "%SRC%\desktop\package.json" (echo desktop\package.json non trovato in "%SRC%". & pause & exit /b 1)

cd /d "%SRC%\desktop"
rem reinstalla le dipendenze solo se package.json o package-lock.json cambiano
set "STAMP=%SRC%\.deps-stamp"
set "NEED_INSTALL=0"
if not exist node_modules\electron set "NEED_INSTALL=1"
if not exist "%STAMP%" set "NEED_INSTALL=1"
for /f %%H in ('certutil -hashfile package.json SHA256 ^| findstr /v ":"') do set "H1=%%H"
for /f %%H in ('certutil -hashfile package-lock.json SHA256 ^| findstr /v ":"') do set "H2=%%H"
if exist "%STAMP%" (
  findstr /c:"%H1%%H2%" "%STAMP%" >nul || set "NEED_INSTALL=1"
)
if "%NEED_INSTALL%"=="1" (
  echo Installo dipendenze...
  call npm install || (echo npm install non riuscito. & pause & exit /b 1)
  > "%STAMP%" echo %H1%%H2%
)

echo Avvio Iride Desktop...
call npm start
echo.
echo Uscito con codice %errorlevel%.
pause
