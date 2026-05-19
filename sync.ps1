content = '''@echo off
title Iride - Sync in corso...
cd /d "C:\\Users\\mrffh\\Desktop\\Iride"

:loop
echo.
echo ========================================
echo        Sincronizzazione in corso...
echo ========================================
echo.

git pull --rebase origin main
git add .
git commit -m "Auto-sync %DATE% %TIME%"
git push origin main

echo.
echo ==============================
echo FATTO! Codice aggiornato.
echo ==============================
echo.
echo.
echo Premi [R] per sincronizzare di nuovo
echo Premi un altro tasto per uscire...
echo.

choice /c:RC /n /m ">"
if errorlevel 2 goto end
if errorlevel 1 goto loop

:end
echo.
echo Uscita...
pause
'''

with open('/tmp/SyncOra.bat', 'w', encoding='ascii') as f:
    f.write(content)

print("Creato! (Versione con tasto R)")