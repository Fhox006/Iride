@echo off
title Iride - Sync in corso...

:SYNC
cd /d "C:\Users\mrffh\Desktop\Iride"
cls
echo.
echo  ================================
echo   IRIDE - Sincronizzazione Git
echo  ================================
echo.
echo  Sincronizzazione in corso...
echo.
git pull --rebase origin main
git add .
git commit -m "."
git push origin main
echo.
echo  ==============================
echo   FATTO! Codice aggiornato.
echo  ==============================
echo.
echo  Premi INVIO per sincronizzare di nuovo.
echo  Premi qualsiasi altro tasto per chiudere.
echo.

:: Usa PowerShell per leggere un singolo tasto
for /f "delims=" %%K in ('powershell -NoProfile -Command "$k = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown'); $k.VirtualKeyCode"') do set KEYCODE=%%K

:: INVIO = VirtualKeyCode 13
if "%KEYCODE%"=="13" goto SYNC

exit