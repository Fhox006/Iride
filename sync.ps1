while ($true) {
    Set-Location "C:\Users\mrffh\Desktop\Iride"
    $status = git status --porcelain
    if ($status) {
        git add .
        git commit -m "."
        git push origin main
        Write-Host "âœ… SINCRONIZZATO - $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Green
    } else {
        Write-Host "â³ Nessuna modifica - $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Gray
    }
    Start-Sleep -Seconds 60
}
