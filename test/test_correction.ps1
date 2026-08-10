# ==========================================
# Script : check_ocr_files.ps1
# Vérifie la présence des fichiers OCR
# ==========================================

Clear-Host

Write-Host @"

╔══════════════════════════════════════════════════════════════╗
║            VÉRIFICATION DES FICHIERS OCR                   ║
╚══════════════════════════════════════════════════════════════╝

"@ -ForegroundColor Cyan

# ------------------------------------------------------------
# Liste des fichiers attendus
# ------------------------------------------------------------

$requiredFiles = @(
    "ai-service\app\routers\ocr.py",
    "ai-service\app\services\ocr_service.py",
    "ai-service\app\schemas\ocr.py",
    "ai-service\app\utils\ocr_utils.py"
)

$allExist = $true

# ------------------------------------------------------------
# Vérification des fichiers
# ------------------------------------------------------------

Write-Host "`n1. Vérification des fichiers OCR..." -ForegroundColor Yellow

foreach ($file in $requiredFiles) {

    if (Test-Path $file) {
        Write-Host "PASS  $file" -ForegroundColor Green
    }
    else {
        Write-Host "FAIL  $file (manquant)" -ForegroundColor Red
        $allExist = $false
    }

}

# ------------------------------------------------------------
# Vérification du main.py
# ------------------------------------------------------------

Write-Host "`n2. Vérification de app/main.py..." -ForegroundColor Yellow

$mainPath = "ai-service\app\main.py"

if (Test-Path $mainPath) {

    $mainContent = Get-Content $mainPath -Raw

    # Vérifie l'import du router OCR

    if (
        $mainContent -match "from\s+app\.routers\s+import.*ocr" `
        -or
        $mainContent -match "import\s+.*ocr"
    ) {
        Write-Host "PASS  Import OCR trouvé." -ForegroundColor Green
    }
    else {
        Write-Host "FAIL  Import OCR absent." -ForegroundColor Red
        $allExist = $false
    }

    # Vérifie include_router

    if ($mainContent -match "include_router\s*\(.*ocr") {
        Write-Host "PASS  include_router(ocr) trouvé." -ForegroundColor Green
    }
    else {
        Write-Host "FAIL  include_router(ocr) absent." -ForegroundColor Red
        $allExist = $false
    }

}
else {

    Write-Host "FAIL  app/main.py introuvable." -ForegroundColor Red
    $allExist = $false

}

# ------------------------------------------------------------
# Résumé
# ------------------------------------------------------------

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan

if ($allExist) {

    Write-Host "Tous les fichiers OCR sont présents." -ForegroundColor Green
    Write-Host "Le router OCR est correctement déclaré." -ForegroundColor Green

    Write-Host ""
    Write-Host "Commande recommandée :" -ForegroundColor Yellow
    Write-Host "docker compose build --no-cache ai-service"
    Write-Host "docker compose up -d ai-service"

}
else {

    Write-Host "Des fichiers ou des imports sont manquants." -ForegroundColor Red

    Write-Host ""
    Write-Host "Vérifie :" -ForegroundColor Yellow
    Write-Host "- routers/ocr.py"
    Write-Host "- services/ocr_service.py"
    Write-Host "- schemas/ocr.py"
    Write-Host "- utils/ocr_utils.py"
    Write-Host "- app/main.py"

}

Write-Host "==============================================================" -ForegroundColor Cyan