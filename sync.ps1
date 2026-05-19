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
echo Premi INVIO per sincronizzare di nuovo...
echo Premi un altro tasto per uscire.
echo.

choice /c:YN /n /m "Premi INVIO per continuare o un altro tasto per uscire"
if errorlevel 2 goto end
goto loop

:end
echo.
echo Uscita...
'''

with open('/tmp/SyncOra.bat', 'w', encoding='ascii') as f:
    f.write(content)

print("Creato! (Versione con loop su INVIO)")