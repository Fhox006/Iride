@echo off
title Iride - Sync in corso...
cd /d "C:\Users\mrffh\Desktop\Iride"

echo.
echo Sincronizzazione in corso...
echo.

git pull --rebase origin main
git add .
git commit -m "."
git push origin main

echo.
echo ==============================
echo   FATTO! Codice aggiornato.
echo ==============================
echo.
pause