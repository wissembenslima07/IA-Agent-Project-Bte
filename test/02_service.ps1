# ==========================================
# Script : 02_verify_ocr_endpoints.ps1
# Vérifie que les endpoints OCR FastAPI
# sont disponibles.
# ==========================================

Clear-Host

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "        TEST 0.2 : Vérification des endpoints OCR" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

#
# Vérification OpenAPI
#

Write-Host ""
Write-Host "1. Vérification de la documentation OpenAPI..." -ForegroundColor Yellow

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

        Write-Host ""
        Write-Host "Aucun endpoint OCR trouvé." -ForegroundColor Red
        exit 1

    }

}
catch {

    Write-Host ""
    Write-Host "Impossible d'accéder à OpenAPI." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor DarkGray
    exit 1

}

#
# Vérification POST /api/ocr/extract
#

Write-Host ""
Write-Host "2. Vérification du endpoint POST /api/ocr/extract..." -ForegroundColor Yellow

try {

    Invoke-RestMethod `
        -Uri "http://localhost:8000/api/ocr/extract" `
        -Method POST `
        -TimeoutSec 5 `
        -ErrorAction Stop

    Write-Host ""
    Write-Host "Endpoint accessible." -ForegroundColor Green

}
catch {

    if ($_.Exception.Response -ne $null) {

        $statusCode = [int]$_.Exception.Response.StatusCode

        if ($statusCode -eq 400 -or $statusCode -eq 422) {

            Write-Host ""
            Write-Host "Endpoint accessible (requête vide rejetée normalement)." -ForegroundColor Green

        }
        else {

            Write-Host ""
            Write-Host "Code HTTP inattendu : $statusCode" -ForegroundColor Red
            exit 1

        }

    }
    else {

        Write-Host ""
        Write-Host "Impossible de contacter le serveur FastAPI." -ForegroundColor Red
        Write-Host $_.Exception.Message -ForegroundColor DarkGray
        exit 1

    }

}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Tous les endpoints OCR sont disponibles." -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan