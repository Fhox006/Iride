@echo off
title Iride Sync
:loop
powershell -ExecutionPolicy Bypass -Command "Set-Location 'C:\Users\mrffh\Desktop\Iride'; $status = git status --porcelain; if ($status) { git add .; git commit -m '.'; git push origin main; Write-Host 'SINCRONIZZATO - ' $(Get-Date -Format 'HH:mm:ss') -ForegroundColor Green } else { Write-Host 'Nessuna modifica - ' $(Get-Date -Format 'HH:mm:ss') -ForegroundColor Gray }"
timeout /t 60 /nobreak > nul
goto loop