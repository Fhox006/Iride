@'
while ($true) {
    Set-Location "C:\Users\mrffh\Desktop\Iride"
    git pull --rebase origin main | Out-Null
    $status = git status --porcelain
    if ($status) {
        git add .
        git commit -m "."
        git push origin main
        Write-Host "SINCRONIZZATO - $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Green
    } else {
        Write-Host "Nessuna modifica - $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Gray
    }
    Start-Sleep -Seconds 60
}
'@ | Set-Content -Path "C:\Users\mrffh\Desktop\Iride\sync.ps1" -Encoding UTF8