# ==========================================
# Script : test_corrections.ps1
# Vérification des corrections
# ==========================================

Clear-Host

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "        Vérification des corrections" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

#
# Test 1 : Spring Boot
#

Write-Host ""
Write-Host "1. Spring Boot (/actuator/health)" -ForegroundColor Yellow

try {

    $response = Invoke-RestMethod `
        -Uri "http://localhost:8080/actuator/health" `
        -Method GET `
        -ErrorAction Stop

    Write-Host "   OK - Status : $($response.status)" -ForegroundColor Green

}
catch {

    if ($_.Exception.Response) {
        Write-Host "   Erreur HTTP : $([int]$_.Exception.Response.StatusCode)" -ForegroundColor Red
    }
    else {
        Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    }

}

#
# Test 2 : FastAPI
#

Write-Host ""
Write-Host "2. FastAPI (/health)" -ForegroundColor Yellow

try {

    $response = Invoke-RestMethod `
        -Uri "http://localhost:8000/health" `
        -Method GET `
        -ErrorAction Stop

    if ($response.status) {
        Write-Host "   OK - Status : $($response.status)" -ForegroundColor Green
    }
    else {
        Write-Host "   OK" -ForegroundColor Green
    }

}
catch {

    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red

}

#
# Test 3 : OpenAPI OCR
#

Write-Host ""
Write-Host "3. Recherche des endpoints OCR..." -ForegroundColor Yellow

try {

    $openapi = Invoke-RestMethod `
        -Uri "http://localhost:8000/openapi.json" `
        -Method GET `
        -ErrorAction Stop

    $ocrPaths = $openapi.paths.PSObject.Properties |
        Where-Object { $_.Name -match "ocr" }

    if ($ocrPaths.Count -gt 0) {

        Write-Host ""
        Write-Host "Endpoints OCR trouvés :" -ForegroundColor Green

        foreach ($path in $ocrPaths) {
            Write-Host "   $($path.Name)" -ForegroundColor Green
        }

    }
    else {

        Write-Host "   Aucun endpoint OCR trouvé." -ForegroundColor Red

    }

}
catch {

    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red

}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Vérification terminée." -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan