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
echo Digita una lettera qualsiasi + INVIO per uscire.
echo.

set "input="
set /p "input=> "

if not defined input (
    goto loop
) else (
    echo.
    echo Uscita...
)
'''

with open('/tmp/SyncOra.bat', 'w', encoding='ascii') as f:
    f.write(content)

print("Creato! (Versione 2 - più affidabile)")