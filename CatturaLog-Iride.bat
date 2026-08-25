@echo off
title Iride - Cattura log
setlocal
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%ADB%" set "ADB=adb"

echo.
echo  ============================================
echo    IRIDE - CATTURA LOG PER SEGNALAZIONE
echo  ============================================
echo.
echo  1. Collega il telefono al PC (USB) o accendi l'emulatore
echo  2. Premi un tasto qui sotto
echo  3. Apri Iride e riproduci il problema (crash / schermata vuota)
echo  4. Torna qui e premi un tasto: il file viene salvato sul Desktop
echo.
pause

set "OUT=%USERPROFILE%\Desktop\log-iride.txt"
echo --- START %date% %time% --- > "%OUT%"
"%ADB%" logcat -v time >> "%OUT%" 2>&1

echo.
echo  REGISTRAZIONE ATTIVA... ora riproduci il problema nell'app.
echo  Quando il crash e' successo (o la schermata e' rimasta vuota),
echo  torna qui e premi un tasto per salvare.
echo.
pause

taskkill /IM adb.exe /F >nul 2>&1
echo --- END %date% %time% --- >> "%OUT%"

echo.
echo  ============================================
echo  FATTO: file salvato sul Desktop
echo  Nome: log-iride.txt
echo  Invia questo file per l'analisi.
echo  ============================================
echo.
pause
